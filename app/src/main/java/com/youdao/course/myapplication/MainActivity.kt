package com.youdao.course.myapplication

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.youdao.course.myapplication.adapter.StickerAdapter
import com.youdao.course.myapplication.camera.CameraManager
import com.youdao.course.myapplication.databinding.ActivityMainBinding
import com.youdao.course.myapplication.detector.FaceDetectorHelper
import com.youdao.course.myapplication.model.FaceDetectionResult
import com.youdao.course.myapplication.viewmodel.MainViewModel
import kotlinx.coroutines.launch

/**
 * 主界面Activity
 * 整合摄像头预览、人脸检测、贴纸渲染功能
 */
class MainActivity : AppCompatActivity(), FaceDetectorHelper.DetectorListener {

    companion object {
        private const val TAG = "MainActivity"
    }

    // ViewBinding
    private lateinit var binding: ActivityMainBinding

    // ViewModel
    private val viewModel: MainViewModel by viewModels()

    // CameraManager
    private var cameraManager: CameraManager? = null

    // FaceDetector
    private var faceDetectorHelper: FaceDetectorHelper? = null

    // 贴纸适配器
    private lateinit var stickerAdapter: StickerAdapter

    // 主线程Handler
    private val mainHandler = Handler(Looper.getMainLooper())

    // 权限请求启动器
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            setupCamera()
        } else {
            showPermissionDenied()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupObservers()
        checkCameraPermission()
    }

    /**
     * 设置UI
     */
    private fun setupUI() {
        // 设置贴纸RecyclerView
        stickerAdapter = StickerAdapter { sticker ->
            viewModel.selectSticker(sticker)
        }
        binding.recyclerViewStickers.apply {
            layoutManager = LinearLayoutManager(
                this@MainActivity,
                LinearLayoutManager.HORIZONTAL,
                false
            )
            adapter = stickerAdapter
        }

        // 切换摄像头按钮
        binding.btnSwitchCamera.setOnClickListener {
            cameraManager?.switchCamera()
        }

        // 调试模式按钮
        binding.btnDebugMode.setOnClickListener {
            viewModel.toggleDebugMode()
        }

        // 授权按钮
        binding.btnGrantPermission.setOnClickListener {
            requestCameraPermission()
        }
    }

    /**
     * 设置观察者
     */
    private fun setupObservers() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // 观察贴纸列表
                launch {
                    viewModel.stickers.collect { stickers ->
                        stickerAdapter.submitList(stickers)
                    }
                }

                // 观察当前选中的贴纸
                launch {
                    viewModel.currentSticker.collect { sticker ->
                        binding.stickerOverlayView.setSticker(sticker)
                        stickerAdapter.setSelectedSticker(sticker?.id ?: 0)
                    }
                }

                // 观察人脸检测结果
                launch {
                    viewModel.faceResults.collect { results ->
                        val imageSize = viewModel.imageSize.value
                        val isFrontCamera = viewModel.isFrontCamera.value

                        binding.stickerOverlayView.updateFaceResults(
                            results,
                            imageSize.first,
                            imageSize.second,
                            isFrontCamera
                        )

                        // 更新人脸检测状态
                        updateFaceStatus(results)
                    }
                }

                // 观察调试模式
                launch {
                    viewModel.debugMode.collect { enabled ->
                        binding.stickerOverlayView.setDebugMode(enabled)
                        // 更新按钮颜色
                        val tintColor = if (enabled) {
                            ContextCompat.getColor(this@MainActivity, R.color.purple_500)
                        } else {
                            ContextCompat.getColor(this@MainActivity, R.color.black)
                        }
                        binding.btnDebugMode.imageTintList = android.content.res.ColorStateList.valueOf(tintColor)
                    }
                }

                // 观察错误消息
                launch {
                    viewModel.errorMessage.collect { error ->
                        error?.let {
                            Toast.makeText(this@MainActivity, it, Toast.LENGTH_SHORT).show()
                            viewModel.clearError()
                        }
                    }
                }

                // 观察相机就绪状态
                launch {
                    viewModel.isCameraReady.collect { isReady ->
                        binding.progressBar.visibility = if (isReady) View.GONE else View.VISIBLE
                    }
                }
            }
        }
    }

    /**
     * 更新人脸检测状态
     */
    private fun updateFaceStatus(results: List<FaceDetectionResult>) {
        val statusText = when {
            results.isEmpty() -> "未检测到人脸"
            results.size == 1 -> "检测到 1 张人脸"
            else -> "检测到 ${results.size} 张人脸"
        }
        binding.tvFaceStatus.text = statusText

        val backgroundColor = if (results.isNotEmpty()) {
            ContextCompat.getColor(this, R.color.face_detected)
        } else {
            ContextCompat.getColor(this, R.color.face_not_detected)
        }
        binding.tvFaceStatus.background.setTint(backgroundColor)
    }

    /**
     * 检查摄像头权限
     */
    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                setupCamera()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                showPermissionRationale()
            }
            else -> {
                requestCameraPermission()
            }
        }
    }

    /**
     * 请求摄像头权限
     */
    private fun requestCameraPermission() {
        requestPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    /**
     * 显示权限说明
     */
    private fun showPermissionRationale() {
        binding.permissionLayout.visibility = View.VISIBLE
        binding.previewView.visibility = View.GONE
    }

    /**
     * 显示权限被拒绝提示
     */
    private fun showPermissionDenied() {
        binding.permissionLayout.visibility = View.VISIBLE
        binding.previewView.visibility = View.GONE
        Toast.makeText(this, "需要相机权限才能使用此功能", Toast.LENGTH_LONG).show()
    }

    /**
     * 设置摄像头
     */
    private fun setupCamera() {
        binding.permissionLayout.visibility = View.GONE
        binding.previewView.visibility = View.VISIBLE
        binding.progressBar.visibility = View.VISIBLE

        // 初始化人脸检测器
        setupFaceDetector()

        // 初始化相机
        cameraManager = CameraManager(
            context = this,
            lifecycleOwner = this,
            previewView = binding.previewView
        ).apply {
            setImageAnalysisCallback { bitmap, timestamp, isFrontCamera ->
                processFrame(bitmap, timestamp, isFrontCamera)
            }
            initialize(object : CameraManager.CameraStateCallback {
                override fun onCameraReady() {
                    mainHandler.post {
                        viewModel.setCameraReady(true)
                        Log.d(TAG, "Camera is ready")
                    }
                }

                override fun onCameraError(error: String) {
                    mainHandler.post {
                        viewModel.setError(error)
                        Log.e(TAG, "Camera error: $error")
                    }
                }

                override fun onCameraSwitched(isFrontCamera: Boolean) {
                    mainHandler.post {
                        viewModel.setFrontCamera(isFrontCamera)
                        binding.stickerOverlayView.clear()
                    }
                }
            })
        }
    }

    /**
     * 设置人脸检测器
     */
    private fun setupFaceDetector() {
        faceDetectorHelper = FaceDetectorHelper(
            context = this,
            minDetectionConfidence = 0.5f,
            listener = this
        ).apply {
            initialize()
        }
    }

    /**
     * 处理相机帧
     */
    private fun processFrame(bitmap: Bitmap, timestamp: Long, isFrontCamera: Boolean) {
        faceDetectorHelper?.let { detector ->
            val results = detector.detectSync(bitmap)
            mainHandler.post {
                viewModel.updateFaceResults(results, bitmap.width, bitmap.height)
                viewModel.setFrontCamera(isFrontCamera)
            }
        }
    }

    // FaceDetectorHelper.DetectorListener 接口实现
    override fun onFaceDetected(faces: List<FaceDetectionResult>) {
        // 用于异步检测模式
        mainHandler.post {
            val imageSize = viewModel.imageSize.value
            viewModel.updateFaceResults(faces, imageSize.first, imageSize.second)
        }
    }

    override fun onError(error: String) {
        mainHandler.post {
            viewModel.setError(error)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraManager?.shutdown()
        faceDetectorHelper?.close()
    }
}