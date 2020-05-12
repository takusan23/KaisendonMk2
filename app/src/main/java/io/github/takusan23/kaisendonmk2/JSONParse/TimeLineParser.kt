package io.github.takusan23.kaisendonmk2.JSONParse

import io.github.takusan23.kaisendonmk2.API.InstanceToken
import io.github.takusan23.kaisendonmk2.DataClass.*
import org.json.JSONArray
import org.json.JSONObject

/**
 * タイムラインのレスポンスJSONをパースする
 * */
class TimeLineParser {

    /**
     * タイムラインAPIのレスポンスJSONをTimeLineDataの配列にパースする
     * @param responseString getLocalTimeLine()#body#string()の値。
     * @param instanceToken ログイン情報
     * @return StatusDataの配列
     * */
    fun parseTL(responseString: String?, instanceToken: InstanceToken): ArrayList<StatusData> {
        val stausList = arrayListOf<StatusData>()
        val jsonArray = JSONArray(responseString)
        for (i in 0 until jsonArray.length()) {
            stausList.add(parseStatus(jsonArray.getJSONObject(i).toString(), instanceToken))
        }
        return stausList
    }

    /**
     * ステータスのJSONをパースする
     * @param jsonString statusのJSON
     * @param instanceToken ログイン情報
     * */
    fun parseStatus(jsonString: String, instanceToken: InstanceToken): StatusData {
        val jsonObject = JSONObject(jsonString)
        val id = jsonObject.getString("id")
        val createdAt = jsonObject.getString("created_at")
        val visibility = jsonObject.getString("visibility")
        val url = jsonObject.getString("url")
        val favouritesCount = jsonObject.getInt("favourites_count")
        val isFavourited = jsonObject.optBoolean("favourited", false)
        val boostCount = jsonObject.getInt("reblogs_count")
        val isBoosted = jsonObject.optBoolean("reblogged", false)
        val content = jsonObject.getString("content")
        val mediaAttachment =
            parseMediaAttachments(jsonObject.getJSONArray("media_attachments"))
        val card = parseCard(jsonObject.optJSONObject("card"))
        val emojis = parseEmoji(jsonObject.getJSONArray("all_emojis"))
        // アカウント情報
        val accountData =
            parseAccount(jsonObject.getJSONObject("account").toString(), instanceToken)
        // ステータスデータクラス
        val statusData = StatusData(
            instanceToken,
            id,
            createdAt,
            visibility,
            url,
            favouritesCount,
            isFavourited,
            boostCount,
            isBoosted,
            accountData,
            content,
            mediaAttachment,
            card,
            emojis
        )
        return statusData
    }

    /**
     * アカウントJSONをパースする
     * @param jsonString アカウントのJSON
     * @param instanceToken ログイン情報
     * */
    fun parseAccount(jsonString: String, instanceToken: InstanceToken): AccountData {
        // アカウント情報
        val accountObject = JSONObject(jsonString)
        val accountId = accountObject.getString("id")
        val userName = accountObject.getString("username")
        val acct = accountObject.getString("acct")
        val accountCreatedAt = accountObject.getString("created_at")
        val note = accountObject.getString("note")
        val avatar = accountObject.getString("avatar")
        val avatarStatic = accountObject.getString("avatar_static")
        val header = accountObject.getString("header")
        val headerStatic = accountObject.getString("header_static")
        val followersCount = accountObject.getInt("followers_count")
        val followingCount = accountObject.getInt("following_count")
        val statusCount = accountObject.getInt("statuses_count")
        val lastStatus = accountObject.getString("last_status_at")
        val accountFields = parseFields(accountObject.getJSONArray("fields"))
        val accountEmojis = parseEmoji(accountObject.getJSONArray("emojis"))
        // アカウントデータクラス
        val accountData = AccountData(
            instanceToken,
            accountId,
            userName,
            userName,
            acct,
            accountCreatedAt,
            note,
            avatar,
            avatarStatic,
            header,
            headerStatic,
            followersCount,
            followingCount,
            statusCount,
            lastStatus,
            accountFields,
            accountEmojis
        )
        return accountData
    }

    // media_attachments をパースする
    private fun parseMediaAttachments(jsonArray: JSONArray?): ArrayList<String> {
        val list = arrayListOf<String>()
        if (jsonArray == null) {
            return list
        }
        for (i in 0 until jsonArray.length()) {
            val url = jsonArray.getJSONObject(i).getString("url")
            list.add(url)
        }
        return list
    }

    // emoji をパースする
    private fun parseEmoji(jsonArray: JSONArray?): ArrayList<EmojiData> {
        val list = arrayListOf<EmojiData>()
        if (jsonArray == null) {
            return list
        }
        for (i in 0 until jsonArray.length()) {
            val emojiObject = jsonArray.getJSONObject(i)
            val shortCode = emojiObject.getString("shortcode")
            val url = emojiObject.getString("url")
            val staticUrl = emojiObject.getString("static_url")
            val emojiData = EmojiData(shortCode, url, staticUrl)
            list.add(emojiData)
        }
        return list
    }

    // fields をパースする
    private fun parseFields(jsonArray: JSONArray?): ArrayList<FieldsData> {
        val list = arrayListOf<FieldsData>()
        if (jsonArray == null) {
            return list
        }
        for (i in 0 until jsonArray.length()) {
            val fieldObject = jsonArray.getJSONObject(i)
            val name = fieldObject.getString("name")
            val value = fieldObject.getString("value")
            val verifyAt = fieldObject.optString("verified_at", "")
        }
        return list
    }

    // card をパースする
    private fun parseCard(jsonObject: JSONObject?): CardData? {
        if (jsonObject == null) {
            return null
        }
        val url = jsonObject.getString("url")
        val title = jsonObject.getString("title")
        val image = jsonObject.getString("image")
        return CardData(url, title, image)
    }

}