package com.rayject.storeray.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StoreConfig(
    @SerialName("key_id")
    val keyId: String,
    
    @SerialName("issuer_id")
    val issuerId: String,
    
    @SerialName("key_file_path")
    val keyFilePath: String,
    
    @SerialName("bundle_id")
    val bundleId: String
)

@Serializable
data class WorkspaceConfig(
    @SerialName("app_store")
    val appStore: StoreConfig? = null
)

