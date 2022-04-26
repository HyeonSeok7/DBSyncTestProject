package com.example.localsyncsampleproject.presentation

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.example.localsyncsampleproject.Utils
import com.example.localsyncsampleproject.data.room.AppDataBase
import com.example.localsyncsampleproject.domain.repository.MediaRepository
import com.example.localsyncsampleproject.domain.repository.SyncTimeRepository
import com.example.localsyncsampleproject.sevice.LocalDataBaseSyncWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.tensorflow.lite.examples.imagesegmentation.tflite.ImageSegmentationModelExecutor
import org.tensorflow.lite.examples.imagesegmentation.tflite.ModelExecutionResult
import org.tensorflow.lite.examples.imagesegmentation.utils.ImageUtils
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val application: Application,
    private val appDataBase: AppDataBase,
    private val syncTimeRepository: SyncTimeRepository,
    private val mediaRepository: MediaRepository,
    private val workManager: WorkManager
) : ViewModel() {

    // Preference -> DataStore
    private val dataStoreFirstTime: Flow<String?> = syncTimeRepository.observeFirstTime()
    private val dataStoreLastSyncTime: Flow<String?> = syncTimeRepository.observeLastTime()

    private val _totalPersonCount = MutableLiveData<Int>()
    val totalPersonCount: LiveData<Int>
        get() = _totalPersonCount

    private val _firstSyncTime = MutableLiveData<String?>()
    val firstSyncTime: LiveData<String?>
        get() = _firstSyncTime

    private val _lastSyncTime = MutableLiveData<String?>()
    val lastSyncTime: LiveData<String?>
        get() = _lastSyncTime

    private val _deviceMediaCounts = MutableStateFlow(0)
    var deviceMediaCounts: StateFlow<Int> = _deviceMediaCounts

    fun createMediaData() {
        viewModelScope.launch {
            startWorker()
            checkFirstSyncTime()
            checkLastSyncTime()
        }
    }

    // Worker 실행
    fun startWorker() {
        checkFirstSyncTime()
        checkLastSyncTime()
        LocalDataBaseSyncWorker.enqueue(workManager)
    }

    // Worker 취소 -> 동기화 초기화 진행 시 모든 worker 작업을 취소하고 다시 실행한다. (임시)
    fun initWorker() {
        LocalDataBaseSyncWorker.initWorker(workManager)
    }

    // Worker 취소
    fun cancel() {
        LocalDataBaseSyncWorker.cancel(workManager)
    }


    // Room DB Counts
    var localDBCounts = mediaRepository.localCounts().stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 0)

    // isPerson == 0, 사람이 존재하는지 판단이 안된 사진들 카운트
    var unCheckedIsPersonCounts = mediaRepository.unCheckedIsPersonCounts().stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 0)

    // MediaStore, Media Counts
    fun getDevicePhotos() { // 임시
        _deviceMediaCounts.value = mediaRepository.createMediaData()
    }

    // Room DB 전부 삭제
    suspend fun deleteAll() = mediaRepository.deleteAllPhotos()

    // isPerson == 1, 전체 카운트
    suspend fun getIsPersonPhotosCounts() = mediaRepository.isPersonCounts()

    // isPerson == 1, 사진이 사람이 있는 사진들
    suspend fun getIsPersonPhotosData() = mediaRepository.isPersonData()

    suspend fun getIsExifGPSInfoCounting() = mediaRepository.exifGPSInfoCounting()

    // 최초 동기화 시간
    private fun checkFirstSyncTime() {
        viewModelScope.launch {
            try {
                val dsFirstSyncTime = dataStoreFirstTime.first()
                _firstSyncTime.value = if (dsFirstSyncTime.isNullOrEmpty()) {
                    syncTimeRepository.setFirstSyncTime(getTime())
                    getTime()
                } else {
                    dsFirstSyncTime
                }
            } catch (e: Exception) {
                Log.e("error", "$e")
            }
        }
    }

    // 최근 동기화 시간 (마지막)
    private fun checkLastSyncTime() {
        viewModelScope.launch {
            syncTimeRepository.setLastSyncTime(getTime())
            _lastSyncTime.value = dataStoreLastSyncTime.first()
        }
    }

    // 현재의 시간을 가져온다.
    private fun getTime(): String {
        val now: Long = System.currentTimeMillis()
        val mDate = Date(now)
        val simpleDate = SimpleDateFormat("yyyy-MM-dd (E) hh:mm:ss")
        return simpleDate.format(mDate)
    }

}