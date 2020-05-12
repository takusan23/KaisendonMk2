package io.github.takusan23.kaisendonmk2.DataClass

import io.github.takusan23.kaisendonmk2.API.InstanceToken

// 通知のデータクラス
data class NotificationData(
    val instanceToken: InstanceToken,
    val createdAt: String,
    val notificationId: String,
    val accountData: AccountData,
    val status: StatusData?, // フォロー通知の場合はnullです。
    val type: String
)