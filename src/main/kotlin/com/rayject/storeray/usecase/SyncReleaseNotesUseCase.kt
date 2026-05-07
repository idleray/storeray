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
            
            val localNotesToUpdate = mutableMapOf<String, String>()
            
            for ((locale, newContentRaw) in localNotes) {
                val newContent = newContentRaw.trim()
                val oldContent = existingNotes[locale]?.trim() ?: ""
                
                if (newContent != oldContent) {
                    localNotesToUpdate[locale] = newContent
                    Console.detail("[~] $locale: Needs update")
                    Console.detail("    Old: \"$oldContent\"")
                    Console.detail("    New: \"$newContent\"")
                } else {
                    Console.detail("[=] $locale: Up to date")
                }
            }
            
            Console.divider()
            
            if (localNotesToUpdate.isEmpty()) {
                Console.success("All Release Notes are already up to date")
                return
            }
            
            Console.info("Total of ${localNotesToUpdate.size} languages need updating")
            
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
}
