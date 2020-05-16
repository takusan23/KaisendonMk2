package io.github.takusan23.kaisendonmk2.MisskeyAPI

import io.github.takusan23.kaisendonmk2.MastodonAPI.InstanceToken
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject

/**
 * Misskeyのタイムライン取得APIを叩く
 * @param instanceToken serviceが"misskey"である必要があります
 * */
class MisskeyTimeLineAPI(val instanceToken: InstanceToken) {

    // ユーザーエージェント
    private val USER_AGENT = "KaisendonMk2;@takusan_23"

    // application/json
    private val APPLICATON_JSON = "application/json".toMediaType()


    /**
     * ホームタイムライン取得
     * */
    fun getHomeNotesTimeLine(limit: Int = 100): Deferred<Response> =
        baseTimeLineAPI("notes/timeline", limit)

    /**
     * ローカルタイムライン取得
     * */
    fun getLocalNotesTimeLine(limit: Int = 100): Deferred<Response> =
        baseTimeLineAPI("notes/local-timeline", limit)

    /**
     * 共通部分
     * */
    private fun baseTimeLineAPI(url: String, limit: Int = 100): Deferred<Response> =
        GlobalScope.async {
            val postData = JSONObject().apply {
                put("limit", limit)
                put("i", instanceToken.token)
            }.toString().toRequestBody(APPLICATON_JSON)
            val request = Request.Builder().apply {
                url("https://${instanceToken.instance}/api/$url")
                header("User-Agent", USER_AGENT)
                post(postData)
            }.build()
            val okHttpClient = OkHttpClient()
            val response = okHttpClient.newCall(request).execute()
            return@async response
        }

}