package io.github.takusan23.kaisendonmk2.Activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import io.github.takusan23.kaisendonmk2.Fragment.PreferenceFragment
import io.github.takusan23.kaisendonmk2.R
/**
 * 設定画面。PreferenceFragment見てね。
 * */
class PreferenceActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preference)

        // Fragment置く
        supportFragmentManager.beginTransaction().replace(R.id.activity_preference_fragment, PreferenceFragment()).commit()

    }
}
