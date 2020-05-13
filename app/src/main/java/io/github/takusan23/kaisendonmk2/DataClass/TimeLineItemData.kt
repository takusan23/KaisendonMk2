package io.github.takusan23.kaisendonmk2.DataClass

/**
 * RecyclerViewに渡すデータクラス
 * @param allTimeLineData 色つけたりするから；；
 * どっちか一個だけ入れろ↓。
 * @param statusData
 * @param notificationData
 * */
data class TimeLineItemData(
    val allTimeLineData: AllTimeLineData,
    val statusData: StatusData? = null, // null以外ならTL表示
    val notificationData: NotificationData? = null // null以外なら通知表示
)