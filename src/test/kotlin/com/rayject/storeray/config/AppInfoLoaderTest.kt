package com.rayject.storeray.config

import com.rayject.storeray.model.AppInfoData
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.File

class AppInfoLoaderTest {

    @Test
    fun `load returns empty map when directory does not exist`() {
        val result = AppInfoLoader.load("/nonexistent/path")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `load returns empty map when directory is empty`(@TempDir tempDir: File) {
        val result = AppInfoLoader.load(tempDir.path)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `load returns data for a single locale`(@TempDir tempDir: File) {
        val localeDir = File(tempDir, "en-US")
        localeDir.mkdirs()
        File(localeDir, "info.json").writeText("""
            {
                "name": "Test App",
                "subtitle": "Test Subtitle",
                "keywords": "kw1,kw2",
                "supportUrl": "https://example.com/support",
                "privacyPolicyUrl": "https://example.com/privacy"
            }
        """.trimIndent())
        File(localeDir, "description.txt").writeText("App description here")

        val result = AppInfoLoader.load(tempDir.path)

        assertEquals(1, result.size)
        val data = result["en-US"]
        assertEquals("Test App", data?.name)
        assertEquals("Test Subtitle", data?.subtitle)
        assertEquals("kw1,kw2", data?.keywords)
        assertEquals("https://example.com/support", data?.supportUrl)
        assertEquals("", data?.marketingUrl)
        assertEquals("https://example.com/privacy", data?.privacyPolicyUrl)
        assertEquals("App description here", data?.description)
        assertEquals("", data?.promotionalText)
    }

    @Test
    fun `load reads promotional text file when present`(@TempDir tempDir: File) {
        val localeDir = File(tempDir, "en-US")
        localeDir.mkdirs()
        File(localeDir, "info.json").writeText("""{"name": "Test App"}""")
        File(localeDir, "promotional_text.txt").writeText("Promo text here")

        val result = AppInfoLoader.load(tempDir.path)
        assertEquals("Promo text here", result["en-US"]?.promotionalText)
    }

    @Test
    fun `load skips locale directories without info json`(@TempDir tempDir: File) {
        val localeDir = File(tempDir, "en-US")
        localeDir.mkdirs()
        // No info.json — should be skipped
        val result = AppInfoLoader.load(tempDir.path)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `load throws on malformed info json`(@TempDir tempDir: File) {
        val localeDir = File(tempDir, "en-US")
        localeDir.mkdirs()
        File(localeDir, "info.json").writeText("not valid json")
        assertThrows<IllegalArgumentException> {
            AppInfoLoader.load(tempDir.path)
        }
    }

    @Test
    fun `load reads multiple locales`(@TempDir tempDir: File) {
        for (locale in listOf("en-US", "zh-Hans", "ja")) {
            val dir = File(tempDir, locale)
            dir.mkdirs()
            File(dir, "info.json").writeText("""{"name": "$locale Name"}""")
        }

        val result = AppInfoLoader.load(tempDir.path)
        assertEquals(3, result.size)
        assertEquals("en-US Name", result["en-US"]?.name)
        assertEquals("zh-Hans Name", result["zh-Hans"]?.name)
        assertEquals("ja Name", result["ja"]?.name)
    }

    @Test
    fun `save creates files for new data`(@TempDir tempDir: File) {
        val data = mapOf(
            "en-US" to AppInfoData(
                name = "My App",
                subtitle = "Best App",
                keywords = "a,b",
                description = "Long description",
                supportUrl = "https://example.com"
            )
        )

        AppInfoLoader.save(tempDir.path, data)

        val loaded = AppInfoLoader.load(tempDir.path)
        assertEquals("My App", loaded["en-US"]?.name)
        assertEquals("Best App", loaded["en-US"]?.subtitle)
        assertEquals("a,b", loaded["en-US"]?.keywords)
        assertEquals("Long description", loaded["en-US"]?.description)
        assertEquals("https://example.com", loaded["en-US"]?.supportUrl)
    }

    @Test
    fun `save with merge mode keeps existing non-blank fields`(@TempDir tempDir: File) {
        val localeDir = File(tempDir, "en-US")
        localeDir.mkdirs()
        File(localeDir, "info.json").writeText("""{"name": "Existing Name", "subtitle": ""}""")
        File(localeDir, "description.txt").writeText("Existing description")

        val newData = mapOf(
            "en-US" to AppInfoData(
                name = "",
                subtitle = "New Subtitle",
                description = ""
            )
        )

        AppInfoLoader.save(tempDir.path, newData) // merge mode (default)

        val loaded = AppInfoLoader.load(tempDir.path)
        assertEquals("Existing Name", loaded["en-US"]?.name) // kept from existing
        assertEquals("New Subtitle", loaded["en-US"]?.subtitle) // set from new
        assertEquals("Existing description", loaded["en-US"]?.description) // kept from existing
    }

    @Test
    fun `save with overwrite replaces all fields`(@TempDir tempDir: File) {
        val localeDir = File(tempDir, "en-US")
        localeDir.mkdirs()
        File(localeDir, "info.json").writeText("""{"name": "Old Name", "subtitle": "Old Subtitle"}""")
        File(localeDir, "description.txt").writeText("Old description")

        val newData = mapOf(
            "en-US" to AppInfoData(
                name = "New Name",
                subtitle = ""
            )
        )

        AppInfoLoader.save(tempDir.path, newData, overwrite = true)

        val loaded = AppInfoLoader.load(tempDir.path)
        assertEquals("New Name", loaded["en-US"]?.name)
        assertEquals("", loaded["en-US"]?.subtitle) // overwritten with blank
        assertEquals("", loaded["en-US"]?.description) // overwritten, blank -> file deleted
    }

    @Test
    fun `save deletes text file when content becomes blank in overwrite mode`(@TempDir tempDir: File) {
        val localeDir = File(tempDir, "en-US")
        localeDir.mkdirs()
        File(localeDir, "info.json").writeText("""{"name": "App"}""")
        File(localeDir, "description.txt").writeText("Some description")

        val data = mapOf(
            "en-US" to AppInfoData(name = "App Updated")
        )

        AppInfoLoader.save(tempDir.path, data, overwrite = true)

        assertTrue(File(localeDir, "info.json").exists())
        assertTrue(!File(localeDir, "description.txt").exists())
    }

    @Test
    fun `save creates locale directory if it does not exist`(@TempDir tempDir: File) {
        val data = mapOf(
            "fr-FR" to AppInfoData(name = "Mon App")
        )

        AppInfoLoader.save(tempDir.path, data)

        assertTrue(File(tempDir, "fr-FR").exists())
        assertTrue(File(tempDir, "fr-FR/info.json").exists())
    }
}
