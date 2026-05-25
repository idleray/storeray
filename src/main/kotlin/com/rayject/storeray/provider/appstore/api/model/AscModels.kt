package com.rayject.storeray.provider.appstore.api.model

import kotlinx.serialization.Serializable

@Serializable
data class AscResponse<T>(
    val data: T,
    val included: List<AscResource>? = null
)

@Serializable
data class AscListResponse<T>(
    val data: List<T>,
    val included: List<AscResource>? = null
)

@Serializable
data class AscResource(
    val type: String,
    val id: String,
    val attributes: Map<String, kotlinx.serialization.json.JsonElement>? = null,
    val relationships: Map<String, AscRelationship>? = null
)

@Serializable
data class AscRelationship(
    val data: AscRelationshipData? = null
)

@Serializable
data class AscRelationshipData(
    val type: String,
    val id: String
)

@Serializable
data class AscErrorResponse(
    val errors: List<AscError>
)

@Serializable
data class AscError(
    val id: String? = null,
    val status: String,
    val code: String,
    val title: String,
    val detail: String
)

// ─── 专用类型：AppInfoLocalization ───

@Serializable
data class AppInfoLocalizationEntry(
    val id: String,
    val attributes: AppInfoLocalizationAttributes
)

@Serializable
data class AppInfoLocalizationAttributes(
    val locale: String,
    val name: String? = null,
    val subtitle: String? = null,
    val privacyPolicyUrl: String? = null,
    val privacyChoicesUrl: String? = null,
    val privacyPolicyText: String? = null
)

@Serializable
data class AppInfoLocalizationListResponse(
    val data: List<AppInfoLocalizationEntry>
)

// ─── 专用类型：AppStoreVersionLocalization ───

@Serializable
data class AppStoreVersionLocalizationEntry(
    val id: String,
    val attributes: AppStoreVersionLocalizationAttributes
)

@Serializable
data class AppStoreVersionLocalizationAttributes(
    val locale: String,
    val description: String? = null,
    val keywords: String? = null,
    val marketingUrl: String? = null,
    val promotionalText: String? = null,
    val supportUrl: String? = null,
    val whatsNew: String? = null
)

@Serializable
data class AppStoreVersionLocalizationListResponse(
    val data: List<AppStoreVersionLocalizationEntry>
)
