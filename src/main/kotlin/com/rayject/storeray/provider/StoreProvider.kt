package com.rayject.storeray.provider

import com.rayject.storeray.model.AppInfoData
import com.rayject.storeray.model.LocalizationInfo
import com.rayject.storeray.model.Subscription

enum class Platform {
    APP_STORE,
    PLAY_STORE
}

interface StoreProvider {
    val platform: Platform
    
    fun releaseNotes(track: String = "production"): ReleaseNotesService
    fun iap(): IapService
    fun appInfo(): AppInfoService
}

interface AppInfoService {
    /** Fetch all app info localizations from the store */
    suspend fun fetch(): Map<String, AppInfoData>

    /** Update app info for the given locales (create if not exists, update if exists) */
    suspend fun update(data: Map<String, AppInfoData>)

    /** Fields that this service can read and write */
    fun supportedFields(): Set<String> = ALL_FIELDS

    companion object {
        val ALL_FIELDS = setOf(
            "name", "subtitle", "keywords", "description", "promotionalText",
            "supportUrl", "marketingUrl", "privacyPolicyUrl", "video"
        )
    }
}

interface ReleaseNotesService {
    /** Fetch the version currently in editable state (e.g., PREPARE_FOR_SUBMISSION) */
    suspend fun fetchEditableVersion(): String

    /** Fetch locales supported by the store metadata for a specific version */
    suspend fun fetchSupportedLocales(appVersion: String): Set<String> = fetch(appVersion).keys

    /** Fetch release-note locales present in the store but unsupported by current store metadata */
    suspend fun fetchUnsupportedLocales(appVersion: String): Set<String> = emptySet()

    /** Map local release-note locale keys to the store locale keys that will be updated */
    fun targetLocalesFor(localLocale: String): List<String> = listOf(localLocale)

    /** Fetch release notes for a specific version */
    suspend fun fetch(appVersion: String): Map<String, String>

    /** Update release notes for a specific version */
    suspend fun update(appVersion: String, notes: Map<String, String>)
}

interface IapService {
    /** 获取所有订阅产品 */
    suspend fun fetchSubscriptions(): List<Subscription>

    /** 获取指定产品的本地化 */
    suspend fun fetchLocalizations(subscriptionId: String): Map<String, LocalizationInfo>

    /** 创建本地化 */
    suspend fun createLocalization(subscriptionId: String, locale: String, name: String, description: String)

    /** 更新本地化 */
    suspend fun updateLocalization(localizationId: String, name: String, description: String)
}
