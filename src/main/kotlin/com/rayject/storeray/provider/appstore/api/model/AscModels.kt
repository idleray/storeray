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
