package ru.mobui.kanji.feature.dictionary

import androidx.lifecycle.SavedStateHandle
import ru.mobui.kanji.domain.catalog.WordDraft
import ru.mobui.kanji.domain.catalog.WordTranslationDraft

internal fun SavedStateHandle.restoreWordDraft(prefix: String): WordDraft? {
    val written = get<String>("$prefix-written")
    val tags = get<ArrayList<String>>("$prefix-tags")
    val texts = get<ArrayList<String>>("$prefix-texts")
    return if (written != null && tags != null && texts != null) {
        WordDraft(
            written,
            get<String>("$prefix-reading").orEmpty(),
            tags.zip(texts).map { WordTranslationDraft(it.first, it.second) },
            tagIds = get<LongArray>("$prefix-tag-ids")?.toSet(),
            pendingTags = get<ArrayList<android.os.Bundle>>("$prefix-pending-tags").orEmpty().map { bundle ->
                ru.mobui.kanji.domain.catalog.TagDraft(
                    ru.mobui.kanji.domain.catalog.TagColor.entries.first { it.key == bundle.getString("color") },
                    bundle.getStringArrayList("languages").orEmpty().zip(bundle.getStringArrayList("names").orEmpty())
                        .map { ru.mobui.kanji.domain.catalog.TagTranslation(it.first, it.second) },
                )
            },
        )
    } else null
}

internal fun SavedStateHandle.persistWordDraft(prefix: String, draft: WordDraft) {
    this["$prefix-written"] = draft.written
    this["$prefix-reading"] = draft.reading
    this["$prefix-tags"] = ArrayList(draft.translations.map { it.languageTag })
    this["$prefix-texts"] = ArrayList(draft.translations.map { it.text })
    this["$prefix-tag-ids"] = draft.tagIds?.toLongArray()
    this["$prefix-pending-tags"] = ArrayList(draft.pendingTags.map { tag ->
        android.os.Bundle().apply {
            putString("color", tag.color.key)
            putStringArrayList("languages", ArrayList(tag.translations.map { it.languageTag }))
            putStringArrayList("names", ArrayList(tag.translations.map { it.name }))
        }
    })
}
