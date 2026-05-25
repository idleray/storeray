package com.rayject.storeray.provider.appstore

import com.rayject.storeray.model.AppInfoData
import com.rayject.storeray.provider.AppInfoService
import com.rayject.storeray.provider.appstore.api.AppStoreConnectApi
import com.rayject.storeray.util.Console
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

class AppStoreAppInfoService(
    private val api: AppStoreConnectApi,
    private val bundleId: String
) : AppInfoService {

    private var appIdCache: String? = null
    private var appInfoId: String? = null
    private var versionId: String? = null
    private var localeToAppInfoLocalizationId: Map<String, String> = emptyMap()
    private var localeToVersionLocalizationId: Map<String, String> = emptyMap()

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
        appInfoId = appInfo.id
        val appInfoLocalizations = api.fetchAppInfoLocalizations(appInfo.id)
        localeToAppInfoLocalizationId = appInfoLocalizations.mapNotNull { loc ->
            val a = loc.attributes ?: return@mapNotNull null
            val locale = a["locale"]?.jsonPrimitive?.content ?: return@mapNotNull null
            locale to loc.id
        }.toMap()

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
            val vId = editableVersions.first().id
            versionId = vId
            val locs = api.fetchAppStoreVersionLocalizations(vId)
            localeToVersionLocalizationId = locs.mapNotNull { loc ->
                val a = loc.attributes ?: return@mapNotNull null
                val locale = a["locale"]?.jsonPrimitive?.content ?: return@mapNotNull null
                locale to loc.id
            }.toMap()
            locs
        } else {
            Console.warning("No editable version found. Version-specific fields (description, keywords, etc.) will be empty.")
            emptyList()
        }

        val versionData: Map<String, AppStoreVersionFields> = versionLocalizations.mapNotNull { loc ->
            val a = loc.attributes ?: return@mapNotNull null
            val locale = a["locale"]?.jsonPrimitive?.content ?: return@mapNotNull null
            val description = a["description"]?.jsonPrimitive?.content ?: ""
            val keywords = a["keywords"]?.jsonPrimitive?.content ?: ""
            val marketingUrl = a["marketingUrl"]?.jsonPrimitive?.content ?: ""
            val promotionalText = a["promotionalText"]?.jsonPrimitive?.content ?: ""
            val supportUrl = a["supportUrl"]?.jsonPrimitive?.content ?: ""
            locale to AppStoreVersionFields(
                description = description,
                keywords = keywords,
                marketingUrl = marketingUrl,
                promotionalText = promotionalText,
                supportUrl = supportUrl
            )
        }.toMap()

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

    override suspend fun update(data: Map<String, AppInfoData>) {
        val aId = appInfoId ?: throw RuntimeException("App info ID not available. Call fetch() first.")
        val vId = versionId

        for ((locale, info) in data) {
            updateAppInfoLocalization(aId, locale, info)
            if (vId != null) {
                updateVersionLocalization(vId, locale, info)
            } else {
                Console.warning("No editable version; skipping version-specific fields for $locale")
            }
        }
    }

    private suspend fun updateAppInfoLocalization(appInfoId: String, locale: String, data: AppInfoData) {
        val attrs = buildJsonObject {
            if (data.name.isNotBlank()) put("name", data.name)
            if (data.subtitle.isNotBlank()) put("subtitle", data.subtitle)
            if (data.privacyPolicyUrl.isNotBlank()) put("privacyPolicyUrl", data.privacyPolicyUrl)
        }
        if (attrs.isEmpty()) return

        val existingId = localeToAppInfoLocalizationId[locale]
        if (existingId != null) {
            api.updateAppInfoLocalization(existingId, attrs)
        } else {
            api.createAppInfoLocalization(appInfoId, locale, attrs)
        }
    }

    private suspend fun updateVersionLocalization(versionId: String, locale: String, data: AppInfoData) {
        val attrs = buildJsonObject {
            if (data.description.isNotBlank()) put("description", data.description)
            if (data.keywords.isNotBlank()) put("keywords", data.keywords)
            if (data.marketingUrl.isNotBlank()) put("marketingUrl", data.marketingUrl)
            if (data.promotionalText.isNotBlank()) put("promotionalText", data.promotionalText)
            if (data.supportUrl.isNotBlank()) put("supportUrl", data.supportUrl)
        }
        if (attrs.isEmpty()) return

        val existingId = localeToVersionLocalizationId[locale]
        if (existingId != null) {
            api.updateAppStoreVersionLocalizationAttributes(existingId, attrs)
        } else {
            api.createAppStoreVersionLocalization(versionId, locale, attrs)
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
