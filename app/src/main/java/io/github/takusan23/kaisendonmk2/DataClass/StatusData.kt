package io.github.takusan23.kaisendonmk2.DataClass

import io.github.takusan23.kaisendonmk2.API.InstanceToken

// タイムラインのデータ
data class StatusData(
    val instanceToken: InstanceToken,
    val id: String,
    val createdAt: String,
    val visibility: String,
    val url: String,
    val favouritesCount: Int,
    var isFavourited: Boolean, // ふぁぼったら反転させるためこれは var
    val boostCount: Int,
    var isBoosted: Boolean,
    val accountData: AccountData,
    val content: String,
    val mediaAttachments: ArrayList<String>,
    val card: CardData?,
    val allEmoji: ArrayList<EmojiData>
)