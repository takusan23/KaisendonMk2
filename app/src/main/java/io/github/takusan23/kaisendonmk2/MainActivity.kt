package io.github.takusan23.kaisendonmk2

import android.content.SharedPreferences
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AnimationUtils
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.interpolator.view.animation.LinearOutSlowInInterpolator
import androidx.preference.PreferenceManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.snackbar.Snackbar
import io.github.takusan23.kaisendonmk2.API.*
import io.github.takusan23.kaisendonmk2.BottomFragment.DialogBottomSheet
import io.github.takusan23.kaisendonmk2.BottomFragment.MenuBottomSheet
import io.github.takusan23.kaisendonmk2.DataClass.AccountData
import io.github.takusan23.kaisendonmk2.DataClass.EmojiData
import io.github.takusan23.kaisendonmk2.Fragment.TimeLineFragment
import io.github.takusan23.kaisendonmk2.JSONParse.TimeLineParser
import io.github.takusan23.kaisendonmk2.TimeLine.isDarkMode
import io.github.takusan23.kaisendonmk2.TimeLine.loadMultiAccount
import io.github.takusan23.kaisendonmk2.TimeLine.setNullTint
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_main.view.*
import kotlinx.android.synthetic.main.fragment_timeline.*
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {

    lateinit var prefSetting: SharedPreferences

    lateinit var postInstanceToken: InstanceToken // Toot投稿用アカウント
    var postVisibility = "public"   // 投稿する時に使う公開範囲

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

        // タイムラインFragment
        val fragment = TimeLineFragment()
        fragment.mainActivity = this
        supportFragmentManager.beginTransaction().replace(R.id.activity_main_fragment, fragment).commit()

        // メニュー初期化
        initMenu()

        // FAB初期化
        initFab()

        // 投稿部分初期化
        initPostCard()

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

    // TimeLineFragment取得
    fun getTimeLineFragment() =
        supportFragmentManager.findFragmentById(R.id.activity_main_fragment) as TimeLineFragment

    private fun initPostCard() {
        // 公開範囲
        postVisibility = StatusAPI.VISIBILITY_PUBLIC
        val visibilityButtons = arrayListOf<DialogBottomSheet.DialogBottomSheetItem>().apply {
            add(DialogBottomSheet.DialogBottomSheetItem(getString(R.string.visibility_public), R.drawable.ic_public_black_24dp))
            add(DialogBottomSheet.DialogBottomSheetItem(getString(R.string.visibility_unlisted), R.drawable.ic_train_black_24dp))
            add(DialogBottomSheet.DialogBottomSheetItem(getString(R.string.visibility_private), R.drawable.ic_home_black_24dp))
            add(DialogBottomSheet.DialogBottomSheetItem(getString(R.string.visibility_direct), R.drawable.ic_alternate_email_black_24dp))
        }
        activity_main_toot_visibility.setOnClickListener {
            DialogBottomSheet(getString(R.string.visibility), visibilityButtons) { i, bottomSheetDialogFragment ->
                postVisibility = when (i) {
                    0 -> StatusAPI.VISIBILITY_PUBLIC
                    1 -> StatusAPI.VISIBILITY_UNLISTED
                    2 -> StatusAPI.VISIBILITY_PRIVATE
                    3 -> StatusAPI.VISIBILITY_DIRECT
                    else -> StatusAPI.VISIBILITY_DIRECT
                }
                activity_main_toot_visibility.setImageDrawable(getDrawable(visibilityButtons[i].icon))
            }.show(supportFragmentManager, "visibility")
        }
        GlobalScope.launch {
            // アカウント切り替え
            initMultiAccountBottomSheet().await()
            // えもじ
            initEmoji().await()
        }
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
                        val statusAPI = StatusAPI(postInstanceToken)
                        statusAPI.postStatus(statusText, postVisibility).await()
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

    /**
     * カスタム絵文字読み込み関数
     * アカウント切り替えしたら呼んでね
     * */
    private fun initEmoji() = GlobalScope.async(Dispatchers.Main) {
        // データ取得
        val emojiDataList = withContext(Dispatchers.IO) {
            val emojiAPI = CustomEmojiAPI(postInstanceToken)
            val response = emojiAPI.getCustomEmoji().await()
            emojiAPI.parseCustomEmoji(response.body?.string()!!)
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
        var multiAccountProfileList = arrayListOf<AccountData>()
        val accountList = withContext(Dispatchers.IO) {
            multiAccountProfileList = loadAccount().await()
            // アカウント切り替えのためのリスト
            multiAccountProfileList.map { accountData ->
                DialogBottomSheet.DialogBottomSheetItem("${accountData.displayName} @${accountData.acct} | ${accountData.instanceToken.instance}", -1, -1, accountData.avatarStatic)
            } as ArrayList<DialogBottomSheet.DialogBottomSheetItem>
        }

        // アカウントセット
        fun setAccount(position: Int) = GlobalScope.async(Dispatchers.Main) {
            activity_main_toot_account_avatar.setNullTint()
            postInstanceToken = multiAccountProfileList[position].instanceToken
            Glide.with(activity_main_toot_account_avatar)
                .load(multiAccountProfileList[position].avatarStatic)
                .apply(RequestOptions.bitmapTransform(RoundedCorners(10)))
                .into(activity_main_toot_account_avatar)
            activity_main_toot_account_name.text = accountList[position].title
            initEmoji().await()
        }
        // デフォ
        setAccount(0).await()
        // 押したら表示
        activity_main_toot_account.setOnClickListener {
            DialogBottomSheet(getString(R.string.account_switching), accountList) { i, bottomSheetDialogFragment ->
                GlobalScope.launch {
                    // 選んだとき
                    setAccount(i).await()
                }
            }.show(supportFragmentManager, "account")
        }
    }


    // アカウント読み込む
    private fun loadAccount(): Deferred<ArrayList<AccountData>> = GlobalScope.async {
        val list = arrayListOf<AccountData>()
        loadMultiAccount(this@MainActivity).forEach {
            val accountAPI = AccountAPI(it)
            val timeLineParser = TimeLineParser()
            val myAccount = accountAPI.getVerifyCredentials().await()
            list.add(timeLineParser.parseAccount(myAccount.body?.string()!!, it))
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
