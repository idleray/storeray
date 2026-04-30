package com.rayject.storeray.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProductsConfig(
    val products: List<ProductConfig>,
    val locales: List<LocaleConfig>
)

@Serializable
data class ProductConfig(
    @SerialName("product_id")
    val productId: String,
    
    @SerialName("reference_name")
    val referenceName: String,
    
    @SerialName("localization_file")
    val localizationFile: String
)

@Serializable
data class LocaleConfig(
    val code: String,
    val name: String,
    val required: Boolean = false
)
