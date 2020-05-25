package io.github.takusan23.kaisendonmk2.DetaBase

import io.github.takusan23.kaisendonmk2.DataClass.CustomTimeLineData
import io.github.takusan23.kaisendonmk2.DetaBase.Entity.CustomTimeLineDBEntity
import org.json.JSONObject

/**
 * タイムライン構成データクラスをデータベースに追加する
 * */
internal fun CustomTimeLineData.toCustomTimeLineDBEntity(): CustomTimeLineDBEntity {
    return CustomTimeLineDBEntity(
        name = this.timeLineName,
        instance = this.instanceToken.instance,
        token = this.instanceToken.token,
        isEnable = this.isEnable,
        timeline = this.toCustomTimeLineDBEntity().toString(),
        service = "mastodon"
    )
}

internal fun CustomTimeLineData.toTimeLineJSON(): JSONObject {
    return JSONObject().apply {
        put("instance", this@toTimeLineJSON.instanceToken.instance)
        put("token", this@toTimeLineJSON.instanceToken.token)
        put("service", this@toTimeLineJSON.service)
        put("load_tl", this@toTimeLineJSON.timeLineLoad)
        put("background_color", this@toTimeLineJSON.timeLineBackground)
        put("text_color", this@toTimeLineJSON.timeLineTextColor)
        put("is_enable", false)
        put("name", this@toTimeLineJSON.timeLineName)
    }
}