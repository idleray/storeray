package com.rayject.storeray.cli.releasenotes

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.default
import com.rayject.storeray.config.WorkspaceConfig
import com.rayject.storeray.config.ConfigLoader
import com.rayject.storeray.provider.Platform
import com.rayject.storeray.provider.ReleaseNotesService
import com.rayject.storeray.provider.StoreProviderFactory
import com.rayject.storeray.usecase.SyncReleaseNotesUseCase
import com.rayject.storeray.util.Console
import kotlinx.coroutines.runBlocking

class ReleaseNotesUpdateCommand : CliktCommand(
    name = "update"
) {
    override fun help(context: com.github.ajalt.clikt.core.Context) = "Update Release Notes for the pending version"

    private val apply by option("--apply", help = "Apply changes to the store (default: dry-run)").flag(default = false)
    
    private val platform by option("-p", "--platform", help = "Target store platform (appstore, playstore). If omitted, runs both in order.")

    private val track by option("-t", "--track", help = "Google Play track (e.g., internal, alpha, beta, production). Default: production").default("production")
    
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
                Console.step("Release Notes: ${targetPlatform.displayName()}")

                try {
                    syncPlatform(
                        platform = targetPlatform,
                        workspaceConfig = workspaceConfig,
                        releaseNotesDir = "$dir/metadata/release_notes",
                        track = track
                    )
                } catch (e: Exception) {
                    failures++
                    Console.error("${targetPlatform.displayName()} failed: ${e.message}")
                }
            }

            if (failures > 0) {
                Console.warning("Release Notes update finished with $failures platform failure(s).")
            }
            
        } catch (e: Exception) {
            echo("❌ Error: ${e.message}", err = true)
        }
    }

    private suspend fun syncPlatform(
        platform: Platform,
        workspaceConfig: WorkspaceConfig,
        releaseNotesDir: String,
        track: String
    ) {
        val provider = StoreProviderFactory.create(platform, workspaceConfig)
        val releaseNotesService: ReleaseNotesService = provider.releaseNotes(track)

        Console.info("Detecting editable version from ${platform.displayName()}...")
        val appVersion = releaseNotesService.fetchEditableVersion()
        Console.success("Found editable version: $appVersion")

        val localNotes = ConfigLoader.loadReleaseNotes(releaseNotesDir, appVersion)
        if (localNotes.isEmpty()) {
            Console.error("No local release notes file found: metadata/release_notes/$appVersion.json")
            Console.info("Please create this file with your release notes content.")
            return
        }

        val useCase = SyncReleaseNotesUseCase(
            releaseNotesService = releaseNotesService,
            localNotes = localNotes
        )

        useCase.execute(appVersion = appVersion, dryRun = !apply)
    }

    private fun parsePlatform(value: String): Platform = when (value.lowercase()) {
        "appstore" -> Platform.APP_STORE
        "playstore" -> Platform.PLAY_STORE
        else -> throw IllegalArgumentException("Unknown platform: $value")
    }

    private fun Platform.displayName(): String = when (this) {
        Platform.APP_STORE -> "App Store Connect"
        Platform.PLAY_STORE -> "Google Play $track track"
    }
}
