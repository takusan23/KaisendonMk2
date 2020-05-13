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

/**
 * ISO8601->yyyy/MM/dd HH:mm:ss:SSS
 * */
internal fun String.toTimeFormat(): String {
    val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    val calendar = Calendar.getInstance()
    calendar.time = simpleDateFormat.parse(this)
    calendar.add(Calendar.HOUR, 9) // 日本時間 +9
    val toSimpleDateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS")
    return toSimpleDateFormat.format(calendar.time)
}