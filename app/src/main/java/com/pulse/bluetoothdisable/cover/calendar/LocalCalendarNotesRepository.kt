package com.pulse.bluetoothdisable.cover.calendar

import android.content.Context
import android.util.AtomicFile
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileNotFoundException
import java.time.LocalDate
import java.util.UUID

/** Own local calendar; no CalendarContract, account, permission or network access. */
class LocalCalendarNotesRepository(context: Context) : CalendarNotesRepository {
    private val file = AtomicFile(File(context.applicationContext.filesDir, FILE_NAME))

    override fun notes(): List<CalendarNote> = synchronized(lock) { readNotes() }

    override fun save(date: LocalDate, text: String, id: String?): CalendarNote = synchronized(lock) {
        require(text.isNotBlank())
        val notes = readNotes().toMutableList()
        val previous = id?.let { noteId -> notes.firstOrNull { it.id == noteId } }
        require(id == null || previous != null)
        val now = System.currentTimeMillis()
        val note = CalendarNote(
            previous?.id ?: UUID.randomUUID().toString(), date, text.trim(),
            previous?.createdAt ?: now, now,
        )
        notes.removeAll { it.id == note.id }
        notes.add(note)
        writeNotes(notes)
        note
    }

    override fun delete(id: String) = synchronized(lock) {
        writeNotes(readNotes().filterNot { it.id == id })
    }

    override fun clear() = synchronized(lock) {
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
        return (0 until array.length()).map { index ->
            val item = array.getJSONObject(index)
            CalendarNote(
                item.getString("id"), LocalDate.parse(item.getString("date")),
                item.getString("text"), item.getLong("createdAt"), item.getLong("updatedAt"),
            )
        }.sortedBy { it.createdAt }
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
