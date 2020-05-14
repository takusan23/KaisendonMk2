package io.github.takusan23.kaisendonmk2.TimeLine

import android.content.Context
import io.github.takusan23.kaisendonmk2.MastodonAPI.InstanceToken
import io.github.takusan23.kaisendonmk2.DataClass.AllTimeLineData
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * タイムライン構成JSONを読み込んだりするクラス。
 * 登録したタイムラインはJSONファイルになって（データベースではない）保存される。
 * */
class AllTimeLineJSON(val context: Context?) {

    /**
     * タイムライン構成JSONを読み込む。ファイルがなければ空っぽの配列が帰ってきます。
     * @return TimeLineSettingData（タイムライン構成データクラス）の配列
     * */
    fun loadTimeLineSettingJSON(): ArrayList<AllTimeLineData> {
        val timeLineSettingJSONList = arrayListOf<AllTimeLineData>()
        val file = File("${context?.getExternalFilesDir(null)?.path}/timeline.json")
        if (file.exists()) {
            val jsonArray = JSONArray(file.readText())
            for (i in 0 until jsonArray.length()) {
                val timelineObject = jsonArray.getJSONObject(i)
                // アカウント
                val instanceToken = getAccountObject(timelineObject.getJSONObject("account"))
                val loadTL = timelineObject.getString("load_tl")
                val backgroundColor = timelineObject.getString("background_color")
                val textColor = timelineObject.getString("text_color")
                val isEnable = timelineObject.getBoolean("is_enable")
                val name = timelineObject.getString("name")
                val service = timelineObject.getString("service")
                val allTimeLineData =
                    AllTimeLineData(instanceToken, service, name, loadTL, backgroundColor, textColor, isEnable)
                timeLineSettingJSONList.add(allTimeLineData)
            }
        }
        return timeLineSettingJSONList
    }

    /**
     * タイムライン構成JSONを書き込む
     * */
    fun saveTimeLineSettingJSON(allTimeLineDataList: ArrayList<AllTimeLineData>) {
        val file = File("${context?.getExternalFilesDir(null)?.path}/timeline.json")
        val jsonArray = JSONArray()
        allTimeLineDataList.forEach {
            val jsonObject = JSONObject().apply {
                put("account", toAccountJSONObject(it.instanceToken))
                put("service", it.service)
                put("load_tl", it.timeLineLoad)
                put("background_color", it.timeLineBackground)
                put("text_color", it.timeLineTextColor)
                put("is_enable", it.isEnable)
                put("name", it.timeLineName)
            }
            jsonArray.put(jsonObject)
        }
        file.writeText(jsonArray.toString(4))
    }

    /**
     * is_enableの値を変更する関数
     * */
    fun setAllTimeLineEnable(name: String, isEnable: Boolean) {
        val list = loadTimeLineSettingJSON()
        list.forEach {
            if (it.timeLineName == name) {
                it.isEnable = isEnable
            }
        }
        saveTimeLineSettingJSON(list)
    }

    // InstanceToken -> JSONObject
    private fun toAccountJSONObject(instanceToken: InstanceToken): JSONObject {
        val jsonObject = JSONObject().apply {
            put("instance", instanceToken.instance)
            put("token", instanceToken.token)
            put("service", instanceToken.service)
        }
        return jsonObject
    }

    // JSONObject -> InstanceToken
    private fun getAccountObject(jsonObject: JSONObject): InstanceToken {
        val instance = jsonObject.getString("instance")
        val token = jsonObject.getString("token")
        val service = jsonObject.getString("service")
        return InstanceToken(instance, token, service)
    }

}