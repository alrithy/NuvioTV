package com.nuvio.tv.fork.livetv

import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import okio.Buffer

/**
 * The saved form of a profile's sources (Reshaped `LiveTvStorage.toJson` / `toSource` @ 0ccf049).
 * The whole text is encrypted before it is stored ([LiveTvStorage]): links, users, passwords and
 * MAC addresses are all account details.
 */
internal object LiveTvSourceCodec {

    fun encode(sources: List<LiveTvSource>): String {
        val buffer = Buffer()
        JsonWriter.of(buffer).use { writer ->
            writer.beginArray()
            sources.forEach { source ->
                writer.beginObject()
                writer.name("id").value(source.id)
                writer.name("type").value(source.type.name)
                writer.name("url").value(source.url)
                when (source.type) {
                    LiveTvSourceType.M3u -> Unit
                    LiveTvSourceType.Stalker -> {
                        writer.name("portal").value(source.stalker.portalUrl)
                        writer.name("mac").value(source.stalker.macAddress)
                        writer.name("stalkerUser").value(source.stalker.username)
                        writer.name("stalkerPassword").value(source.stalker.password)
                    }
                    LiveTvSourceType.Xtream -> {
                        writer.name("server").value(source.xtream.serverUrl)
                        writer.name("xtreamUser").value(source.xtream.username)
                        writer.name("xtreamPassword").value(source.xtream.password)
                    }
                }
                writer.endObject()
            }
            writer.endArray()
        }
        return buffer.readUtf8()
    }

    /** The saved sources; entries it cannot read are skipped, unreadable text is no sources. */
    fun decode(text: String): List<LiveTvSource> {
        if (text.isBlank()) return emptyList()
        return runCatching {
            val reader = JsonReader.of(Buffer().writeUtf8(text))
            val sources = ArrayList<LiveTvSource>()
            reader.beginArray()
            while (reader.hasNext()) {
                val fields = HashMap<String, String>()
                reader.beginObject()
                while (reader.hasNext()) {
                    val name = reader.nextName()
                    if (reader.peek() == JsonReader.Token.STRING) fields[name] = reader.nextString() else reader.skipValue()
                }
                reader.endObject()
                fields.toSource()?.let(sources::add)
            }
            reader.endArray()
            sources
        }.getOrDefault(emptyList())
    }

    private fun Map<String, String>.toSource(): LiveTvSource? {
        val id = this["id"]?.takeIf(String::isNotBlank) ?: return null
        val type = LiveTvSourceType.entries.firstOrNull { it.name == this["type"] } ?: return null
        return LiveTvSource(
            id = id,
            type = type,
            url = this["url"].orEmpty(),
            stalker = LiveTvStalkerSettings(
                this["portal"].orEmpty(), this["mac"].orEmpty(), this["stalkerUser"].orEmpty(), this["stalkerPassword"].orEmpty(),
            ),
            xtream = LiveTvXtreamSettings(this["server"].orEmpty(), this["xtreamUser"].orEmpty(), this["xtreamPassword"].orEmpty()),
        )
    }
}
