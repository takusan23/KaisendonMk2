package io.github.takusan23.kaisendonmk2.DataClass

import io.github.takusan23.kaisendonmk2.API.InstanceToken

// アカウント関係
data class AccountData(
    val instanceToken: InstanceToken,
    val accountId: String,
    val userName: String,
    val displayName: String,
    val acct: String,
    val createdAt: String,
    val note: String,
    val avatar: String,
    val avatarStatic: String,
    val header: String,
    val headerStatic: String,
    val followersCount: Int,
    val followingCount: Int,
    val statusCount: Int,
    val lastStatusAt: String,
    val fields: ArrayList<FieldsData>,
    val allEmoji: ArrayList<EmojiData>
)