package com.pulse.bluetoothdisable.cover.notes

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotesGeneratorTest {
    @Test fun generatorCreatesRequestedNumberOfValidEditableNotes() {
        for (count in listOf(2, 5, 7, 10)) {
            val generated = NotesGenerator.generate(count, "ru", Random(42 + count))
            assertEquals(count, generated.size)
            assertTrue(generated.all { NotesPolicy.isValid(it.title, it.body) })
        }
    }

    @Test fun secretRangeRejectsBlankAndOversizedSelections() {
        assertTrue(NotesPolicy.isValidSecretRange("hello world", 0, 5))
        assertFalse(NotesPolicy.isValidSecretRange("hello world", 5, 6))
        assertFalse(NotesPolicy.isValidSecretRange("hello", 2, 2))
        assertFalse(NotesPolicy.isValidSecretRange("x".repeat(100), 0, 81))
    }
}
