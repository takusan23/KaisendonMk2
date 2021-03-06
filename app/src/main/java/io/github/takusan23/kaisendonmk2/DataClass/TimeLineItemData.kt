package io.github.takusan23.kaisendonmk2.DataClass

import io.github.takusan23.kaisendonmk2.DetaBase.Entity.CustomTimeLineDBEntity
import io.github.takusan23.kaisendonmk2.MisskeyDataClass.MisskeyNoteData
import io.github.takusan23.kaisendonmk2.MisskeyDataClass.MisskeyNotificationData
import java.io.Serializable

/**
 * RecyclerViewに渡すデータクラス
 * @param customTimeLineData 色つけたりするから；；
 * どっちか一個だけ入れろ↓。
 * @param statusData
 * @param notificationData
 * */
data class TimeLineItemData(
    val customTimeLineDBEntity: CustomTimeLineDBEntity,
    val statusData: StatusData? = null, // null以外ならTL表示
    val notificationData: NotificationData? = null,// null以外なら通知表示
    val misskeyNoteData: MisskeyNoteData? = null, // null以外ならNote表示
    val misskeyNotificationData: MisskeyNotificationData? = null // null以外なら通知表示
) : Serializable