package ru.mobui.kanji.data.kanjivg

import ru.mobui.kanji.domain.handwriting.HookRequirement

/** Explicit CJK stroke mapping; undocumented suffixes do not change the base stroke shape. */
internal object KanjiVgStrokeFeatures {
    private val hooked = "㇁㇂㇃㇆㇈㇉㇖㇚㇟㇡".toSet()
    private val plain = "㇀㇄㇅㇇㇋㇏㇐㇑㇒㇓㇔㇕㇗㇙㇛㇜㇞".toSet()

    fun hookRequirement(type: String?): HookRequirement {
        if (type.isNullOrBlank()) return HookRequirement.Unknown
        val variants = type.split('/').map { variant ->
            val value = variant.trim()
            if (value.isEmpty() || value.drop(1).any { it !in "abcv" }) return HookRequirement.Unknown
            when (value.first()) {
                in hooked -> true
                in plain -> false
                else -> return HookRequirement.Unknown
            }
        }
        return when {
            variants.all { it } -> HookRequirement.Required
            variants.none { it } -> HookRequirement.NotRequired
            else -> HookRequirement.Optional
        }
    }
}
