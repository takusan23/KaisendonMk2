package io.github.takusan23.kaisendonmk2.BottomFragment

import android.media.Image
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.github.takusan23.kaisendonmk2.CustomEmoji.CustomEmoji
import io.github.takusan23.kaisendonmk2.DataClass.AccountData
import io.github.takusan23.kaisendonmk2.JSONParse.MisskeyParser
import io.github.takusan23.kaisendonmk2.MisskeyAPI.MisskeyAccountAPI
import io.github.takusan23.kaisendonmk2.MisskeyDataClass.MisskeyUserData
import io.github.takusan23.kaisendonmk2.R
import kotlinx.android.synthetic.main.bottom_fragment_quick_profile.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * QuickProfile機能。
 * 入れて欲しいもの↓
 * data     | Serialize | Mastodonのアカウントデータクラス　か　Misskeyのユーザーデータクラス
 * このためにAPI叩くのもあれなので・・・
 * */
class QuickProfileBottomSheet : BottomSheetDialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.bottom_fragment_quick_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 受け取る
        val userData = arguments?.getSerializable("data")

        // カスタム絵文字など
        val customEmoji = CustomEmoji()

        when {
            userData is AccountData -> {
                // Mastodon
                bottom_fragment_quick_profile_id.text = "${userData.acct} / ${userData.accountId}"
                // カスタム絵文字に対応させる
                customEmoji.setCustomEmoji(bottom_fragment_quick_profile_name, userData.displayName, userData.allEmoji)
                customEmoji.setCustomEmoji(bottom_fragment_quick_profile_profile, userData.note, userData.allEmoji)
                // 数値
                bottom_fragment_quick_profile_follow.append("：\n${userData.followingCount}")
                bottom_fragment_quick_profile_follower.append("：\n${userData.followersCount}")
                bottom_fragment_quick_profile_status_count.append("：\n${userData.statusCount}")
                bottom_fragment_quick_profile_last_updated.append("：${userData.lastStatusAt}")
                // なんかしらんけど findViewById しないと Back-end (JVM) Internal error: wrong bytecode generated 吐くので・・？
                val imageView = view.findViewById<ImageView>(R.id.bottom_fragment_quick_profile_avatar_imageview)
                Glide.with(imageView)
                    .load(userData.avatar)
                    .into(imageView)
            }
            userData is MisskeyUserData -> {
                // Misskey は API叩かないと行けないっぽい？（そんなに情報がないので取りに行く）
                GlobalScope.launch(Dispatchers.Main) {
                    val user = withContext(Dispatchers.IO) {
                        MisskeyAccountAPI(userData.instanceToken).showAccount(userData.id).await()
                    }
                    if (!isAdded) return@launch
                    if (user.isSuccessful) {
                        val misskeyUserData = MisskeyParser().parseShowAccount(user.response?.body?.string()!!, userData.instanceToken)
                        // Mastodon
                        bottom_fragment_quick_profile_id.text = "${userData.name} / ${userData.id}"
                        // カスタム絵文字に対応させる
                        customEmoji.setCustomEmoji(bottom_fragment_quick_profile_name, userData.name, userData.emoji)
                        customEmoji.setCustomEmoji(bottom_fragment_quick_profile_profile, misskeyUserData.description, misskeyUserData.emoji)
                        // 数値
                        bottom_fragment_quick_profile_follow.append("：\n${misskeyUserData.followingCount}")
                        bottom_fragment_quick_profile_follower.append("：\n${misskeyUserData.followersCount}")
                        bottom_fragment_quick_profile_status_count.append("：\n${misskeyUserData.notesCount}")
                        bottom_fragment_quick_profile_last_updated.visibility = View.GONE
                        // なんかしらんけど findViewById しないと Back-end (JVM) Internal error: wrong bytecode generated 吐くので・・？
                        val imageView = view.findViewById<ImageView>(R.id.bottom_fragment_quick_profile_avatar_imageview)
                        Glide.with(imageView)
                            .load(misskeyUserData.avatarUrl)
                            .into(imageView)
                    } else {
                        Toast.makeText(context, getString(R.string.error) + "| api/users/show\n${user.response?.code}|${user.ioException?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

    }

}