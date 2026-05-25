package com.rayject.storeray.cli.appinfo

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.rayject.storeray.config.AppInfoLoader
import com.rayject.storeray.config.ConfigLoader
import com.rayject.storeray.provider.Platform
import com.rayject.storeray.provider.StoreProviderFactory
import com.rayject.storeray.util.Console
import kotlinx.coroutines.runBlocking

class AppInfoFetchCommand : CliktCommand(
    name = "fetch"
) {
    override fun help(context: com.github.ajalt.clikt.core.Context) = "Fetch app info metadata from store to local"

    private val force by option("--force", help = "Overwrite local data even if remote fields are empty").flag(default = false)

    private val platform by option("-p", "--platform", help = "Target store platform (appstore, playstore). If omitted, runs both.")

    override fun run() = runBlocking {
        try {
            val dir = com.rayject.storeray.cli.GlobalState.workspaceDir
            val workspaceConfig = ConfigLoader.loadWorkspaceConfig("$dir/storeray.json")

            val platforms = if (platform == null) {
                listOf(Platform.APP_STORE, Platform.PLAY_STORE)
            } else {
                listOf(parsePlatform(platform!!))
            }

            var failures = 0
            for (targetPlatform in platforms) {
                Console.divider()
                Console.step("Fetching app info from ${targetPlatform.displayName()}...")

                try {
                    fetchPlatform(targetPlatform, dir, workspaceConfig)
                } catch (e: Exception) {
                    failures++
                    Console.error("${targetPlatform.displayName()} failed: ${e.message}")
                }
            }

            if (failures > 0) {
                Console.warning("Fetch finished with $failures platform failure(s).")
            }

        } catch (e: Exception) {
            Console.error("Fetch failed: ${e.message}")
        }
    }

    private suspend fun fetchPlatform(platform: Platform, dir: String, workspaceConfig: com.rayject.storeray.config.WorkspaceConfig) {
        val provider = StoreProviderFactory.create(platform, workspaceConfig)
        val appInfoService = provider.appInfo()

        val data = appInfoService.fetch()
        if (data.isEmpty()) {
            Console.warning("No app info data found on ${platform.displayName()}")
            return
        }

        Console.info("Fetched ${data.size} locale(s) from ${platform.displayName()}")

        val appInfoDir = "$dir/metadata/app_info"
        if (force) Console.warning("Force mode: overwriting all local data with remote values")
        AppInfoLoader.save(appInfoDir, data, overwrite = force)

        Console.success("Saved to $appInfoDir")
        Console.detail("Locales:")
        for ((locale, info) in data.entries.sortedBy { it.key }) {
            val fields = mutableListOf<String>()
            if (info.name.isNotBlank()) fields.add("name=${info.name}")
            if (info.description.isNotBlank()) fields.add("desc=${info.description.take(50)}...")
            Console.detail("  $locale: ${fields.joinToString(", ")}")
        }
    }

    private fun parsePlatform(value: String): Platform = when (value.lowercase()) {
        "appstore" -> Platform.APP_STORE
        "playstore" -> Platform.PLAY_STORE
        else -> throw IllegalArgumentException("Unknown platform: $value")
    }

    private fun Platform.displayName(): String = when (this) {
        Platform.APP_STORE -> "App Store Connect"
        Platform.PLAY_STORE -> "Google Play Console"
    }
}
