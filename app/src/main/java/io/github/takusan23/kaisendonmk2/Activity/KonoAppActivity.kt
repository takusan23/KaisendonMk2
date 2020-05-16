package io.github.takusan23.kaisendonmk2.Activity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.net.toUri
import io.github.takusan23.kaisendonmk2.R
import kotlinx.android.synthetic.main.activity_kono_app.*

class KonoAppActivity : AppCompatActivity() {

    /**
     * 定数
     * */
    val CODE_NAME = "海鮮丼"
    val TWITTER_LINK = "https://twitter.com/takusan__23"
    val MASTODON_LINK = "https://best-friends.chat/@takusan_23"
    val SOURCE_CODE = "https://github.com/takusan23/KaisendonMk2"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_kono_app)

        // アプリのバージョン
        activity_kono_app_version.text = getVersion()
        // こーどねーむ
        activity_kono_app_codename.text = CODE_NAME
        // 各種リンク
        activity_kono_app_twitter.setOnClickListener { showBrowser(TWITTER_LINK) }
        activity_kono_app_mastodon.setOnClickListener { showBrowser(MASTODON_LINK) }
        activity_kono_app_code.setOnClickListener { showBrowser(SOURCE_CODE) }
    }

    // バージョン取得関数
    private fun getVersion(): CharSequence? {
        val info = packageManager.getPackageInfo(packageName, 0)
        return info.versionName
    }

    // ブラウザで開く関数
    private fun showBrowser(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, url.toUri())
        startActivity(intent)
    }

}
