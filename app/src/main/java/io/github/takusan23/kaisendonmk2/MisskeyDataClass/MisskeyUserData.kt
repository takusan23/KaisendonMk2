package io.github.takusan23.kaisendonmk2.MisskeyDataClass

import io.github.takusan23.kaisendonmk2.DataClass.EmojiData
import io.github.takusan23.kaisendonmk2.MastodonAPI.InstanceToken
import java.io.Serializable

/**
 * Misskeyのユーザのデータクラス
 * */
data class MisskeyUserData(
    val instanceToken: InstanceToken,
    val name: String,
    val username: String,
    val isAdmin: Boolean,
    val id: String,
    val emoji: ArrayList<EmojiData>,
    val avatarUrl: String,
    val bannerUrl: String? = null // 無いときある？
): Serializable