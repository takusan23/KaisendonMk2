package io.github.takusan23.kaisendonmk2.API

import io.github.takusan23.kaisendonmk2.DataClass.AppData
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
 * ログインするためのAPIを叩くクラス。
 * @param instanceName インスタンス名
 * */
class AppsAPI(val instanceName: String) {

    /**
     * 新しいアプリケーションを作成するAPIを叩く関数。
     * Via芸とかがここでやる
     * @param instanceName インスタンス名
     * @param viaName Via芸する場合は。しないならなんか適当入れて。クライアント名になります。
     * @return AppData。めんどいから通信成功したかどうかとか見ないよ。失敗したらnull帰ってくる
     * */
    fun createApp(viaName: String): Deferred<AppData?> = GlobalScope.async {
        val postData = JSONObject().apply {
            put("client_name", viaName)
            put("redirect_uris", "https://takusan23.github.io/Kaisendon-Callback-Website/")
            put("scopes", "read write follow")
        }.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder().apply {
            url("https://${instanceName}/api/v1/apps")
            header("User-Agent", "KaisendonMk2;@takusan_23")
            post(postData)
        }.build()
        val okHttpClient = OkHttpClient()
        val response = okHttpClient.newCall(request).execute()
        val jsonObject = JSONObject(response.body?.string() ?: return@async null)
        val clientId = jsonObject.getString("client_id")
        val clientSecret = jsonObject.getString("client_secret")
        val redirectUrl = jsonObject.getString("redirect_uri")
        return@async AppData(clientId, clientSecret, redirectUrl)
    }

    /**
     * アクセストークン取得
     * @param data createApp()の中身
     * @param instanceName インスタンス名
     * @param responseCode OAuthの許可貰えた時にリダイレクトするじゃん。あれのパラメーターの値。
     * @return アクセストークン
     * */
    fun getAccessToken(instanceName: String, data: AppData, responseCode: String): Deferred<String?> =
        GlobalScope.async {
            val postData = JSONObject().apply {
                put("client_id", data.clientId)
                put("client_secret", data.clientSecret)
                put("redirect_uri", data.redirectUrl)
                put("scope", "read write follow")
                put("code", responseCode)
                put("grant_type", "authorization_code")
            }.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder().apply {
                url("https://${instanceName}/oauth/token")
                header("User-Agent", "KaisendonMk2;@takusan_23")
                post(postData)
            }.build()
            val okHttpClient = OkHttpClient()
            val response = okHttpClient.newCall(request).execute()
            val jsonObject = JSONObject(response.body?.string() ?: return@async null)
            val accessToken = jsonObject.getString("access_token")
            return@async accessToken
        }

}