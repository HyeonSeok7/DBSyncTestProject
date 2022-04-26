package com.example.localsyncsampleproject.data.room.dao

import androidx.room.*
import com.example.localsyncsampleproject.data.room.entity.Media
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE) // PK가 겹치는게 있으면 덮어 씌운다라느 뜻.
    fun insertAll(vararg media: Media) // 일괄 삽입

    @Insert
    fun insert(media: Media)

    @Update
    fun update(media: Media)

    @Query("SELECT * FROM Media")
    fun getAll(): Flow<List<Media>> // 애는 왜 suspend를 추가 안하지? 확인해봐야할듯..

    @Query("SELECT COUNT(*) FROM Media") // roomDB all count
    fun localAllCounts(): Flow<Int>

    @Query("UPDATE media SET isPerson = :isPerson WHERE id = :id")
    suspend fun updateIsPersonPhotoAt(id: Int, isPerson: Int)

    @Query("UPDATE media SET isPerson = :isPerson WHERE id = :id")
    suspend fun updateIsNotPersonPhotoAt(id: Int, isPerson: Int)

    @Query("DELETE FROM media")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM media WHERE isPerson == 1")
    fun getIsPersonPhotos(): Flow<Int>

    @Query("SELECT COUNT(*) FROM media WHERE lat IS NOT NULL AND lng IS NOT NULL")
    fun getExifGPSInfoCounts(): Flow<Int>

    @Query("SELECT * FROM media WHERE isPerson = 1")
    fun getIsPersonPhotosData(): Flow<List<Media>>

    // 사람
    @Query("SELECT * FROM media WHERE isPerson = 0 ") // 사람 유, 무 판단을 못한 데이터
    fun getUnCheckedIsPersonData(): Flow<List<Media>>

    // 사람 체크가 안된 사진 카운트
    @Query("SELECT COUNT(*) FROM media WHERE isPerson = 0")
    fun getUnCheckedISPersonCounts(): Flow<Int>
}