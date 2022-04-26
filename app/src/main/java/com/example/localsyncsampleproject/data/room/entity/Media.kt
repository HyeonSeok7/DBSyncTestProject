package com.example.localsyncsampleproject.data.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Media(

    @PrimaryKey val id: Int,

    @ColumnInfo(name = "path") val path: String,                            // 파일 경로

    @ColumnInfo(name = "createdAt") val createdAt: String?,                 // 파일 생성 일시

    @ColumnInfo(name = "takenAt") val takenAt: String?,                     // 촬영 일시

    @ColumnInfo(name = "lat") val lat: Double?,                             // 위도

    @ColumnInfo(name = "lng") val lng: Double?,                             // 경도

    @ColumnInfo(name = "isUpload") val isUpload: Int = 0,                   // 업로드 여부

    @ColumnInfo(name = "localUpdatedAt") var localUpdatedAt: String,        // 업데이트된 일시

    @ColumnInfo(name = "editedAt") val editedAt: String?,                   // 사진 수정일

    @ColumnInfo(name = "modificationDate") val modificationDate: String?,   // 마지막 수정일시

    @ColumnInfo(name = "isPerson") val isPerson: Int = 0                    // 체크전, 사람의 유, 무 -> 0, 1, 2

//    @ColumnInfo(name = "pid") val pid: String,                              // IOS UID
)
