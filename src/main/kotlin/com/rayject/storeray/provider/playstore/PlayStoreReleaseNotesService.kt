package com.rayject.storeray.provider.playstore

import com.google.api.services.androidpublisher.model.LocalizedText
import com.google.api.services.androidpublisher.model.Track
import com.google.api.services.androidpublisher.model.TrackRelease
import com.rayject.storeray.provider.ReleaseNotesService
import com.rayject.storeray.provider.playstore.api.PlayStorePublisherApi
import com.rayject.storeray.util.Console

class PlayStoreReleaseNotesService(
    private val api: PlayStorePublisherApi
) : ReleaseNotesService {

    override suspend fun fetchEditableVersion(): String {
        val track = api.fetchProductionTrack()
        val release = selectSingleEditableRelease(track)
        val versionName = parseVersionName(release.name)

        Console.detail("Selected Play Store production release: ${release.name} (${release.status})")
        return versionName
    }

    override suspend fun fetch(appVersion: String): Map<String, String> {
        val track = api.fetchProductionTrack()
        val release = selectDraftReleaseByVersion(track, appVersion)
        val listingLocales = api.fetchListingLocales()

        return release.releaseNotes
            .orEmpty()
            .mapNotNull { note ->
                val language = note.language
                if (language.isNullOrBlank() || language !in listingLocales) {
                    null
                } else {
                    PlayStoreLocaleMapper.toAppStoreLocale(language) to (note.text ?: "")
                }
            }
            .groupingBy { it.first }
            .aggregate { _, accumulator: String?, element, _ ->
                accumulator ?: element.second
            }
            .mapValues { it.value.orEmpty() }
    }

    override suspend fun fetchSupportedLocales(appVersion: String): Set<String> {
        return api.fetchListingLocales()
            .map { PlayStoreLocaleMapper.toAppStoreLocale(it) }
            .toSet()
    }

    override suspend fun fetchUnsupportedLocales(appVersion: String): Set<String> {
        val track = api.fetchProductionTrack()
        val release = selectDraftReleaseByVersion(track, appVersion)
        val listingLocales = api.fetchListingLocales()

        return release.releaseNotes
            .orEmpty()
            .mapNotNull { it.language?.takeIf { language -> language.isNotBlank() } }
            .filter { it !in listingLocales }
            .toSet()
    }

    override fun targetLocalesFor(localLocale: String): List<String> {
        return PlayStoreLocaleMapper.toPlayStoreLocales(localLocale)
    }

    override suspend fun update(appVersion: String, notes: Map<String, String>) {
        val listingLocales = api.fetchListingLocales()

        api.updateProductionTrack { track ->
            val release = selectDraftReleaseByVersion(track, appVersion)
            val existingNotes = release.releaseNotes.orEmpty()
            val playStoreNotes = notes.flatMap { (locale, text) ->
                PlayStoreLocaleMapper.toPlayStoreLocales(locale).map { playStoreLocale ->
                    playStoreLocale to text
                }
            }.toMap()
            val localLanguages = playStoreNotes.keys

            val mergedNotes = mutableListOf<LocalizedText>()
            val existingLanguages = mutableSetOf<String>()

            for (existing in existingNotes) {
                val language = existing.language
                if (language.isNullOrBlank() || language !in listingLocales) {
                    continue
                }

                existingLanguages.add(language)
                val text = if (language in localLanguages) playStoreNotes.getValue(language).trim() else existing.text
                mergedNotes.add(LocalizedText().setLanguage(language).setText(text))
            }

            for ((language, text) in playStoreNotes) {
                if (language !in existingLanguages) {
                    mergedNotes.add(LocalizedText().setLanguage(language).setText(text.trim()))
                }
            }

            release.releaseNotes = mergedNotes
        }
    }

    private fun selectSingleEditableRelease(track: Track): TrackRelease {
        val editableReleases = track.releases
            .orEmpty()
            .filter { it.status in EDITABLE_STATUSES }
            .sortedBy { EDITABLE_STATUSES.indexOf(it.status) }

        return when (editableReleases.size) {
            0 -> throw RuntimeException("No draft or completed release found in Google Play production track.")
            1 -> editableReleases.first()
            else -> {
                val draftReleases = editableReleases.filter { it.status == DRAFT_STATUS }
                if (draftReleases.size == 1) {
                    return draftReleases.first()
                }

                val names = editableReleases.joinToString(", ") { "${it.name ?: "(unnamed)"} (${it.status ?: "unknown"})" }
                throw RuntimeException("Multiple editable releases found in Google Play production track: $names")
            }
        }
    }

    private fun selectDraftReleaseByVersion(track: Track, appVersion: String): TrackRelease {
        val matches = track.releases
            .orEmpty()
            .filter { it.status in EDITABLE_STATUSES }
            .filter { parseVersionNameOrNull(it.name) == appVersion }
            .sortedBy { EDITABLE_STATUSES.indexOf(it.status) }

        return when (matches.size) {
            0 -> throw RuntimeException("No draft or completed release matching version $appVersion found in Google Play production track.")
            1 -> matches.first()
            else -> {
                val draftMatches = matches.filter { it.status == DRAFT_STATUS }
                if (draftMatches.size == 1) {
                    return draftMatches.first()
                }

                val names = matches.joinToString(", ") { "${it.name ?: "(unnamed)"} (${it.status ?: "unknown"})" }
                throw RuntimeException("Multiple editable releases match version $appVersion in Google Play production track: $names")
            }
        }
    }

    private fun parseVersionName(releaseName: String?): String {
        return parseVersionNameOrNull(releaseName)
            ?: throw RuntimeException("Cannot read version name from Google Play draft release name: ${releaseName ?: "(empty)"}")
    }

    private fun parseVersionNameOrNull(releaseName: String?): String? {
        val name = releaseName?.trim()
        if (name.isNullOrEmpty()) {
            return null
        }

        val match = RELEASE_NAME_REGEX.matchEntire(name)
        if (match != null) {
            return match.groupValues[1].trim()
        }

        Console.warning("Google Play release name does not match version_code(version_name): $name")
        return name
    }

    private companion object {
        const val DRAFT_STATUS = "draft"
        const val COMPLETED_STATUS = "completed"
        val EDITABLE_STATUSES = listOf(DRAFT_STATUS, COMPLETED_STATUS)
        val RELEASE_NAME_REGEX = Regex("""^\s*\d+\s*\((.+)\)\s*$""")
    }
}
