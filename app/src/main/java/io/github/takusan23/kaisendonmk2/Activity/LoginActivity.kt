package io.github.takusan23.kaisendonmk2.Activity

import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import io.github.takusan23.kaisendonmk2.R
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.android.synthetic.main.activity_login.view.*

class LoginActivity : AppCompatActivity() {

    lateinit var prefSetting: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // タイトル
        supportActionBar?.title = getString(R.string.login)

        prefSetting = PreferenceManager.getDefaultSharedPreferences(this)

        activity_login_login.setOnClickListener {
            prefSetting.edit {
                putString("instance", activity_login_instance.text.toString())
                putString("token", activity_login_token.text.toString())
            }
            // 閉じる
            finish()
        }

    }
}
