package io.github.takusan23.kaisendonmk2.Fragment

import android.os.Bundle
import android.view.View
import androidx.preference.PreferenceFragmentCompat
import io.github.takusan23.kaisendonmk2.R
/**
 * 設定画面
 * */
class PreferenceFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preference, rootKey)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }
}