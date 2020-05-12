package io.github.takusan23.kaisendonmk2.TimeLine

import java.text.SimpleDateFormat
import java.util.*

/**
 * ISO8601形式をUnixTimeへ変換する
 * */
internal fun toUnixTime(iso8601: String): Long {
    val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    return simpleDateFormat.parse(iso8601).time / 1000
}