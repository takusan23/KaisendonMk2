package io.github.takusan23.kaisendonmk2.MisskeyAPI

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigInteger
import java.security.MessageDigest

/**
 * Misskeyにログインしてアクセストークンもらう関数とかがあるクラス。
 * @param instanceName インスタンス名。私が居るのは「misskey.m544.net」
 * */
class MisskeyLoginAPI(val instanceName: String) {

    // ユーザーエージェント
    private val USER_AGENT = "KaisendonMk2;@takusan_23"

    // application/json
    private val APPLICATON_JSON = "application/json".toMediaType()

    private val BASE_URL = "https://${instanceName}/api"

    /**
     * Misskeyにアプリを登録する。コルーチンです
     * MiAuthは私の居るインスタンスにはバージョン的に使えなかった。
     * @param viaName クライアント名。Via芸する場合はどうぞ。しないなら空白でおk
     * @return secretが帰ってきます。この後使います。
     * */
    fun createMisskeyApp(viaName: String = "KaisendonMk2"): Deferred<String?> = GlobalScope.async {
        val postData = JSONObject().apply {
            put("name", viaName)
            put("description", "AndroidのMisskeyクライアント？")
            put("callbackUrl", "https://takusan23.github.io/Kaisendon-Callback-Website/")
            put("permission", JSONArray().apply {
                // いつの間にかPermission一覧ができていた
                put("read:account")
                // 投稿権限
                put("write:notes")
                put("note-write")
                // fav
                put("read:favorites")
                put("write:favorites")
                // 通知
                put("read:notifications")
                // リアクション
                put("write:reactions")
            })
        }.toString().toRequestBody(APPLICATON_JSON)
        val request = Request.Builder().apply {
            url("${BASE_URL}/app/create")
            header("User-Agent", USER_AGENT)
            post(postData)
        }.build()
        val okHttpClient = OkHttpClient()
        val response = okHttpClient.newCall(request).execute()
        val responseString = response.body?.string()
        if (!response.isSuccessful) return@async null // 失敗時は落とす
        val jsonObject = JSONObject(responseString)
        val secret = jsonObject.getString("secret")
        return@async secret
    }


    /**
     * 認証画面（ユーザーにブラウザ開いて許可してもらう）URLを返してもらうAPI叩く関数。コルーチンです。
     * @param secret createMisskeyApp()関数の返り値
     * @return SessionGenerateData#urlでブラウザで開くURLが取得できます。
     * */
    fun sessionGenerate(secret: String): Deferred<SessionGenerateData?> = GlobalScope.async {
        val postData = JSONObject().apply {
            put("appSecret", secret)
        }.toString().toRequestBody(APPLICATON_JSON)
        val request = Request.Builder().apply {
            url("${BASE_URL}/auth/session/generate")
            header("User-Agent", USER_AGENT)
            post(postData)
        }.build()
        val okHttpClient = OkHttpClient()
        val response = okHttpClient.newCall(request).execute()
        if (!response.isSuccessful) return@async null // 失敗時
        val jsonObject = JSONObject(response.body?.string())
        val url = jsonObject.getString("url")
        val token = jsonObject.getString("token")
        return@async SessionGenerateData(token, url)
    }

    /**
     * ユーザーキーを取得する関数。コルーチンです。
     * @param secret createMisskeyApp()の返り値
     * @param token sessionGenerate()で生成したURLにアクセスして許可が降りたときのコールバックURLにくっついてるtoken
     * @return アクセストークン生成に使う値。アクセストークンではない！！！！！
     * */
    fun sessionUserkey(secret: String, token: String): Deferred<String?> =
        GlobalScope.async {
            val postData = JSONObject().apply {
                put("appSecret", secret)
                put("token", token)
            }.toString().toRequestBody(APPLICATON_JSON)
            val request = Request.Builder().apply {
                url("${BASE_URL}/auth/session/userkey")
                header("User-Agent", USER_AGENT)
                post(postData)
            }.build()
            val okHttpClient = OkHttpClient()
            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) return@async null
            val jsonObject = JSONObject(response.body?.string())
            val accessToken = jsonObject.getString("accessToken")
            return@async accessToken
        }

    /**
     * おつかれさま。アクセストークン取得に使う最後の関数です。コルーチンではありません。
     * createMisskeyApp()の返り値とsessionUserkey()の返り値を結合してSHA256する関数です。
     * これがアクセストークンになり、本人確認となります。
     * @param secret createMisskeyApp()の返り値
     * @param token sessionUserkey()の返り値
     * @return アクセストークン
     * */
    fun generateAccessToken(secret: String, token: String): String {
        val text = token + secret
        // こっから先コピペなのでなにやってんのかわからん。
        val digest = MessageDigest.getInstance("SHA-256")
        digest.reset()
        digest.update(text.toByteArray(charset("utf8")))
        val accessToken = String.format("%040x", BigInteger(1, digest.digest()))
        return accessToken

    }

    data class SessionGenerateData(val token: String, val url: String)

}