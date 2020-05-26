package io.github.takusan23.kaisendonmk2.DetaBase.Entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable
import java.util.*

/**
 * データベースのカラム定義
 * */
@Entity
data class CustomTimeLineDBEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "enable") var isEnable: Boolean,
    @ColumnInfo(name = "name") val name: String?,
    @ColumnInfo(name = "service") var service: String?,
    @ColumnInfo(name = "instance") val instance: String?,
    @ColumnInfo(name = "token") val token: String?,
    @ColumnInfo(name = "timeline") var timeline: String?
) : Serializable