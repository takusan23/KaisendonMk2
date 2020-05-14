package io.github.takusan23.kaisendonmk2.MisskeyAPI

import android.opengl.Visibility
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
 * Note関係
 * @param instanceToken serviceが"misskey"である必要ある
 * */
class MisskeyNoteAPI(val instanceToken: InstanceToken) {

    /** 公開範囲 */
    companion object {
        /** 公開 */
        val MISSKEY_VISIBILITY_PUBLIC = "public"

        /** ホームのみ公開 */
        val MISSKEY_VISIBILITY_HOME = "home"

        /** フォロワーにのみ公開 */
        val MISSKEY_VISIBILITY_FOLLOWERS = "followers"

        /** ダイレクト */
        val MISSKEY_VISIBILITY_SPECIFIED = "specified"

        /** ローカルのみ？ */
        val MISSKEY_VISIBILITY_PRIVATE = "private"
    }

    // ユーザーエージェント
    private val USER_AGENT = "KaisendonMk2;@takusan_23"

    // application/json
    private val APPLICATON_JSON = "application/json".toMediaType()

    private val BASE_URL = "https://${instanceToken.instance}/api"

    /**
     * 投稿する。コルーチンです。
     * @param text 投稿内容
     * @param visibility 公開範囲。省略時「public」
     * @param isViaMobile 携帯電話から投稿するときはtrue。省略時「true」
     * @param renoteId RenoteするときはNoteIdを入れてね。
     * */
    fun notesCreate(text: String, visibility: String = "public", isViaMobile: Boolean = true, renoteId: String? = null): Deferred<Response> =
        GlobalScope.async {
            val postData = JSONObject().apply {
                put("text", text)
                put("i", instanceToken.token)
                put("visibility", visibility)
                put("viaMobile", isViaMobile)
                if (renoteId != null) {
                    put("renoteId", renoteId)
                }
            }.toString().toRequestBody(APPLICATON_JSON)
            val request = Request.Builder().apply {
                url("$BASE_URL/notes/create")
                header("User-Agent", USER_AGENT)
                post(postData)
            }.build()
            val okHttpClient = OkHttpClient()
            val response = okHttpClient.newCall(request).execute()
            return@async response
        }

}