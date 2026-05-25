package com.rayject.storeray.provider.playstore.api

import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.androidpublisher.AndroidPublisher
import com.google.api.services.androidpublisher.AndroidPublisherScopes
import com.google.api.services.androidpublisher.model.Listing
import com.google.api.services.androidpublisher.model.Track
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.GoogleCredentials
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream

class PlayStorePublisherApi(
    serviceAccountJsonPath: String,
    private val packageName: String
) {
    private val publisher: AndroidPublisher

    init {
        val credentialsFile = File(serviceAccountJsonPath.replaceFirst("~", System.getProperty("user.home")))
        if (!credentialsFile.exists()) {
            throw IllegalArgumentException("Google Play service account JSON not found: ${credentialsFile.absolutePath}")
        }

        val credentials = FileInputStream(credentialsFile).use { input ->
            GoogleCredentials.fromStream(input)
                .createScoped(listOf(AndroidPublisherScopes.ANDROIDPUBLISHER))
        }

        publisher = AndroidPublisher.Builder(
            NetHttpTransport.Builder().build(),
            GsonFactory.getDefaultInstance(),
            HttpCredentialsAdapter(credentials)
        )
            .setApplicationName("storeray")
            .build()
    }

    suspend fun fetchProductionTrack(): Track = withContext(Dispatchers.IO) {
        withEdit(commit = false) { editId ->
            publisher.edits().tracks().get(packageName, editId, PRODUCTION_TRACK).execute()
        }
    }

    suspend fun fetchTracks(): List<Track> = withContext(Dispatchers.IO) {
        withEdit(commit = false) { editId ->
            publisher.edits().tracks().list(packageName, editId).execute().tracks.orEmpty()
        }
    }

    suspend fun fetchListings(): List<Listing> = withContext(Dispatchers.IO) {
        withEdit(commit = false) { editId ->
            publisher.edits().listings().list(packageName, editId)
                .execute()
                .listings
                .orEmpty()
        }
    }

    suspend fun fetchListingLocales(): Set<String> = withContext(Dispatchers.IO) {
        withEdit(commit = false) { editId ->
            publisher.edits().listings().list(packageName, editId)
                .execute()
                .listings
                .orEmpty()
                .mapNotNull { it.language?.takeIf { language -> language.isNotBlank() } }
                .toSet()
        }
    }

    suspend fun updateListing(locale: String, title: String, shortDescription: String, fullDescription: String, video: String) = withContext(Dispatchers.IO) {
        withEdit(commit = true) { editId ->
            val listing = Listing()
                .setLanguage(locale)
                .setTitle(title.ifBlank { null })
                .setShortDescription(shortDescription.ifBlank { null })
                .setFullDescription(fullDescription.ifBlank { null })
                .setVideo(video.ifBlank { null })
            publisher.edits().listings().update(packageName, editId, locale, listing).execute()
        }
    }

    suspend fun updateProductionTrack(update: (Track) -> Unit) = withContext(Dispatchers.IO) {
        withEdit(commit = true) { editId ->
            val track = publisher.edits().tracks().get(packageName, editId, PRODUCTION_TRACK).execute()
            update(track)
            publisher.edits().tracks().update(packageName, editId, PRODUCTION_TRACK, track).execute()
        }
    }

    private fun <T> withEdit(commit: Boolean, block: (String) -> T): T {
        val edit = publisher.edits().insert(packageName, null).execute()
        val editId = edit.id ?: throw RuntimeException("Failed to create Google Play edit")
        var committed = false

        try {
            val result = block(editId)
            if (commit) {
                publisher.edits().commit(packageName, editId).execute()
                committed = true
            }
            return result
        } finally {
            if (!committed) {
                runCatching {
                    publisher.edits().delete(packageName, editId).execute()
                }
            }
        }
    }

    private companion object {
        const val PRODUCTION_TRACK = "production"
    }
}
