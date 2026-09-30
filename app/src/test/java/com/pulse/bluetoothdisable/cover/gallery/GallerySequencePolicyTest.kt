package com.pulse.bluetoothdisable.cover.gallery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GallerySequencePolicyTest {
    @Test fun exactlySixtyDistinctThreeTapSequencesExist() {
        val zones = GalleryTapZone.entries
        val sequences = buildList {
            for (a in zones) for (b in zones) for (c in zones) {
                val sequence = listOf(a, b, c)
                if (GalleryAccessPolicy.isValidSequence(sequence)) add(sequence)
            }
        }
        assertEquals(GalleryAccessPolicy.UNIQUE_SEQUENCE_COUNT, sequences.size)
        assertEquals(60, sequences.distinct().size)
    }

    @Test fun repeatedZonesAreRejected() {
        assertFalse(
            GalleryAccessPolicy.isValidSequence(
                listOf(GalleryTapZone.CENTER, GalleryTapZone.CENTER, GalleryTapZone.TOP_LEFT),
            ),
        )
        assertTrue(
            GalleryAccessPolicy.isValidSequence(
                listOf(GalleryTapZone.CENTER, GalleryTapZone.TOP_RIGHT, GalleryTapZone.BOTTOM_LEFT),
            ),
        )
    }

    @Test fun detectorResetsOnTimeoutAndRepeatedZone() {
        val detector = GallerySequenceDetector(timeoutMillis = 100)
        assertNull(detector.tap(GalleryTapZone.TOP_LEFT, 1))
        assertNull(detector.tap(GalleryTapZone.TOP_RIGHT, 200))
        assertNull(detector.tap(GalleryTapZone.TOP_RIGHT, 210))
        assertNull(detector.tap(GalleryTapZone.CENTER, 220))
        assertEquals(
            listOf(GalleryTapZone.TOP_RIGHT, GalleryTapZone.CENTER, GalleryTapZone.BOTTOM_LEFT),
            detector.tap(GalleryTapZone.BOTTOM_LEFT, 230),
        )
    }
}
