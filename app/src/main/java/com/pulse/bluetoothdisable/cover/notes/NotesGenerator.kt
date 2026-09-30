package com.pulse.bluetoothdisable.cover.notes

import kotlin.random.Random

data class GeneratedNote(val title: String, val body: String)

/** Procedural local generator: templates + dictionaries only, no AI model, API or network. */
internal object NotesGenerator {
    fun generate(count: Int, language: String, random: Random = Random.Default): List<GeneratedNote> {
        require(count in 1..20)
        val russian = language.lowercase().startsWith("ru")
        return List(count) { index ->
            if (russian) russianNote(index, random) else englishNote(index, random)
        }
    }

    private fun russianNote(index: Int, random: Random): GeneratedNote {
        val shopping = listOf("молоко", "хлеб", "чай", "яблоки", "рис", "батарейки", "вода", "кофе")
        val tasks = listOf(
            "проверить расписание", "забрать заказ", "ответить на сообщение", "разобрать документы",
            "зарядить наушники", "позвонить после 18:00", "посмотреть дедлайн", "купить продукты",
        )
        val study = listOf(
            "повторить конспект", "решить несколько задач", "проверить домашнее задание",
            "подготовить вопросы", "посмотреть расписание пар", "доделать практическую работу",
        )
        return when ((index + random.nextInt(6)) % 6) {
            0 -> GeneratedNote("Покупки", pickLines(shopping, random, 3, 6))
            1 -> GeneratedNote("На сегодня", pickLines(tasks, random, 3, 5))
            2 -> GeneratedNote("Учёба", pickLines(study, random, 3, 5))
            3 -> GeneratedNote("Не забыть", pickSentences(tasks, random, 2, 4))
            4 -> GeneratedNote("На неделю", pickLines((tasks + study), random, 4, 7))
            else -> GeneratedNote("Идеи", pickSentences(listOf(
                "разобрать фотографии", "обновить список дел", "пройтись вечером",
                "проверить старые заметки", "составить список покупок", "навести порядок на столе",
            ), random, 2, 4))
        }
    }

    private fun englishNote(index: Int, random: Random): GeneratedNote {
        val shopping = listOf("milk", "bread", "tea", "apples", "rice", "batteries", "water", "coffee")
        val tasks = listOf(
            "check the schedule", "pick up the order", "reply to the message", "sort the documents",
            "charge the headphones", "call after 6 PM", "check the deadline", "buy groceries",
        )
        val study = listOf(
            "review notes", "solve a few exercises", "check homework", "prepare questions",
            "check the class schedule", "finish the practical assignment",
        )
        return when ((index + random.nextInt(6)) % 6) {
            0 -> GeneratedNote("Shopping", pickLines(shopping, random, 3, 6))
            1 -> GeneratedNote("Today", pickLines(tasks, random, 3, 5))
            2 -> GeneratedNote("Study", pickLines(study, random, 3, 5))
            3 -> GeneratedNote("Don't forget", pickSentences(tasks, random, 2, 4))
            4 -> GeneratedNote("This week", pickLines((tasks + study), random, 4, 7))
            else -> GeneratedNote("Ideas", pickSentences(listOf(
                "sort the photos", "update the task list", "take an evening walk",
                "review old notes", "make a shopping list", "tidy the desk",
            ), random, 2, 4))
        }
    }

    private fun pickLines(items: List<String>, random: Random, min: Int, max: Int): String =
        items.shuffled(random).take(random.nextInt(min, max + 1).coerceAtMost(items.size))
            .joinToString("\n") { "• $it" }

    private fun pickSentences(items: List<String>, random: Random, min: Int, max: Int): String =
        items.shuffled(random).take(random.nextInt(min, max + 1).coerceAtMost(items.size))
            .joinToString("\n") { value -> value.replaceFirstChar { it.uppercase() } + "." }
}
