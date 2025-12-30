package com.youdao.course.myapplication.detector

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facedetector.FaceDetector
import com.google.mediapipe.tasks.vision.facedetector.FaceDetectorResult
import com.youdao.course.myapplication.model.FaceBoundingBox
import com.youdao.course.myapplication.model.FaceDetectionResult
import com.youdao.course.myapplication.model.FaceKeypoint
import com.youdao.course.myapplication.model.FaceKeypointType

/**
 * 人脸检测帮助类
 * 封装MediaPipe Face Detector，提供简单的人脸检测接口
 */
class FaceDetectorHelper(
    private val context: Context,
    private val minDetectionConfidence: Float = 0.5f,
    private val listener: DetectorListener? = null
) {
    companion object {
        private const val TAG = "FaceDetectorHelper"
        private const val MODEL_FILE = "face_detection_short_range.tflite"
    }

    private var faceDetector: FaceDetector? = null
    private var isInitialized = false

    /**
     * 初始化人脸检测器
     */
    fun initialize() {
        try {
            val baseOptions = BaseOptions.builder()
                .setDelegate(Delegate.CPU)
                .setModelAssetPath(MODEL_FILE)
                .build()

            val options = FaceDetector.FaceDetectorOptions.builder()
                .setBaseOptions(baseOptions)
                .setMinDetectionConfidence(minDetectionConfidence)
                .setRunningMode(RunningMode.IMAGE)
                .build()

            faceDetector = FaceDetector.createFromOptions(context, options)
            isInitialized = true
            Log.d(TAG, "Face detector initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize face detector", e)
            listener?.onError("Failed to initialize face detector: ${e.message}")
        }
    }

    /**
     * 初始化用于实时视频流的人脸检测器
     */
    fun initializeForLiveStream() {
        try {
            val baseOptions = BaseOptions.builder()
                .setDelegate(Delegate.CPU)
                .setModelAssetPath(MODEL_FILE)
                .build()

            val options = FaceDetector.FaceDetectorOptions.builder()
                .setBaseOptions(baseOptions)
                .setMinDetectionConfidence(minDetectionConfidence)
                .setRunningMode(RunningMode.LIVE_STREAM)
                .setResultListener { result, input ->
                    val faces = processDetectionResult(result, input.width, input.height)
                    listener?.onFaceDetected(faces)
                }
                .setErrorListener { e ->
                    Log.e(TAG, "Face detection error", e)
                    listener?.onError("Face detection error: ${e.message}")
                }
                .build()

            faceDetector = FaceDetector.createFromOptions(context, options)
            isInitialized = true
            Log.d(TAG, "Face detector for live stream initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize face detector for live stream", e)
            listener?.onError("Failed to initialize face detector: ${e.message}")
        }
    }

    /**
     * 同步检测图片中的人脸
     */
    fun detectSync(bitmap: Bitmap): List<FaceDetectionResult> {
        if (!isInitialized) {
            Log.w(TAG, "Face detector not initialized")
            return emptyList()
        }

        return try {
            val mpImage = BitmapImageBuilder(bitmap).build()
            val result = faceDetector?.detect(mpImage)
            result?.let { processDetectionResult(it, bitmap.width, bitmap.height) } ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error during face detection", e)
            listener?.onError("Detection error: ${e.message}")
            emptyList()
        }
    }

    /**
     * 异步检测视频帧中的人脸（用于实时流）
     */
    fun detectAsync(bitmap: Bitmap, timestampMs: Long) {
        if (!isInitialized) {
            Log.w(TAG, "Face detector not initialized")
            return
        }

        try {
            val mpImage = BitmapImageBuilder(bitmap).build()
            faceDetector?.detectAsync(mpImage, timestampMs)
        } catch (e: Exception) {
            Log.e(TAG, "Error during async face detection", e)
            listener?.onError("Detection error: ${e.message}")
        }
    }

    /**
     * 处理检测结果，转换为自定义数据结构
     */
    private fun processDetectionResult(
        result: FaceDetectorResult,
        imageWidth: Int,
        imageHeight: Int
    ): List<FaceDetectionResult> {
        return result.detections().map { detection ->
            val boundingBox = detection.boundingBox()
            
            // 转换边界框坐标为像素值
            val faceBoundingBox = FaceBoundingBox(
                left = boundingBox.left,
                top = boundingBox.top,
                right = boundingBox.right,
                bottom = boundingBox.bottom
            )

            // 转换关键点
            val keypoints = detection.keypoints().orElse(emptyList()).mapIndexedNotNull { index, keypoint ->
                val type = when (index) {
                    0 -> FaceKeypointType.LEFT_EYE
                    1 -> FaceKeypointType.RIGHT_EYE
                    2 -> FaceKeypointType.NOSE_TIP
                    3 -> FaceKeypointType.MOUTH_CENTER
                    4 -> FaceKeypointType.LEFT_EAR_TRAGION
                    5 -> FaceKeypointType.RIGHT_EAR_TRAGION
                    else -> null
                }
                type?.let {
                    FaceKeypoint(
                        type = it,
                        x = keypoint.x() * imageWidth,
                        y = keypoint.y() * imageHeight
                    )
                }
            }

            FaceDetectionResult(
                boundingBox = faceBoundingBox,
                keypoints = keypoints,
                confidence = detection.categories().firstOrNull()?.score() ?: 0f
            )
        }
    }

    /**
     * 释放资源
     */
    fun close() {
        faceDetector?.close()
        faceDetector = null
        isInitialized = false
    }

    /**
     * 检测器回调接口
     */
    interface DetectorListener {
        fun onFaceDetected(faces: List<FaceDetectionResult>)
        fun onError(error: String)
    }
}
