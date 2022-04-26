package com.example.localsyncsampleproject.presentation

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.coroutineScope
import androidx.recyclerview.widget.GridLayoutManager
import com.example.localsyncsampleproject.R
import com.example.localsyncsampleproject.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.tensorflow.lite.examples.imagesegmentation.tflite.ImageSegmentationModelExecutor
import java.util.concurrent.Executors


@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var mediaListAdapter: MediaListAdapter
    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>
    private var imageSegmentationModel: ImageSegmentationModelExecutor? = null
    private val inferenceThread = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        checkPermissions()
        createModelExecutor()
        initClickEvent()
        initActivityResultLauncher()
        initLifecycleCoroutineScope()

        binding.lifecycleOwner = this
        binding.viewModel = mainViewModel
        binding.recyclerView.apply {
            this.layoutManager = GridLayoutManager(this@MainActivity, 4)
            mediaListAdapter = MediaListAdapter()
            mediaListAdapter.setHasStableIds(true)
            this.adapter = mediaListAdapter
        }

    }

    private fun initLifecycleCoroutineScope() {

        lifecycle.coroutineScope.launch {
            mainViewModel.getIsPersonPhotosCounts().collectLatest { // 새로운 데이터가 들어오면 이전 데이터의 처리를 강제 종료 시키고 새로운 데이터를 처리한다.
                binding.tvPersonCounts.text = String.format(
                    getString(R.string.person_counting),
                    it
                )
            }
        }

        lifecycle.coroutineScope.launch {
            mainViewModel.getIsPersonPhotosData().collectLatest {
                // isPerson == 1 (사진속 사람 존재)
                binding.tvPersonCounts.text = String.format(
                    getString(R.string.person_counting),
                    it.size
                )
                mediaListAdapter.clear() // clear는 상관없다. 계속 어차피 isPerson == 1 인 데이터만 불러올거니깐?
                it.forEach { media ->
                    mediaListAdapter.addItem(media)
                }
            }
        }

        lifecycle.coroutineScope.launch {
            mainViewModel.getIsExifGPSInfoCounting().collectLatest {
                Log.e("ExifGPSCount", "$it")
                binding.tvExifGpsCounts.text = String.format(
                    getString(R.string.exif_gps_info_counts),
                    it, mainViewModel.localDBCounts.value
                )
            }
        }
    }


    // 클릭 이벤트
    private fun initClickEvent() {
        binding.btnInit.setOnClickListener { // 동기화 초기화 버튼
            lifecycle.coroutineScope.launch {
                mediaListAdapter.clear()
                mainViewModel.deleteAll()
                mainViewModel.initWorker()
                mainViewModel.createMediaData()
            }
        }

        binding.btnAllDelete.setOnClickListener { // 전부 지우기
            lifecycle.coroutineScope.launch {
                mainViewModel.deleteAll()
            }
        }
    }

    // 텐서플로우 모델 셋팅
    private fun createModelExecutor() {
        if (imageSegmentationModel != null) {
            imageSegmentationModel!!.close()
            imageSegmentationModel = null
        }
        try {
            Log.e("try", "ImagesSegmentationModelExecutor")
            imageSegmentationModel = ImageSegmentationModelExecutor(this)
        } catch (e: Exception) {
            Log.e("Main", "Fail to create ImageSegmentationModelExecutor: ${e.message}")
        }
    }


    private fun checkPermissions() {
        if (allPermissionsGranted()) {
            Log.e("checkPermissions", " in")
            mainViewModel.startWorker()
            mainViewModel.getDevicePhotos()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
    }

    private fun initActivityResultLauncher() { // 갤러리 다녀오고 나서의 이벤트 처리, 현재 사용 안함
        activityResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val intent = result.data
                    val uri: Uri? = intent?.data
                }
            }
    }

    private fun progressBar(show: Boolean) {
        binding.progressBar.visibility =
            if (show) View.VISIBLE else View.INVISIBLE
    }

    // 퍼미션 관련
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all { // 이 코드의 작동 원리를 연구.
        ContextCompat.checkSelfPermission(
            applicationContext,
            it
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            REQUEST_CODE_PERMISSIONS -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.v("main", "permission granted")
                    mainViewModel.startWorker()
                    mainViewModel.getDevicePhotos()
                } else {
                    Toast.makeText(this, "저장소 접근 권한을 허용해주세요.", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    companion object {
        const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

}

