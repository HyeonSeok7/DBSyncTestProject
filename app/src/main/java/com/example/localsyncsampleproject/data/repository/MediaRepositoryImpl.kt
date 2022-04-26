package com.example.localsyncsampleproject.data.repository

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import com.example.localsyncsampleproject.data.datastore.DataStorePreferenceManager
import com.example.localsyncsampleproject.data.room.AppDataBase
import com.example.localsyncsampleproject.data.room.entity.Media
import com.example.localsyncsampleproject.domain.repository.MediaRepository
import com.example.localsyncsampleproject.domain.repository.SyncTimeRepository
import dagger.hilt.android.qualifiers.ActivityContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

class MediaRepositoryImpl @Inject constructor(
    @ActivityContext private val context: Context,
    private val database: AppDataBase,
    private val dataStorePreferenceManager: DataStorePreferenceManager,
    private val syncTimeRepository: SyncTimeRepository
) : MediaRepository {

    override suspend fun getAllPhoto(): Flow<List<Media>> = database.mediaDao().getAll()

    override suspend fun deleteAllPhotos() = database.mediaDao().deleteAll()

    override fun localCounts(): Flow<Int> =
        database.mediaDao().localAllCounts()

    // gps 정보를 가지고 있는 사진 카운팅
    override suspend fun exifGPSInfoCounting(): Flow<Int> =
        database.mediaDao().getExifGPSInfoCounts()

    // 사진 속 사람의 유, 무 체크를 하지 않은 데이터 갯수
    override fun unCheckedIsPersonCounts(): Flow<Int> =
        database.mediaDao().getUnCheckedISPersonCounts()

    // 사람이 존재하는 사진 isPerson == 1로 업데이트
    override suspend fun updateIsPerson(id: Int, isPerson: Int) {
        withContext(Dispatchers.IO) {
            database.mediaDao().updateIsPersonPhotoAt(id, isPerson)
        }
    }

    // 사람이 존재하는 사진 isPerson == 1인 사진 리스트 가져오기
    override suspend fun isPersonData(): Flow<List<Media>> {
        return database.mediaDao().getIsPersonPhotosData()
    }

    // 사진 속 사람 유, 무 판단 전
    override suspend fun unCheckedIsPersonData(): Flow<List<Media>> {
        return database.mediaDao().getUnCheckedIsPersonData()
            .flatMapLatest { // 최신 데이터만
                flow {
                    emit(it)
                }.flowOn(Dispatchers.IO)
            }
    }

    override fun createMediaData(): Int { // (임시) 갤러리 Media 개수만 카운팅해서 반환, 함수 명 변경해야한다.
        var mediaListSize: Int = 0
        val columns = arrayOf(
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media._ID
        )
        val orderBy = MediaStore.Images.ImageColumns.DATE_TAKEN + " DESC" // 찍힌 날자 기준 내림차순
        val query = context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            columns,
            null,
            null,
            orderBy
        )

        try {
            query?.use { cursor ->
                mediaListSize = cursor.count
            }
        } catch (e: Exception) {
            Log.e("error", "$e")
        }
        return mediaListSize
    }

    // 로컬 DB Sync
    override suspend fun syncMediaData(): List<Media> {
        val mediaList = arrayListOf<Media>()
        val roomDB = database.mediaDao().getAll().first()
        val columns = arrayOf(
            MediaStore.Images.Media.DATA,           // Disk 미디어 절대 파일 시스템 경로
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DATE_ADDED,     // 미디어 항목이 처음 추가된 시간
            MediaStore.Images.Media.DATE_MODIFIED,  // 마지막 수정시간
            MediaStore.Images.Media.DATE_TAKEN,     // METADATA_KEY_DATE or ExifInterface#TAG_DATETIME_ORIGINAL에서 추출된 결과.
        )
        val orderBy = MediaStore.Images.ImageColumns.DATE_TAKEN + " DESC" // 찍힌 날자 기준 내림차순
        val query = context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            columns,
            null,
            null,
            orderBy
        )
        try {
            query?.use { cursor ->
                // 1. 각 컬럼의 열 인덱스를 취득한다.
                val urlColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val dateAddedColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                val dateModifiedColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)
                val dateTakenColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)

                while (cursor.moveToNext()) {
                    // 2. 인덱스를 바탕으로 데이터를 Cursor로부터 취득
                    val url = cursor.getString(urlColumn)
                    val id = cursor.getLong(idColumn)
                    val contentUri: Uri = ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        id
                    )
                    val addDate = cursor.getLong(dateAddedColumn)
                    val modifiedDate = cursor.getLong(dateModifiedColumn)
                    val takenDate = cursor.getLong(dateTakenColumn)
                    val dataStoreLastTime =
                        syncTimeRepository.observeLastTime().first() // 마지막 업로드 시간

                    try {
                        // lat, lng를 Exif에서 파싱해서 넣어주기
                    } catch (e: Exception) {
                        Log.e("error => ", "$e")
                    }

                    var sLat: String? = null // 위도
                    var sLon: String? = null // 경도
                    var sLonR: String? = null
                    var sLatR: String? = null
                    val ef = ExifInterface(url)
                    sLat = ef.getAttribute(ExifInterface.TAG_GPS_LATITUDE)
                    sLon = ef.getAttribute(ExifInterface.TAG_GPS_LONGITUDE)
                    sLatR = ef.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF)
                    sLonR = ef.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF)

                    val hashMap: HashMap<String, Double?> =
                        calculateGPSInfo(sLat, sLatR, sLon, sLonR)

                    // local DB에 바로 Insert 하면된다.
                    var check = false
                    roomDB.forEach {
                        if (it.id == id.toInt()) check = true
                    }

                    if (check) {
                        // 다른게 없다.
                        Log.e("syncMediaData", "Check true")
                    } else {
                        // 상이한 부분이 있다.
                        Log.e("syncMediaData", "Check false")
                        database.mediaDao().insert(
                            Media(
                                id.toInt(),
                                contentUri.toString(),
                                addDate.toString(),
                                takenDate.toString(),
                                lat = hashMap["lat"],
                                lng = hashMap["lon"],
                                isUpload = 0,
                                localUpdatedAt = dataStoreLastTime ?: getTime(),
                                editedAt = modifiedDate.toString(),
                                modificationDate = modifiedDate.toString()
                            )
                        )
                        dataStorePreferenceManager.setLastSyncTime(getTime())
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("syncMediaData", "$e")
        }
        return mediaList // 확인을 위해서 return 해줌, 삭제해도 된다.
    }

    // 위도 경도 반환
    private fun calculateGPSInfo(
        sLat: String?,
        sLatR: String?,
        sLon: String?,
        sLonR: String?
    ): HashMap<String, Double?> { // 매개변수 임시
        val hashMap = HashMap<String, Double?>()
        var lat: Double = dms2Dbl(sLat)
        var lon: Double = dms2Dbl(sLon)

        lat = if (sLatR != null && sLatR.contains("S")) -lat else lat
        lon = if (sLonR != null && sLonR.contains("W")) -lon else lon

        hashMap["lat"] = if (lat > 180.0) null else lat
        hashMap["lon"] = if (lon > 180.0) null else lon
        return hashMap
    }

    // 위도 경도 변환
    private fun dms2Dbl(sDMS: String?): Double {
        var dRV = 999.0
        try {
            if (sDMS != null) {
                Log.e("sDms", " is not Null!!")
                val DMSs = sDMS.split(",", limit = 3)
                var s: Array<String?> = DMSs[0].split("/", limit = 2).toTypedArray()
                dRV = java.lang.Double.valueOf(s[0]) / java.lang.Double.valueOf(s[1])
                s = DMSs[1].split("/", limit = 2).toTypedArray()
                dRV += java.lang.Double.valueOf(s[0]) / java.lang.Double.valueOf(s[1]) / 60
                s = DMSs[2].split("/", limit = 2).toTypedArray()
                dRV += java.lang.Double.valueOf(s[0]) / java.lang.Double.valueOf(s[1]) / 3600
            }
        } catch (e: Exception) {
            Log.e("error", "$e")
        }
        return dRV
    }

    // delete도 결국 풀 스캔.... 당장은 보류

    private fun getTime(): String {
        val now: Long = System.currentTimeMillis()
        val mDate = Date(now)
        val simpleDate = SimpleDateFormat("yyyy-MM-dd (E) hh:mm:ss")
        return simpleDate.format(mDate)
    }

}