package io.github.takusan23.kaisendonmk2.DataClass

import java.io.Serializable

// Card。あのURLとは別になんかサイト先の情報が表示されるやつ
data class CardData(val url: String, val title: String, val image: String): Serializable