package com.nuvio.tv.fork.livetv

import android.content.Context
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * Live TV sources per profile (G10a, D055). Reshaped keeps them in plain SharedPreferences; here the
 * whole source list is one AES-GCM value ([KeystoreCipher], Android Keystore key) in the profile's
 * `fork_live_tv` DataStore, so a backed-up or copied settings file carries only ciphertext. An
 * imported playlist is an app-private file per profile and source, because it can be megabytes.
 */
@Singleton
class LiveTvStorage @Inject constructor(
    @ApplicationContext private val context: Context,
    private val factory: ProfileDataStoreFactory,
) : LiveTvSourceStore {

    private val cipher = KeystoreCipher(KEY_ALIAS)
    private val sourcesKey = stringPreferencesKey("sources_encrypted")

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
