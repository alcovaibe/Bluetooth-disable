package com.pulse.bluetoothdisable.cover.notes

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LocalNotesRepositoryInstrumentedTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val repository = LocalNotesRepository(context)

    @Before fun before() = repository.clear()
    @After fun after() = repository.clear()

    @Test
    fun favoritesDoNotOverrideLastEditedOrdering() {
        val first = repository.save("First", "one")
        Thread.sleep(4)
        val second = repository.save("Second", "two")
        repository.setFavorite(first.id, true)

        val notes = repository.notes()
        assertEquals(listOf(second.id, first.id), notes.map { it.id })
        assertTrue(notes.single { it.id == first.id }.favorite)
    }

    @Test
    fun v1PinnedSchemaMigratesWithoutLosingText() {
        val old = JSONArray().put(
            JSONObject().apply {
                put("id", "legacy")
                put("title", "Legacy")
                put("body", "  keep whitespace  \n")
                put("createdAt", 10L)
                put("updatedAt", 20L)
                put("pinned", true)
            },
        ).toString()
        File(context.filesDir, LocalNotesRepository.FILE_NAME).writeBytes(NotesCipher.encrypt(old))

        val migrated = repository.notes().single()
        assertEquals("legacy", migrated.id)
        assertEquals("  keep whitespace  \n", migrated.body)
        assertTrue(migrated.favorite)
        assertEquals(NoteType.TEXT, migrated.type)
    }

    @Test
    fun checklistPersistsAndDeleteKeepsOtherNotes() {
        val now = System.currentTimeMillis()
        val checklist = LocalNote(
            id = "checklist",
            title = "Trip",
            body = "",
            createdAt = now,
            updatedAt = now,
            favorite = false,
            type = NoteType.CHECKLIST,
            checklist = listOf(
                ChecklistItem("a", "Passport", true),
                ChecklistItem("b", "Charger", false),
            ),
        )
        repository.upsert(checklist)
        val text = repository.save("Text", "body")

        val restored = repository.notes().single { it.id == "checklist" }
        assertEquals(2, restored.checklist.size)
        assertTrue(restored.checklist.first().checked)

        repository.delete(restored.id)
        assertEquals(text.id, repository.notes().single().id)
    }

    @Test
    fun emptyNewNoteIsNotPersistableButTitleOnlyIs() {
        val now = System.currentTimeMillis()
        val empty = LocalNote("empty", "", "", now, now, false)
        val titled = empty.copy(id = "title", title = "Only title")
        assertFalse(NotesPolicy.isPersistable(empty))
        assertTrue(NotesPolicy.isPersistable(titled))
    }
}
