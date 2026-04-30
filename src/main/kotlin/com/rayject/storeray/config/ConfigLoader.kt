package com.rayject.storeray.config

import kotlinx.serialization.json.Json
import java.io.File

object ConfigLoader {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    fun loadStoreConfig(path: String = "storeray.json"): StoreConfig {
        val file = File(path)
        if (!file.exists()) {
            throw IllegalArgumentException("配置文件不存在: ${file.absolutePath}")
        }
        return json.decodeFromString(file.readText())
    }

    fun loadProductsConfig(path: String = "products.json"): ProductsConfig {
        val file = File(path)
        if (!file.exists()) {
            throw IllegalArgumentException("产品配置文件不存在: ${file.absolutePath}")
        }
        return json.decodeFromString(file.readText())
    }

    fun loadLocalizationFile(path: String): Map<String, LocalizationMap> {
        val file = File(path)
        if (!file.exists()) {
            throw IllegalArgumentException("本地化文件不存在: ${file.absolutePath}")
        }
        return json.decodeFromString(file.readText())
    }
}

/**
 * 对应 monthly.json / yearly.json 中的键值对结构
 */
@kotlinx.serialization.Serializable
data class LocalizationMap(
    val name: String,
    val description: String
)
