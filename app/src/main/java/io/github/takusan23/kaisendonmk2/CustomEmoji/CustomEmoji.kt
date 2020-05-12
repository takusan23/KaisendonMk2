package io.github.takusan23.kaisendonmk2.CustomEmoji

import android.view.ViewTreeObserver
import android.webkit.WebSettings
import android.widget.TextView
import io.github.takusan23.kaisendonmk2.DataClass.EmojiData
import io.github.takusan23.kaisendonmk2.DataClass.StatusData
import io.github.takusan23.kaisendonmk2.R
import ru.noties.markwon.AbstractMarkwonPlugin
import ru.noties.markwon.Markwon
import ru.noties.markwon.html.HtmlPlugin
import ru.noties.markwon.image.AsyncDrawableLoader
import ru.noties.markwon.image.ImagesPlugin
import ru.noties.markwon.image.gif.GifPlugin

/**
 * Mastodonのカスタム絵文字関係。
 * GIFの絵文字にも対応するために、今回はMarkdown表示ライブラリを使う。
 * */
class CustomEmoji {

    /**
     * トゥート内容の絵文字の部分（例「:partyblob:」）をHTMLタグ（例：「<img src="">」）に置き換える関数
     * */
    private fun replaceEmoji(content: String, emojiList: ArrayList<EmojiData>, textSize: Float = 10F): String {
        var replaceText = content
        emojiList.forEach {
            val emojiText = ":${it.shortCode}:"
            val imgTag = "<img src=\"${it.url}\" width=\"${textSize.toInt()}\">"
            replaceText = replaceText.replace(emojiText, imgTag) // replaceAllない代わりにreplaceを使う。
        }
        return replaceText
    }

    /**
     * カスタム絵文字にTextViewを対応させる
     * @param content 文字列
     * @param emojiList EmojiDataの配列
     * @param textView setText代わり
     * */
    fun setCustomEmoji(textView: TextView, content: String, emojiList: ArrayList<EmojiData>) {
        // Markdownのライブラリ入れた
        val markwon = Markwon.builder(textView.context)
            .usePlugin(HtmlPlugin.create())
            .usePlugin(ImagesPlugin.create(textView.context))
            .usePlugin(GifPlugin.create())
            .usePlugin(object : AbstractMarkwonPlugin() {
                // 読み込み中は別のDrawableを表示する
                override fun configureImages(builder: AsyncDrawableLoader.Builder) {
                    builder.placeholderDrawableProvider {
                        // your custom placeholder drawable
                        textView.context.getDrawable(R.drawable.ic_refresh_black_24dp)
                    }
                }
            }).build()
        // カスタム絵文字を含んだテキストをimgタグで囲った文字に変換する
        val textSize = textView.textSize
        val customEmojiReplaceText = replaceEmoji(content, emojiList, textSize)
        markwon.setMarkdown(textView, customEmojiReplaceText)
    }

}