package io.github.takusan23.kaisendonmk2.MisskeyDataClass

import io.github.takusan23.kaisendonmk2.DataClass.EmojiData
import io.github.takusan23.kaisendonmk2.MastodonAPI.InstanceToken
import java.io.Serializable

/**
 * Misskeyの /api/users/show のデータクラス。タイムラインのではない。
 * */
data class MisskeyShowUserData(
    val instanceToken: InstanceToken,
    val name: String,
    val username: String,
    val isAdmin: Boolean,
    val id: String,
    val emoji: ArrayList<EmojiData>,
    val avatarUrl: String,
    val bannerUrl: String? = null, // 無いときある？
    val createdAt: String,
    val description: String,
    val followersCount: Int,
    val followingCount: Int,
    val notesCount: Int
) : Serializable