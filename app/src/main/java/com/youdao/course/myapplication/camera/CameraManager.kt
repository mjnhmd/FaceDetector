package com.youdao.course.myapplication.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * CameraX管理类
 * 封装摄像头预览和图像分析逻辑
 */
class CameraManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val previewView: PreviewView
) {
    companion object {
        private const val TAG = "CameraManager"
        private const val ANALYSIS_WIDTH = 480
        private const val ANALYSIS_HEIGHT = 640
    }

    private var cameraProvider: ProcessCameraProvider? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    // 当前摄像头选择器（默认前置摄像头）
    private var lensFacing = CameraSelector.LENS_FACING_FRONT

    // 图像分析回调
    private var imageAnalysisCallback: ((Bitmap, Long, Boolean) -> Unit)? = null

    // 相机状态回调
    private var cameraStateCallback: CameraStateCallback? = null

    /**
     * 初始化相机
     */
    fun initialize(callback: CameraStateCallback? = null) {
        this.cameraStateCallback = callback
        
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                bindCameraUseCases()
                cameraStateCallback?.onCameraReady()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize camera", e)
                cameraStateCallback?.onCameraError("Camera initialization failed: ${e.message}")
            }
        }, ContextCompat.getMainExecutor(context))
    }

    /**
     * 绑定相机用例
     */
    private fun bindCameraUseCases() {
        val cameraProvider = cameraProvider ?: return

        // 解绑所有用例
        cameraProvider.unbindAll()

        // 创建相机选择器
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()

        // 创建预览用例
        val preview = Preview.Builder()
            .build()
            .also {
                it.surfaceProvider = previewView.surfaceProvider
            }

        // 创建图像分析用例
        imageAnalyzer = ImageAnalysis.Builder()
            .setTargetResolution(Size(ANALYSIS_WIDTH, ANALYSIS_HEIGHT))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build()
            .also { analysis ->
                analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                    processImage(imageProxy)
                }
            }

        try {
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageAnalyzer
            )
            Log.d(TAG, "Camera use cases bound successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to bind camera use cases", e)
            cameraStateCallback?.onCameraError("Failed to bind camera: ${e.message}")
        }
    }

    /**
     * 处理图像帧
     */
    private fun processImage(imageProxy: ImageProxy) {
        try {
            val bitmap = imageProxyToBitmap(imageProxy)
            bitmap?.let {
                val rotatedBitmap = rotateBitmap(it, imageProxy.imageInfo.rotationDegrees)
                val timestamp = System.currentTimeMillis()
                val isFrontCamera = lensFacing == CameraSelector.LENS_FACING_FRONT
                
                imageAnalysisCallback?.invoke(rotatedBitmap, timestamp, isFrontCamera)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing image", e)
        } finally {
            imageProxy.close()
        }
    }

    /**
     * 将ImageProxy转换为Bitmap
     */
    private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap? {
        val buffer = imageProxy.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)

        val bitmap = Bitmap.createBitmap(
            imageProxy.width,
            imageProxy.height,
            Bitmap.Config.ARGB_8888
        )
        buffer.rewind()
        bitmap.copyPixelsFromBuffer(buffer)
        return bitmap
    }

    /**
     * 旋转位图
     */
    private fun rotateBitmap(bitmap: Bitmap, rotationDegrees: Int): Bitmap {
        if (rotationDegrees == 0) return bitmap

        val matrix = Matrix().apply {
            postRotate(rotationDegrees.toFloat())
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    /**
     * 设置图像分析回调
     * @param callback (bitmap, timestamp, isFrontCamera) -> Unit
     */
    fun setImageAnalysisCallback(callback: (Bitmap, Long, Boolean) -> Unit) {
        this.imageAnalysisCallback = callback
    }

    /**
     * 切换前后摄像头
     */
    fun switchCamera() {
        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
            CameraSelector.LENS_FACING_BACK
        } else {
            CameraSelector.LENS_FACING_FRONT
        }
        bindCameraUseCases()
        cameraStateCallback?.onCameraSwitched(lensFacing == CameraSelector.LENS_FACING_FRONT)
    }

    /**
     * 是否为前置摄像头
     */
    fun isFrontCamera(): Boolean {
        return lensFacing == CameraSelector.LENS_FACING_FRONT
    }

    /**
     * 释放资源
     */
    fun shutdown() {
        cameraExecutor.shutdown()
        cameraProvider?.unbindAll()
    }

    /**
     * 相机状态回调接口
     */
    interface CameraStateCallback {
        fun onCameraReady()
        fun onCameraError(error: String)
        fun onCameraSwitched(isFrontCamera: Boolean)
    }
}
