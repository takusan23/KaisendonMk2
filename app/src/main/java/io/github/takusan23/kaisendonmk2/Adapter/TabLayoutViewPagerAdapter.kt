package io.github.takusan23.kaisendonmk2.Adapter

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.room.Room
import androidx.viewpager2.adapter.FragmentStateAdapter
import io.github.takusan23.kaisendonmk2.DetaBase.RoomDataBase.CustomTimeLineDB
import io.github.takusan23.kaisendonmk2.Fragment.TabLayoutTimeLineFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class TabLayoutViewPagerAdapter(val activity: AppCompatActivity) : FragmentStateAdapter(activity) {

    // Fragmentの配列
    val fragmentList = arrayListOf<TabLayoutTimeLineFragment>()

    // カスタムタイムラインの名前配列
    val customMenuNameList = arrayListOf<String>()

    init {
        // カスタムタイムラインのデータベースから取り出す
        val customTimeLineDB = Room.databaseBuilder(activity, CustomTimeLineDB::class.java, "CustomTimeLineDB").build()
        val customTimeLineDBDao = customTimeLineDB.customTimeLineDBDao()
        runBlocking(Dispatchers.IO) {
            customTimeLineDBDao.getAll().forEach {
                if (it.isEnable) {
                    println(it.name)
                    val tabLayoutTimeLineFragment = TabLayoutTimeLineFragment()
                    val bundle = Bundle()
                    bundle.putSerializable("timeline", it)
                    tabLayoutTimeLineFragment.arguments = bundle
                    fragmentList.add(tabLayoutTimeLineFragment)
                    customMenuNameList.add(it.name ?: "未設定")
                }
            }
        }
    }

    override fun getItemCount(): Int = fragmentList.size

    override fun createFragment(position: Int): Fragment {
        return fragmentList[position]
    }


}