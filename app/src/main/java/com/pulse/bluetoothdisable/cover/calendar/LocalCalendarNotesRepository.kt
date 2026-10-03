package com.pulse.bluetoothdisable.cover.calendar

import android.content.Context
import android.util.AtomicFile
import java.io.File
import java.io.FileNotFoundException
import java.time.LocalDate
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

/** Own local calendar; no CalendarContract, account, permission or network access. */
class LocalCalendarNotesRepository(context: Context) : CalendarNotesRepository {
    private val appContext = context.applicationContext
    private val file = AtomicFile(File(appContext.filesDir, FILE_NAME))

    override fun notes(): List<CalendarNote> = synchronized(lock) { readNotes() }

    override fun save(date: LocalDate, text: String, id: String?): CalendarNote = synchronized(lock) {
        val notes = readNotes().toMutableList()
        val previous = id?.let { noteId -> notes.firstOrNull { it.id == noteId } }
        require(id == null || previous != null)
        require(previous == null || previous.date == date) { "Calendar note date cannot be changed" }

        val normalized = CalendarNotePolicy.normalize(text)
        CalendarNotePolicy.validate(date, normalized, notes, id)?.let { violation ->
            throw CalendarNoteValidationException(violation)
        }

        val clock = System.currentTimeMillis()
        val latestStoredTimestamp = notes.maxOfOrNull { note ->
            maxOf(note.createdAt, note.updatedAt)
        }
        val timestamp = if (latestStoredTimestamp != null && clock <= latestStoredTimestamp) {
            latestStoredTimestamp + 1
        } else {
            clock
        }
        val note = CalendarNote(
            id = previous?.id ?: UUID.randomUUID().toString(),
            date = previous?.date ?: date,
            text = normalized,
            createdAt = previous?.createdAt ?: timestamp,
            updatedAt = timestamp,
        )
        notes.removeAll { it.id == note.id }
        notes.add(note)
        writeNotes(CalendarNotePolicy.sorted(notes))
        note
    }

    override fun delete(id: String) = synchronized(lock) {
        writeNotes(readNotes().filterNot { it.id == id })
    }

    override fun clear() = synchronized(lock) {
        CalendarDraftStore(appContext).clearAll()
        file.delete()
        CalendarNotesCipher.clearKey()
    }

    private fun readNotes(): List<CalendarNote> {
        val content = try {
            file.openRead().bufferedReader(Charsets.UTF_8).use { it.readText() }
        } catch (_: FileNotFoundException) {
            return emptyList()
        }
        val array = JSONArray(CalendarNotesCipher.decrypt(content))
        return CalendarNotePolicy.sorted(
            (0 until array.length()).map { index ->
                val item = array.getJSONObject(index)
                CalendarNote(
                    item.getString("id"),
                    LocalDate.parse(item.getString("date")),
                    item.getString("text"),
                    item.getLong("createdAt"),
                    item.getLong("updatedAt"),
                )
            },
        )
    }

    private fun writeNotes(notes: List<CalendarNote>) {
        val json = JSONArray().apply {
            notes.forEach { note ->
                put(JSONObject().apply {
                    put("id", note.id)
                    put("date", note.date.toString())
                    put("text", note.text)
                    put("createdAt", note.createdAt)
                    put("updatedAt", note.updatedAt)
                })
            }
        }.toString()
        val data = CalendarNotesCipher.encrypt(json)
        val stream = file.startWrite()
        try {
            stream.write(data)
            file.finishWrite(stream)
        } catch (error: Exception) {
            file.failWrite(stream)
            throw error
        }
    }

    companion object {
        const val FILE_NAME = "calendar_notes.json"
        private val lock = Any()
    }
}
