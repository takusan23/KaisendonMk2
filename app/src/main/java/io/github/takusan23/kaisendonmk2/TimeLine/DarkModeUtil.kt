package io.github.takusan23.kaisendonmk2.TimeLine

import android.content.Context
import android.content.res.Configuration
import android.view.View
import android.widget.ImageView

// ダークモードでよく使いそうな関数まとめ

/**
 * tintをnullに設定する
 * */
internal fun ImageView.setNullTint() {
    this.imageTintList = null
}

/**
 * ダークモードかどうか
 * @param context こんてきすと
 * @return ダークモードならtrue
 * */
internal fun isDarkMode(context: Context?): Boolean {
    val nightMode = context?.resources?.configuration?.uiMode?.and(Configuration.UI_MODE_NIGHT_MASK)
    return nightMode == Configuration.UI_MODE_NIGHT_YES
}