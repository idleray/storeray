package com.rayject.storeray.cli.releasenotes

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.rayject.storeray.config.ConfigLoader
import com.rayject.storeray.provider.Platform
import com.rayject.storeray.provider.StoreProviderFactory
import com.rayject.storeray.usecase.SyncReleaseNotesUseCase
import com.rayject.storeray.util.Console
import kotlinx.coroutines.runBlocking

class ReleaseNotesUpdateCommand : CliktCommand(
    name = "update"
) {
    override fun help(context: com.github.ajalt.clikt.core.Context) = "Update Release Notes for the pending version"

    private val apply by option("--apply", help = "Apply changes to the store (default: dry-run)").flag(default = false)
    
    private val platform by option("-p", "--platform", help = "Target store platform (appstore, playstore)").default("appstore")
    
    override fun run() = runBlocking {
        try {
            val dir = com.rayject.storeray.cli.GlobalState.workspaceDir
            val workspaceConfig = ConfigLoader.loadWorkspaceConfig("$dir/storeray.json")
            
            val platformEnum = when (platform.lowercase()) {
                "appstore" -> Platform.APP_STORE
                "playstore" -> Platform.PLAY_STORE
                else -> throw IllegalArgumentException("Unknown platform: $platform")
            }
            
            val provider = StoreProviderFactory.create(platformEnum, workspaceConfig)
            val releaseNotesService = provider.releaseNotes()
            
            // Auto-detect the editable version from the selected store.
            Console.info("Detecting editable version from ${platformEnum.displayName()}...")
            val appVersion = releaseNotesService.fetchEditableVersion()
            Console.success("Found editable version: $appVersion")
            
            // Load matching local release notes file
            val localNotes = ConfigLoader.loadReleaseNotes("$dir/metadata/release_notes", appVersion)
            if (localNotes.isEmpty()) {
                Console.error("No local release notes file found: metadata/release_notes/$appVersion.json")
                Console.info("Please create this file with your release notes content.")
                return@runBlocking
            }
            
            val useCase = SyncReleaseNotesUseCase(
                releaseNotesService = releaseNotesService,
                localNotes = localNotes
            )
            
            useCase.execute(appVersion = appVersion, dryRun = !apply)
            
        } catch (e: Exception) {
            echo("❌ Error: ${e.message}", err = true)
        }
    }

    private fun Platform.displayName(): String = when (this) {
        Platform.APP_STORE -> "App Store Connect"
        Platform.PLAY_STORE -> "Google Play production release"
    }
}
