package io.github.takusan23.kaisendonmk2.BottomFragment

import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.github.takusan23.kaisendonmk2.R
import io.github.takusan23.kaisendonmk2.TimeLine.setNullTint
import kotlinx.android.synthetic.main.bottom_fragment_dialog.*
import kotlinx.coroutines.GlobalScope
import org.w3c.dom.Text

/**
 * BottomSheet版ダイアログを自作してみた。
 * @param description ダイアログの説明
 * @param buttonItems DialogBottomSheetItemの配列。
 * @param clickEvent クリック押したときのコールバック。引数は押した位置です。
 * */
class DialogBottomSheet(val description: String, val buttonItems: ArrayList<DialogBottomSheetItem>, val clickEvent: (Int, BottomSheetDialogFragment) -> Unit) :
    BottomSheetDialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.bottom_fragment_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // 説明文
        bottom_fragment_dialog_description.text = description
        // ボタン作成
        for (position in 0 until buttonItems.size) {
            val item = buttonItems[position]
            // 押したときのRippleつけたいがためにinflateしてる
            val layout = layoutInflater.inflate(R.layout.textview_ripple, null)
            // テキスト
            val textView = layout.findViewById<TextView>(R.id.dialog_layout_textview)
            textView.text = item.title
            if (item.textColor != -1) {
                textView.setTextColor(item.textColor)
            }
            // 画像
            val imageView = layout.findViewById<ImageView>(R.id.dialog_layout_imageview)
            imageView.setNullTint()
            when {
                item.icon != -1 -> {
                    // drawableから
                    imageView.setImageDrawable(context?.getDrawable(item.icon))
                }
                item.imageUrl != "" -> {
                    // オンライン上から
                    Glide.with(imageView)
                        .load(item.imageUrl)
                        .apply(RequestOptions.bitmapTransform(RoundedCorners(10)))
                        .into(imageView)
                }
                item.textColor != -1 -> {
                    // いろ
                    textView.setTextColor(item.textColor)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        imageView.imageTintList = ColorStateList.valueOf(item.textColor)
                    }
                }
            }
            layout.setOnClickListener {
                // 高階関数
                clickEvent(position, this@DialogBottomSheet)
                // 閉じる
                dismiss()
            }
            bottom_fragment_dialog_linearlayout.addView(layout)
        }
    }

    // ボタンのテキスト、アイコンなど。IconとtextColorは無指定では-1（設定しない）になります。imageUrlは画像URLを入れると表示します
    data class DialogBottomSheetItem(val title: String, val icon: Int = -1, val textColor: Int = -1, val imageUrl: String = "")

}
