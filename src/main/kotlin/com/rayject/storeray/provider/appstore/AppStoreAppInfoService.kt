package com.rayject.storeray.provider.appstore

import com.rayject.storeray.model.AppInfoData
import com.rayject.storeray.provider.AppInfoService
import com.rayject.storeray.provider.appstore.api.AppStoreConnectApi
import com.rayject.storeray.util.Console
import kotlinx.serialization.json.jsonPrimitive

class AppStoreAppInfoService(
    private val api: AppStoreConnectApi,
    private val bundleId: String
) : AppInfoService {

    private var appIdCache: String? = null

    private suspend fun getAppId(): String {
        if (appIdCache == null) {
            val app = api.fetchApp(bundleId)
                ?: throw RuntimeException("App not found for bundle ID: $bundleId")
            appIdCache = app.id
        }
        return appIdCache!!
    }

    override suspend fun fetch(): Map<String, AppInfoData> {
        val appId = getAppId()

        val appInfos = api.fetchAppInfos(appId)
        val appInfo = appInfos.firstOrNull()
            ?: throw RuntimeException("No app info found for app ID: $appId")
        val appInfoLocalizations = api.fetchAppInfoLocalizations(appInfo.id)

        val nameAndSubtitle: Map<String, AppInfoLocalizationFields> = appInfoLocalizations.mapNotNull { loc ->
            val a = loc.attributes ?: return@mapNotNull null
            val locale = a["locale"]?.jsonPrimitive?.content ?: return@mapNotNull null
            val name = a["name"]?.jsonPrimitive?.content ?: ""
            val subtitle = a["subtitle"]?.jsonPrimitive?.content ?: ""
            val privacyUrl = a["privacyPolicyUrl"]?.jsonPrimitive?.content ?: ""
            locale to AppInfoLocalizationFields(name, subtitle, privacyUrl)
        }.toMap()

        val editableVersions = api.fetchEditableAppStoreVersions(appId)
        val versionLocalizations = if (editableVersions.isNotEmpty()) {
            val versionId = editableVersions.first().id
            api.fetchAppStoreVersionLocalizations(versionId)
        } else {
            Console.warning("No editable version found. Version-specific fields (description, keywords, etc.) will be empty.")
            emptyList()
        }

        val versionData: Map<String, AppStoreVersionFields> = versionLocalizations.associate { loc ->
            val locale = loc.attributes?.get("locale")?.jsonPrimitive?.content ?: ""
            val description = loc.attributes?.get("description")?.jsonPrimitive?.content ?: ""
            val keywords = loc.attributes?.get("keywords")?.jsonPrimitive?.content ?: ""
            val marketingUrl = loc.attributes?.get("marketingUrl")?.jsonPrimitive?.content ?: ""
            val promotionalText = loc.attributes?.get("promotionalText")?.jsonPrimitive?.content ?: ""
            val supportUrl = loc.attributes?.get("supportUrl")?.jsonPrimitive?.content ?: ""
            locale to AppStoreVersionFields(
                description = description,
                keywords = keywords,
                marketingUrl = marketingUrl,
                promotionalText = promotionalText,
                supportUrl = supportUrl
            )
        }.filterKeys { it.isNotEmpty() }

        val allLocales = (nameAndSubtitle.keys + versionData.keys).toSet()

        return allLocales.associateWith { locale ->
            val info = nameAndSubtitle[locale]
            val version = versionData[locale]
            AppInfoData(
                name = info?.name ?: "",
                subtitle = info?.subtitle ?: "",
                privacyPolicyUrl = info?.privacyPolicyUrl ?: "",
                description = version?.description ?: "",
                keywords = version?.keywords ?: "",
                marketingUrl = version?.marketingUrl ?: "",
                promotionalText = version?.promotionalText ?: "",
                supportUrl = version?.supportUrl ?: ""
            )
        }
    }

    private data class AppInfoLocalizationFields(
        val name: String,
        val subtitle: String,
        val privacyPolicyUrl: String
    )

    private data class AppStoreVersionFields(
        val description: String,
        val keywords: String,
        val marketingUrl: String,
        val promotionalText: String,
        val supportUrl: String
    )
}
