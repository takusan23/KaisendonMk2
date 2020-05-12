package io.github.takusan23.kaisendonmk2

import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.preference.PreferenceManager
import com.google.android.material.snackbar.Snackbar
import io.github.takusan23.kaisendonmk2.API.StatusAPI
import io.github.takusan23.kaisendonmk2.API.getInstanceToken
import io.github.takusan23.kaisendonmk2.BottomFragment.MenuBottomSheet
import io.github.takusan23.kaisendonmk2.Fragment.TimeLineFragment
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_timeline.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    lateinit var prefSetting: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 初期化
        supportActionBar?.hide()

        prefSetting = PreferenceManager.getDefaultSharedPreferences(this)

        //  // ログイン情報ない
        //  if (prefSetting.getString("instance", null) == null) {
        //      val intent = Intent(this, LoginActivity::class.java)
        //      startActivity(intent)
        //      return
        //  }

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
        activity_main_post_card.visibility = View.GONE
        floatingActionButton.setImageDrawable(getDrawable(R.drawable.ic_create_black_24dp))
    }

    // 投稿領域を表示
    fun showPostCard() {
        activity_main_post_card.visibility = View.VISIBLE
        floatingActionButton.setImageDrawable(getDrawable(R.drawable.ic_close_black_24dp))
    }

    // TimeLineFragment取得
    fun getTimeLineFragment() =
        supportFragmentManager.findFragmentById(R.id.activity_main_fragment) as TimeLineFragment

    private fun initPostCard() {
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
                        val statusAPI = StatusAPI(getInstanceToken(this@MainActivity))
                        statusAPI.postStatus(statusText, StatusAPI.VISIBILITY_DIRECT).await()
                    }
                    // 帰ってきたらUIスレッドに戻る
                    if (response.isSuccessful) {
                        showSnackBar(getString(R.string.post_ok))
                    } else {
                        showSnackBar(getString(R.string.post_error))
                    }
                    // Card非表示
                    hidePostCard()
                }
            }
        }
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
