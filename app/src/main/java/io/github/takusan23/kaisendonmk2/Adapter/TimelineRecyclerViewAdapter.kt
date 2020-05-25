package io.github.takusan23.kaisendonmk2.Adapter

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import io.github.takusan23.kaisendonmk2.BottomFragment.MisskeyReactionBottomSheet
import io.github.takusan23.kaisendonmk2.MastodonAPI.StatusAPI
import io.github.takusan23.kaisendonmk2.CustomEmoji.CustomEmoji
import io.github.takusan23.kaisendonmk2.DataClass.CustomTimeLineData
import io.github.takusan23.kaisendonmk2.DataClass.StatusData
import io.github.takusan23.kaisendonmk2.DataClass.TimeLineItemData
import io.github.takusan23.kaisendonmk2.JSONParse.MisskeyParser
import io.github.takusan23.kaisendonmk2.MainActivity
import io.github.takusan23.kaisendonmk2.MisskeyAPI.MisskeyNoteAPI
import io.github.takusan23.kaisendonmk2.MisskeyDataClass.MisskeyNoteData
import io.github.takusan23.kaisendonmk2.R
import io.github.takusan23.kaisendonmk2.TimeLine.*
import io.github.takusan23.kaisendonmk2.TimeLine.escapeToBrTag
import io.github.takusan23.kaisendonmk2.TimeLine.isConnectionMobileData
import io.github.takusan23.kaisendonmk2.TimeLine.isDarkMode
import io.github.takusan23.kaisendonmk2.TimeLine.setNullTint
import io.github.takusan23.kaisendonmk2.TimeLine.toTimeFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

// タイムライン表示RecyclerView
class TimelineRecyclerViewAdapter(val timeLineItemDataList: ArrayList<TimeLineItemData>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    lateinit var mainActivity: MainActivity

    val customEmoji = CustomEmoji()
    val glideImageLoad = GlideImageLoad()

    // フォントなど
    lateinit var font: Typeface

    // 詳細表示してるCardViewのトゥートID配列
    private val infoVISIBLEList = arrayListOf<String>()

    // TextViewでふぉからー
    lateinit var defaultTextColor: ColorStateList

    // レイアウトの定数（onCreateViewHolder()で使う）
    companion object {
        /** Mastodon トゥート */
        val TOOT_LAYOUT = 0

        /** Mastodon 通知 */
        val NOTIFICATION_LAYOUT = 1

        /** Mastodon ブースト */
        val TOOT_BOOST_LAYOUT = 2

        /** Misskey 投稿 */
        val MISSKEY_NOTE_LAYOUT = 3

        /** Misskey 通知 */
        val MISSKEY_NOTIFICATION_LAYOUT = 4

        /** Misskey Renote */
        val MISSKEY_RENOTE_LAYOUT = 5
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        // レイアウト分岐
        val view = when (viewType) {
            TOOT_BOOST_LAYOUT -> BoostViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.adapter_boost, parent, false))
            TOOT_LAYOUT -> TootViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.adapter_timeline, parent, false))
            NOTIFICATION_LAYOUT -> NotificationViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.adapter_notification, parent, false))
            MISSKEY_NOTE_LAYOUT -> MisskeyNoteViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.adapter_misskey_note, parent, false))
            MISSKEY_NOTIFICATION_LAYOUT -> MisskeyNotificationViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.adapter_misskey_notification, parent, false))
            MISSKEY_RENOTE_LAYOUT -> MisskeyRenoteViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.adapter_misskey_renote, parent, false))
            else -> TootViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.adapter_timeline, parent, false))
        }
        return view
    }

    // 通知と投稿で分岐させる
    override fun getItemViewType(position: Int): Int {
        return when {
            // Mastodon
            timeLineItemDataList[position].statusData != null && timeLineItemDataList[position].statusData!!.reblogStatusData != null -> TOOT_BOOST_LAYOUT
            timeLineItemDataList[position].statusData != null -> TOOT_LAYOUT
            timeLineItemDataList[position].notificationData != null -> NOTIFICATION_LAYOUT
            // Misskey
            timeLineItemDataList[position].misskeyNoteData != null && timeLineItemDataList[position].misskeyNoteData!!.renote != null -> MISSKEY_RENOTE_LAYOUT
            timeLineItemDataList[position].misskeyNoteData != null -> MISSKEY_NOTE_LAYOUT
            timeLineItemDataList[position].misskeyNotificationData != null -> MISSKEY_NOTIFICATION_LAYOUT
            else -> TOOT_LAYOUT
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when {
            holder is TootViewHolder -> {
                holder.apply {
                    // トゥート
                    val context = nameTextView.context
                    val status = timeLineItemDataList.get(position).statusData ?: return
                    // TL名
                    timeLineName.text =
                        timeLineItemDataList.get(position).customTimeLineData.timeLineName
                    // トゥート表示
                    idTextView.text = "@${status.accountData.acct}"
                    customEmoji.setCustomEmoji(nameTextView, status.accountData.displayName, status.accountData.allEmoji)
                    customEmoji.setCustomEmoji(contentTextView, status.content, status.allEmoji)
                    avatarImageView.setNullTint()
                    // 画像読み込み関数
                    loadImage(avatarImageView, context, status.accountData.avatar)
                    // お気に入り、ブースト
                    initFav(favoutiteButton, status)
                    initBoost(boostButton, status)
                    applyButton(favoutiteButton, status.favouritesCount.toString(), status.isFavourited, R.drawable.ic_star_border_black_24dp)
                    applyButton(boostButton, status.boostCount.toString(), status.isBoosted, R.drawable.ic_repeat_black_24dp)
                    // 詳細表示
                    initInfo(moreButton, infoTextView, status)
                    // 見た目
                    setCardViewStyle(cardView, timeLineName, timeLineItemDataList.get(position).customTimeLineData)
                    setFont(nameTextView, idTextView, contentTextView, timeLineName, favoutiteButton, boostButton)
                }
            }
            holder is NotificationViewHolder -> {
                holder.apply {
                    // 通知
                    val context = nameTextView.context
                    val notificationData =
                        timeLineItemDataList.get(position).notificationData ?: return
                    // TL名
                    timeLineName.text =
                        timeLineItemDataList.get(position).customTimeLineData.timeLineName
                    // 通知タイプ
                    notificationTextView.text = when (notificationData.type) {
                        "favourite" -> context.getText(R.string.notification_favourite)
                        "reblog" -> context.getText(R.string.notification_reblog)
                        "mention" -> context.getText(R.string.notification_mention)
                        "follow" -> context.getText(R.string.notification_follow)
                        else -> context.getText(R.string.notification_favourite)
                    }
                    // アカウント情報
                    idTextView.text = "@${notificationData.accountData.acct}"
                    customEmoji.setCustomEmoji(nameTextView, notificationData.accountData.displayName, notificationData.accountData.allEmoji)
                    avatarImageView.setNullTint()
                    // 画像読み込み関数
                    loadImage(avatarImageView, context, notificationData.accountData.avatar)
                    // statusあればトゥート表示
                    if (notificationData.status != null) {
                        customEmoji.setCustomEmoji(contentTextView, notificationData.status.content, notificationData.status.allEmoji)
                    }
                    // 見た目
                    setCardViewStyle(cardView, timeLineName, timeLineItemDataList.get(position).customTimeLineData)
                    setFont(nameTextView, idTextView, contentTextView, timeLineName)
                }
            }
            holder is BoostViewHolder -> {
                holder.apply {
                    // トゥート
                    val context = nameTextView.context
                    val status = timeLineItemDataList.get(position).statusData ?: return
                    val reblogStatus = status.reblogStatusData ?: return
                    // TL名
                    timeLineName.text =
                        timeLineItemDataList.get(position).customTimeLineData.timeLineName
                    // ブースト元トゥート表示
                    boostIDTextView.text = "@${reblogStatus.accountData.acct}"
                    customEmoji.setCustomEmoji(boostNameTextView, reblogStatus.accountData.displayName, reblogStatus.accountData.allEmoji)
                    customEmoji.setCustomEmoji(boostContentTextView, reblogStatus.content, reblogStatus.allEmoji)
                    boostAvatarImageView.setNullTint()
                    // 画像読み込み関数
                    loadImage(boostAvatarImageView, context, reblogStatus.accountData.avatar)
                    // ブーストしたユーザーのアバター
                    idTextView.text = "@${status.accountData.acct}"
                    customEmoji.setCustomEmoji(nameTextView, "${status.accountData.displayName}<br>${context.getString(R.string.boosted)}", status.accountData.allEmoji)
                    avatarImageView.setNullTint()
                    // 画像読み込み関数
                    loadImage(avatarImageView, context, status.accountData.avatar)
                    // お気に入り、ブースト
                    initFav(favoutiteButton, status)
                    initBoost(boostButton, status)
                    applyButton(favoutiteButton, reblogStatus.favouritesCount.toString(), reblogStatus.isFavourited, R.drawable.ic_star_border_black_24dp)
                    applyButton(boostButton, reblogStatus.boostCount.toString(), reblogStatus.isBoosted, R.drawable.ic_repeat_black_24dp)
                    // 詳細表示
                    initInfo(moreButton, infoTextView, status)
                    // 見た目
                    setCardViewStyle(cardView, timeLineName, timeLineItemDataList.get(position).customTimeLineData)
                    setFont(boostNameTextView, boostIDTextView, boostContentTextView, nameTextView, idTextView, timeLineName, favoutiteButton, boostButton)
                }
            }
            // Misskey
            holder is MisskeyNoteViewHolder -> {
                holder.apply {
                    // Note
                    val context = nameTextView.context
                    val status = timeLineItemDataList.get(position).misskeyNoteData ?: return
                    // TL名
                    timeLineName.text =
                        timeLineItemDataList.get(position).customTimeLineData.timeLineName
                    // トゥート表示
                    idTextView.text = "@${status.user.username}"
                    customEmoji.setCustomEmoji(nameTextView, status.user.name, status.user.emoji)
                    customEmoji.setCustomEmoji(contentTextView, status.text.escapeToBrTag(), status.emoji)
                    avatarImageView.setNullTint()
                    // 画像読み込み関数
                    loadImage(avatarImageView, context, status.user.avatarUrl)
                    // リアクション、りのーと
                    reactionTextView.setText(status.reaction.joinToString(separator = " | ") { misskeyReactionData -> "${misskeyReactionData.reaction}:${misskeyReactionData.reactionCount}" })
                    initMisskeyFav(favoutiteButton, status)
                    initRenote(boostButton, status)
                    applyButton(boostButton, status.renoteCount.toString(), status.isRenote, R.drawable.ic_repeat_black_24dp)
                    // 詳細表示
                    initMisskeyInfo(moreButton, infoTextView, status)
                    // 見た目
                    setCardViewStyle(cardView, timeLineName, timeLineItemDataList.get(position).customTimeLineData)
                    setFont(nameTextView, idTextView, contentTextView, timeLineName, favoutiteButton, boostButton)
                }
            }
            holder is MisskeyNotificationViewHolder -> {
                holder.apply {
                    // 通知
                    val context = nameTextView.context
                    val notificationData =
                        timeLineItemDataList.get(position).misskeyNotificationData ?: return
                    // TL名
                    timeLineName.text =
                        timeLineItemDataList.get(position).customTimeLineData.timeLineName
                    // 通知タイプ
                    notificationTextView.text = when (notificationData.type) {
                        "reaction" -> notificationData.reaction
                        "renote" -> context.getText(R.string.notification_reblog)
                        "mention" -> context.getText(R.string.notification_mention)
                        "follow" -> context.getText(R.string.notification_follow)
                        else -> context.getText(R.string.notification_favourite)
                    }
                    // アカウント情報
                    idTextView.text = "@${notificationData.user.username}"
                    customEmoji.setCustomEmoji(nameTextView, notificationData.user.name, notificationData.user.emoji)
                    avatarImageView.setNullTint()
                    // 画像読み込み関数
                    loadImage(avatarImageView, context, notificationData.user.avatarUrl)
                    // statusあればトゥート表示
                    if (notificationData.note != null) {
                        customEmoji.setCustomEmoji(contentTextView, notificationData.note.text.escapeToBrTag(), notificationData.note.emoji)
                    }
                    // 見た目
                    setCardViewStyle(cardView, timeLineName, timeLineItemDataList.get(position).customTimeLineData)
                    setFont(nameTextView, idTextView, contentTextView, timeLineName)
                }
            }
            holder is MisskeyRenoteViewHolder -> {
                holder.apply {
                    // トゥート
                    val context = nameTextView.context
                    val status = timeLineItemDataList.get(position).misskeyNoteData ?: return
                    val reblogStatus = status.renote ?: return
                    // TL名
                    timeLineName.text =
                        timeLineItemDataList.get(position).customTimeLineData.timeLineName
                    // ブースト元トゥート表示
                    boostIDTextView.text = "@${reblogStatus.user.username}"
                    customEmoji.setCustomEmoji(boostNameTextView, reblogStatus.user.name, reblogStatus.user.emoji)
                    customEmoji.setCustomEmoji(boostContentTextView, reblogStatus.text.escapeToBrTag(), reblogStatus.emoji)
                    boostAvatarImageView.setNullTint()
                    // 画像読み込み関数
                    loadImage(boostAvatarImageView, context, reblogStatus.user.avatarUrl)
                    // ブーストしたユーザーのアバター
                    idTextView.text = "@${status.user.username}"
                    customEmoji.setCustomEmoji(nameTextView, "${status.user.name}<br>${context.getString(R.string.renoted)}", status.user.emoji)
                    // nullのときある
                    if (status.text != "null") {
                        customEmoji.setCustomEmoji(contentTextView, status.text.escapeToBrTag(), status.emoji)
                    } else {
                        contentTextView.visibility = View.GONE
                    }
                    avatarImageView.setNullTint()
                    // 画像読み込み関数
                    loadImage(avatarImageView, context, status.user.avatarUrl)
                    // リアクション、りのーと
                    initMisskeyFav(favoutiteButton, status)
                    initRenote(boostButton, status)
                    applyButton(boostButton, status.renoteCount.toString(), status.isRenote, R.drawable.ic_repeat_black_24dp)
                    // 詳細表示
                    initMisskeyInfo(moreButton, infoTextView, status)
                    // 見た目
                    setCardViewStyle(cardView, timeLineName, timeLineItemDataList.get(position).customTimeLineData)
                    setFont(boostNameTextView, boostIDTextView, boostContentTextView, nameTextView, idTextView, contentTextView, timeLineName, favoutiteButton, boostButton)
                }
            }
        }
    }

    /**
     * 画像を読み込む関数。
     * モバイルデータ回線なら読み込まいとか
     * */
    private fun loadImage(avatarImageView: ImageView, context: Context?, url: String) {
        // 画像読み込み設定
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        val isHideImage = preferences.getBoolean("timeline_setting_image_hide", false) // 強制画像非表示
        val isMobileDataImageHide =
            preferences.getBoolean("timeline_setting_image_hide_mobile", false) && isConnectionMobileData(context) // モバイルデータ回線なら非表示
        val isStopGifAnimation = preferences.getBoolean("timeline_setting_image_gif_stop", false)
        if (isHideImage || isMobileDataImageHide) {
            // キャッシュがあれば表示。なければ消す
            glideImageLoad.loadOffline(avatarImageView, url, true, false)
        } else {
            // ネットから持ってくる
            Glide.with(avatarImageView)
                .load(url)
                .apply(RequestOptions.bitmapTransform(RoundedCorners(10)))
                .into(avatarImageView)
        }
        // GIF止める
        if (isStopGifAnimation) {
            val drawable = avatarImageView.drawable
            if (drawable is GifDrawable) {
                drawable.stop()
            }
        }
    }

    // CardViewの見た目
    private fun setCardViewStyle(cardView: CardView, textView: TextView, customTimeLineData: CustomTimeLineData) {
        (cardView as MaterialCardView).apply {
            if (!::defaultTextColor.isInitialized) {
                defaultTextColor = TextView(context).textColors
            }
            strokeWidth = 2
            if (customTimeLineData.timeLineTextColor.isNotEmpty()) {
                textView.setTextColor(Color.parseColor(customTimeLineData.timeLineTextColor))
            } else {
                textView.setTextColor(defaultTextColor)
            }
            // 色設定があれば
            if (customTimeLineData.timeLineBackground.isNotEmpty()) {
                setStrokeColor(ColorStateList.valueOf(Color.parseColor(customTimeLineData.timeLineBackground)))
            } else {
                setStrokeColor(ColorStateList.valueOf(Color.parseColor("#757575")))
            }
             if (isDarkMode(context)) {
                 setCardBackgroundColor(ColorStateList.valueOf(Color.parseColor("#000000")))
             } else {
                 setCardBackgroundColor(ColorStateList.valueOf(Color.parseColor("#ffffff")))
             }
            alpha = 0.8F
        }
    }

    /**
     * フォントをTextViewに適用する
     * @param textView 可変長引数です。指定したViewをまとめてフォント設定します
     * */
    fun setFont(vararg textView: TextView) {
        textView.forEach {
            if (!::font.isInitialized) {
                val file = File("${it.context.getExternalFilesDir(null)}/font.ttf")
                if (file.exists()) {
                    font = Typeface.createFromFile(file)
                } else {
                    font = Typeface.DEFAULT
                }
            }
            it.typeface = font
        }
    }


    private fun initInfo(moreButton: Button, infoTextView: TextView, status: StatusData) {
        // 表示・非表示
        moreButton.setOnClickListener {
            if (infoTextView.visibility == View.GONE) {
                infoTextView.visibility = View.VISIBLE
                (moreButton as MaterialButton).icon =
                    infoTextView.context.getDrawable(R.drawable.ic_expand_less_black_24dp)
                infoVISIBLEList.add(status.id)
            } else {
                infoTextView.visibility = View.GONE
                (moreButton as MaterialButton).icon =
                    infoTextView.context.getDrawable(R.drawable.ic_expand_more_black_24dp)
                infoVISIBLEList.remove(status.id)
            }
        }
        // リサイクルされるので
        if (infoVISIBLEList.contains(status.id)) {
            infoTextView.visibility = View.VISIBLE
            (moreButton as MaterialButton).icon =
                infoTextView.context.getDrawable(R.drawable.ic_expand_less_black_24dp)
        } else {
            infoTextView.visibility = View.GONE
            (moreButton as MaterialButton).icon =
                infoTextView.context.getDrawable(R.drawable.ic_expand_more_black_24dp)
        }

        val text = """
                投稿日時：
                ${status.createdAt.toTimeFormat()}
                トゥートID：
                ${status.id}
            """.trimIndent()
        infoTextView.text = text
    }

    private fun initMisskeyInfo(moreButton: Button, infoTextView: TextView, note: MisskeyNoteData) {
        // 表示・非表示
        moreButton.setOnClickListener {
            if (infoTextView.visibility == View.GONE) {
                infoTextView.visibility = View.VISIBLE
                (moreButton as MaterialButton).icon =
                    infoTextView.context.getDrawable(R.drawable.ic_expand_less_black_24dp)
                infoVISIBLEList.add(note.noteId)
            } else {
                infoTextView.visibility = View.GONE
                (moreButton as MaterialButton).icon =
                    infoTextView.context.getDrawable(R.drawable.ic_expand_more_black_24dp)
                infoVISIBLEList.remove(note.noteId)
            }
        }
        // リサイクルされるので
        if (infoVISIBLEList.contains(note.noteId)) {
            infoTextView.visibility = View.VISIBLE
            (moreButton as MaterialButton).icon =
                infoTextView.context.getDrawable(R.drawable.ic_expand_less_black_24dp)
        } else {
            infoTextView.visibility = View.GONE
            (moreButton as MaterialButton).icon =
                infoTextView.context.getDrawable(R.drawable.ic_expand_more_black_24dp)
        }

        val text = """
                投稿日時：
                ${note.createdAt.toTimeFormat()}
                ノートID：
                ${note.noteId}
            """.trimIndent()
        infoTextView.text = text
    }


    /**
     * ふぁぼ、ブーストを適用する
     * @param button ボタン
     * @param defaultDrawable ふぁぼ/ブーストしてないときのDrawable
     * @param isCheck doneにする場合はtrue
     * @param text Buttonに入れるテキスト
     * */
    private fun applyButton(button: Button, text: String, isCheck: Boolean, defaultDrawable: Int) {
        button.text = text
        (button as MaterialButton).icon = if (isCheck) {
            button.context.getDrawable(R.drawable.ic_done_black_24dp)
        } else {
            button.context.getDrawable(defaultDrawable)
        }
    }

    private fun initBoost(button: Button, status: StatusData) {
        val context = button.context
        // ブーストAPI叩く
        button.setOnClickListener {
            mainActivity.showSnackBar(context.getString(R.string.boost_message), context.getString(R.string.boost)) {
                GlobalScope.launch(Dispatchers.Main) {
                    val statusAPI = StatusAPI(status.instanceToken)
                    val response = withContext(Dispatchers.IO) {
                        if (!status.isBoosted) {
                            statusAPI.postStatusBoost(status.id, status.instanceToken).await()
                        } else {
                            statusAPI.postDeleteStatusBoost(status.id, status.instanceToken).await()
                        }
                    }
                    if (response.isSuccessful) {
                        mainActivity.showSnackBar("${context.getString(R.string.boost_ok)}：${status.id}")
                        // 反転
                        status.isBoosted = !status.isBoosted
                        // UI反映
                        applyButton(button, status.favouritesCount.toString(), status.isFavourited, R.drawable.ic_star_border_black_24dp)
                    } else {
                        mainActivity.showSnackBar("${context.getString(R.string.error)}：${response.code}")
                    }
                }
            }
        }
    }

    private fun initMisskeyFav(button: Button, note: MisskeyNoteData) {
        button.setOnClickListener {
            // りアクションBottomSheet出す
            val misskeyReactionBottomSheet = MisskeyReactionBottomSheet(note)
            misskeyReactionBottomSheet.show(mainActivity.supportFragmentManager, "reaction")
        }
    }

    // renoteする。
    private fun initRenote(button: Button, note: MisskeyNoteData) {
        val context = button.context
        button.setOnClickListener {
            // Renoteする
            val misskeyNoteAPI = MisskeyNoteAPI(note.instanceToken)
            val misskeyParser = MisskeyParser()
            GlobalScope.launch(Dispatchers.Main) {
                val response = withContext(Dispatchers.IO) {
                    misskeyNoteAPI.notesCreate("", MisskeyNoteAPI.MISSKEY_VISIBILITY_PUBLIC, true, note.renote!!.noteId).await()
                }
                if (response.isSuccessful) {
                    // 成功
                    val responseNote =
                        misskeyParser.parseNote(response.body?.string()!!, note.instanceToken)
                    applyMisskeyButton(button, responseNote.renoteCount.toString(), true, R.drawable.ic_repeat_black_24dp)
                    // 適用
                    note.isRenote = true
                    note.renoteCount = responseNote.renoteCount
                } else {
                    mainActivity.showSnackBar("${context.getString(R.string.error)}：${response.code}")
                }
            }
        }
    }

    /**
     * UIに反映させる
     * @param button ボタン
     * @param isCheck チェックマーク付けるなら
     * @param drawable isCheckedがfalseのときのDrawable
     * @param text Buttonにセットするテキスト
     * */
    fun applyMisskeyButton(button: Button, text: String, isCheck: Boolean, drawable: Int) {
        (button as MaterialButton).apply {
            setText(text)
            if (isCheck) {
                icon = context.getDrawable(R.drawable.ic_done_black_24dp)
            } else {
                icon = context.getDrawable(drawable)
            }
        }
    }

    private fun initFav(button: Button, status: StatusData) {
        val context = button.context
        button.setOnClickListener {
            // 本当に叩いていいか聞く
            mainActivity.showSnackBar(context.getString(R.string.favourite_message), context.getString(R.string.favourite)) {
                GlobalScope.launch(Dispatchers.Main) {
                    // ふぁぼAPI叩く
                    val statusAPI = StatusAPI(status.instanceToken)
                    // コルーチンガチ有能
                    val response = withContext(Dispatchers.IO) {
                        if (!status.isFavourited) {
                            statusAPI.postStatusFav(status.id, status.instanceToken).await()// ふぁぼ
                        } else {
                            statusAPI.postDeleteStatusFav(status.id, status.instanceToken).await()// ふぁぼ取り消し
                        }
                    }
                    if (response.isSuccessful) {
                        mainActivity.showSnackBar("${context.getString(R.string.favourite_ok)}：${status.id}")
                        // 反転
                        status.isFavourited = !status.isFavourited
                        // UI反映
                        applyButton(button, status.favouritesCount.toString(), status.isFavourited, R.drawable.ic_star_border_black_24dp)
                    } else {
                        mainActivity.showSnackBar("${context.getString(R.string.error)}：${response.code}")
                    }
                }
            }
        }
    }

    // トゥートViewHolder
    inner class TootViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView = itemView.findViewById<CardView>(R.id.adapter_timeline_cardview)
        val timeLineName = itemView.findViewById<TextView>(R.id.adapter_timeline_name)
        val nameTextView = itemView.findViewById<TextView>(R.id.adapter_timeline_user_name)
        val idTextView = itemView.findViewById<TextView>(R.id.adapter_timeline_id)
        val contentTextView = itemView.findViewById<TextView>(R.id.adapter_timeline_content)
        val avatarImageView = itemView.findViewById<ImageView>(R.id.adapter_timeline_avatar)
        val favoutiteButton = itemView.findViewById<Button>(R.id.adapter_timeline_favourite)
        val boostButton = itemView.findViewById<Button>(R.id.adapter_timeline_boost)

        // 詳細表示
        val moreButton = itemView.findViewById<Button>(R.id.adapter_timeline_more)
        val infoTextView = itemView.findViewById<TextView>(R.id.adapter_timeline_info_textview)
    }

    // 通知ViewHolder
    inner class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView = itemView.findViewById<CardView>(R.id.adapter_timeline_cardview)
        val notificationTextView =
            itemView.findViewById<TextView>(R.id.adapter_notification_type)
        val timeLineName = itemView.findViewById<TextView>(R.id.adapter_timeline_name)
        val nameTextView = itemView.findViewById<TextView>(R.id.adapter_timeline_user_name)
        val idTextView = itemView.findViewById<TextView>(R.id.adapter_timeline_id)
        val contentTextView = itemView.findViewById<TextView>(R.id.adapter_timeline_content)
        val avatarImageView = itemView.findViewById<ImageView>(R.id.adapter_timeline_avatar)
    }

    // ブーストViewHolder
    inner class BoostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView = itemView.findViewById<CardView>(R.id.adapter_timeline_cardview)
        val timeLineName = itemView.findViewById<TextView>(R.id.adapter_timeline_name)
        val nameTextView = itemView.findViewById<TextView>(R.id.adapter_timeline_user_name)
        val idTextView = itemView.findViewById<TextView>(R.id.adapter_timeline_id)
        val avatarImageView = itemView.findViewById<ImageView>(R.id.adapter_timeline_avatar)

        // Boost
        val boostNameTextView =
            itemView.findViewById<TextView>(R.id.adapter_timeline_boost_user_name)
        val boostIDTextView = itemView.findViewById<TextView>(R.id.adapter_timeline_boost_id)
        val boostAvatarImageView =
            itemView.findViewById<ImageView>(R.id.adapter_timeline_boost_avatar)
        val boostContentTextView =
            itemView.findViewById<TextView>(R.id.adapter_timeline_boost_content)

        // fav
        val favoutiteButton = itemView.findViewById<Button>(R.id.adapter_timeline_favourite)
        val boostButton = itemView.findViewById<Button>(R.id.adapter_timeline_boost)

        // 詳細表示
        val moreButton = itemView.findViewById<Button>(R.id.adapter_timeline_more)
        val infoTextView = itemView.findViewById<TextView>(R.id.adapter_timeline_info_textview)
    }

    // Misskey Note ViewHolder
    inner class MisskeyNoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView = itemView.findViewById<CardView>(R.id.adapter_timeline_cardview)
        val timeLineName = itemView.findViewById<TextView>(R.id.adapter_timeline_name)
        val nameTextView = itemView.findViewById<TextView>(R.id.adapter_timeline_user_name)
        val idTextView = itemView.findViewById<TextView>(R.id.adapter_timeline_id)
        val contentTextView = itemView.findViewById<TextView>(R.id.adapter_timeline_content)
        val avatarImageView = itemView.findViewById<ImageView>(R.id.adapter_timeline_avatar)
        val favoutiteButton = itemView.findViewById<Button>(R.id.adapter_timeline_favourite)
        val boostButton = itemView.findViewById<Button>(R.id.adapter_timeline_boost)
        val reactionTextView = itemView.findViewById<TextView>(R.id.adapter_timeline_reaction)

        // 詳細表示
        val moreButton = itemView.findViewById<Button>(R.id.adapter_timeline_more)
        val infoTextView = itemView.findViewById<TextView>(R.id.adapter_timeline_info_textview)
    }

    // Misskey 通知ViewHolder
    inner class MisskeyNotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView = itemView.findViewById<CardView>(R.id.adapter_timeline_cardview)
        val notificationTextView =
            itemView.findViewById<TextView>(R.id.adapter_notification_type)
        val timeLineName = itemView.findViewById<TextView>(R.id.adapter_timeline_name)
        val nameTextView = itemView.findViewById<TextView>(R.id.adapter_timeline_user_name)
        val idTextView = itemView.findViewById<TextView>(R.id.adapter_timeline_id)
        val contentTextView = itemView.findViewById<TextView>(R.id.adapter_timeline_content)
        val avatarImageView = itemView.findViewById<ImageView>(R.id.adapter_timeline_avatar)
    }

    // Misskey Renote ViewHolder
    inner class MisskeyRenoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView = itemView.findViewById<CardView>(R.id.adapter_timeline_cardview)
        val timeLineName = itemView.findViewById<TextView>(R.id.adapter_timeline_name)
        val nameTextView = itemView.findViewById<TextView>(R.id.adapter_timeline_user_name)
        val idTextView = itemView.findViewById<TextView>(R.id.adapter_timeline_id)
        val avatarImageView = itemView.findViewById<ImageView>(R.id.adapter_timeline_avatar)
        val contentTextView = itemView.findViewById<TextView>(R.id.adapter_timeline_content)

        // Boost
        val boostNameTextView =
            itemView.findViewById<TextView>(R.id.adapter_timeline_boost_user_name)
        val boostIDTextView = itemView.findViewById<TextView>(R.id.adapter_timeline_boost_id)
        val boostAvatarImageView =
            itemView.findViewById<ImageView>(R.id.adapter_timeline_boost_avatar)
        val boostContentTextView =
            itemView.findViewById<TextView>(R.id.adapter_timeline_boost_content)

        // fav
        val favoutiteButton = itemView.findViewById<Button>(R.id.adapter_timeline_favourite)
        val boostButton = itemView.findViewById<Button>(R.id.adapter_timeline_boost)

        // 詳細表示
        val moreButton = itemView.findViewById<Button>(R.id.adapter_timeline_more)
        val infoTextView = itemView.findViewById<TextView>(R.id.adapter_timeline_info_textview)
    }

    override fun getItemCount(): Int {
        return timeLineItemDataList.size
    }

    // 画像を非表示するか
    fun isImageHide(context: Context?): Boolean {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        return preferences.getBoolean("timeline_setting_image_hide", false)
    }

    // モバイルデータ通信で画像を非表示にするか。返り値は設定有効+モバイルデータ通信ならtrue
    fun isImageHideMobileData(context: Context?): Boolean {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        return preferences.getBoolean("timeline_setting_image_hide_mobile", false) && isConnectionMobileData(context)
    }

}