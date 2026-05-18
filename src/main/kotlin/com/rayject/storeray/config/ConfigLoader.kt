package com.rayject.storeray.config

import kotlinx.serialization.json.Json
import java.io.File

object ConfigLoader {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    fun loadStoreConfig(path: String): StoreConfig {
        val file = File(path)
        if (!file.exists()) {
            throw IllegalArgumentException("配置文件不存在: ${file.absolutePath}")
        }
        val wrapper = json.decodeFromString<WorkspaceConfig>(file.readText())
        return wrapper.appStore ?: throw IllegalArgumentException("配置中缺少 app_store 部分: ${file.absolutePath}")
    }

    fun loadIapProducts(dirPath: String): List<IapProductConfig> {
        val dir = File(dirPath)
        if (!dir.exists() || !dir.isDirectory) {
            return emptyList()
        }

        return dir.listFiles { file -> file.extension == "json" }
            ?.map { file ->
                try {
                    json.decodeFromString<IapProductConfig>(file.readText())
                } catch (e: Exception) {
                    throw IllegalArgumentException("解析文件失败: ${file.absolutePath}, 错误: ${e.message}")
                }
            } ?: emptyList()
    }
    
    fun loadReleaseNotes(dirPath: String, version: String): Map<String, String> {
        val file = File(dirPath, "$version.json")
        if (!file.exists()) {
            return emptyMap()
        }
        
        try {
            return json.decodeFromString(file.readText())
        } catch (e: Exception) {
             throw IllegalArgumentException("解析 Release Notes 失败: ${file.absolutePath}, 错误: ${e.message}")
        }
    }
}
