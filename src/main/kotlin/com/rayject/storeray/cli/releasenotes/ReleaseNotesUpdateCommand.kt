package com.rayject.storeray.cli.releasenotes

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.rayject.storeray.config.ConfigLoader
import com.rayject.storeray.provider.Platform
import com.rayject.storeray.provider.StoreProviderFactory
import com.rayject.storeray.usecase.SyncReleaseNotesUseCase
import kotlinx.coroutines.runBlocking

class ReleaseNotesUpdateCommand : CliktCommand(
    name = "update"
) {
    override fun help(context: com.github.ajalt.clikt.core.Context) = "Update Release Notes for a specific version"

    private val appVersion by argument(help = "The version number on the store (e.g., 1.2.0)")

    private val apply by option("--apply", help = "Apply changes to the store (default: dry-run)").flag(default = false)
    
    private val platform by option("--platform", help = "Target store platform (appstore, playstore)").default("appstore")
    
    override fun run() = runBlocking {
        try {
            val dir = com.rayject.storeray.cli.GlobalState.workspaceDir
            val storeConfig = ConfigLoader.loadStoreConfig("$dir/storeray.json")
            val localNotes = ConfigLoader.loadReleaseNotes("$dir/metadata/release_notes", appVersion)
            
            val platformEnum = when (platform.lowercase()) {
                "appstore" -> Platform.APP_STORE
                "playstore" -> throw UnsupportedOperationException("暂不支持 Play Store")
                else -> throw IllegalArgumentException("未知的平台: $platform")
            }
            
            val provider = StoreProviderFactory.create(platformEnum, storeConfig)
            val useCase = SyncReleaseNotesUseCase(
                releaseNotesService = provider.releaseNotes(),
                localNotes = localNotes
            )
            
            useCase.execute(appVersion = appVersion, dryRun = !apply)
            
        } catch (e: Exception) {
            echo("❌ Error: ${e.message}", err = true)
        }
    }
}
