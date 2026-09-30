package com.pulse.bluetoothdisable.cover.notes

import android.content.Context
import android.util.AtomicFile
import java.io.File
import java.io.FileNotFoundException
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

/** Persistent, app-private notes. There is deliberately no export/share API. */
class LocalNotesRepository(context: Context) {
    private val file = AtomicFile(File(context.applicationContext.filesDir, FILE_NAME))

    fun notes(): List<LocalNote> = synchronized(lock) { readNotes() }

    fun save(
        title: String,
        body: String,
        id: String? = null,
        pinned: Boolean? = null,
    ): LocalNote = synchronized(lock) {
        require(NotesPolicy.isValid(title, body))
        val notes = readNotes().toMutableList()
        val previous = id?.let { noteId -> notes.firstOrNull { it.id == noteId } }
        require(id == null || previous != null)
        val now = System.currentTimeMillis()
        val note = LocalNote(
            id = previous?.id ?: UUID.randomUUID().toString(),
            title = NotesPolicy.normalizeTitle(title),
            body = NotesPolicy.normalizeBody(body),
            createdAt = previous?.createdAt ?: now,
            updatedAt = now,
            pinned = pinned ?: previous?.pinned ?: false,
        )
        notes.removeAll { it.id == note.id }
        notes.add(note)
        writeNotes(notes)
        note
    }

    fun delete(id: String) = synchronized(lock) {
        writeNotes(readNotes().filterNot { it.id == id })
    }

    fun setPinned(id: String, pinned: Boolean): LocalNote = synchronized(lock) {
        val previous = readNotes().firstOrNull { it.id == id } ?: error("Unknown note")
        save(previous.title, previous.body, previous.id, pinned)
    }

    /** Test/reset helper only. Normal Cover Mode transitions must never call this. */
    internal fun clear() = synchronized(lock) {
        file.delete()
        NotesCipher.clearKey()
    }

    private fun readNotes(): List<LocalNote> {
        val content = try {
            file.openRead().bufferedReader(Charsets.UTF_8).use { it.readText() }
        } catch (_: FileNotFoundException) {
            return emptyList()
        }
        val array = JSONArray(NotesCipher.decrypt(content))
        return (0 until array.length()).map { index ->
            val item = array.getJSONObject(index)
            LocalNote(
                id = item.getString("id"),
                title = item.optString("title", ""),
                body = item.getString("body"),
                createdAt = item.getLong("createdAt"),
                updatedAt = item.getLong("updatedAt"),
                pinned = item.optBoolean("pinned", false),
            )
        }.sortedWith(
            compareByDescending<LocalNote> { it.pinned }
                .thenByDescending { it.updatedAt },
        )
    }

    private fun writeNotes(notes: List<LocalNote>) {
        val json = JSONArray().apply {
            notes.forEach { note ->
                put(JSONObject().apply {
                    put("id", note.id)
                    put("title", note.title)
                    put("body", note.body)
                    put("createdAt", note.createdAt)
                    put("updatedAt", note.updatedAt)
                    put("pinned", note.pinned)
                })
            }
        }.toString()
        val encrypted = NotesCipher.encrypt(json)
        val stream = file.startWrite()
        try {
            stream.write(encrypted)
            file.finishWrite(stream)
        } catch (error: Exception) {
            file.failWrite(stream)
            throw error
        }
    }

    companion object {
        const val FILE_NAME = "local_notes_v1.json"
        private val lock = Any()
    }
}
