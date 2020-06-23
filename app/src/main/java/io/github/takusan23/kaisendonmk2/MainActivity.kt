package io.github.takusan23.kaisendonmk2

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.interpolator.view.animation.LinearOutSlowInInterpolator
import androidx.preference.PreferenceManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.snackbar.Snackbar
import io.github.takusan23.kaisendonmk2.Activity.LoginActivity
import io.github.takusan23.kaisendonmk2.BottomFragment.DialogBottomSheet
import io.github.takusan23.kaisendonmk2.BottomFragment.MenuBottomSheet
import io.github.takusan23.kaisendonmk2.CustomEmoji.CustomEmoji
import io.github.takusan23.kaisendonmk2.DataClass.MultiAccountData
import io.github.takusan23.kaisendonmk2.Fragment.TimeLineFragment
import io.github.takusan23.kaisendonmk2.JSONParse.MisskeyParser
import io.github.takusan23.kaisendonmk2.JSONParse.TimeLineParser
import io.github.takusan23.kaisendonmk2.MastodonAPI.AccountAPI
import io.github.takusan23.kaisendonmk2.MastodonAPI.CustomEmojiAPI
import io.github.takusan23.kaisendonmk2.MastodonAPI.InstanceToken
import io.github.takusan23.kaisendonmk2.MastodonAPI.StatusAPI
import io.github.takusan23.kaisendonmk2.MisskeyAPI.MisskeyAccountAPI
import io.github.takusan23.kaisendonmk2.MisskeyAPI.MisskeyEmojiAPI
import io.github.takusan23.kaisendonmk2.MisskeyAPI.MisskeyNoteAPI
import io.github.takusan23.kaisendonmk2.TimeLine.DeviceInfo
import io.github.takusan23.kaisendonmk2.TimeLine.isDarkMode
import io.github.takusan23.kaisendonmk2.TimeLine.loadMultiAccount
import io.github.takusan23.kaisendonmk2.TimeLine.setNullTint
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_timeline.*
import kotlinx.coroutines.*


class MainActivity : AppCompatActivity() {

    lateinit var prefSetting: SharedPreferences

    lateinit var postInstanceToken: InstanceToken // Toot投稿用アカウント
    var postVisibility = "public"   // 投稿する時に使う公開範囲
    var isMisskeyLocalOnly = false // Misskeyのローカルのみ公開のやつ

    var isTabTLMode = false // タブレイアウトモード有効時true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (isDarkMode(this)) {
            // ダークモード
            setTheme(R.style.OLEDTheme)
        } else {
            // そうじゃないとき
            setTheme(R.style.AppTheme)
        }

        setContentView(R.layout.activity_main)

        // 初期化
        supportActionBar?.hide()

        prefSetting = PreferenceManager.getDefaultSharedPreferences(this)

        // ログイン情報ない
        if (prefSetting.getString("account_json", null) == null || prefSetting.getString("account_json", null) == "[]") {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            return
        }

        // 画面回転時はFragment作らない
        if (savedInstanceState == null) {
            // タイムラインFragment
            val fragment = TimeLineFragment()
            fragment.mainActivity = this
            supportFragmentManager.beginTransaction().replace(R.id.activity_main_fragment, fragment, "timeline_fragment").commit()
        }

        // メニュー初期化
        initMenu()

        // FAB初期化
        initFab()

        // 投稿部分初期化
        initPostCard()

        // スリープ無効化
        if (prefSetting.getBoolean("setting_not_sleep", false)) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

    }

    private fun initMenu() {
        bottomAppBar.setNavigationOnClickListener {
            val menuBottomSheet = MenuBottomSheet()
            menuBottomSheet.mainActivity = this
            menuBottomSheet.show(supportFragmentManager, "menu")
        }
    }

    private fun initFab() {
        // 投稿Card表示、非表示
        floatingActionButton.setOnClickListener {
            activity_main_post_card.apply {
                if (visibility == View.GONE) {
                    showPostCard()
                } else {
                    hidePostCard()
                }
            }
        }
    }

    // 投稿領域を非表示
    fun hidePostCard() {
        floatingActionButton.setImageDrawable(getDrawable(R.drawable.ic_create_black_24dp))
        // アニメーション
        val hideAnimation =
            AnimationUtils.loadAnimation(this, R.anim.toot_card_hide_animation);
        hideAnimation.interpolator = LinearOutSlowInInterpolator()
        activity_main_post_card.startAnimation(hideAnimation)
        activity_main_post_card.visibility = View.GONE
    }

    // 投稿領域を表示
    fun showPostCard() {
        floatingActionButton.setImageDrawable(getDrawable(R.drawable.ic_close_black_24dp))
        // アニメーション
        val showAnimation =
            AnimationUtils.loadAnimation(this, R.anim.toot_card_show_animation);
        showAnimation.interpolator = FastOutSlowInInterpolator()
        activity_main_post_card.startAnimation(showAnimation)
        activity_main_post_card.visibility = View.VISIBLE
    }

    /**
     * TimeLineFragmentを返します。
     * 注意：isTabTLModeがtrueの場合は落ちます。
     * */
    fun getTimeLineFragment() = supportFragmentManager.findFragmentById(R.id.activity_main_fragment) as TimeLineFragment

    private fun initPostCard() {
        GlobalScope.launch {
            // アカウント切り替え
            initMultiAccountBottomSheet().await()
            // 公開範囲
            initVisibility()
            // えもじ
            initEmoji().await()
        }
        // 共有から開いたとき
        if (intent.action == Intent.ACTION_SEND) {
            intent.extras?.apply {
                val text = this.getCharSequence(Intent.EXTRA_TEXT)
                activity_main_text_input.setText(text)
            }
            showPostCard()
        }
        // デバイス情報
        initDeviceInfo()
        // 投稿ボタン
        activity_main_post.setOnClickListener {
            // 本当に投稿しても良い？
            showSnackBar(getString(R.string.status_message), getString(R.string.post)) {
                // 投稿API叩く
                val statusText = activity_main_text_input.text.toString()
                // コルーチン
                GlobalScope.launch(Dispatchers.Main) {
                    // UIスレッドのコルーチン -> メUIスレッドではないスレッドへ切り替え
                    val response = withContext(Dispatchers.IO) {
                        if (postInstanceToken.service == "mastodon") {
                            val statusAPI = StatusAPI(postInstanceToken)
                            statusAPI.postStatus(statusText, postVisibility).await()
                        } else {
                            val noteAPI = MisskeyNoteAPI(postInstanceToken)
                            noteAPI.notesCreate(statusText, postVisibility).await()
                        }
                    }
                    // 帰ってきたらUIスレッドに戻る
                    if (response.isSuccessful) {
                        showSnackBar(getString(R.string.post_ok))
                    } else {
                        showSnackBar(getString(R.string.post_error))
                    }
                    activity_main_text_input.setText("")
                    // Card非表示
                    hidePostCard()
                }
            }
        }
    }

    private fun initDeviceInfo() {
        activity_main_toot_device.setOnClickListener {
            val items = arrayListOf<DialogBottomSheet.DialogBottomSheetItem>().apply {
                add(DialogBottomSheet.DialogBottomSheetItem(getString(R.string.device_mmc_nnc), -1, -1))
                add(DialogBottomSheet.DialogBottomSheetItem(getString(R.string.device_carrier_name), -1, -1))
                add(DialogBottomSheet.DialogBottomSheetItem(getString(R.string.device_carrier_country), -1, -1))
                add(DialogBottomSheet.DialogBottomSheetItem(getString(R.string.device_signal_level), -1, -1))
                add(DialogBottomSheet.DialogBottomSheetItem(getString(R.string.device_battery_level), -1, -1))
                add(DialogBottomSheet.DialogBottomSheetItem(getString(R.string.device_android_version), -1, -1))
                add(DialogBottomSheet.DialogBottomSheetItem(getString(R.string.device_device_name), -1, -1))
                add(DialogBottomSheet.DialogBottomSheetItem(getString(R.string.device_maker_name), -1, -1))
                add(DialogBottomSheet.DialogBottomSheetItem(getString(R.string.device_sdk_version), -1, -1))
            }
            val deviceInfo = DeviceInfo(this)
            DialogBottomSheet(getString(R.string.device_info), items) { i, bottomSheetDialogFragment ->
                val text = when (i) {
                    0 -> deviceInfo.mobilePLMN
                    1 -> deviceInfo.carrierName
                    2 -> deviceInfo.countryISO
                    3 -> deviceInfo.signalLevel.toString()
                    4 -> deviceInfo.batteryLevel.toString()
                    5 -> deviceInfo.version.toString()
                    6 -> deviceInfo.name
                    7 -> deviceInfo.maker
                    8 -> deviceInfo.sdk.toString()
                    else -> ""
                }
                activity_main_text_input.append("\n$text")
            }.show(supportFragmentManager, "device")
        }
    }

    private fun initVisibility() {
        // 公開範囲
        postVisibility = if (postInstanceToken.service == "mastodon") {
            StatusAPI.VISIBILITY_PUBLIC
        } else {
            MisskeyNoteAPI.MISSKEY_VISIBILITY_PUBLIC
        }
        activity_main_toot_visibility.setOnClickListener {
            val visibilityButtons = arrayListOf<DialogBottomSheet.DialogBottomSheetItem>().apply {
                if (postInstanceToken.service == "mastodon") {
                    add(DialogBottomSheet.DialogBottomSheetItem(getString(R.string.visibility_public), R.drawable.ic_public_black_24dp))
                    add(DialogBottomSheet.DialogBottomSheetItem(getString(R.string.visibility_unlisted), R.drawable.ic_train_black_24dp))
                    add(DialogBottomSheet.DialogBottomSheetItem(getString(R.string.visibility_private), R.drawable.ic_home_black_24dp))
                    add(DialogBottomSheet.DialogBottomSheetItem(getString(R.string.visibility_direct), R.drawable.ic_alternate_email_black_24dp))
                } else {
                    add(DialogBottomSheet.DialogBottomSheetItem(getString(R.string.misskey_visibility_public), R.drawable.ic_public_black_24dp))
                    add(DialogBottomSheet.DialogBottomSheetItem(getString(R.string.misskey_visibility_home), R.drawable.ic_train_black_24dp))
                    add(DialogBottomSheet.DialogBottomSheetItem(getString(R.string.misskey_visibility_local_only), R.drawable.ic_favorite_border_black_24dp))
                    add(DialogBottomSheet.DialogBottomSheetItem(getString(R.string.misskey_visibility_followers), R.drawable.ic_home_black_24dp))
                    add(DialogBottomSheet.DialogBottomSheetItem(getString(R.string.misskey_visibility_specified), R.drawable.ic_alternate_email_black_24dp))
                    add(DialogBottomSheet.DialogBottomSheetItem(getString(R.string.misskey_visibility_private), R.drawable.ic_account_box_black_24dp))
                }
            }
            DialogBottomSheet(getString(R.string.visibility), visibilityButtons) { i, bottomSheetDialogFragment ->
                if (postInstanceToken.service == "mastodon") {
                    when (i) {
                        0 -> postVisibility = StatusAPI.VISIBILITY_PUBLIC
                        1 -> postVisibility = StatusAPI.VISIBILITY_UNLISTED
                        2 -> postVisibility = StatusAPI.VISIBILITY_PRIVATE
                        3 -> postVisibility = StatusAPI.VISIBILITY_DIRECT
                    }
                } else {
                    isMisskeyLocalOnly = false
                    when (i) {
                        0 -> postVisibility = MisskeyNoteAPI.MISSKEY_VISIBILITY_PUBLIC
                        1 -> postVisibility = MisskeyNoteAPI.MISSKEY_VISIBILITY_HOME
                        2 -> {
                            // ローカルのみ公開
                            isMisskeyLocalOnly = true
                            postVisibility = MisskeyNoteAPI.MISSKEY_VISIBILITY_PUBLIC
                        }
                        3 -> postVisibility = MisskeyNoteAPI.MISSKEY_VISIBILITY_FOLLOWERS
                        4 -> postVisibility = MisskeyNoteAPI.MISSKEY_VISIBILITY_SPECIFIED
                        5 -> postVisibility = MisskeyNoteAPI.MISSKEY_VISIBILITY_PRIVATE
                    }
                }
                activity_main_toot_visibility.setImageDrawable(getDrawable(visibilityButtons[i].icon))
            }.show(supportFragmentManager, "visibility")
        }
    }

    /**
     * カスタム絵文字読み込み関数
     * アカウント切り替えしたら呼んでね
     * */
    private fun initEmoji() = GlobalScope.async(Dispatchers.Main) {
        // データ取得
        val emojiDataList = withContext(Dispatchers.IO) {
            val emojiAPI = CustomEmojiAPI(postInstanceToken)
            val misskeyEmojiAPI = MisskeyEmojiAPI(postInstanceToken)
            val misskeyParser = MisskeyParser()
            // カスタム絵文字取得
            if (postInstanceToken.service == "mastodon") {
                emojiAPI.parseCustomEmoji(emojiAPI.getCustomEmoji().await().body?.string()!!)
            } else {
                misskeyParser.parseEmoji(misskeyEmojiAPI.getMisskeyEmoji().await().body?.string()!!)
            }
        }
        // DialogBottomSheet
        val message = getString(R.string.custom_emoji)
        val items = emojiDataList.map { emojiData ->
            DialogBottomSheet.DialogBottomSheetItem(emojiData.shortCode, -1, -1, emojiData.url)
        }
        // 押したら追加
        activity_main_toot_emoji.setOnClickListener {
            DialogBottomSheet(message, items as ArrayList<DialogBottomSheet.DialogBottomSheetItem>) { i, bottomSheetDialogFragment ->
                activity_main_text_input.append(" :${items[i].title}:")
            }.show(supportFragmentManager, "custom_emoji")
        }
    }


    private fun initMultiAccountBottomSheet() = GlobalScope.async(Dispatchers.Main) {
        // アカウント情報取得
        var multiAccountProfileList = arrayListOf<MultiAccountData>()
        // DialogBottomSheetに入れる値
        val accountList = withContext(Dispatchers.IO) {
            multiAccountProfileList = loadAccount().await()
            // アカウント切り替えのためのリスト
            multiAccountProfileList.map { accountData ->
                if (accountData.accountData != null) {
                    DialogBottomSheet.DialogBottomSheetItem("${accountData.accountData.displayName} @${accountData.accountData.acct} | ${accountData.accountData.instanceToken.instance}", -1, -1, accountData.accountData.avatarStatic)
                } else {
                    DialogBottomSheet.DialogBottomSheetItem("${accountData.misskeyUserData!!.name} @${accountData.misskeyUserData.username} | ${accountData.misskeyUserData.instanceToken.instance}", -1, -1, accountData.misskeyUserData.avatarUrl)
                }
            } as ArrayList<DialogBottomSheet.DialogBottomSheetItem>
        }

        // アカウントセット
        fun setAccount(position: Int) = GlobalScope.async(Dispatchers.Main) {
            activity_main_toot_account_avatar.setNullTint()
            postInstanceToken = multiAccountProfileList[position].accountData?.instanceToken ?: multiAccountProfileList[position].misskeyUserData!!.instanceToken
            Glide.with(activity_main_toot_account_avatar)
                .load(multiAccountProfileList[position].accountData?.avatarStatic ?: multiAccountProfileList[position].misskeyUserData!!.avatarUrl)
                .apply(RequestOptions.bitmapTransform(RoundedCorners(10)))
                .into(activity_main_toot_account_avatar)
            // カスタム絵文字 support
            val customEmoji = CustomEmoji()
            if (multiAccountProfileList[position].service == "mastodon") {
                customEmoji.setCustomEmoji(activity_main_toot_account_name, accountList[position].title, multiAccountProfileList[position].accountData!!.allEmoji)
            } else {
                customEmoji.setCustomEmoji(activity_main_toot_account_name, accountList[position].title, multiAccountProfileList[position].misskeyUserData!!.emoji)
            }
            // 選択したアカウント覚えておく
            prefSetting.edit { putString("last_use_account", accountList[position].title) }
            initEmoji().await()
        }

        // 最後に使ったアカウントが記録されてるか
        val lastUseAccount = prefSetting.getString("last_use_account", null)
        if (lastUseAccount == null) {
            setAccount(0).await() // 記録なし
        } else {
            setAccount(accountList.indexOfFirst { dialogBottomSheetItem -> dialogBottomSheetItem.title == lastUseAccount }).await() // 記録あり
        }
        // 押したら表示
        activity_main_toot_account_name.setOnClickListener {
            DialogBottomSheet(getString(R.string.account_switching), accountList) { i, bottomSheetDialogFragment ->
                GlobalScope.launch {
                    // 選んだとき
                    setAccount(i).await()
                }
            }.show(supportFragmentManager, "account")
        }
    }


    // アカウント読み込む
    private fun loadAccount(): Deferred<ArrayList<MultiAccountData>> = GlobalScope.async {
        val list = arrayListOf<MultiAccountData>()
        loadMultiAccount(this@MainActivity).forEach {
            if (it.service == "mastodon") {
                val accountAPI = AccountAPI(it)
                val timeLineParser = TimeLineParser()
                val myAccount = accountAPI.getVerifyCredentials().await()
                list.add(MultiAccountData(it.service, timeLineParser.parseAccount(myAccount.body?.string()!!, it)))
            } else {
                val misskeyAccountAPI = MisskeyAccountAPI(it)
                val misskeyParser = MisskeyParser()
                val account = misskeyAccountAPI.getMyAccount().await()
                val userData = misskeyParser.parseUser(account.body?.string()!!, it)
                list.add(MultiAccountData(it.service, null, userData))
            }
        }
        return@async list
    }

    /**
     * Snackbarを表示する関数
     * @param message 表示するテキスト
     * @param actionText ボタンを表示する時に表示するテキスト。nullだと表示されません。
     * @param action 押したときのコールバック。nullだと表示されません
     * */
    fun showSnackBar(message: String, actionText: String? = null, action: (() -> Unit)? = null) {
        Handler(Looper.getMainLooper()).post {
            Snackbar.make(fragment_timeline_swipe, message, Snackbar.LENGTH_SHORT).apply {
                if (actionText != null && action != null) {
                    setAction(actionText) {
                        action()
                    }
                }
                anchorView = if (activity_main_post_card.visibility == View.GONE) {
                    floatingActionButton
                } else {
                    activity_main_post_card
                }
                show()
            }
        }
    }


}
