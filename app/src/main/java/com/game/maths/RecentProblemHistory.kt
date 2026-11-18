package com.game.maths

/** Bounded local practice history; only canonical arithmetic signatures are stored. */
object RecentProblemHistory {
    const val MAX_ITEMS = 40

    fun read(text: String?): List<String> = text.orEmpty().split('\n')
        .filter { it.isNotBlank() }.takeLast(MAX_ITEMS)

    fun add(text: String?, learningKey: String): String =
        (read(text).filterNot { it == learningKey } + learningKey)
            .takeLast(MAX_ITEMS).joinToString("\n")
}
