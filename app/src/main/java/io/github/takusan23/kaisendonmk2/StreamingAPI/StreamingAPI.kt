package io.github.takusan23.kaisendonmk2.StreamingAPI

import io.github.takusan23.kaisendonmk2.MastodonAPI.InstanceToken
import io.github.takusan23.kaisendonmk2.DataClass.NotificationData
import io.github.takusan23.kaisendonmk2.DataClass.StatusData
import io.github.takusan23.kaisendonmk2.JSONParse.NotificationParser
import io.github.takusan23.kaisendonmk2.JSONParse.TimeLineParser
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import org.json.JSONObject
import java.lang.Exception
import java.net.URI

/**
 * ストリーミングAPI。WebSocketです
 * Twitterと違ってリアルタイムで更新される。（FilterStream？TweetDeck？知らないですね）
 * */
class StreamingAPI(val instanceToken: InstanceToken) {

    // WebSocketのアドレス
    private val BASE_URL =
        "wss://${instanceToken.instance}/api/v1/streaming/?access_token=${instanceToken.token}"

    // WebSocketClientの配列
    private val webSocketClientList = arrayListOf<WebSocketClient>()

    // JSON -> StatusParse
    private val timeLineParser = TimeLineParser()
    private val notificationParser = NotificationParser()

    /**
     * ローカルタイムラインのStreamingAPIに接続する
     * @param onMessage 新しい投稿があれば流れてきます。
     * */
    fun streamingLocalTL(receiveMessage: (StatusData) -> Unit) = streaming("$BASE_URL&stream=public:local", receiveMessage)

    /**
     * 連合TLに接続する
     * @param receiveMessage 新しい投稿があれば流れてきます。
     * */
    fun streamingPublicTL(receiveMessage: (StatusData) -> Unit) = streaming("$BASE_URL&stream=public", receiveMessage)

    /**
     * ホームTLと通知に接続する。通知も流れてきます。
     * @param receiveMessage 新しい投稿があれば流れてきます。
     * @param receiveNotification 新しい通知があれば流れてきます。(でも通知データクラスにステータスデータクラスがあるかどうかは別。フォロー通知のときはない)
     * */
    fun streamingUser(receiveMessage: (StatusData) -> Unit, receiveNotification: (NotificationData) -> Unit) {
        val webSocketClient = object : WebSocketClient(URI("$BASE_URL&stream=user")) {
            override fun onOpen(handshakedata: ServerHandshake?) {

            }

            override fun onClose(code: Int, reason: String?, remote: Boolean) {

            }

            override fun onMessage(message: String?) {
                // JSON -> StatusData
                val jsonObject = JSONObject(message)
                val event = jsonObject.getString("event")
                if (event == "update") {
                    // 新しい投稿
                    val payload = jsonObject.getString("payload")
                    val statusData = timeLineParser.parseStatus(payload, instanceToken)
                    receiveMessage(statusData)
                }
                if (event == "notification") {
                    // 通知
                    val payload = jsonObject.getString("payload")
                    val notificationData =
                        notificationParser.parseNotification(payload, instanceToken)
                    receiveNotification(notificationData)
                }
            }

            override fun onError(ex: Exception?) {

            }
        }
        webSocketClient.connect()
        webSocketClientList.add(webSocketClient)
    }

    /**
     * StreamingAPIで共通部分(LocalかPublic以外で使わないけど)
     * */
    private fun streaming(url: String, receiveMessage: (StatusData) -> Unit) {
        val webSocketClient = object : WebSocketClient(URI(url)) {
            override fun onOpen(handshakedata: ServerHandshake?) {

            }

            override fun onClose(code: Int, reason: String?, remote: Boolean) {

            }

            override fun onMessage(message: String?) {
                // JSON -> StatusData
                val jsonObject = JSONObject(message)
                val event = jsonObject.getString("event")
                if (event == "update") {
                    val payload = jsonObject.getString("payload")
                    val statusData = timeLineParser.parseStatus(payload, instanceToken)
                    receiveMessage(statusData)
                }
            }

            override fun onError(ex: Exception?) {

            }
        }
        webSocketClient.connect()
        webSocketClientList.add(webSocketClient)
    }

    /**
     * 終了時に呼んでね
     * */
    fun destroy() {
        webSocketClientList.forEach {
            it.close()
        }
    }


}