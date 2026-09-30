package com.pulse.bluetoothdisable.cover.notes

data class LocalNote(
    val id: String,
    val title: String,
    val body: String,
    val createdAt: Long,
    val updatedAt: Long,
    val pinned: Boolean,
)
