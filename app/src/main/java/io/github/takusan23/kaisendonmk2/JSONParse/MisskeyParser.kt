package io.github.takusan23.kaisendonmk2.JSONParse

import io.github.takusan23.kaisendonmk2.DataClass.EmojiData
import io.github.takusan23.kaisendonmk2.MastodonAPI.InstanceToken
import io.github.takusan23.kaisendonmk2.MisskeyDataClass.MisskeyNoteData
import io.github.takusan23.kaisendonmk2.MisskeyDataClass.MisskeyNotificationData
import io.github.takusan23.kaisendonmk2.MisskeyDataClass.MisskeyReactionData
import io.github.takusan23.kaisendonmk2.MisskeyDataClass.MisskeyUserData
import org.json.JSONArray
import org.json.JSONObject

/**
 * MisskeyのJSONぱーさー
 * */
class MisskeyParser {

    /**
     * タイムラインをパース。parseNote()をforで回してるだけ
     * */
    fun parseTimeLine(jsonString: String?, instanceToken: InstanceToken): ArrayList<MisskeyNoteData> {
        val list = arrayListOf<MisskeyNoteData>()
        val jsonArray = JSONArray(jsonString)
        for (i in 0 until jsonArray.length()) {
            val jsonObject = jsonArray.getString(i)
            val note = parseNote(jsonObject.toString(), instanceToken)
            list.add(note)
        }
        return list
    }

    /**
     * Noteをパースする
     * */
    fun parseNote(jsonString: String, instanceToken: InstanceToken): MisskeyNoteData {
        val jsonObject = JSONObject(jsonString)
        val createdAt = jsonObject.getString("createdAt")
        val text = jsonObject.getString("text")
        val isMobile = jsonObject.getBoolean("viaMobile")
        val noteId = jsonObject.getString("id")
        val renoteCount = jsonObject.getInt("renoteCount")
        val reaction = parseReaction(jsonObject.getJSONObject("reactions").toString())
        val emoji = parseEmoji(jsonObject.getJSONArray("emojis").toString())
        val user = parseUser(jsonObject.getJSONObject("user").toString(), instanceToken)
        // Renoteがあれば
        val renote = if (jsonObject.has("renote")) {
            parseNote(jsonObject.getJSONObject("renote").toString(), instanceToken)
        } else {
            null
        }
        return MisskeyNoteData(instanceToken, createdAt, text, isMobile, noteId, renoteCount, reaction, emoji, user, renote)
    }

    /**
     * userをパースする
     * */
    fun parseUser(jsonString: String, instanceToken: InstanceToken): MisskeyUserData {
        val jsonObject = JSONObject(jsonString)
        val name = jsonObject.getString("name")
        val username = jsonObject.getString("username")
        val isAdmin = jsonObject.optBoolean("isAdmin", false)
        val id = jsonObject.getString("id")
        val emoji = parseEmoji(jsonObject.optJSONArray("emojis")?.toString())
        val avatarUrl = jsonObject.getString("avatarUrl")
        val bannerUrl = jsonObject.optString("bannerUrl", null)
        return MisskeyUserData(instanceToken, name, username, isAdmin, id, emoji, avatarUrl, bannerUrl)
    }

    /**
     * reactionsをパースする
     * */
    fun parseReaction(jsonString: String): ArrayList<MisskeyReactionData> {
        val reactionList = arrayListOf<MisskeyReactionData>()
        val jsonObject = JSONObject(jsonString)
        // リアクション
        jsonObject.keys().forEach {
            val emoji = it
            val reactionCount = jsonObject.getInt(emoji)
            val misskeyReactionData = MisskeyReactionData(emoji, reactionCount)
            reactionList.add(misskeyReactionData)
        }
        return reactionList
    }

    /**
     * appをパースする。
     * クライアント名ぐらいしかいらないのでクライアント名だけ（まず本家クライアント名出ないし）
     * */
    fun parseApp(jsonString: String): String {
        val jsonObject = JSONObject(jsonString)
        val name = jsonObject.getString("name")
        return name
    }

    /**
     * 絵文字をパースする。
     * @return Mastodonと共通ですが、staticもurlも値が変わりません。
     * */
    fun parseEmoji(jsonString: String?): ArrayList<EmojiData> {
        val emojiList = arrayListOf<EmojiData>()
        if (jsonString == null) return emojiList
        val jsonArray = JSONArray(jsonString)
        for (i in 0 until jsonArray.length()) {
            val jsonObject = jsonArray.getJSONObject(i)
            val name = jsonObject.getString("name")
            val url = jsonObject.getString("url")
            val emojiData = EmojiData(name, url, url)
            emojiList.add(emojiData)
        }
        return emojiList
    }

    /**
     * 通知をパースする
     * */
    fun parseNotification(notification: String, instanceToken: InstanceToken): MisskeyNotificationData {
        val jsonObject = JSONObject(notification)
        val type = jsonObject.getString("type")
        val createdAt = jsonObject.getString("createdAt")
        val id = jsonObject.getString("id")
        val note = parseNote(jsonObject.getJSONObject("note").toString(), instanceToken)
        val reaction = jsonObject.getString("reaction")
        val user = parseUser(jsonObject.getJSONObject("user").toString(), instanceToken)
        return MisskeyNotificationData(type, createdAt, id, note, reaction, user)
    }

}