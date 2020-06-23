package io.github.takusan23.kaisendonmk2.DetaBase.Entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable
import java.util.*

/**
 * データベースのカラム定義
 * @param id 主キーです
 * @param instance インスタンス名
 * @param isEnable TLを読み込むか
 * @param isWiFiOnly Wi-Fi接続時のみ読み込むかどうか
 * @param name 名前。
 * @param service Mastodon / Misskey のどれか
 * @param token アクセストークン
 * @param labelColor ラベルの色
 * @param timeline 読み込むTL。home / notification / local / public など？
 * */
@Entity
data class CustomTimeLineDBEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "enable") var isEnable: Boolean,
    @ColumnInfo(name = "wifi_only") var isWiFiOnly: Boolean,
    @ColumnInfo(name = "name") val name: String?,
    @ColumnInfo(name = "service") var service: String,
    @ColumnInfo(name = "instance") val instance: String,
    @ColumnInfo(name = "token") val token: String,
    @ColumnInfo(name = "timeline") var timeline: String,
    @ColumnInfo(name = "color") var labelColor: String?
) : Serializable