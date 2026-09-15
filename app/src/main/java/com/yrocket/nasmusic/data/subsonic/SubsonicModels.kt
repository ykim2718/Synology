// app/src/main/java/com/yrocket/nasmusic/data/subsonic/SubsonicModels.kt
// Author: yRocket
package com.yrocket.nasmusic.data.subsonic

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Every Subsonic REST reply is wrapped in a single "subsonic-response" object. */
@Serializable
data class SubsonicEnvelope(
    @SerialName("subsonic-response") val response: SubsonicResponse,
)

/**
 * Union of the reply payloads this app asks for.
 *
 * The server fills exactly one payload field per endpoint, so the unused ones stay null.
 */
@Serializable
data class SubsonicResponse(
    val status: String,
    val version: String,
    val error: SubsonicError? = null,
    val artists: ArtistIndexes? = null,
    val artist: ArtistDetail? = null,
    val album: AlbumDetail? = null,
)

@Serializable
data class SubsonicError(
    val code: Int,
    val message: String = "",
)

@Serializable
data class ArtistIndexes(
    val index: List<ArtistIndex> = emptyList(),
)

@Serializable
data class ArtistIndex(
    val name: String,
    val artist: List<Artist> = emptyList(),
)

@Serializable
data class Artist(
    val id: String,
    val name: String,
    val albumCount: Int = 0,
    val coverArt: String? = null,
)

@Serializable
data class ArtistDetail(
    val id: String,
    val name: String,
    val album: List<Album> = emptyList(),
)

@Serializable
data class Album(
    val id: String,
    val name: String,
    val artist: String? = null,
    val year: Int? = null,
    val songCount: Int = 0,
    val coverArt: String? = null,
)

@Serializable
data class AlbumDetail(
    val id: String,
    val name: String,
    val artist: String? = null,
    val coverArt: String? = null,
    val song: List<Song> = emptyList(),
)

@Serializable
data class Song(
    val id: String,
    val title: String,
    val album: String? = null,
    val artist: String? = null,
    val track: Int? = null,
    val duration: Int? = null,
    val suffix: String? = null,
    val coverArt: String? = null,
)
