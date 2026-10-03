#!/usr/bin/env python3
"""Audit resolved release-runtime dependency licenses using Maven POM metadata."""

from __future__ import annotations

import argparse
import concurrent.futures
import re
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
import xml.etree.ElementTree as ET
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable

REPOSITORIES = (
    "https://dl.google.com/dl/android/maven2",
    "https://repo.maven.apache.org/maven2",
)

# Conservative permissive allowlist for dependencies redistributed by a
# GPL-3.0-or-later application. Anything else requires explicit review.
APPROVED_LICENSES = {
    "Apache-2.0",
    "BSD-2-Clause",
    "BSD-3-Clause",
    "CC0-1.0",
    "ISC",
    "MIT",
    "Unlicense",
    "Zlib",
}

USER_AGENT = "Bluetooth-Disable-license-audit/1.0"
COORDINATE_RE = re.compile(
    r"^([A-Za-z0-9_.-]+):([A-Za-z0-9_.-]+)(?::([^\s]+))?(?:\s+->\s+([^\s]+))?"
)
TREE_PREFIX_RE = re.compile(r"^[|+\\\-\s]+")


@dataclass(frozen=True, order=True)
class Coordinate:
    group: str
    artifact: str
    version: str

    def __str__(self) -> str:
        return f"{self.group}:{self.artifact}:{self.version}"


@dataclass(frozen=True)
class LicenseInfo:
    name: str
    url: str
    spdx: str | None


@dataclass(frozen=True)
class AuditResult:
    coordinate: Coordinate
    licenses: tuple[LicenseInfo, ...]
    pom_url: str | None
    error: str | None = None


def parse_coordinates(text: str) -> list[Coordinate]:
    coordinates: set[Coordinate] = set()
    in_runtime_section = False

    for raw_line in text.splitlines():
        if raw_line.startswith("releaseRuntimeClasspath - "):
            in_runtime_section = True
            continue
        if not in_runtime_section:
            continue

        stripped = raw_line.strip()
        if not stripped or "(c)" in stripped:
            continue

        line = TREE_PREFIX_RE.sub("", stripped)
        match = COORDINATE_RE.match(line)
        if not match:
            continue

        group, artifact, source_version, resolved = match.groups()
        version = source_version
        if resolved:
            resolved = resolved.removesuffix("(*)")
            if resolved.count(":") >= 2:
                group, artifact, version, *_ = resolved.split(":")
            else:
                version = resolved

        if version:
            coordinates.add(Coordinate(group, artifact, version.removesuffix("(*)")))

    if not coordinates:
        raise ValueError("No concrete releaseRuntimeClasspath dependencies were found")
    return sorted(coordinates)


def child_text(element: ET.Element, local_name: str) -> str:
    child = element.find(f"{{*}}{local_name}")
    return (child.text or "").strip() if child is not None else ""


def normalize_license(name: str, url: str) -> str | None:
    compact = re.sub(r"[^a-z0-9]+", " ", f"{name} {url}".lower())

    if "apache" in compact and (" 2 0 " in f" {compact} " or "license 2" in compact):
        return "Apache-2.0"
    if "opensource org licenses mit" in compact or re.search(r"\bmit\b", compact):
        return "MIT"
    if "bsd" in compact:
        if any(token in compact for token in ("3 clause", "new bsd", "revised bsd")):
            return "BSD-3-Clause"
        if any(token in compact for token in ("2 clause", "simplified bsd", "freebsd")):
            return "BSD-2-Clause"
    if re.search(r"\bisc\b", compact):
        return "ISC"
    if "unlicense" in compact:
        return "Unlicense"
    if re.search(r"\bzlib\b", compact):
        return "Zlib"
    if "cc0" in compact or "creativecommons org publicdomain zero 1 0" in compact:
        return "CC0-1.0"
    return None


def pom_path(coordinate: Coordinate) -> str:
    group_path = coordinate.group.replace(".", "/")
    artifact = urllib.parse.quote(coordinate.artifact, safe="")
    version = urllib.parse.quote(coordinate.version, safe="")
    filename = urllib.parse.quote(f"{coordinate.artifact}-{coordinate.version}.pom", safe="")
    return f"{group_path}/{artifact}/{version}/{filename}"


def fetch_bytes(url: str, attempts: int = 3) -> bytes:
    request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    last_error: Exception | None = None
    for attempt in range(attempts):
        try:
            with urllib.request.urlopen(request, timeout=20) as response:
                return response.read()
        except urllib.error.HTTPError as exc:
            if exc.code == 404:
                raise
            last_error = exc
        except (urllib.error.URLError, TimeoutError) as exc:
            last_error = exc
        if attempt + 1 < attempts:
            time.sleep(attempt + 1)
    assert last_error is not None
    raise last_error


def fetch_pom(coordinate: Coordinate) -> tuple[bytes, str]:
    for base in REPOSITORIES:
        url = f"{base}/{pom_path(coordinate)}"
        try:
            return fetch_bytes(url), url
        except urllib.error.HTTPError as exc:
            if exc.code == 404:
                continue
            raise
    raise FileNotFoundError(f"POM not found in configured repositories: {coordinate}")


def parse_pom(data: bytes) -> tuple[list[tuple[str, str]], Coordinate | None]:
    root = ET.fromstring(data)
    licenses: list[tuple[str, str]] = []
    licenses_element = root.find("{*}licenses")
    if licenses_element is not None:
        for element in licenses_element.findall("{*}license"):
            name = child_text(element, "name")
            url = child_text(element, "url")
            if name or url:
                licenses.append((name, url))

    parent_coordinate: Coordinate | None = None
    parent = root.find("{*}parent")
    if parent is not None:
        group = child_text(parent, "groupId")
        artifact = child_text(parent, "artifactId")
        version = child_text(parent, "version")
        if group and artifact and version and not any("${" in value for value in (group, artifact, version)):
            parent_coordinate = Coordinate(group, artifact, version)
    return licenses, parent_coordinate


def resolve_licenses(
    coordinate: Coordinate,
    stack: frozenset[Coordinate] = frozenset(),
) -> tuple[tuple[LicenseInfo, ...], str | None]:
    if coordinate in stack:
        raise RuntimeError(f"Parent POM cycle detected at {coordinate}")

    data, pom_url = fetch_pom(coordinate)
    raw_licenses, parent = parse_pom(data)
    if raw_licenses:
        return (
            tuple(
                LicenseInfo(name=name or "(unnamed)", url=url, spdx=normalize_license(name, url))
                for name, url in raw_licenses
            ),
            pom_url,
        )
    if parent is not None:
        return resolve_licenses(parent, stack | {coordinate})
    return tuple(), pom_url


def audit_one(coordinate: Coordinate) -> AuditResult:
    try:
        licenses, pom_url = resolve_licenses(coordinate)
        return AuditResult(coordinate, licenses, pom_url)
    except Exception as exc:
        return AuditResult(coordinate, tuple(), None, f"{type(exc).__name__}: {exc}")


def markdown_escape(value: str) -> str:
    return value.replace("|", "\\|").replace("\n", " ")


def write_report(results: Iterable[AuditResult], output: Path) -> tuple[int, int]:
    result_list = list(results)
    rows: list[str] = []
    problems: list[str] = []

    for result in result_list:
        coordinate = str(result.coordinate)
        if result.error:
            summary = f"ERROR: {result.error}"
            problems.append(f"{coordinate}: {result.error}")
        elif not result.licenses:
            summary = "MISSING LICENSE METADATA"
            problems.append(f"{coordinate}: no license metadata in POM or parent POM")
        else:
            formatted: list[str] = []
            for license_info in result.licenses:
                normalized = license_info.spdx or "UNRECOGNIZED"
                formatted.append(f"{normalized} ({license_info.name})")
                if license_info.spdx is None:
                    problems.append(
                        f"{coordinate}: unrecognized license '{license_info.name}' {license_info.url}".strip()
                    )
                elif license_info.spdx not in APPROVED_LICENSES:
                    problems.append(
                        f"{coordinate}: license {license_info.spdx} is not in the approved allowlist"
                    )
            summary = "; ".join(formatted)

        rows.append(
            f"| `{markdown_escape(coordinate)}` | {markdown_escape(summary)} | "
            f"{markdown_escape(result.pom_url or '')} |"
        )

    lines = [
        "# Runtime Dependency License Audit",
        "",
        "Scope: concrete modules resolved in `releaseRuntimeClasspath`.",
        "",
        "Policy: fail closed when Maven POM license metadata is missing, cannot be normalized, "
        "or is outside the conservative permissive allowlist.",
        "",
        "Approved licenses: " + ", ".join(f"`{item}`" for item in sorted(APPROVED_LICENSES)) + ".",
        "",
        "| Dependency | License metadata | License-source POM |",
        "| --- | --- | --- |",
        *rows,
        "",
    ]
    if problems:
        lines.extend(["## Problems", "", *[f"- {problem}" for problem in problems], ""])
    else:
        lines.extend(["## Result", "", f"PASS — {len(result_list)} runtime modules audited.", ""])

    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text("\n".join(lines), encoding="utf-8")
    return len(result_list), len(problems)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("dependency_tree", type=Path)
    parser.add_argument("report", type=Path)
    args = parser.parse_args()

    coordinates = parse_coordinates(args.dependency_tree.read_text(encoding="utf-8", errors="replace"))
    with concurrent.futures.ThreadPoolExecutor(max_workers=min(12, len(coordinates))) as executor:
        results = sorted(executor.map(audit_one, coordinates), key=lambda item: item.coordinate)

    audited, problem_count = write_report(results, args.report)
    print(f"Audited {audited} resolved runtime modules")
    print(f"Problems: {problem_count}")
    print(f"Report: {args.report}")
    return 1 if problem_count else 0


if __name__ == "__main__":
    sys.exit(main())
