package io.github.takusan23.kaisendonmk2.MisskeyDataClass

import java.io.Serializable

/**
 * Misskeyの通知データクラス
 * */
data class MisskeyNotificationData(
    val type: String,
    val createdAt: String,
    val id: String,
    val note: MisskeyNoteData? = null, // フォロー通知ならnull
    val reaction: String,
    val user: MisskeyUserData
) : Serializable