package com.nuvio.tv.fork.livetv

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.nuvio.tv.data.local.ProfileDataStoreFactory
import com.nuvio.tv.fork.foundation.KeystoreCipher
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Live TV sources per profile (G10a, D055). Reshaped keeps them in plain SharedPreferences; here the
 * whole source list is one AES-GCM value ([KeystoreCipher], Android Keystore key) in the profile's
 * `fork_live_tv` DataStore, so a backed-up or copied settings file carries only ciphertext. An
 * imported playlist is an app-private file per profile and source, because it can be megabytes.
 * The profile's choices (favorites, categories, hidden channels, last channel) and the menu switch
 * sit beside them (G10b); they hold channel keys and names, never a link.
 */
@Singleton
class LiveTvStorage @Inject constructor(
    @ApplicationContext private val context: Context,
    private val factory: ProfileDataStoreFactory,
) : LiveTvSourceStore, LiveTvLibraryStore {

    private val cipher = KeystoreCipher(KEY_ALIAS)
    private val sourcesKey = stringPreferencesKey("sources_encrypted")
    private val libraryKey = stringPreferencesKey("library")
    private val menuKey = booleanPreferencesKey("menu_enabled")
    private val previewsKey = booleanPreferencesKey("previews_enabled")
    private val previewSoundKey = booleanPreferencesKey("preview_sound")

    private fun store(profileId: Int) = factory.get(profileId, FEATURE)

    override suspend fun sources(profileId: Int): List<LiveTvSource> = withContext(Dispatchers.IO) {
        LiveTvSourceCodec.decode(cipher.decryptOrEmpty(store(profileId).data.first()[sourcesKey]))
    }

    override suspend fun saveSources(profileId: Int, sources: List<LiveTvSource>) {
        withContext(Dispatchers.IO) {
            val encrypted = if (sources.isEmpty()) null else cipher.encrypt(LiveTvSourceCodec.encode(sources))
            store(profileId).edit { prefs ->
                if (encrypted == null) prefs.remove(sourcesKey) else prefs[sourcesKey] = encrypted
            }
        }
    }

    override suspend fun library(profileId: Int): LiveTvLibrary = withContext(Dispatchers.IO) {
        LiveTvLibraryCodec.decode(store(profileId).data.first()[libraryKey].orEmpty())
    }

    override suspend fun saveLibrary(profileId: Int, library: LiveTvLibrary) {
        withContext(Dispatchers.IO) {
            val text = LiveTvLibraryCodec.encode(library)
            store(profileId).edit { it[libraryKey] = text }
        }
    }

    override fun menuEnabled(profileId: Int): Flow<Boolean> =
        store(profileId).data.map { it[menuKey] == true }.catch { emit(false) }.distinctUntilChanged()

    override suspend fun setMenuEnabled(profileId: Int, enabled: Boolean) {
        store(profileId).edit { it[menuKey] = enabled }
    }

    override fun previewChoice(profileId: Int): Flow<LiveTvPreviewChoice> =
        store(profileId).data
            .map { LiveTvPreviewChoice(previews = it[previewsKey], sound = it[previewSoundKey] ?: true) }
            .catch { emit(LiveTvPreviewChoice()) }
            .distinctUntilChanged()

    override suspend fun setPreviewChoice(profileId: Int, choice: LiveTvPreviewChoice) {
        store(profileId).edit { prefs ->
            val previews = choice.previews
            if (previews == null) prefs.remove(previewsKey) else prefs[previewsKey] = previews
            prefs[previewSoundKey] = choice.sound
        }
    }

    private fun playlistDir(profileId: Int) = File(context.filesDir, "live_tv/profile_$profileId")

    private fun playlistFile(profileId: Int, sourceId: String) = File(playlistDir(profileId), "playlist_$sourceId.m3u")

    override suspend fun <T> readPlaylist(profileId: Int, sourceId: String, block: (Sequence<String>) -> T): T? =
        withContext(Dispatchers.IO) {
            val file = playlistFile(profileId, sourceId).takeIf { it.isFile && it.length() > 0L }
            file?.bufferedReader()?.useLines(block)
        }

    override suspend fun savePlaylist(profileId: Int, sourceId: String, input: InputStream, maxBytes: Long): Boolean =
        withContext(Dispatchers.IO) {
            val target = playlistFile(profileId, sourceId)
            target.parentFile?.mkdirs()
            val temp = File(target.path + ".tmp")
            try {
                var total = 0L
                temp.outputStream().use { out ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        total += read
                        if (total > maxBytes) return@withContext false
                        out.write(buffer, 0, read)
                    }
                }
                total > 0L && (temp.renameTo(target) || (target.delete() && temp.renameTo(target)))
            } finally {
                temp.delete()
            }
        }

    override suspend fun deletePlaylist(profileId: Int, sourceId: String) {
        withContext(Dispatchers.IO) { playlistFile(profileId, sourceId).delete() }
    }

    private companion object {
        const val FEATURE = "fork_live_tv"
        const val KEY_ALIAS = "com.nuvio.tv.livetv.sources.v1"
    }
}
