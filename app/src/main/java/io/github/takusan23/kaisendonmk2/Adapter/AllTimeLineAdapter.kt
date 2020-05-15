package io.github.takusan23.kaisendonmk2.Adapter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Switch
import androidx.recyclerview.widget.RecyclerView
import io.github.takusan23.kaisendonmk2.BottomFragment.LoadTimeLineEditBottomSheet
import io.github.takusan23.kaisendonmk2.BottomFragment.LoadTimeLineListBottomSheet
import io.github.takusan23.kaisendonmk2.DataClass.AllTimeLineData
import io.github.takusan23.kaisendonmk2.MainActivity
import io.github.takusan23.kaisendonmk2.R
import io.github.takusan23.kaisendonmk2.TimeLine.AllTimeLineJSON

// タイムラインの構成一覧表示のAdapter
class AllTimeLineAdapter(val allTimeLineList: ArrayList<AllTimeLineData>) : RecyclerView.Adapter<AllTimeLineAdapter.ViewHolder>() {

    lateinit var mainActivity: MainActivity
    lateinit var loadTimeLineListBottomSheet: LoadTimeLineListBottomSheet
    lateinit var allTimeLineJSON: AllTimeLineJSON

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.adapter_timeline_setting, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return allTimeLineList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.apply {
            // Context
            val context = allTimeLineAdapterSwitch.context
            if (!::allTimeLineJSON.isInitialized) {
                allTimeLineJSON = AllTimeLineJSON(context)
            }

            // 値入れる
            val allTimeLineData = allTimeLineList[position]
            allTimeLineAdapterSwitch.isChecked = allTimeLineData.isEnable
            allTimeLineAdapterSwitch.text = allTimeLineData.timeLineName

            // スイッチ
            allTimeLineAdapterSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
                // 有効/無効を反転
                allTimeLineJSON.setAllTimeLineEnable(allTimeLineData.timeLineName, isChecked)
            }

            // 見た目
            allTimeLineAdapterStyle.setOnClickListener {
                val loadTimeLineEditBottomSheet = LoadTimeLineEditBottomSheet()
                val bundle = Bundle().apply {
                    putString("name", allTimeLineData.timeLineName)
                }
                loadTimeLineEditBottomSheet.arguments = bundle
                loadTimeLineEditBottomSheet.show(mainActivity.supportFragmentManager, "style")
            }

        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val allTimeLineAdapterSwitch =
            itemView.findViewById<Switch>(R.id.adapter_all_tl_setting_switch)
        val allTimeLineAdapterStyle =
            itemView.findViewById<ImageView>(R.id.adapter_tl_setting_color)
    }

}
