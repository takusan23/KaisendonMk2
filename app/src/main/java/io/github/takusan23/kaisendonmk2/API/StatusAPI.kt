package io.github.takusan23.kaisendonmk2.API

import android.net.wifi.aware.PublishConfig
import android.opengl.Visibility
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject

// Mastodonトゥート投稿API
class StatusAPI(val instanceToken: InstanceToken) {

    // ベースURL
    private val BASE_URL = "https://${instanceToken.instance}/api/v1/statuses"

    // Content-Type : application/json
    private val APPLICATION_JSON = "application/json".toMediaType()

    private val USER_AGENT = "KaisendonMk2;@takusan_23"

    // 公開範囲
    companion object {
        val VISIBILITY_PUBLIC = "public" // 公開TL（ローカルとか連合とか）
        val VISIBILITY_UNLISTED = "unlisted" // 公開TLに出さない（ホームTLとかユーザーの投稿では見れる）
        val VISIBILITY_PRIVATE = "private" // フォロワー限定（名の通り）
        val VISIBILITY_DIRECT = "direct" // メンション（DM）した相手だけに公開
    }

    /**
     * トゥートする関数。コルーチンです
     * @param status 投稿内容
     * @param visibility 公開範囲（省略時：公開TLに投稿）
     * */
    fun postStatus(status: String, visibility: String = VISIBILITY_PUBLIC): Deferred<Response> =
        GlobalScope.async {
            // 投稿内容
            val postData = JSONObject().apply {
                put("status", status)
                put("visibility", visibility)
                put("access_token", instanceToken.token)
            }.toString().toRequestBody(APPLICATION_JSON)
            val request = Request.Builder().apply {
                url(BASE_URL)
                header("User-Agent", USER_AGENT)
                post(postData)
            }.build()
            val okHttpClient = OkHttpClient()
            val response = okHttpClient.newCall(request).execute()
            return@async response
        }

    /**
     * ふぁぼるAPI叩く
     * @param id トゥートのID
     * @param instanceToken ログイン情報
     * */
    fun postStatusFav(id: String, instanceToken: InstanceToken): Deferred<Response> =
        postStatusAction("$BASE_URL/$id/favourite", id, instanceToken)

    /**
     * ふぁぼ取り消しAPI叩く。
     * @param id トゥートID
     * @param instanceToken ログイン情報
     * */
    fun postDeleteStatusFav(id: String, instanceToken: InstanceToken): Deferred<Response> =
        postStatusAction("$BASE_URL/$id/unfavourite", id, instanceToken)

    /**
     * ブーストAPI叩く
     * @param id トゥートID
     * @param instanceToken ログイン情報
     * */
    fun postStatusBoost(id: String, instanceToken: InstanceToken): Deferred<Response> =
        postStatusAction("$BASE_URL/$id/reblog", id, instanceToken)

    /**
     * ブースト取り消しAPI叩く
     * @param id トゥートID
     * @param instanceToken ログイン情報
     * */
    fun postDeleteStatusBoost(id: String, instanceToken: InstanceToken): Deferred<Response> =
        postStatusAction("$BASE_URL/$id/unreblog", id, instanceToken)


    /**
     * ふぁぼ、ブースト共通部分。
     * 多分URLが違うだけでふぁぼとか取り消しとか送る内容変わらんと思う。
     * @param id トゥートのID
     * @param instanceToken ログイン情報
     * */
    private fun postStatusAction(url: String, id: String, instanceToken: InstanceToken): Deferred<Response> =
        GlobalScope.async {
            val postData = JSONObject().apply {
                put("access_token", instanceToken.token)
            }.toString().toRequestBody(APPLICATION_JSON)
            val request = Request.Builder().apply {
                url(url)
                header("User-Agent", USER_AGENT)
                post(postData)
            }.build()
            val okHttpClient = OkHttpClient()
            val response = okHttpClient.newCall(request).execute()
            return@async response
        }


}