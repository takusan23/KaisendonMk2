package io.github.takusan23.kaisendonmk2.DataClass

import io.github.takusan23.kaisendonmk2.MastodonAPI.InstanceToken
import java.io.Serializable

/**
 * 表示するタイムラインデータクラス
 * */
data class AllTimeLineData(
    val instanceToken: InstanceToken,   // ログイン情報
    val service: String,                // mastodonしか無い（現状）
    val timeLineName: String,           // タイムライン名
    val timeLineLoad: String,           // 読み込むTL。home_notification / local しか無い
    var timeLineBackground: String,     // タイムラインのCardViewの背景色
    var timeLineTextColor: String,      // 文字色
    var isEnable: Boolean               // 有効にするか
) : Serializable