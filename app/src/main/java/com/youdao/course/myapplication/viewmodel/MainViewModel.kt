package com.youdao.course.myapplication.viewmodel

import androidx.lifecycle.ViewModel
import com.youdao.course.myapplication.R
import com.youdao.course.myapplication.model.FaceDetectionResult
import com.youdao.course.myapplication.model.Sticker
import com.youdao.course.myapplication.model.StickerType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 主界面ViewModel
 * 管理UI状态（当前贴纸、人脸检测结果、摄像头状态）
 */
class MainViewModel : ViewModel() {

    // 可用贴纸列表
    private val _stickers = MutableStateFlow(createDefaultStickers())
    val stickers: StateFlow<List<Sticker>> = _stickers.asStateFlow()

    // 当前选中的贴纸
    private val _currentSticker = MutableStateFlow<Sticker?>(null)
    val currentSticker: StateFlow<Sticker?> = _currentSticker.asStateFlow()

    // 人脸检测结果
    private val _faceResults = MutableStateFlow<List<FaceDetectionResult>>(emptyList())
    val faceResults: StateFlow<List<FaceDetectionResult>> = _faceResults.asStateFlow()

    // 图像尺寸
    private val _imageSize = MutableStateFlow(Pair(1, 1))
    val imageSize: StateFlow<Pair<Int, Int>> = _imageSize.asStateFlow()

    // 是否为前置摄像头
    private val _isFrontCamera = MutableStateFlow(true)
    val isFrontCamera: StateFlow<Boolean> = _isFrontCamera.asStateFlow()

    // 相机就绪状态
    private val _isCameraReady = MutableStateFlow(false)
    val isCameraReady: StateFlow<Boolean> = _isCameraReady.asStateFlow()

    // 错误消息
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // 调试模式
    private val _debugMode = MutableStateFlow(false)
    val debugMode: StateFlow<Boolean> = _debugMode.asStateFlow()

    /**
     * 创建默认贴纸列表
     */
    private fun createDefaultStickers(): List<Sticker> {
        return listOf(
            Sticker(
                id = 0,
                type = StickerType.NONE,
                drawableRes = R.drawable.ic_no_sticker,
                name = "无",
                offsetYRatio = 0f,
                scaleRatio = 1f
            ),
            Sticker(
                id = 1,
                type = StickerType.GLASSES,
                drawableRes = R.drawable.sticker_glasses,
                name = "眼镜",
                offsetYRatio = 0.35f,
                scaleRatio = 1.0f
            ),
            Sticker(
                id = 2,
                type = StickerType.HAT,
                drawableRes = R.drawable.sticker_hat,
                name = "帽子",
                offsetYRatio = 0.3f,
                scaleRatio = 1.2f
            ),
            Sticker(
                id = 3,
                type = StickerType.CAT_EARS,
                drawableRes = R.drawable.sticker_cat_ears,
                name = "猫耳",
                offsetYRatio = 0.2f,
                scaleRatio = 1.3f
            ),
            Sticker(
                id = 4,
                type = StickerType.MUSTACHE,
                drawableRes = R.drawable.sticker_mustache,
                name = "胡子",
                offsetYRatio = 0.7f,
                scaleRatio = 1.0f
            ),
            Sticker(
                id = 5,
                type = StickerType.DOG_NOSE,
                drawableRes = R.drawable.sticker_dog_nose,
                name = "狗鼻子",
                offsetYRatio = 0.55f,
                scaleRatio = 1.0f
            ),
            Sticker(
                id = 6,
                type = StickerType.CROWN,
                drawableRes = R.drawable.sticker_crown,
                name = "皇冠",
                offsetYRatio = 0.15f,
                scaleRatio = 1.1f
            ),
            Sticker(
                id = 7,
                type = StickerType.MASK,
                drawableRes = R.drawable.sticker_mask,
                name = "面具",
                offsetYRatio = 0.5f,
                scaleRatio = 1.0f
            )
        )
    }

    /**
     * 选择贴纸
     */
    fun selectSticker(sticker: Sticker) {
        _currentSticker.value = if (sticker.type == StickerType.NONE) null else sticker
    }

    /**
     * 更新人脸检测结果
     */
    fun updateFaceResults(results: List<FaceDetectionResult>, imageWidth: Int, imageHeight: Int) {
        _faceResults.value = results
        _imageSize.value = Pair(imageWidth, imageHeight)
    }

    /**
     * 设置相机就绪状态
     */
    fun setCameraReady(isReady: Boolean) {
        _isCameraReady.value = isReady
    }

    /**
     * 设置前后摄像头状态
     */
    fun setFrontCamera(isFront: Boolean) {
        _isFrontCamera.value = isFront
    }

    /**
     * 设置错误消息
     */
    fun setError(message: String?) {
        _errorMessage.value = message
    }

    /**
     * 清除错误消息
     */
    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * 切换调试模式
     */
    fun toggleDebugMode() {
        _debugMode.value = !_debugMode.value
    }

    /**
     * 设置调试模式
     */
    fun setDebugMode(enabled: Boolean) {
        _debugMode.value = enabled
    }
}
