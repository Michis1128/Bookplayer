package com.michis.player.core.common

object NaturalOrder : Comparator<String> {
    private val tokens = Regex("\\d+|\\D+")

    override fun compare(left: String, right: String): Int {
        val leftTokens = tokens.findAll(left.lowercase()).map { it.value }.toList()
        val rightTokens = tokens.findAll(right.lowercase()).map { it.value }.toList()
        for (index in 0 until minOf(leftTokens.size, rightTokens.size)) {
            val a = leftTokens[index]
            val b = rightTokens[index]
            val result = if (a.firstOrNull()?.isDigit() == true && b.firstOrNull()?.isDigit() == true) {
                a.trimStart('0').ifEmpty { "0" }.let { normalizedA ->
                    b.trimStart('0').ifEmpty { "0" }.let { normalizedB ->
                        normalizedA.length.compareTo(normalizedB.length).takeIf { it != 0 }
                            ?: normalizedA.compareTo(normalizedB).takeIf { it != 0 }
                            ?: a.length.compareTo(b.length)
                    }
                }
            } else a.compareTo(b)
            if (result != 0) return result
        }
        return leftTokens.size.compareTo(rightTokens.size)
    }
}
