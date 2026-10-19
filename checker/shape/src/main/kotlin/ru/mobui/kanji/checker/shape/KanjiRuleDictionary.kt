package ru.mobui.kanji.checker.shape

import ru.mobui.kanji.domain.practice.*
import ru.mobui.kanji.domain.practice.StrokeRuleRelation.*

/** Only confirmed distinguishing details. Missing entry means no additional dictionary rules. */
class KanjiRuleDictionary : KanjiRulesSource {
    private val entries = mapOf(
        // 牛: the vertical stroke crosses the upper horizontal and protrudes above it.
        "u725b" to KanjiRuleSet(4, listOf(KanjiRule(4, Crosses, 2))),
        // 午: the vertical stroke ends at the upper horizontal.
        "u5348" to KanjiRuleSet(4, listOf(KanjiRule(4, EndsAt, 2))),
        // 未 / 末: relative lengths of the two upper horizontal strokes.
        "u672a" to KanjiRuleSet(5, listOf(KanjiRule(1, ShorterThan, 2))),
        "u672b" to KanjiRuleSet(5, listOf(KanjiRule(2, ShorterThan, 1))),
        // 土 / 士: relative lengths of the upper and lower horizontal strokes.
        "u571f" to KanjiRuleSet(3, listOf(KanjiRule(1, ShorterThan, 3))),
        "u58eb" to KanjiRuleSet(3, listOf(KanjiRule(3, ShorterThan, 1))),
    )

    override fun rulesFor(id: KanjiId): KanjiRuleSet? {
        if (!id.value.startsWith("u", ignoreCase = true) || id.value.length <= 1) return null
        val hexPart = id.value.substring(1).lowercase().dropWhile { it == '0' }
        val key = "u" + if (hexPart.isEmpty()) "0" else hexPart
        val stored = entries[key] ?: return null
        return stored.copy(rules = stored.rules.toList())
    }
}
