package io.github.takusan23.kaisendonmk2.Fragment

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayoutMediator
import io.github.takusan23.kaisendonmk2.Adapter.TabLayoutViewPagerAdapter
import io.github.takusan23.kaisendonmk2.R
import io.github.takusan23.kaisendonmk2.TimeLine.isDarkMode
import kotlinx.android.synthetic.main.fragment_tablayout.*

/**
 * TabLayoutなタイムライン。スワイプで切り替えできるよ
 * */
class TabLayoutFragment : Fragment() {

    lateinit var viewPagerAdapter: TabLayoutViewPagerAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_tablayout, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViewPager()

        if (isDarkMode(context)) {
            fragment_tablayout_tablayout.backgroundTintList = ColorStateList.valueOf(Color.BLACK)
        }
    }

    /**
     * ViewPager初期化
     * */
    fun initViewPager() {
        viewPagerAdapter = TabLayoutViewPagerAdapter(activity as AppCompatActivity)
        fragment_tablayout_viewpager.adapter = viewPagerAdapter
        // TabLayout
        TabLayoutMediator(fragment_tablayout_tablayout, fragment_tablayout_viewpager) { tab, i ->
            tab.text = viewPagerAdapter.customMenuNameList[i]
        }.attach()
    }

}