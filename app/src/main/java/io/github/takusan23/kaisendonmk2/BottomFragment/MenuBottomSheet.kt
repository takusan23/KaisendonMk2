package io.github.takusan23.kaisendonmk2.BottomFragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.github.takusan23.kaisendonmk2.Activity.LoginActivity
import io.github.takusan23.kaisendonmk2.MainActivity
import io.github.takusan23.kaisendonmk2.R
import kotlinx.android.synthetic.main.bottom_fragment_menu.*

/**
 * タイムライン設定とか工具マーク押した時に開くやつ
 * */
class MenuBottomSheet : BottomSheetDialogFragment() {

    lateinit var mainActivity: MainActivity

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.bottom_fragment_menu, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // タイムラインの設定
        bottom_fragment_menu_timeline_edit.setOnClickListener {
            val loadTimeLineListBottomSheet = LoadTimeLineListBottomSheet()
            loadTimeLineListBottomSheet.mainActivity = mainActivity
            loadTimeLineListBottomSheet.show(childFragmentManager, "timeline_setting_list")
        }

        // 見た目設定
        bottom_fragment_menu_style_edit.setOnClickListener {

        }

        // ログイン
        bottom_fragment_menu_login.setOnClickListener {
            val intent = Intent(context, LoginActivity::class.java)
            startActivity(intent)
        }

    }

}