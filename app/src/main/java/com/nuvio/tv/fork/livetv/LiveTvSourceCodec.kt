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

/**
 * The saved form of a profile's [LiveTvLibrary] (G10b): channel keys as hex, category names as
 * given. No stream link is part of it, so it is stored as plain JSON.
 */
internal object LiveTvLibraryCodec {

    fun encode(library: LiveTvLibrary): String {
        val buffer = Buffer()
        JsonWriter.of(buffer).use { writer ->
            writer.beginObject()
            writer.name("favorites").keys(library.favorites)
            writer.name("hiddenGroups").strings(library.hiddenGroups)
            writer.name("groupOrder").strings(library.groupOrder)
            writer.name("groupNames").beginObject()
            library.groupNames.forEach { (group, name) -> writer.name(group).value(name) }
            writer.endObject()
            writer.name("hiddenChannels").keys(library.hiddenChannels)
            library.recent?.let { recent ->
                writer.name("recent").beginObject()
                writer.name("key").value(recent.key.toULong().toString(16))
                writer.name("name").value(recent.name)
                recent.logoUrl?.let { writer.name("logo").value(it) }
                writer.name("group").value(recent.group)
                recent.tvgId?.let { writer.name("tvgId").value(it) }
                writer.endObject()
            }
            writer.endObject()
        }
        return buffer.readUtf8()
    }

    /** The saved library; unreadable text is an empty one. */
    fun decode(text: String): LiveTvLibrary {
        if (text.isBlank()) return LiveTvLibrary()
        return runCatching {
            val reader = JsonReader.of(Buffer().writeUtf8(text))
            var library = LiveTvLibrary()
            reader.beginObject()
            while (reader.hasNext()) {
                library = when (reader.nextName()) {
                    "favorites" -> library.copy(favorites = reader.keys())
                    "hiddenGroups" -> library.copy(hiddenGroups = reader.strings().toHashSet())
                    "groupOrder" -> library.copy(groupOrder = reader.strings())
                    "groupNames" -> library.copy(groupNames = reader.stringMap())
                    "hiddenChannels" -> library.copy(hiddenChannels = reader.keys())
                    "recent" -> library.copy(recent = reader.recent())
                    else -> library.also { reader.skipValue() }
                }
            }
            reader.endObject()
            library
        }.getOrDefault(LiveTvLibrary())
    }

    private fun JsonWriter.keys(keys: Collection<Long>): JsonWriter {
        beginArray()
        keys.forEach { value(it.toULong().toString(16)) }
        return endArray()
    }

    private fun JsonWriter.strings(values: Collection<String>): JsonWriter {
        beginArray()
        values.forEach { value(it) }
        return endArray()
    }

    private fun JsonReader.strings(): List<String> {
        val values = ArrayList<String>()
        beginArray()
        while (hasNext()) if (peek() == JsonReader.Token.STRING) values += nextString() else skipValue()
        endArray()
        return values
    }

    private fun JsonReader.keys(): Set<Long> = strings().mapNotNullTo(HashSet()) { it.toULongOrNull(16)?.toLong() }

    private fun JsonReader.stringMap(): Map<String, String> {
        val values = HashMap<String, String>()
        beginObject()
        while (hasNext()) {
            val name = nextName()
            if (peek() == JsonReader.Token.STRING) nextString().takeIf(String::isNotBlank)?.let { values[name] = it } else skipValue()
        }
        endObject()
        return values
    }

    private fun JsonReader.recent(): LiveTvRecentChannel? {
        val fields = HashMap<String, String>()
        beginObject()
        while (hasNext()) {
            val name = nextName()
            if (peek() == JsonReader.Token.STRING) fields[name] = nextString() else skipValue()
        }
        endObject()
        val key = fields["key"]?.toULongOrNull(16)?.toLong() ?: return null
        val name = fields["name"]?.takeIf(String::isNotBlank) ?: return null
        return LiveTvRecentChannel(key, name, fields["logo"], fields["group"].orEmpty(), fields["tvgId"])
    }
}
