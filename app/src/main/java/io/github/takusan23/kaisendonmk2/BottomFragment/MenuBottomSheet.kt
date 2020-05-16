package io.github.takusan23.kaisendonmk2.BottomFragment

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.github.takusan23.kaisendonmk2.Activity.LoginActivity
import io.github.takusan23.kaisendonmk2.Activity.PreferenceActivity
import io.github.takusan23.kaisendonmk2.JSONParse.MisskeyParser
import io.github.takusan23.kaisendonmk2.JSONParse.TimeLineParser
import io.github.takusan23.kaisendonmk2.MainActivity
import io.github.takusan23.kaisendonmk2.MastodonAPI.AccountAPI
import io.github.takusan23.kaisendonmk2.MisskeyAPI.MisskeyAccountAPI
import io.github.takusan23.kaisendonmk2.R
import io.github.takusan23.kaisendonmk2.TimeLine.AllTimeLineJSON
import io.github.takusan23.kaisendonmk2.TimeLine.loadMultiAccount
import kotlinx.android.synthetic.main.bottom_fragment_menu.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * タイムライン設定とか工具マーク押した時に開くやつ
 * */
class MenuBottomSheet : BottomSheetDialogFragment() {

    lateinit var mainActivity: MainActivity
    lateinit var prefSetting: SharedPreferences

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.bottom_fragment_menu, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        prefSetting = PreferenceManager.getDefaultSharedPreferences(context)

        // 読み込むタイムラインの設定
        bottom_fragment_menu_load_timeline_edit.setOnClickListener {
            val loadTimeLineListBottomSheet = LoadTimeLineListBottomSheet()
            loadTimeLineListBottomSheet.mainActivity = mainActivity
            loadTimeLineListBottomSheet.show(childFragmentManager, "timeline_setting_list")
        }

        // TL設定
        bottom_fragment_menu_timeline_setting.setOnClickListener {
            val timeLineSettingBottomSheet = TimeLineSettingBottomSheet()
            timeLineSettingBottomSheet.show(childFragmentManager, "timeline_setting")
        }

        // アカウント一覧読み込む
        loadAccount()

        // ログイン
        bottom_fragment_menu_login.setOnClickListener {
            val intent = Intent(context, LoginActivity::class.java)
            startActivity(intent)
        }

        // 設定
        bottom_fragment_menu_setting.setOnClickListener {
            val intent = Intent(context, PreferenceActivity::class.java)
            startActivity(intent)
        }

    }

    private fun loadAccount() {
        GlobalScope.launch(Dispatchers.Main) {
            bottom_fragment_menu_account.visibility = View.GONE
            // トークン一覧
            val tokenList = loadMultiAccount(context).map { instanceToken -> instanceToken.token }
            val accountTextList = withContext(Dispatchers.IO) {
                loadMultiAccount(context).map { instanceToken ->
                    if (instanceToken.service == "mastodon") {
                        val mastodonAccountAPI = AccountAPI(instanceToken)
                        val timeLineParser = TimeLineParser()
                        val account =
                            timeLineParser.parseAccount(mastodonAccountAPI.getVerifyCredentials().await().body?.string()!!, instanceToken)
                        DialogBottomSheet.DialogBottomSheetItem("${account.displayName}@${account.acct} | ${instanceToken.instance}", -1, -1, account.avatar)
                    } else {
                        val misskeyAccountAPI = MisskeyAccountAPI(instanceToken)
                        val misskeyParser = MisskeyParser()
                        val account =
                            misskeyParser.parseUser(misskeyAccountAPI.getMyAccount().await().body?.string()!!, instanceToken)
                        DialogBottomSheet.DialogBottomSheetItem("${account.name}@${account.username} | ${instanceToken.instance}", -1, -1, account.avatarUrl)
                    }
                }
            } as ArrayList<DialogBottomSheet.DialogBottomSheetItem>
            // アカウント一覧
            if (isAdded) {
                bottom_fragment_menu_account.visibility = View.VISIBLE
                bottom_fragment_menu_account.setOnClickListener {
                    DialogBottomSheet(getString(R.string.account_list), accountTextList) { selectPos, bottomSheetDialogFragment ->
                        // 押したら削除など
                        val items = arrayListOf<DialogBottomSheet.DialogBottomSheetItem>().apply {
                            add(DialogBottomSheet.DialogBottomSheetItem(getString(R.string.delete), R.drawable.ic_delete_black_24dp, -1))
                            add(DialogBottomSheet.DialogBottomSheetItem(getString(R.string.cancel), R.drawable.ic_close_black_24dp, -1))
                        }
                        DialogBottomSheet(getString(R.string.menu), items) { i, deleteFragment ->
                            when (i) {
                                0 -> {
                                    AllTimeLineJSON(context).deleteAccount(tokenList[selectPos])
                                    prefSetting.edit { putString("last_use_account", null) }
                                    dismiss()
                                    // Activity再起動
                                    activity?.finish()
                                    val intent = Intent(context, MainActivity::class.java)
                                    startActivity(intent)
                                }
                                1 -> {
                                    // けしゅ
                                    deleteFragment.dismiss()
                                    dismiss()
                                }
                            }
                        }.show(childFragmentManager, "delete")
                    }.show(childFragmentManager, "account")
                }
            }
        }
    }

}