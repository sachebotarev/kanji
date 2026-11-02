package ru.mobui.kanji.data.kanjivg

import ru.mobui.kanji.domain.handwriting.HookRequirement

/** Explicit CJK stroke mapping; undocumented suffixes do not change the base stroke shape. */
internal object KanjiVgStrokeFeatures {
    private val hooked = "㇁㇂㇃㇆㇈㇉㇖㇚㇟㇡".toSet()
    private val plain = "㇀㇄㇅㇇㇋㇏㇐㇑㇒㇓㇔㇕㇗㇙㇛㇜㇞".toSet()

    fun hookRequirement(type: String?): HookRequirement {
        val variants = type?.takeUnless { it.isBlank() }?.split('/')?.map(::variant)
        return when {
            variants == null || variants.any { it == null } -> HookRequirement.Unknown
            variants.all { it == true } -> HookRequirement.Required
            variants.all { it == false } -> HookRequirement.NotRequired
            else -> HookRequirement.Optional
        }
    }

    private fun variant(variant: String): Boolean? {
        val value = variant.trim()
        return when {
            value.isEmpty() || value.drop(1).any { it !in "abcv" } -> null
            value.first() in hooked -> true
            value.first() in plain -> false
            else -> null
        }
    }
}
