package com.rayject.storeray.cli.appinfo

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.rayject.storeray.config.AppInfoLoader
import com.rayject.storeray.config.ConfigLoader
import com.rayject.storeray.provider.Platform
import com.rayject.storeray.provider.StoreProviderFactory
import com.rayject.storeray.usecase.SyncAppInfoUseCase
import com.rayject.storeray.util.Console
import kotlinx.coroutines.runBlocking

class AppInfoSyncCommand : CliktCommand(
    name = "sync"
) {
    override fun help(context: com.github.ajalt.clikt.core.Context) = "Sync app info metadata to store (compare local vs remote, then create/update)"

    private val apply by option("--apply", help = "Apply changes to the store (default: dry-run)").flag(default = false)

    private val platform by option("-p", "--platform", help = "Target store platform (appstore, playstore). If omitted, runs both in order.")

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
                Console.step("App Info sync: ${targetPlatform.displayName()}")

                try {
                    syncPlatform(targetPlatform, dir, workspaceConfig)
                } catch (e: Exception) {
                    failures++
                    Console.error("${targetPlatform.displayName()} failed: ${e.message}")
                }
            }

            if (failures > 0) {
                Console.warning("Sync finished with $failures platform failure(s).")
            }

        } catch (e: Exception) {
            echo("❌ Error: ${e.message}", err = true)
        }
    }

    private suspend fun syncPlatform(platform: Platform, dir: String, workspaceConfig: com.rayject.storeray.config.WorkspaceConfig) {
        val provider = StoreProviderFactory.create(platform, workspaceConfig)
        val appInfoService = provider.appInfo()

        val localData = AppInfoLoader.load("$dir/metadata/app_info")
        if (localData.isEmpty()) {
            Console.error("No local app info data found.")
            Console.info("Use `storeray appinfo fetch` to download data first.")
            return
        }

        val useCase = SyncAppInfoUseCase(
            appInfoService = appInfoService,
            localData = localData
        )

        useCase.execute(dryRun = !apply)
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
