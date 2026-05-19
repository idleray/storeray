package com.rayject.storeray.cli.releasenotes

import com.github.ajalt.clikt.core.CliktCommand
import com.rayject.storeray.cli.GlobalState
import com.rayject.storeray.config.ConfigLoader
import com.rayject.storeray.provider.playstore.api.PlayStorePublisherApi
import com.rayject.storeray.util.Console
import kotlinx.coroutines.runBlocking

class PlayStoreTracksCommand : CliktCommand(
    name = "playstore-tracks"
) {
    override fun help(context: com.github.ajalt.clikt.core.Context) = "Print Google Play tracks and releases"

    override fun run() = runBlocking {
        try {
            val dir = GlobalState.workspaceDir
            val config = ConfigLoader.loadPlayStoreConfig("$dir/storeray.json")
            val api = PlayStorePublisherApi(
                serviceAccountJsonPath = config.serviceAccountJsonPath,
                packageName = config.packageName
            )

            Console.info("Fetching Google Play tracks for ${config.packageName}...")
            val listingLocales = api.fetchListingLocales().sorted()
            val tracks = api.fetchTracks()

            Console.step("Listing locales")
            if (listingLocales.isEmpty()) {
                Console.detail("(No listings)")
            } else {
                Console.detail(listingLocales.joinToString(", "))
            }

            if (tracks.isEmpty()) {
                Console.warning("No tracks found.")
                return@runBlocking
            }

            for (track in tracks) {
                Console.step("Track: ${track.track ?: "(unknown)"}")
                val releases = track.releases.orEmpty()

                if (releases.isEmpty()) {
                    Console.detail("(No releases)")
                    continue
                }

                for ((index, release) in releases.withIndex()) {
                    Console.info("Release #${index + 1}")
                    Console.detail("Name: ${release.name ?: "(empty)"}")
                    Console.detail("Status: ${release.status ?: "(empty)"}")
                    Console.detail("Version codes: ${release.versionCodes?.joinToString(", ") ?: "(empty)"}")
                    Console.detail("User fraction: ${release.userFraction?.toString() ?: "(empty)"}")
                    Console.detail("Release notes languages: ${release.releaseNotes?.mapNotNull { it.language }?.joinToString(", ") ?: "(empty)"}")
                }
            }
        } catch (e: Exception) {
            Console.error("Execution failed: ${e.message}")
        }
    }
}
