package com.rayject.storeray.provider.appstore

import com.rayject.storeray.provider.ReleaseNotesService
import com.rayject.storeray.provider.appstore.api.AppStoreConnectApi
import kotlinx.serialization.json.jsonPrimitive

class AppStoreReleaseNotesService(
    private val api: AppStoreConnectApi,
    private val bundleId: String
) : ReleaseNotesService {
    
    private var appIdCache: String? = null
    
    private suspend fun getAppId(): String {
        if (appIdCache == null) {
            val app = api.fetchApp(bundleId) ?: throw RuntimeException("未找到 Bundle ID 为 $bundleId 的 App")
            appIdCache = app.id
        }
        return appIdCache!!
    }

    override suspend fun fetchEditableVersion(): String {
        val appId = getAppId()
        val versions = api.fetchEditableAppStoreVersions(appId)
        
        if (versions.isEmpty()) {
            throw RuntimeException("No editable version found. Please create a new version in App Store Connect first.")
        }
        
        return versions.first().attributes?.get("versionString")?.jsonPrimitive?.content
            ?: throw RuntimeException("Failed to read version string from App Store Connect response.")
    }

    private suspend fun getVersionId(appVersion: String): String {
        val appId = getAppId()
        val versions = api.fetchAppStoreVersions(appId, appVersion)
        
        if (versions.isEmpty()) {
            throw RuntimeException("未在 App Store Connect 中找到版本 $appVersion")
        }
        
        // 通常只会有一个版本匹配，优先取第一个
        return versions.first().id
    }

    override suspend fun fetch(appVersion: String): Map<String, String> {
        val versionId = getVersionId(appVersion)
        val localizations = api.fetchAppStoreVersionLocalizations(versionId)
        
        return localizations.associate { loc ->
            val locale = loc.attributes?.get("locale")?.jsonPrimitive?.content ?: ""
            val whatsNew = loc.attributes?.get("whatsNew")?.jsonPrimitive?.content ?: ""
            // 如果远端没有填写 whatsNew，返回空字符串
            locale to whatsNew
        }.filterKeys { it.isNotEmpty() }
    }

    override suspend fun update(appVersion: String, notes: Map<String, String>) {
        val versionId = getVersionId(appVersion)
        val localizations = api.fetchAppStoreVersionLocalizations(versionId)
        
        // 创建一个 locale 到 id 的映射
        val localeToIdMap = localizations.associate { loc ->
            val locale = loc.attributes?.get("locale")?.jsonPrimitive?.content ?: ""
            locale to loc.id
        }
        
        for ((locale, newText) in notes) {
            val localizationId = localeToIdMap[locale]
            if (localizationId == null) {
                // TODO: 严格来说如果远端没有这个语言的配置，应该先创建 appStoreVersionLocalization。
                // 但通常我们会要求在 App Store Connect 先添加好支持的语言。这里仅处理已存在的语言更新。
                throw RuntimeException("无法更新 $locale 的 Release Notes，因为 App Store Connect 中尚未配置该语言的版本元数据。")
            }
            
            api.updateAppStoreVersionLocalization(localizationId, newText)
        }
    }
}
