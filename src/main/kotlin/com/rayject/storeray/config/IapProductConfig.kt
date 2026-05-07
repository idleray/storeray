package com.rayject.storeray.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class IapProductConfig(
    @SerialName("product_id")
    val productId: String,
    @SerialName("reference_name")
    val referenceName: String,
    val localizations: Map<String, IapLocalizationConfig>
)

@Serializable
data class IapLocalizationConfig(
    val name: String,
    val description: String
)
