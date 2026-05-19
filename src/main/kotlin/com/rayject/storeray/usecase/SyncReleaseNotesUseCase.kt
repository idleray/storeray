package com.rayject.storeray.usecase

import com.rayject.storeray.provider.ReleaseNotesService
import com.rayject.storeray.util.Console

class SyncReleaseNotesUseCase(
    private val releaseNotesService: ReleaseNotesService,
    private val localNotes: Map<String, String>
) {

    suspend fun execute(appVersion: String, dryRun: Boolean) {
        Console.step(if (dryRun) "Dry-run mode (no changes will be applied to Release Notes)" else "Executing Release Notes sync")
        
        if (localNotes.isEmpty()) {
            Console.error("No Release Notes found in the workspace for version $appVersion.")
            return
        }

        try {
            Console.info("Fetching remote Release Notes for version $appVersion...")
            val existingNotes = releaseNotesService.fetch(appVersion)
            val supportedLocales = releaseNotesService.fetchSupportedLocales(appVersion)
            val unsupportedStoreLocales = releaseNotesService.fetchUnsupportedLocales(appVersion).sorted()

            val missingLocalLocales = supportedLocales
                .filter { it !in localNotes.keys }
                .sorted()

            if (missingLocalLocales.isNotEmpty()) {
                Console.warning("Store has ${missingLocalLocales.size} locale(s) missing from local release notes:")
                missingLocalLocales.forEach { locale ->
                    Console.detail("[!] $locale: Missing locally; remote text will be kept unchanged")
                }
                Console.divider()
            }

            if (unsupportedStoreLocales.isNotEmpty()) {
                Console.warning("Store has ${unsupportedStoreLocales.size} unsupported release-note locale(s):")
                unsupportedStoreLocales.forEach { locale ->
                    Console.detail("[-] $locale: Unsupported by store listings; will be removed on apply")
                }
                Console.divider()
            }
            
            val localNotesToUpdate = mutableMapOf<String, String>()
            
            for ((locale, newContentRaw) in localNotes) {
                val newContent = newContentRaw.trim()
                val oldContent = existingNotes[locale]?.trim() ?: ""
                
                if (newContent != oldContent) {
                    localNotesToUpdate[locale] = newContent
                    Console.detail("[~] ${locale.displayWithTargets()}: Needs update")
                    Console.detail("    Old: \"$oldContent\"")
                    Console.detail("    New: \"$newContent\"")
                } else {
                    Console.detail("[=] ${locale.displayWithTargets()}: Up to date")
                }
            }
            
            Console.divider()
            
            if (localNotesToUpdate.isEmpty() && unsupportedStoreLocales.isEmpty()) {
                Console.success("All Release Notes are already up to date")
                return
            }
            
            val targetUpdateCount = localNotesToUpdate.keys.sumOf { locale ->
                releaseNotesService.targetLocalesFor(locale).size
            }
            val totalMessage = if (targetUpdateCount == localNotesToUpdate.size) {
                "Total of ${localNotesToUpdate.size} languages need updating"
            } else {
                "Total of ${localNotesToUpdate.size} local languages / $targetUpdateCount store locales need updating"
            }
            Console.info(totalMessage)
            if (unsupportedStoreLocales.isNotEmpty()) {
                Console.info("Total of ${unsupportedStoreLocales.size} unsupported store locale(s) need cleanup")
            }
            
            if (!dryRun) {
                try {
                    releaseNotesService.update(appVersion, localNotesToUpdate)
                    Console.success("Release Notes updated successfully!")
                } catch (e: Exception) {
                    Console.error("Update failed: ${e.message}")
                }
            } else {
                Console.info("💡 Hint: Use the --apply flag to execute the actual sync")
            }
            
        } catch (e: Exception) {
            Console.error("An error occurred during sync: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun String.displayWithTargets(): String {
        val targets = releaseNotesService.targetLocalesFor(this)
        return if (targets.size == 1 && targets.first() == this) {
            this
        } else {
            "$this -> ${targets.joinToString(", ")}"
        }
    }
}
