package io.github.takusan23.kaisendonmk2.DataClass

import java.io.Serializable

// カスタム絵文字のデータクラス
data class EmojiData(val shortCode: String, val url: String, val staticUrl: String): Serializable