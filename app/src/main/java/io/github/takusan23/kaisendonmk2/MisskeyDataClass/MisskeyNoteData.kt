package io.github.takusan23.kaisendonmk2.MisskeyDataClass

import io.github.takusan23.kaisendonmk2.DataClass.EmojiData
import io.github.takusan23.kaisendonmk2.MastodonAPI.InstanceToken
import java.io.Serializable

/**
 * MisskeyのNoteのデータクラス
 * */
data class MisskeyNoteData(
    val instanceToken: InstanceToken,
    val createdAt: String,
    val text: String,
    val isMobile: Boolean,
    val noteId: String,
    var renoteCount: Int,
    val reaction: ArrayList<MisskeyReactionData>,
    val emoji: ArrayList<EmojiData>,
    val user: MisskeyUserData,
    val renote: MisskeyNoteData? = null,
    var isRenote: Boolean = false, // APIにはない。
    var isReaction: Boolean = false // APIにはない。
) : Serializable