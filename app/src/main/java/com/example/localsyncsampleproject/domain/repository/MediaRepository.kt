package com.example.localsyncsampleproject.domain.repository

import com.example.localsyncsampleproject.data.room.entity.Media
import kotlinx.coroutines.flow.Flow

interface MediaRepository {


    // 디바이스의 사진을 전부 가져와서 RoomD DB에 InsertAll 해줌
    fun createMediaData(): Int

    // 디바이스의 사진과 MediaStore의 사진을 비교해서 Id가 다른 media를 추가
    suspend fun syncMediaData()

    // RoomD DB의 모든 사진 가져오기
    suspend fun getAllPhoto(): Flow<List<Media>>

    // 사진에 사람이 있는 사진 isPerson 값 1로 변경
    suspend fun updateIsPerson(id: Int, isPerson: Int)

    // Room DB의 데이터 전부 삭제
    suspend fun deleteAllPhotos()

    // Room DB의 데이터 개수
    fun localCounts(): Flow<Int>

    // 사람이 있는 사진 데이터
    suspend fun isPersonData(): Flow<List<Media>>

    // 사람이 체크가 안된 사진 개수
    fun unCheckedIsPersonCounts(): Flow<Int>

    // 사람이 체크가 안된 사진 데이터
    suspend fun unCheckedIsPersonData(): Flow<List<Media>>

    // 지역 정보가 있는 사진들 카운트
    suspend fun exifGPSInfoCounting(): Flow<Int>


}