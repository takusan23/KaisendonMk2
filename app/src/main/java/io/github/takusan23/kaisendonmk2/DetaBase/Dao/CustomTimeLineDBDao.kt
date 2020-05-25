package io.github.takusan23.kaisendonmk2.DetaBase.Dao

import androidx.room.*
import io.github.takusan23.kaisendonmk2.DetaBase.Entity.CustomTimeLineDBEntity

/**
 * データベース操作？
 * */
@Dao
interface CustomTimeLineDBDao {
    
    @Query("SELECT * FROM CustomTimeLineDBEntity")
    fun getAll(): List<CustomTimeLineDBEntity>

    @Update
    fun update(customTimeLineDBEntity: CustomTimeLineDBEntity)

    @Insert
    fun insert(customTimeLineDBEntity: CustomTimeLineDBEntity)

    @Delete
    fun delete(customTimeLineDBEntity: CustomTimeLineDBEntity)

    /**
     * 指定したアカウント（アクセストークン）のカスタムタイムラインを消す
     * */
    @Query("DELETE FROM CustomTimeLineDBEntity WHERE token = :token")
    fun accountDelete(token:String)

}