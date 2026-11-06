package ru.mobui.kanji.core.designsystem

import android.content.res.Configuration

/** Preferred BCP 47 tag from the configuration; Russian when the locale list is empty. */
fun Configuration.preferredLanguageTag(fallback: String = "ru"): String =
    locales.takeIf { it.size() > 0 }?.get(0)?.toLanguageTag() ?: fallback

/** Preferred ISO language subtag; Russian when the locale list is empty. */
fun Configuration.preferredLanguage(fallback: String = "ru"): String =
    locales.takeIf { it.size() > 0 }?.get(0)?.language?.takeIf { it.isNotBlank() } ?: fallback
