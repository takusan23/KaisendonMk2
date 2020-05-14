package io.github.takusan23.kaisendonmk2.MastodonAPI

import io.github.takusan23.kaisendonmk2.MisskeyDataClass.MisskeyNoteData
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
 * リアクションする時に使う
 * */
class MisskeyReactionAPI(val instanceToken: InstanceToken) {

    // ベースURL
    private val BASE_URL = "https://${instanceToken.instance}/api"

    // Content-Type : application/json
    private val APPLICATION_JSON = "application/json".toMediaType()

    private val USER_AGENT = "KaisendonMk2;@takusan_23"

    /**
     * リアクションAPIを叩く関数
     * @param noteId NoteId
     * @param reaction 絵文字。寿司とか
     * */
    fun reaction(noteId: String, reaction: String): Deferred<Response> = GlobalScope.async {
        val postData = JSONObject().apply {
            put("i", instanceToken.token)
            put("noteId", noteId)
            put("reaction", reaction)
        }.toString().toRequestBody(APPLICATION_JSON)
        val request = Request.Builder().apply {
            url("$BASE_URL/notes/reactions/create")
            header("User-Agent", USER_AGENT)
            post(postData)
        }.build()
        val okHttpClient = OkHttpClient()
        val response = okHttpClient.newCall(request).execute()
        return@async response
    }

}