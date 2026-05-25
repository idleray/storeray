package com.rayject.storeray.provider.appstore

import com.rayject.storeray.model.AppInfoData
import com.rayject.storeray.provider.AppInfoService
import com.rayject.storeray.provider.appstore.api.AppStoreConnectApi
import com.rayject.storeray.util.Console
import kotlinx.serialization.json.buildJsonObject
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

        val appInfoLocales = api.fetchTypedAppInfoLocalizations(appInfo.id)
        localeToAppInfoLocalizationId = appInfoLocales.associate { it.id to it.attributes.locale }
            .entries.associate { it.value to it.key }

        val nameAndSubtitle = appInfoLocales.associate { entry ->
            entry.attributes.locale to AppInfoLocalizationFields(
                name = entry.attributes.name ?: "",
                subtitle = entry.attributes.subtitle ?: "",
                privacyPolicyUrl = entry.attributes.privacyPolicyUrl ?: ""
            )
        }

        val editableVersions = api.fetchEditableAppStoreVersions(appId)
        val versionLocales = if (editableVersions.isNotEmpty()) {
            val vId = editableVersions.first().id
            versionId = vId
            val locs = api.fetchTypedAppStoreVersionLocalizations(vId)
            localeToVersionLocalizationId = locs.associate { it.attributes.locale to it.id }
            locs
        } else {
            Console.warning("No editable version found (PREPARE_FOR_SUBMISSION). Version-specific fields (description, keywords, etc.) will be empty and cannot be synced until a new version is created.")
            emptyList()
        }

        val versionData = versionLocales.associate { entry ->
            entry.attributes.locale to AppStoreVersionFields(
                description = entry.attributes.description ?: "",
                keywords = entry.attributes.keywords ?: "",
                marketingUrl = entry.attributes.marketingUrl ?: "",
                promotionalText = entry.attributes.promotionalText ?: "",
                supportUrl = entry.attributes.supportUrl ?: ""
            )
        }

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
        val hasVersionFields = data.values.any {
            it.description.isNotBlank() || it.keywords.isNotBlank() ||
            it.marketingUrl.isNotBlank() || it.promotionalText.isNotBlank() || it.supportUrl.isNotBlank()
        }
        if (hasVersionFields && versionId == null) {
            throw RuntimeException(
                "No editable version found in App Store Connect. " +
                "Version-specific fields (description, keywords, marketingUrl, promotionalText, supportUrl) " +
                "require a version in PREPARE_FOR_SUBMISSION state. " +
                "Please create a new version in App Store Connect first, then retry."
            )
        }

        for ((locale, info) in data) {
            updateAppInfoLocalization(aId, locale, info)
            if (versionId != null) {
                updateVersionLocalization(versionId!!, locale, info)
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
