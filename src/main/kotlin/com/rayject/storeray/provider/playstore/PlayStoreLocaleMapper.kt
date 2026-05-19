package com.rayject.storeray.provider.playstore

object PlayStoreLocaleMapper {
    private val appStoreToPlayStore = mapOf(
        "ar-SA" to listOf("ar"),
        "ca" to listOf("ca"),
        "zh-Hans" to listOf("zh-CN"),
        "zh-Hant" to listOf("zh-HK", "zh-TW"),
        "hr" to listOf("hr"),
        "cs" to listOf("cs-CZ"),
        "da" to listOf("da-DK"),
        "nl-NL" to listOf("nl-NL"),
        "en-AU" to listOf("en-AU"),
        "en-CA" to listOf("en-CA"),
        "en-GB" to listOf("en-GB"),
        "en-US" to listOf("en-US"),
        "fi" to listOf("fi-FI"),
        "fr-FR" to listOf("fr-FR"),
        "fr-CA" to listOf("fr-CA"),
        "de-DE" to listOf("de-DE"),
        "el" to listOf("el-GR"),
        "he" to listOf("iw-IL"),
        "hi" to listOf("hi-IN"),
        "hu" to listOf("hu-HU"),
        "id" to listOf("id"),
        "it" to listOf("it-IT"),
        "ja" to listOf("ja-JP"),
        "ko" to listOf("ko-KR"),
        "ms" to listOf("ms"),
        "no" to listOf("no-NO"),
        "pl" to listOf("pl-PL"),
        "pt-BR" to listOf("pt-BR"),
        "pt-PT" to listOf("pt-PT"),
        "ro" to listOf("ro"),
        "ru" to listOf("ru-RU"),
        "sk" to listOf("sk"),
        "es-MX" to listOf("es-419"),
        "es-ES" to listOf("es-ES"),
        "sv" to listOf("sv-SE"),
        "th" to listOf("th"),
        "tr" to listOf("tr-TR"),
        "uk" to listOf("uk"),
        "vi" to listOf("vi")
    )

    private val playStoreToAppStore = appStoreToPlayStore
        .flatMap { (appStoreLocale, playStoreLocales) ->
            playStoreLocales.map { playStoreLocale -> playStoreLocale to appStoreLocale }
        }
        .toMap()

    val appStoreLocaleShortcodes: Set<String> = appStoreToPlayStore.keys

    fun toPlayStoreLocales(appStoreLocale: String): List<String> {
        return appStoreToPlayStore[appStoreLocale] ?: listOf(appStoreLocale)
    }

    fun toAppStoreLocale(playStoreLocale: String): String {
        return playStoreToAppStore[playStoreLocale] ?: playStoreLocale
    }
}
