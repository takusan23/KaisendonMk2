package io.github.takusan23.kaisendonmk2.DetaBase.RoomDataBase

import androidx.room.Database
import androidx.room.RoomDatabase
import io.github.takusan23.kaisendonmk2.DetaBase.Dao.CustomTimeLineDBDao
import io.github.takusan23.kaisendonmk2.DetaBase.Entity.CustomTimeLineDBEntity

@Database(entities = [CustomTimeLineDBEntity::class], version = 1)
abstract class CustomTimeLineDB : RoomDatabase() {
    abstract fun customTimeLineDBDao(): CustomTimeLineDBDao
}