package com.rayject.storeray.provider

import com.rayject.storeray.model.LocalizationInfo
import com.rayject.storeray.model.Subscription

enum class Platform {
    APP_STORE,
    PLAY_STORE
}

interface StoreProvider {
    val platform: Platform
    
    fun releaseNotes(): ReleaseNotesService
    fun iap(): IapService
}

interface ReleaseNotesService {
    /** Fetch the version currently in editable state (e.g., PREPARE_FOR_SUBMISSION) */
    suspend fun fetchEditableVersion(): String

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
