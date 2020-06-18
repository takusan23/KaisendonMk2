package io.github.takusan23.kaisendonmk2.Adapter

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.core.net.toUri
import androidx.core.text.HtmlCompat
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.shape.ShapeAppearanceModel
import com.google.android.material.snackbar.Snackbar
import io.github.takusan23.kaisendonmk2.BottomFragment.MisskeyReactionBottomSheet
import io.github.takusan23.kaisendonmk2.BottomFragment.QuickProfileBottomSheet
import io.github.takusan23.kaisendonmk2.CustomEmoji.CustomEmoji
import io.github.takusan23.kaisendonmk2.DataClass.CustomTimeLineData
import io.github.takusan23.kaisendonmk2.DataClass.StatusData
import io.github.takusan23.kaisendonmk2.DataClass.TimeLineItemData
import io.github.takusan23.kaisendonmk2.JSONParse.MisskeyParser
import io.github.takusan23.kaisendonmk2.MainActivity
import io.github.takusan23.kaisendonmk2.MastodonAPI.StatusAPI
import io.github.takusan23.kaisendonmk2.MisskeyAPI.MisskeyNoteAPI
import io.github.takusan23.kaisendonmk2.MisskeyAPI.MisskeyReactionAPI
import io.github.takusan23.kaisendonmk2.MisskeyDataClass.MisskeyNoteData
import io.github.takusan23.kaisendonmk2.MisskeyDataClass.MisskeyReactionData
import io.github.takusan23.kaisendonmk2.R
import io.github.takusan23.kaisendonmk2.TimeLine.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.Serializable

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
                    timeLineName.text = timeLineItemDataList.get(position).customTimeLineData.timeLineName
                    // トゥート表示
                    idTextView.text = "@${status.accountData.acct}"
                    // こんてんとわーにんぐ
                    if (status.spoilerText.isEmpty()) {
                        // CW無し
                        customEmoji.setCustomEmoji(contentTextView, status.content, status.allEmoji)
                        contentTextView.visibility = View.VISIBLE
                        cwButton.visibility = View.GONE
                        cwContentTextView.visibility = View.GONE
                    } else {
                        // CWで保護された投稿
                        // ボタン、CWテキスト表示
                        cwButton.visibility = View.VISIBLE
                        cwContentTextView.visibility = View.VISIBLE
                        contentTextView.visibility = View.GONE
                        cwButton.setOnClickListener {
                            contentTextView.apply {
                                visibility = if (visibility == View.GONE) {
                                    View.VISIBLE
                                } else {
                                    View.GONE
                                }
                            }
                        }
                        customEmoji.setCustomEmoji(cwContentTextView, status.spoilerText, status.allEmoji)
                        customEmoji.setCustomEmoji(contentTextView, status.content, status.allEmoji)
                    }
                    customEmoji.setCustomEmoji(nameTextView, status.accountData.displayName, status.accountData.allEmoji)
                    avatarImageView.setNullTint()
                    // 画像読み込み関数
                    loadImage(avatarImageView, context, status.accountData.avatar)
                    // QuickProfile
                    setQuickProfile(avatarImageView, status.accountData)
                    // お気に入り、ブースト
                    initFav(favoutiteButton, status)
                    initBoost(boostButton, status)
                    // 長押しでFav+BT
                    initFavBT(favoutiteButton, boostButton, status)
                    applyButton(favoutiteButton, status.favouritesCount.toString(), status.isFavourited, R.drawable.ic_star_border_black_24dp)
                    applyButton(boostButton, status.boostCount.toString(), status.isBoosted, R.drawable.ic_repeat_black_24dp)
                    // 詳細表示
                    initInfo(moreButton, infoTextView, status)
                    // 見た目
                    // setCardViewStyle(cardView, timeLineName, timeLineItemDataList.get(position).customTimeLineData)
                    setFont(nameTextView, idTextView, contentTextView, timeLineName, favoutiteButton, boostButton)
                    // 画像表示
                    mediaLinearLayout.removeAllViews()
                    // 添付画像表示
                    loadAttachImage(mediaLinearLayout, status.mediaAttachments)
                }
            }
            holder is NotificationViewHolder -> {
                holder.apply {
                    // 通知
                    val context = nameTextView.context
                    val notificationData = timeLineItemDataList.get(position).notificationData ?: return
                    // TL名
                    timeLineName.text = timeLineItemDataList.get(position).customTimeLineData.timeLineName
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
                    // QuickProfile
                    setQuickProfile(avatarImageView, notificationData.accountData)
                    // 見た目
//                    setCardViewStyle(cardView, timeLineName, timeLineItemDataList.get(position).customTimeLineData)
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
                    timeLineName.text = timeLineItemDataList.get(position).customTimeLineData.timeLineName
                    // ブースト元トゥート表示
                    boostIDTextView.text = "@${reblogStatus.accountData.acct}"
                    customEmoji.setCustomEmoji(boostNameTextView, reblogStatus.accountData.displayName, reblogStatus.accountData.allEmoji)
                    customEmoji.setCustomEmoji(boostContentTextView, reblogStatus.content, reblogStatus.allEmoji)
                    boostAvatarImageView.setNullTint()
                    // QuickProfile
                    setQuickProfile(boostAvatarImageView, reblogStatus.accountData)
                    // 画像読み込み関数
                    loadImage(boostAvatarImageView, context, reblogStatus.accountData.avatar)
                    // ブーストしたユーザーのアバター
                    idTextView.text = "@${status.accountData.acct}"
                    customEmoji.setCustomEmoji(nameTextView, "${status.accountData.displayName}<br>${context.getString(R.string.boosted)}", status.accountData.allEmoji)
                    avatarImageView.setNullTint()
                    // QuickProfile
                    setQuickProfile(avatarImageView, status.accountData)
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
//                    setCardViewStyle(cardView, timeLineName, timeLineItemDataList.get(position).customTimeLineData)
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
                    timeLineName.text = timeLineItemDataList.get(position).customTimeLineData.timeLineName
                    // トゥート表示
                    idTextView.text = "@${status.user.username}"
                    customEmoji.setCustomEmoji(nameTextView, status.user.name, status.user.emoji)
                    customEmoji.setCustomEmoji(contentTextView, status.text.escapeToBrTag(), status.emoji)
                    avatarImageView.setNullTint()
                    // QuickProfile
                    setQuickProfile(avatarImageView, status.user)
                    // 画像読み込み関数
                    loadImage(avatarImageView, context, status.user.avatarUrl)
                    // リアクション無いとき（かなしいのだわ）TextView非表示
                    if (status.reaction.isEmpty()) {
                        reactionChipGroup.visibility = View.GONE
                    } else {
                        reactionChipGroup.visibility = View.VISIBLE
                    }
                    setReaction(reactionChipGroup, status, position)
                    // reactionTextView.text = status.reaction.joinToString(separator = " | ") { misskeyReactionData -> "${misskeyReactionData.reaction}:${misskeyReactionData.reactionCount}" }
                    initMisskeyFav(favoutiteButton, status)
                    initRenote(boostButton, status)
                    applyButton(boostButton, status.renoteCount.toString(), status.isRenote, R.drawable.ic_repeat_black_24dp)
                    // 詳細表示
                    initMisskeyInfo(moreButton, infoTextView, status)
                    // 見た目
//                    setCardViewStyle(cardView, timeLineName, timeLineItemDataList[position].customTimeLineData)
                    setFont(nameTextView, idTextView, contentTextView, timeLineName, favoutiteButton, boostButton)
                    // 添付画像表示
                    loadAttachImage(mediaLinearLayout, status.fields)
                }
            }
            holder is MisskeyNotificationViewHolder -> {
                holder.apply {
                    // 通知
                    val context = nameTextView.context
                    val notificationData = timeLineItemDataList.get(position).misskeyNotificationData ?: return
                    // TL名
                    timeLineName.text = timeLineItemDataList.get(position).customTimeLineData.timeLineName
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
                    // QuickProfile
                    setQuickProfile(avatarImageView, notificationData.user)
                    // 画像読み込み関数
                    loadImage(avatarImageView, context, notificationData.user.avatarUrl)
                    // statusあればトゥート表示
                    if (notificationData.note != null) {
                        customEmoji.setCustomEmoji(contentTextView, notificationData.note.text.escapeToBrTag(), notificationData.note.emoji)
                    }
                    // 見た目
//                    setCardViewStyle(cardView, timeLineName, timeLineItemDataList[position].customTimeLineData)
                    setFont(nameTextView, idTextView, contentTextView, timeLineName)
                }
            }
            holder is MisskeyRenoteViewHolder -> {
                holder.apply {
                    // Note
                    val context = nameTextView.context
                    val status = timeLineItemDataList.get(position).misskeyNoteData ?: return
                    val reblogStatus = status.renote ?: return
                    // TL名
                    timeLineName.text = timeLineItemDataList.get(position).customTimeLineData.timeLineName
                    // ブースト元トゥート表示
                    boostIDTextView.text = "@${reblogStatus.user.username}"
                    customEmoji.setCustomEmoji(boostNameTextView, reblogStatus.user.name, reblogStatus.user.emoji)
                    customEmoji.setCustomEmoji(boostContentTextView, reblogStatus.text.escapeToBrTag(), reblogStatus.emoji)
                    boostAvatarImageView.setNullTint()
                    // QuickProfile
                    setQuickProfile(boostAvatarImageView, reblogStatus.user)
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
                    // QuickProfile
                    setQuickProfile(avatarImageView, status.user)
                    // 画像読み込み関数
                    loadImage(avatarImageView, context, status.user.avatarUrl)
                    // リアクション、りのーと
                    initMisskeyFav(favoutiteButton, status)
                    initRenote(boostButton, status)
                    applyButton(boostButton, status.renoteCount.toString(), status.isRenote, R.drawable.ic_repeat_black_24dp)
                    // 詳細表示
                    initMisskeyInfo(moreButton, infoTextView, status)
                    // 見た目
//                    setCardViewStyle(cardView, timeLineName, timeLineItemDataList.get(position).customTimeLineData)
                    setFont(boostNameTextView, boostIDTextView, boostContentTextView, nameTextView, idTextView, contentTextView, timeLineName, favoutiteButton, boostButton)
                }
            }
        }
    }

    /**
     * 長押しでFavとBTできるやつ
     * */
    private fun initFavBT(favouriteButton: Button?, boostButton: Button, status: StatusData) {
        favouriteButton?.setOnLongClickListener {
            val context = it.context
            // 本当に良い？
            mainActivity.showSnackBar(context.getString(R.string.fav_bt_message), context.getString(R.string.fav_bt_button)) {
                GlobalScope.launch(Dispatchers.Main) {
                    // fav
                    val favResponse = withContext(Dispatchers.IO) {
                        StatusAPI(status.instanceToken).postStatusFav(status.id, status.instanceToken).await()
                    }
                    // bt
                    val btResponse = withContext(Dispatchers.IO) {
                        StatusAPI(status.instanceToken).postStatusBoost(status.id, status.instanceToken).await()
                    }
                    if (favResponse.isSuccessful && btResponse.isSuccessful) {
                        // 成功時
                        mainActivity.showSnackBar(context.getString(R.string.fav_bt_ok))
                        status.isFavourited = !status.isFavourited
                        status.isBoosted = !status.isBoosted
                        // UI反映
                        applyButton(favouriteButton, status.favouritesCount.toString(), status.isFavourited, R.drawable.ic_star_border_black_24dp)
                        applyButton(boostButton, status.boostCount.toString(), status.isBoosted, R.drawable.ic_repeat_black_24dp)
                    } else {
                        // 失敗時
                        mainActivity.showSnackBar("${context.getString(R.string.error)}\n${favResponse.code} / ${btResponse.code}")
                    }
                }
            }
            true
        }
    }

    /**
     * QuickProfile表示関数。
     * @param imageView 押すView
     * @param serializable bundleに詰める。AccountDataなど
     * */
    private fun setQuickProfile(imageView: ImageView, serializable: Serializable) {
        imageView.setOnClickListener {
            val quickProfileBottomSheet = QuickProfileBottomSheet()
            val bundle = Bundle()
            bundle.putSerializable("data", serializable)
            quickProfileBottomSheet.arguments = bundle
            quickProfileBottomSheet.show(mainActivity.supportFragmentManager, "quick")
        }
    }

    /**
     * MisskeyのリアクションChipをセットする関数。
     * @param position 配列の位置
     * @param reactionChipGroup Chipを入れるChipGroup
     * @param status MisskeyNoteData
     * */
    private fun setReaction(reactionChipGroup: ChipGroup, status: MisskeyNoteData, position: Int) {
        val context = reactionChipGroup.context
        // リアクション、りのーと
        reactionChipGroup.removeAllViews()
        status.reaction.forEach { misskeyReactionData ->
            val chip = Chip(context)
            chip.shapeAppearanceModel = ShapeAppearanceModel().withCornerSize(10f) // 丸み
            chip.text = "${misskeyReactionData.reaction}：${misskeyReactionData.reactionCount}"
            // 押したとき
            chip.setOnClickListener {
                // リアクションする
                mainActivity.showSnackBar("${context.getString(R.string.misskey_reaction_message)}：${misskeyReactionData.reaction}", context.getString(R.string.reaction)) {
                    GlobalScope.launch(Dispatchers.Main) {
                        withContext(Dispatchers.IO) {
                            MisskeyReactionAPI(status.instanceToken).reaction(status.noteId, misskeyReactionData.reaction).await()
                        }
                        Toast.makeText(context, context.getString(R.string.reaction_ok), Toast.LENGTH_SHORT).show()
                        // カウント増やす
                        timeLineItemDataList[position].misskeyNoteData?.reaction?.forEach { reaction ->
                            if (reaction.reaction == misskeyReactionData.reaction) {
                                reaction.reactionCount++
                            }
                            // さいせいせい
                            setReaction(reactionChipGroup, status, position)
                        }
                    }
                }
            }
            reactionChipGroup.addView(chip)
        }
    }

    /**
     * 添付画像表示関数。
     * @param isAlwaysShow 設定や環境（モバイルデータ時など）に関係なく強制的にインターネットから（キャッシュではなく）添付画像を表示させる場合はtrue
     * @param list 画像のURLの配列
     * @param mediaLinearLayout ImageViewを追加するLinearLayout
     * */
    private fun loadAttachImage(mediaLinearLayout: LinearLayout, list: ArrayList<String>, isAlwaysShow: Boolean = false) {
        val context = mediaLinearLayout.context
        // 画像読み込み設定
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        val isHideImage = preferences.getBoolean("timeline_setting_image_hide", false) // 強制画像非表示
        val isMobileDataImageHide =
            preferences.getBoolean("timeline_setting_image_hide_mobile", false) && isConnectionMobileData(context) // モバイルデータ回線なら非表示
        val isStopGifAnimation = preferences.getBoolean("timeline_setting_image_gif_stop", false)
        if ((isHideImage || isMobileDataImageHide) && !isAlwaysShow) {
            list.forEach { url ->
                val imageView = createAttachMediaImageView(mediaLinearLayout)
                // キャッシュがあれば表示。なければ消す
                glideImageLoad.loadOffline(imageView, url, true, false)
                imageView.setOnClickListener {
                    launchBrowser(context, url)
                }
                // GIF止める
                if (isStopGifAnimation) {
                    val drawable = imageView.drawable
                    if (drawable is GifDrawable) {
                        drawable.stop()
                    }
                }
            }
        } else {
            list.forEach { url ->
                val imageView = createAttachMediaImageView(mediaLinearLayout)
                // ネットから持ってくる
                Glide.with(context)
                    .load(url)
                    .apply(RequestOptions.bitmapTransform(RoundedCorners(10)))
                    .into(imageView)
                imageView.setOnClickListener {
                    launchBrowser(context, url)
                }
                // GIF止める
                if (isStopGifAnimation) {
                    val drawable = imageView.drawable
                    if (drawable is GifDrawable) {
                        drawable.stop()
                    }
                }
            }
        }
    }

    private fun launchBrowser(context: Context?, link: String) {
        val intent = Intent(Intent.ACTION_VIEW, link.toUri())
        context?.startActivity(intent)
    }

    /**
     * 引数に入れたLinearLayoutにImageViewを追加して、追加したImageViewを返す関数。
     * */
    private fun createAttachMediaImageView(linearLayout: LinearLayout): ImageView {
        // ImageView生成からザイズ変更
        val imageView = ImageView(linearLayout.context)
        val layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, 300)
        layoutParams.weight = 1F
        layoutParams.setMargins(5, 5, 5, 5)
        imageView.layoutParams = layoutParams
        imageView.scaleType = ImageView.ScaleType.CENTER_CROP
        imageView.setNullTint()
        linearLayout.addView(imageView)
        return imageView
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
        // val cardView = itemView.findViewById<CardView>(R.id.adapter_timeline_cardview)
        val timeLineName = itemView.findViewById<TextView>(R.id.adapter_timeline_name)
        val nameTextView = itemView.findViewById<TextView>(R.id.adapter_timeline_user_name)
        val idTextView = itemView.findViewById<TextView>(R.id.adapter_timeline_id)
        val contentTextView = itemView.findViewById<TextView>(R.id.adapter_timeline_content)
        val cwContentTextView = itemView.findViewById<TextView>(R.id.adapter_timeline_cw_content) // こんてんとわーにんぐ
        val cwButton = itemView.findViewById<Button>(R.id.adapter_timeline_cw_button) // こんてんとわーにんぐ表示

        val avatarImageView = itemView.findViewById<ImageView>(R.id.adapter_timeline_avatar)
        val favoutiteButton = itemView.findViewById<Button>(R.id.adapter_timeline_favourite)
        val boostButton = itemView.findViewById<Button>(R.id.adapter_timeline_boost)
        val mediaLinearLayout = itemView.findViewById<LinearLayout>(R.id.adapter_timeline_media)

        // 詳細表示
        val moreButton = itemView.findViewById<Button>(R.id.adapter_timeline_more)
        val infoTextView = itemView.findViewById<TextView>(R.id.adapter_timeline_info_textview)
    }

    // 通知ViewHolder
    inner class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // val cardView = itemView.findViewById<CardView>(R.id.adapter_timeline_cardview)
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
        // val cardView = itemView.findViewById<CardView>(R.id.adapter_timeline_cardview)
        val timeLineName = itemView.findViewById<TextView>(R.id.adapter_timeline_name)
        val nameTextView = itemView.findViewById<TextView>(R.id.adapter_timeline_user_name)
        val idTextView = itemView.findViewById<TextView>(R.id.adapter_timeline_id)
        val avatarImageView = itemView.findViewById<ImageView>(R.id.adapter_timeline_avatar)

        // Boost
        val boostNameTextView = itemView.findViewById<TextView>(R.id.adapter_timeline_boost_user_name)
        val boostIDTextView = itemView.findViewById<TextView>(R.id.adapter_timeline_boost_id)
        val boostAvatarImageView = itemView.findViewById<ImageView>(R.id.adapter_timeline_boost_avatar)
        val boostContentTextView = itemView.findViewById<TextView>(R.id.adapter_timeline_boost_content)

        // fav
        val favoutiteButton = itemView.findViewById<Button>(R.id.adapter_timeline_favourite)
        val boostButton = itemView.findViewById<Button>(R.id.adapter_timeline_boost)

        // 詳細表示
        val moreButton = itemView.findViewById<Button>(R.id.adapter_timeline_more)
        val infoTextView = itemView.findViewById<TextView>(R.id.adapter_timeline_info_textview)
    }

    // Misskey Note ViewHolder
    inner class MisskeyNoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // val cardView = itemView.findViewById<CardView>(R.id.adapter_timeline_cardview)
        val timeLineName = itemView.findViewById<TextView>(R.id.adapter_timeline_name)
        val nameTextView = itemView.findViewById<TextView>(R.id.adapter_timeline_user_name)
        val idTextView = itemView.findViewById<TextView>(R.id.adapter_timeline_id)
        val contentTextView = itemView.findViewById<TextView>(R.id.adapter_timeline_content)
        val avatarImageView = itemView.findViewById<ImageView>(R.id.adapter_timeline_avatar)
        val favoutiteButton = itemView.findViewById<Button>(R.id.adapter_timeline_favourite)
        val boostButton = itemView.findViewById<Button>(R.id.adapter_timeline_boost)
        val mediaLinearLayout = itemView.findViewById<LinearLayout>(R.id.adapter_timeline_media)
        val reactionChipGroup = itemView.findViewById<ChipGroup>(R.id.adapter_timeline_reaction_chip)

        // 詳細表示
        val moreButton = itemView.findViewById<Button>(R.id.adapter_timeline_more)
        val infoTextView = itemView.findViewById<TextView>(R.id.adapter_timeline_info_textview)
    }

    // Misskey 通知ViewHolder
    inner class MisskeyNotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // val cardView = itemView.findViewById<CardView>(R.id.adapter_timeline_cardview)
        val notificationTextView = itemView.findViewById<TextView>(R.id.adapter_notification_type)
        val timeLineName = itemView.findViewById<TextView>(R.id.adapter_timeline_name)
        val nameTextView = itemView.findViewById<TextView>(R.id.adapter_timeline_user_name)
        val idTextView = itemView.findViewById<TextView>(R.id.adapter_timeline_id)
        val contentTextView = itemView.findViewById<TextView>(R.id.adapter_timeline_content)
        val avatarImageView = itemView.findViewById<ImageView>(R.id.adapter_timeline_avatar)
    }

    // Misskey Renote ViewHolder
    inner class MisskeyRenoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // val cardView = itemView.findViewById<CardView>(R.id.adapter_timeline_cardview)
        val timeLineName = itemView.findViewById<TextView>(R.id.adapter_timeline_name)
        val nameTextView = itemView.findViewById<TextView>(R.id.adapter_timeline_user_name)
        val idTextView = itemView.findViewById<TextView>(R.id.adapter_timeline_id)
        val avatarImageView = itemView.findViewById<ImageView>(R.id.adapter_timeline_avatar)
        val contentTextView = itemView.findViewById<TextView>(R.id.adapter_timeline_content)

        // Boost
        val boostNameTextView = itemView.findViewById<TextView>(R.id.adapter_timeline_boost_user_name)
        val boostIDTextView = itemView.findViewById<TextView>(R.id.adapter_timeline_boost_id)
        val boostAvatarImageView = itemView.findViewById<ImageView>(R.id.adapter_timeline_boost_avatar)
        val boostContentTextView = itemView.findViewById<TextView>(R.id.adapter_timeline_boost_content)

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