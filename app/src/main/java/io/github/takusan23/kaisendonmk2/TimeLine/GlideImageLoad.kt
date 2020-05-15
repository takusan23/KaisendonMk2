package io.github.takusan23.kaisendonmk2.TimeLine

import android.graphics.drawable.Drawable
import android.opengl.Visibility
import android.view.View
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target

/**
 * Glideの画像読み込みでキャッシュのみとか
 * */
class GlideImageLoad {

    /**
     * キャッシュが存在すればキャッシュから読み込む。
     *  @param isErrorVisibility キャッシュが存在しない時に非表示にするならtrue。省略時false
     *  @param isErrorGet キャッシュが存在しない時にインターネットで取得するならtrue。省略時true
     * */
    fun loadOffline(imageView: ImageView, url: String, isErrorVisibility: Boolean = false, isErrorGet: Boolean = true) {
        Glide.with(imageView)
            .load(url)
            .onlyRetrieveFromCache(true)
            .apply(RequestOptions.bitmapTransform(RoundedCorners(10)))
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean): Boolean {
                    // 失敗時
                    when {
                        isErrorVisibility -> imageView.visibility = View.GONE
                        isErrorGet -> loadOnline(imageView, url)
                    }
                    return false
                }

                override fun onResourceReady(resource: Drawable?, model: Any?, target: Target<Drawable>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                    return false
                }
            })
            .into(imageView)
    }

    // インターネットから取得
    private fun loadOnline(imageView: ImageView, url: String) {
        Glide.with(imageView)
            .load(url)
            .into(imageView)
    }

}