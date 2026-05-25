package com.rayject.storeray.config

import com.rayject.storeray.model.AppInfoData
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

object AppInfoLoader {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    private const val INFO_FILE = "info.json"
    private const val DESCRIPTION_FILE = "description.txt"
    private const val PROMOTIONAL_TEXT_FILE = "promotional_text.txt"

    fun load(dirPath: String): Map<String, AppInfoData> {
        val dir = File(dirPath)
        if (!dir.exists() || !dir.isDirectory) {
            return emptyMap()
        }

        return dir.listFiles { file -> file.isDirectory }
            ?.mapNotNull { localeDir ->
                val locale = localeDir.name
                val infoFile = File(localeDir, INFO_FILE)
                if (!infoFile.exists()) return@mapNotNull null

                val infoData = try {
                    json.decodeFromString<InfoJson>(infoFile.readText())
                } catch (e: Exception) {
                    throw IllegalArgumentException("Failed to parse $INFO_FILE for $locale: ${e.message}")
                }

                val description = readTextFile(localeDir, DESCRIPTION_FILE)
                val promotionalText = readTextFile(localeDir, PROMOTIONAL_TEXT_FILE)

                locale to AppInfoData(
                    name = infoData.name,
                    subtitle = infoData.subtitle,
                    keywords = infoData.keywords,
                    supportUrl = infoData.supportUrl,
                    marketingUrl = infoData.marketingUrl,
                    privacyPolicyUrl = infoData.privacyPolicyUrl,
                    video = infoData.video,
                    description = description,
                    promotionalText = promotionalText
                )
            }
            ?.toMap() ?: emptyMap()
    }

    fun save(dirPath: String, data: Map<String, AppInfoData>, overwrite: Boolean = false) {
        val writeData = if (overwrite) {
            data
        } else {
            val existing = load(dirPath)
            data.mapValues { (locale, newData) ->
                val oldData = existing[locale] ?: return@mapValues newData
                newData.copy(
                    name = newData.name.ifBlank { oldData.name },
                    subtitle = newData.subtitle.ifBlank { oldData.subtitle },
                    keywords = newData.keywords.ifBlank { oldData.keywords },
                    supportUrl = newData.supportUrl.ifBlank { oldData.supportUrl },
                    marketingUrl = newData.marketingUrl.ifBlank { oldData.marketingUrl },
                    privacyPolicyUrl = newData.privacyPolicyUrl.ifBlank { oldData.privacyPolicyUrl },
                    video = newData.video.ifBlank { oldData.video },
                    description = newData.description.ifBlank { oldData.description },
                    promotionalText = newData.promotionalText.ifBlank { oldData.promotionalText }
                )
            }
        }

        for ((locale, appInfo) in writeData) {
            val localeDir = File(dirPath, locale)
            localeDir.mkdirs()

            val infoData = InfoJson(
                name = appInfo.name,
                subtitle = appInfo.subtitle,
                keywords = appInfo.keywords,
                supportUrl = appInfo.supportUrl,
                marketingUrl = appInfo.marketingUrl,
                privacyPolicyUrl = appInfo.privacyPolicyUrl,
                video = appInfo.video
            )
            File(localeDir, INFO_FILE).writeText(json.encodeToString(InfoJson.serializer(), infoData))

            writeTextFile(localeDir, DESCRIPTION_FILE, appInfo.description)
            writeTextFile(localeDir, PROMOTIONAL_TEXT_FILE, appInfo.promotionalText)
        }
    }

    private fun readTextFile(dir: File, fileName: String): String {
        val file = File(dir, fileName)
        return if (file.exists()) file.readText().trimEnd() else ""
    }

    private fun writeTextFile(dir: File, fileName: String, content: String) {
        if (content.isNotBlank()) {
            File(dir, fileName).writeText(content.trimEnd() + "\n")
        } else {
            val file = File(dir, fileName)
            if (file.exists()) file.delete()
        }
    }

    @Serializable
    private data class InfoJson(
        val name: String = "",
        val subtitle: String = "",
        val keywords: String = "",
        val supportUrl: String = "",
        val marketingUrl: String = "",
        val privacyPolicyUrl: String = "",
        val video: String = ""
    )
}
