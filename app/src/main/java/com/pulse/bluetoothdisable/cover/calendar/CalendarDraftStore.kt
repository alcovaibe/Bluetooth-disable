package com.pulse.bluetoothdisable.cover.calendar

import android.content.Context
import android.util.AtomicFile
import java.io.File
import java.io.FileNotFoundException
import java.time.LocalDate
import org.json.JSONArray
import org.json.JSONObject

/** Encrypted persistent drafts. Drafts are local user data and are never backed up or transferred. */
class CalendarDraftStore(context: Context) {
    private val file = AtomicFile(File(context.applicationContext.filesDir, FILE_NAME))

    fun text(date: LocalDate, noteId: String? = null): String? = synchronized(lock) {
        readDrafts()[key(date, noteId)]?.text
    }

    fun put(date: LocalDate, noteId: String? = null, text: String) = synchronized(lock) {
        require(text.length <= CalendarNotePolicy.MAX_TEXT_LENGTH)
        val drafts = readDrafts().toMutableMap()
        val key = key(date, noteId)
        if (text.isEmpty()) {
            drafts.remove(key)
        } else {
            drafts[key] = CalendarDraft(date, noteId, text)
        }
        writeDrafts(drafts)
    }

    fun remove(date: LocalDate, noteId: String? = null) = synchronized(lock) {
        val drafts = readDrafts().toMutableMap()
        drafts.remove(key(date, noteId))
        writeDrafts(drafts)
    }

    fun clearAll() = synchronized(lock) {
        file.delete()
    }

    private fun readDrafts(): Map<String, CalendarDraft> {
        val content = try {
            file.openRead().bufferedReader(Charsets.UTF_8).use { it.readText() }
        } catch (_: FileNotFoundException) {
            return emptyMap()
        }
        val array = JSONArray(CalendarNotesCipher.decrypt(content))
        return buildMap {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                val date = LocalDate.parse(item.getString("date"))
                val noteId = item.optString("noteId").takeIf { it.isNotEmpty() }
                val draft = CalendarDraft(date, noteId, item.getString("text"))
                put(key(date, noteId), draft)
            }
        }
    }

    private fun writeDrafts(drafts: Map<String, CalendarDraft>) {
        if (drafts.isEmpty()) {
            file.delete()
            return
        }
        val json = JSONArray().apply {
            drafts.values.forEach { draft ->
                put(JSONObject().apply {
                    put("date", draft.date.toString())
                    put("noteId", draft.noteId ?: "")
                    put("text", draft.text)
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

    private fun key(date: LocalDate, noteId: String?): String =
        noteId?.let { "edit:$it" } ?: "new:$date"

    companion object {
        const val FILE_NAME = "calendar_drafts.json"
        private val lock = Any()
    }
}

data class CalendarDraft(
    val date: LocalDate,
    val noteId: String?,
    val text: String,
)
