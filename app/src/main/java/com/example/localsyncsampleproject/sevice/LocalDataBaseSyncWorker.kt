package com.example.localsyncsampleproject.sevice

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.core.net.toUri
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.example.localsyncsampleproject.Utils
import com.example.localsyncsampleproject.data.room.entity.Media
import com.example.localsyncsampleproject.domain.repository.MediaRepository
import com.example.localsyncsampleproject.domain.repository.SyncTimeRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import org.tensorflow.lite.examples.imagesegmentation.tflite.ImageSegmentationModelExecutor
import org.tensorflow.lite.examples.imagesegmentation.tflite.ModelExecutionResult
import org.tensorflow.lite.examples.imagesegmentation.utils.ImageUtils
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@HiltWorker
class LocalDataBaseSyncWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParma: WorkerParameters,
    private val syncTimeRepository: SyncTimeRepository,
    private val mediaRepository: MediaRepository,
) : CoroutineWorker(context, workerParma) {

    // viewModel 에서 사용했던 임시로 tensorFlowLite 예제를 따라했던 방법
    private val job = Job()
    private val coroutineScopes = CoroutineScope(job)

    private lateinit var mediaItems: Flow<List<Media>>
    private var imageSegmentationModel: ImageSegmentationModelExecutor? = null
    private val inferenceThread = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

    override suspend fun doWork(): Result {
        // 백그라운드에서 작업할 작업.

        try {
            coroutineScope {
                mediaRepository.syncMediaData() // 로컬 DB 동기화 작업
                createModelExecutor()

                mediaItems = mediaRepository.unCheckedIsPersonData()
                mediaItems.collectLatest { medias ->
                    // isPerson == 0 아직 체크가 안된 사진들
                    if (medias.isNotEmpty()) {
                        val media = medias[0]
                        onApplyModel(
                            media.id,
                            media.path.toUri(),
                            imageSegmentationModel,
                            inferenceThread
                        )
                    }
                }
            }

        } catch (e: Error) {
            Log.e("error", "$e")
        }
        return Result.success()
    }


    private fun createModelExecutor() { // TIF Model 셋팅, Inject 주입
        if (imageSegmentationModel != null) {
            imageSegmentationModel!!.close()
            imageSegmentationModel = null
        }
        try {
            Log.e("try", "ImagesSegmentationModelExecutor")
            imageSegmentationModel = ImageSegmentationModelExecutor(context)
        } catch (e: Exception) {
            Log.e("Main", "Fail to create ImageSegmentationModelExecutor: ${e.message}")
        }
    }

    private fun onApplyModel( // label 체크
        id: Int,
        uri: Uri,
        imageSegmentationModel: ImageSegmentationModelExecutor?,
        inferenceThread: ExecutorCoroutineDispatcher // 이걸 꼭 넣어야 하나?
    ) {
        coroutineScopes.launch(inferenceThread) {
            val filePath = Utils.getFilePath(context, uri)
            val contentImage: Bitmap = ImageUtils.decodeBitmap(File(filePath))
            try {
                if (filePath.isNotEmpty() && filePath != null) {
                    val result = imageSegmentationModel?.execute(contentImage)
                    if (result != null) checkPerson(id, result)
                } else {
                    // filePath가 없을 경우에도 업데이트를 해줘야 collect 에서 인지한다.
                    mediaRepository.updateIsPerson(id, 2)
                }
            } catch (e: Exception) {
                Log.e("error", "$e")
            }
        }
    }

    private suspend fun checkPerson(id: Int, rtImage: ModelExecutionResult) {
        val itemFount: Map<String, Int> = rtImage.itemsFound
        var check = false

        itemFount.forEach { (label, color) ->
            Log.e("label", "$label")
            if (label == "person") check = true
            // 여기 forEach 내부에서 계속해서 updateIsPerson, updateIsNotPerson(삭제했음) 호출
            // 필요한 정보는 label == "person"이였는데,
            // forEach 내부에 있으니 label == person 아니여도 계속 돌았고,
            // 이 때 updateIsPerson 함수들을 계속해서 업데이트를 진행하니
            // MainActivity -> collectLatest 사용해서 "변화"를 감지하고 있는 부분들이
            // 계속해서 호출이 되었다...
            // [ 해결 방법 ]
            // 1. itemFount, label -> 체크하는 boolean 변수를 두고 person일 경우에만 true로 변경
            // 2. check == ture or false에 따른 updateIsPerson(id, {1, 2})로 업데이트해줬다.
        }

        if (check) { // 사진 내부 사람 존재
            coroutineScope {
                mediaRepository.updateIsPerson(id, 1)
            }

        } else { // 사진 내부 사람 없음
            coroutineScope {
                mediaRepository.updateIsPerson(id, 2)
            }
        }
    }

    companion object {

        private const val WORKER_NAME = "UploadWorker"
        private const val POOL_SIZE = 5

        fun enqueue(
            workManager: WorkManager,
        ) {

            val constraints = Constraints.Builder()
                .addContentUriTrigger(MediaStore.Images.Media.INTERNAL_CONTENT_URI, true)
                .addContentUriTrigger(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true)
                .build()

            // 15분 주기를 줬는데,
            // 왜 계속 도는지 이해가 안되서 좀 더 알아봐야함.
            // code labs를 한번 해보는게 좋을 것 같다.
            // worker id가 존재하며 해당 id로 뭔가 작업을 컨트롤 할 수 있나? -> 가능
            val periodicRequest: PeriodicWorkRequest =
                PeriodicWorkRequestBuilder<LocalDataBaseSyncWorker>(
                    15, TimeUnit.MINUTES
                ).setConstraints(Constraints.Builder().setRequiresCharging(true).build())
                    .build()

            // 고유 작업 충돌 해결 정책
            // uniqueWorkName - 작업 요청을 식별하는 데 사용되는 String
            // existingWorkPolicy -  고유 이름이 있는 작업 체인이 아직 완료되지 않은 경우 WorkManager에 해야 할 작업을 알려주는 enum
            // REPLACE: 기존 작업을 새 작업으로 대체합니다. 기존 작업을 취소하는 옵션입니다.
            // KEEP: 기존 작업을 유지하고 새 작업을 무시합니다.
            // APPEND: 새 작업을 기존 작업의 끝에 추가합니다. 새 작업을 기존 작업에 체이닝하여 기존 작업이 완료된 후에 새 작업을 실행하는 정책입니다. -> 일회성 작업일 경우에 해당된다.

            workManager.enqueueUniquePeriodicWork(
                WORKER_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicRequest
            )

            // KEEP으로 하는게 맞을까?
            // REPLACE으로 하는게 맞는게 아닐까
            // 어차피 MediaStroe id == RoomDB id로 체크하기 때문에
            // DB 업데이트 및 Person 체크도 되기 때문에 REPLACE를...
            // 예상 문제점
            // 그럼 항상 앱을 킬 때마다 유지가 안되고 계속 다시 체크하기 때문에 좋지 않을 것 같다.
            // 계속 껏다 키고 껏다 키고 하면 체크되는 사진들은 줄어들겠지만.
            // 결국은 KEEP으로 가야할 것 같다.

        }

        fun initWorker(workManager: WorkManager) { // 로그아웃 경우일 경우에만,
            workManager.cancelAllWork()
        }

        fun cancel(workManager: WorkManager) {
            workManager.cancelUniqueWork(WORKER_NAME)
        }

    }


}