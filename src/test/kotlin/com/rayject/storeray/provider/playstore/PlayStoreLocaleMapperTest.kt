package com.rayject.storeray.provider.playstore

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PlayStoreLocaleMapperTest {
    @Test
    fun `covers all app store locale shortcodes`() {
        val appStoreLocaleShortcodes = setOf(
            "ar-SA",
            "ca",
            "zh-Hans",
            "zh-Hant",
            "hr",
            "cs",
            "da",
            "nl-NL",
            "en-AU",
            "en-CA",
            "en-GB",
            "en-US",
            "fi",
            "fr-FR",
            "fr-CA",
            "de-DE",
            "el",
            "he",
            "hi",
            "hu",
            "id",
            "it",
            "ja",
            "ko",
            "ms",
            "no",
            "pl",
            "pt-BR",
            "pt-PT",
            "ro",
            "ru",
            "sk",
            "es-MX",
            "es-ES",
            "sv",
            "th",
            "tr",
            "uk",
            "vi"
        )

        assertEquals(appStoreLocaleShortcodes, PlayStoreLocaleMapper.appStoreLocaleShortcodes)
    }

    @Test
    fun `maps app store locale shortcodes to google play locales`() {
        assertEquals(listOf("zh-CN"), PlayStoreLocaleMapper.toPlayStoreLocales("zh-Hans"))
        assertEquals(listOf("zh-HK", "zh-TW"), PlayStoreLocaleMapper.toPlayStoreLocales("zh-Hant"))
        assertEquals(listOf("it-IT"), PlayStoreLocaleMapper.toPlayStoreLocales("it"))
        assertEquals(listOf("ja-JP"), PlayStoreLocaleMapper.toPlayStoreLocales("ja"))
        assertEquals(listOf("ko-KR"), PlayStoreLocaleMapper.toPlayStoreLocales("ko"))
        assertEquals(listOf("ru-RU"), PlayStoreLocaleMapper.toPlayStoreLocales("ru"))
        assertEquals(listOf("de-DE"), PlayStoreLocaleMapper.toPlayStoreLocales("de-DE"))
    }

    @Test
    fun `maps google play locales back to app store locale shortcodes`() {
        assertEquals("zh-Hans", PlayStoreLocaleMapper.toAppStoreLocale("zh-CN"))
        assertEquals("zh-Hant", PlayStoreLocaleMapper.toAppStoreLocale("zh-HK"))
        assertEquals("zh-Hant", PlayStoreLocaleMapper.toAppStoreLocale("zh-TW"))
        assertEquals("it", PlayStoreLocaleMapper.toAppStoreLocale("it-IT"))
        assertEquals("ja", PlayStoreLocaleMapper.toAppStoreLocale("ja-JP"))
        assertEquals("ko", PlayStoreLocaleMapper.toAppStoreLocale("ko-KR"))
        assertEquals("ru", PlayStoreLocaleMapper.toAppStoreLocale("ru-RU"))
        assertEquals("de-DE", PlayStoreLocaleMapper.toAppStoreLocale("de-DE"))
    }
}
