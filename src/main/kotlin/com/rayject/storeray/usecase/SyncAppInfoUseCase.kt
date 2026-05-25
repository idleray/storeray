package com.rayject.storeray.usecase

import com.rayject.storeray.model.AppInfoData
import com.rayject.storeray.provider.AppInfoService
import com.rayject.storeray.util.Console

class SyncAppInfoUseCase(
    private val appInfoService: AppInfoService,
    private val localData: Map<String, AppInfoData>,
    private val supportedFields: Set<String> = AppInfoService.ALL_FIELDS
) {
    private val fieldLabels = mapOf(
        "name" to "Name",
        "subtitle" to "Subtitle",
        "keywords" to "Keywords",
        "description" to "Description",
        "promotionalText" to "Promotional Text",
        "supportUrl" to "Support URL",
        "marketingUrl" to "Marketing URL",
        "privacyPolicyUrl" to "Privacy Policy URL",
        "video" to "Video"
    )

    suspend fun execute(dryRun: Boolean, preChangedLocales: Set<String> = emptySet()): Set<String> {
        Console.step(if (dryRun) "Dry-run mode (no changes will be applied)" else "Executing sync")

        if (localData.isEmpty()) {
            Console.error("No local app info data found in the workspace.")
            Console.info("Use `storeray appinfo fetch` to download data first.")
            return emptySet()
        }

        try {
            Console.info("Fetching remote app info data...")
            val remoteData = appInfoService.fetch()

            val changedLocales = mutableMapOf<String, AppInfoData>()
            var addCount = 0
            var updateCount = 0
            var skipCount = 0

            for ((locale, local) in localData.entries.sortedBy { it.key }) {
                val remote = remoteData[locale]
                if (remote == null) {
                    Console.detail("[+] $locale: New locale, will be created")
                    changedLocales[locale] = local
                    addCount++
                } else {
                    val displayDiffs = diffFields(local, remote)
                    val inheritedChange = locale in preChangedLocales
                    if (displayDiffs.isEmpty() && !inheritedChange) {
                        Console.detail("[=] $locale: Up to date")
                        skipCount++
                    } else {
                        Console.detail("[~] $locale: Needs update")
                        for ((field, oldVal, newVal) in displayDiffs) {
                            val label = fieldLabels[field] ?: field
                            Console.detail("    $label: \"${truncate(oldVal)}\" → \"${truncate(newVal)}\"")
                        }
                        changedLocales[locale] = local
                        updateCount++
                    }
                }
            }

            val missingLocales = remoteData.keys - localData.keys
            if (missingLocales.isNotEmpty()) {
                Console.divider()
                Console.warning("Store has ${missingLocales.size} locale(s) not present locally (will be kept unchanged):")
                for (locale in missingLocales.sorted()) {
                    Console.detail("[-] $locale: Skipped (no local data)")
                }
            }

            Console.divider()
            if (addCount + updateCount == 0) {
                Console.success("All app info is already up to date")
                return emptySet()
            }

            Console.info("Summary: $addCount to create, $updateCount to update, $skipCount up to date")

            if (!dryRun) {
                try {
                    appInfoService.update(changedLocales)
                    Console.success("App info updated successfully!")
                } catch (e: Exception) {
                    Console.error("Update failed: ${e.message}")
                }
            } else {
                Console.info("💡 Hint: Use the --apply flag to execute the actual sync")
            }

            return changedLocales.keys + preChangedLocales

        } catch (e: Exception) {
            Console.error("An error occurred during sync: ${e.message}")
            return emptySet()
        }
    }

    private fun diffFields(local: AppInfoData, remote: AppInfoData): List<Triple<String, String, String>> {
        val diffs = mutableListOf<Triple<String, String, String>>()
        if ("name" in supportedFields && local.name != remote.name) diffs.add(Triple("name", remote.name, local.name))
        if ("subtitle" in supportedFields && local.subtitle != remote.subtitle) diffs.add(Triple("subtitle", remote.subtitle, local.subtitle))
        if ("keywords" in supportedFields && local.keywords != remote.keywords) diffs.add(Triple("keywords", remote.keywords, local.keywords))
        if ("description" in supportedFields && local.description != remote.description) diffs.add(Triple("description", remote.description, local.description))
        if ("promotionalText" in supportedFields && local.promotionalText != remote.promotionalText) diffs.add(Triple("promotionalText", remote.promotionalText, local.promotionalText))
        if ("supportUrl" in supportedFields && local.supportUrl != remote.supportUrl) diffs.add(Triple("supportUrl", remote.supportUrl, local.supportUrl))
        if ("marketingUrl" in supportedFields && local.marketingUrl != remote.marketingUrl) diffs.add(Triple("marketingUrl", remote.marketingUrl, local.marketingUrl))
        if ("privacyPolicyUrl" in supportedFields && local.privacyPolicyUrl != remote.privacyPolicyUrl) diffs.add(Triple("privacyPolicyUrl", remote.privacyPolicyUrl, local.privacyPolicyUrl))
        if ("video" in supportedFields && local.video != remote.video) diffs.add(Triple("video", remote.video, local.video))
        return diffs
    }

    private fun truncate(s: String): String = if (s.length > 60) s.take(57) + "..." else s
}
