package io.github.takusan23.kaisendonmk2.Activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import io.github.takusan23.kaisendonmk2.R
import kotlinx.android.synthetic.main.activity_license.*

/**
 * ライセンス
 * */
class LicenseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_license)

        val text = """
            
        """.trimIndent()

        activity_license_textview.text = text

    }
}
