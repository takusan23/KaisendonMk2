package io.github.takusan23.kaisendonmk2.JSONParse

import io.github.takusan23.kaisendonmk2.API.InstanceToken
import io.github.takusan23.kaisendonmk2.DataClass.NotificationData
import org.json.JSONObject

/**
 * 通知JSONをパースするクラス。
 * TimeLineParserクラスでstatusとかaccountオブジェクトをパースしている。
 * */
class NotificationParser {

    val timeLineParser = TimeLineParser()

    /**
     * 通知をパースする
     * @param instanceToken ログイン情報
     * @param jsonString 通知JSON
     * */
    fun parseNotification(jsonString: String, instanceToken: InstanceToken): NotificationData {
        val jsonObject = JSONObject(jsonString)
        val accountData =
            timeLineParser.parseAccount(jsonObject.getJSONObject("account").toString(), instanceToken)
        val createdAt = jsonObject.getString("created_at")
        val id = jsonObject.getString("id")
        val type = jsonObject.getString("type")
        val statusData = if (type != "follow") {
            timeLineParser.parseStatus(jsonObject.getString("status").toString(), instanceToken)
        } else {
            null
        }
        val notificationData =
            NotificationData(instanceToken, createdAt, id, accountData, statusData, type)
        return notificationData
    }

}