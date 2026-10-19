package ru.mobui.kanji.domain.practice

/** Human-maintained triple: stroke A — relationship — stroke B. Numbers are one-based. */
enum class StrokeRuleRelation { Crosses, EndsAt, ShorterThan }

data class KanjiRule(
    val firstStroke: Int,
    val relation: StrokeRuleRelation,
    val secondStroke: Int,
    val tolerancePercent: Float? = null,
) {
    init {
        require(firstStroke > 0 && secondStroke > 0 && firstStroke != secondStroke)
        require(tolerancePercent == null || (tolerancePercent.isFinite() && tolerancePercent in 0f..100f))
    }
}

data class KanjiRuleSet(val referenceStrokeCount: Int, val rules: List<KanjiRule>) {
    init {
        require(referenceStrokeCount > 0)
        require(rules.all { it.firstStroke <= referenceStrokeCount && it.secondStroke <= referenceStrokeCount })
        require(rules.distinct().size == rules.size)
    }
}

data class KanjiRuleViolation(val rule: KanjiRule, val actualValue: Float? = null, val actualRelation: StrokeRelation? = null)

/** A compiled dictionary today; a database-backed implementation can provide the same snapshots. */
fun interface KanjiRulesSource { fun rulesFor(id: KanjiId): KanjiRuleSet? }
