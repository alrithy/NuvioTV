package com.nuvio.tv.updater

import com.nuvio.tv.fork.distribution.ReleaseDigest
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApkDownloader @Inject constructor(
    private val okHttpClient: OkHttpClient
) {

    suspend fun download(
        url: String,
        destinationFile: File,
        expectedSha256: String?,
        onProgress: (downloadedBytes: Long, totalBytes: Long?) -> Unit
    ): Result<File> {
        return runCatching {
            destinationFile.parentFile?.mkdirs()
            if (destinationFile.exists()) destinationFile.delete()

            val request = Request.Builder()
                .url(url)
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    error("Download failed: HTTP ${response.code}")
                }

                val body = response.body ?: error("Empty download body")
                val total = body.contentLength().takeIf { it > 0 }
                val sha256 = ReleaseDigest.newDigest()

                body.byteStream().use { input ->
                    FileOutputStream(destinationFile).use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        var downloaded = 0L
                        while (true) {
                            val read = input.read(buffer)
                            if (read <= 0) break
                            output.write(buffer, 0, read)
                            sha256.update(buffer, 0, read)
                            downloaded += read
                            onProgress(downloaded, total)
                        }
                        output.flush()
                    }
                }
                // G14b (D064): nothing unverified reaches the installer.
                if (!ReleaseDigest.verified(expectedSha256, sha256.digest())) {
                    destinationFile.delete()
                    throw ReleaseDigest.UnverifiedUpdate()
                }
            }

            destinationFile
        }
    }
}
