package io.github.takusan23.kaisendonmk2.StreamingAPI

import io.github.takusan23.kaisendonmk2.MastodonAPI.InstanceToken
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import org.json.JSONObject
import java.lang.Exception
import java.net.URI
import java.net.URL

/**
 * MisskeyのストリーミングAPI
 * */
class MisskeyStreamingAPI(val instanceToken: InstanceToken) {

    /** initStreaming()関数に渡す引数 */
    companion object {
        /** ローカルタイムライン */
        val CHANNEL_LOCAL = "localTimeline"

        /** ホームタイムライン */
        val CHANNEL_HOME = "homeTimeline"

        /** 通知 */
        val CHANNEL_MAIN = "main"
    }

    lateinit var webSocketClient: WebSocketClient

    /**
     * ストリーミングAPIに接続する。
     * @param channel 接続するチャンネル名の配列。
     * @param receiveMessage WebSocketから流れてきたメッセージ。高階関数。typeで分岐してね、
     * */
    fun initStreaming(channel: ArrayList<String>, receiveMessage: (String?) -> Unit) {
        // WebSocket
        val url = "wss://${instanceToken.instance}/streaming?i=${instanceToken.token}"
        webSocketClient = object : WebSocketClient(URI(url)) {
            override fun onOpen(handshakedata: ServerHandshake?) {
                // 購読するもの
                channel.forEach {
                    send(createJSON(it))
                }
            }

            override fun onClose(code: Int, reason: String?, remote: Boolean) {

            }

            override fun onMessage(message: String?) {
                receiveMessage(message)
            }

            override fun onError(ex: Exception?) {

            }
        }
        webSocketClient.connect()
    }

    /**
     * ストリーミングAPIで購読するデータの登録時に使うJSONを作成する
     * */
    private fun createJSON(channel: String = "main"): String {
        val jsonObject = JSONObject().apply {
            put("type", "connect")
            put("body", JSONObject().apply {
                put("channel", channel)
                put("id", System.currentTimeMillis() / 1000)
            })
        }
        return jsonObject.toString()
    }

    /**
     * 最後に呼んで
     * */
    fun destroy() {
        if (::webSocketClient.isInitialized) {
            webSocketClient.close()
        }
    }

}