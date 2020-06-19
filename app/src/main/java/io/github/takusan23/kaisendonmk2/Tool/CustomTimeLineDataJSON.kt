package io.github.takusan23.kaisendonmk2.Tool

import io.github.takusan23.kaisendonmk2.DataClass.CustomTimeLineData
import io.github.takusan23.kaisendonmk2.MastodonAPI.InstanceToken
import org.json.JSONObject

/**
 * カスタムタイムラインのtimelineカラムにあるJSONを扱うクラス
 * */
class CustomTimeLineDataJSON {

    /**
     * データクラスにパースする
     * @param json DBのtimelineの値。なければnullになります。
     * */
    fun parse(json: String?): CustomTimeLineData? {
        if (json == null) return null
        val timelineJSON = JSONObject(json)
        val loadTL = timelineJSON.getString("load_tl")
        val instanceName = timelineJSON.getString("instance")
        val token = timelineJSON.getString("token")
        val service = timelineJSON.getString("service")
        val backgroundColor = timelineJSON.getString("background_color")
        val name = timelineJSON.getString("name")
        return CustomTimeLineData(
            instanceToken = InstanceToken(instance = instanceName, token = token, service = service),
            service = service,
            timeLineName = name,
            timeLineLoad = loadTL,
            timeLineBackground = backgroundColor
        )
    }

}