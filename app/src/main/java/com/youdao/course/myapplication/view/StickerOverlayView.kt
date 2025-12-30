package com.youdao.course.myapplication.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.util.LruCache
import android.view.View
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.toBitmap
import com.youdao.course.myapplication.model.FaceBoundingBox
import com.youdao.course.myapplication.model.FaceDetectionResult
import com.youdao.course.myapplication.model.FaceKeypoint
import com.youdao.course.myapplication.model.FaceKeypointType
import com.youdao.course.myapplication.model.Sticker
import com.youdao.course.myapplication.model.StickerType

/**
 * 贴纸覆盖层View
 * 在人脸检测结果位置上绘制贴纸特效
 */
class StickerOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // 当前选中的贴纸
    private var currentSticker: Sticker? = null

    // 人脸检测结果列表
    private var faceResults: List<FaceDetectionResult> = emptyList()

    // 图像尺寸（用于坐标转换）
    private var imageWidth: Int = 1
    private var imageHeight: Int = 1

    // 是否为前置摄像头（需要水平翻转）
    private var isFrontCamera: Boolean = true

    // 绘制相关
    private val paint = Paint().apply {
        isAntiAlias = true
        isFilterBitmap = true
    }

    // 位图缓存
    private val bitmapCache = LruCache<Int, Bitmap>(10)

    // 调试模式（绘制人脸框和关键点）
    private var debugMode = false

    private val debugPaint = Paint().apply {
        isAntiAlias = true
        color = 0xFF00FF00.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    private val keypointPaint = Paint().apply {
        isAntiAlias = true
        color = 0xFFFF0000.toInt()
        style = Paint.Style.FILL
    }

    /**
     * 设置当前贴纸
     */
    fun setSticker(sticker: Sticker?) {
        currentSticker = sticker
        invalidate()
    }

    /**
     * 更新人脸检测结果
     */
    fun updateFaceResults(
        results: List<FaceDetectionResult>,
        imageWidth: Int,
        imageHeight: Int,
        isFrontCamera: Boolean = true
    ) {
        this.faceResults = results
        this.imageWidth = imageWidth
        this.imageHeight = imageHeight
        this.isFrontCamera = isFrontCamera
        invalidate()
    }

    /**
     * 设置调试模式
     */
    fun setDebugMode(enabled: Boolean) {
        debugMode = enabled
        invalidate()
    }

    /**
     * 清除检测结果
     */
    fun clear() {
        faceResults = emptyList()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (faceResults.isEmpty()) return

        val scaleX = width.toFloat() / imageWidth
        val scaleY = height.toFloat() / imageHeight

        for (face in faceResults) {
            // 转换坐标
            val scaledBox = scaleBoundingBox(face.boundingBox, scaleX, scaleY)
            val scaledKeypoints = scaleKeypoints(face.keypoints, scaleX, scaleY)

            // 调试模式：绘制人脸框和关键点
            if (debugMode) {
                drawDebugInfo(canvas, scaledBox, scaledKeypoints)
            }

            // 绘制贴纸
            currentSticker?.let { sticker ->
                drawSticker(canvas, sticker, scaledBox, scaledKeypoints)
            }
        }
    }

    /**
     * 缩放边界框坐标
     */
    private fun scaleBoundingBox(box: FaceBoundingBox, scaleX: Float, scaleY: Float): RectF {
        return if (isFrontCamera) {
            // 前置摄像头需要水平镜像
            RectF(
                width - box.right * scaleX,
                box.top * scaleY,
                width - box.left * scaleX,
                box.bottom * scaleY
            )
        } else {
            RectF(
                box.left * scaleX,
                box.top * scaleY,
                box.right * scaleX,
                box.bottom * scaleY
            )
        }
    }

    /**
     * 缩放关键点坐标
     */
    private fun scaleKeypoints(keypoints: List<FaceKeypoint>, scaleX: Float, scaleY: Float): List<FaceKeypoint> {
        return keypoints.map { kp ->
            if (isFrontCamera) {
                FaceKeypoint(kp.type, width - kp.x * scaleX, kp.y * scaleY)
            } else {
                FaceKeypoint(kp.type, kp.x * scaleX, kp.y * scaleY)
            }
        }
    }

    /**
     * 绘制调试信息
     */
    private fun drawDebugInfo(canvas: Canvas, box: RectF, keypoints: List<FaceKeypoint>) {
        // 绘制人脸边界框
        canvas.drawRect(box, debugPaint)

        // 绘制关键点
        for (kp in keypoints) {
            canvas.drawCircle(kp.x, kp.y, 8f, keypointPaint)
        }
    }

    /**
     * 绘制贴纸
     */
    private fun drawSticker(
        canvas: Canvas,
        sticker: Sticker,
        faceBox: RectF,
        keypoints: List<FaceKeypoint>
    ) {
        if (sticker.type == StickerType.NONE) return

        val bitmap = getBitmap(sticker.drawableRes) ?: return

        when (sticker.type) {
            StickerType.GLASSES -> drawGlasses(canvas, bitmap, sticker, faceBox, keypoints)
            StickerType.HAT, StickerType.CAT_EARS, StickerType.CROWN -> drawHat(canvas, bitmap, sticker, faceBox)
            StickerType.MUSTACHE -> drawMustache(canvas, bitmap, sticker, faceBox, keypoints)
            StickerType.DOG_NOSE -> drawNose(canvas, bitmap, sticker, faceBox, keypoints)
            StickerType.MASK -> drawMask(canvas, bitmap, sticker, faceBox)
            else -> {}
        }
    }

    /**
     * 绘制眼镜贴纸
     */
    private fun drawGlasses(
        canvas: Canvas,
        bitmap: Bitmap,
        sticker: Sticker,
        faceBox: RectF,
        keypoints: List<FaceKeypoint>
    ) {
        val leftEye = keypoints.find { it.type == FaceKeypointType.LEFT_EYE }
        val rightEye = keypoints.find { it.type == FaceKeypointType.RIGHT_EYE }

        if (leftEye != null && rightEye != null) {
            // 计算眼睛中心和宽度
            val centerX = (leftEye.x + rightEye.x) / 2
            val centerY = (leftEye.y + rightEye.y) / 2
            val eyeDistance = kotlin.math.abs(rightEye.x - leftEye.x)
            
            // 贴纸宽度为眼距的2.2倍
            val stickerWidth = eyeDistance * 2.2f * sticker.scaleRatio
            val stickerHeight = stickerWidth * bitmap.height / bitmap.width

            val destRect = RectF(
                centerX - stickerWidth / 2,
                centerY - stickerHeight / 2,
                centerX + stickerWidth / 2,
                centerY + stickerHeight / 2
            )
            canvas.drawBitmap(bitmap, null, destRect, paint)
        } else {
            // 如果没有关键点，使用人脸框估算位置
            val centerX = faceBox.centerX()
            val centerY = faceBox.top + faceBox.height() * 0.35f
            val stickerWidth = faceBox.width() * sticker.scaleRatio
            val stickerHeight = stickerWidth * bitmap.height / bitmap.width

            val destRect = RectF(
                centerX - stickerWidth / 2,
                centerY - stickerHeight / 2,
                centerX + stickerWidth / 2,
                centerY + stickerHeight / 2
            )
            canvas.drawBitmap(bitmap, null, destRect, paint)
        }
    }

    /**
     * 绘制帽子/猫耳朵/皇冠贴纸
     * 贴纸应该显示在头顶上方
     */
    private fun drawHat(
        canvas: Canvas,
        bitmap: Bitmap,
        sticker: Sticker,
        faceBox: RectF
    ) {
        val stickerWidth = faceBox.width() * 1.3f * sticker.scaleRatio
        val stickerHeight = stickerWidth * bitmap.height / bitmap.width
        val centerX = faceBox.centerX()

        // 贴纸底部对齐到人脸框顶部，再向上偏移一些
        // offsetYRatio 用于微调位置
        val bottomY = faceBox.top - faceBox.height() * 0.1f * sticker.offsetYRatio
        val topY = bottomY - stickerHeight

        val destRect = RectF(
            centerX - stickerWidth / 2,
            topY,
            centerX + stickerWidth / 2,
            bottomY
        )
        canvas.drawBitmap(bitmap, null, destRect, paint)
    }

    /**
     * 绘制胡子贴纸
     */
    private fun drawMustache(
        canvas: Canvas,
        bitmap: Bitmap,
        sticker: Sticker,
        faceBox: RectF,
        keypoints: List<FaceKeypoint>
    ) {
        val noseTip = keypoints.find { it.type == FaceKeypointType.NOSE_TIP }
        val mouthCenter = keypoints.find { it.type == FaceKeypointType.MOUTH_CENTER }

        val centerX: Float
        val centerY: Float

        if (noseTip != null && mouthCenter != null) {
            centerX = (noseTip.x + mouthCenter.x) / 2
            centerY = (noseTip.y + mouthCenter.y) / 2
        } else {
            centerX = faceBox.centerX()
            centerY = faceBox.top + faceBox.height() * 0.7f
        }

        val stickerWidth = faceBox.width() * 0.6f * sticker.scaleRatio
        val stickerHeight = stickerWidth * bitmap.height / bitmap.width

        val destRect = RectF(
            centerX - stickerWidth / 2,
            centerY - stickerHeight / 2,
            centerX + stickerWidth / 2,
            centerY + stickerHeight / 2
        )
        canvas.drawBitmap(bitmap, null, destRect, paint)
    }

    /**
     * 绘制鼻子贴纸
     */
    private fun drawNose(
        canvas: Canvas,
        bitmap: Bitmap,
        sticker: Sticker,
        faceBox: RectF,
        keypoints: List<FaceKeypoint>
    ) {
        val noseTip = keypoints.find { it.type == FaceKeypointType.NOSE_TIP }

        val centerX = noseTip?.x ?: faceBox.centerX()
        val centerY = noseTip?.y ?: (faceBox.top + faceBox.height() * 0.55f)

        val stickerWidth = faceBox.width() * 0.4f * sticker.scaleRatio
        val stickerHeight = stickerWidth * bitmap.height / bitmap.width

        val destRect = RectF(
            centerX - stickerWidth / 2,
            centerY - stickerHeight / 2,
            centerX + stickerWidth / 2,
            centerY + stickerHeight / 2
        )
        canvas.drawBitmap(bitmap, null, destRect, paint)
    }

    /**
     * 绘制面具贴纸
     */
    private fun drawMask(
        canvas: Canvas,
        bitmap: Bitmap,
        sticker: Sticker,
        faceBox: RectF
    ) {
        val stickerWidth = faceBox.width() * sticker.scaleRatio
        val stickerHeight = stickerWidth * bitmap.height / bitmap.width
        val centerX = faceBox.centerX()
        val centerY = faceBox.centerY() - faceBox.height() * 0.1f

        val destRect = RectF(
            centerX - stickerWidth / 2,
            centerY - stickerHeight / 2,
            centerX + stickerWidth / 2,
            centerY + stickerHeight / 2
        )
        canvas.drawBitmap(bitmap, null, destRect, paint)
    }

    /**
     * 获取缓存的位图（支持VectorDrawable）
     */
    private fun getBitmap(@DrawableRes resId: Int): Bitmap? {
        var bitmap = bitmapCache.get(resId)
        if (bitmap == null) {
            try {
                val drawable = AppCompatResources.getDrawable(context, resId)
                bitmap = drawable?.toBitmap()
                bitmap?.let { bitmapCache.put(resId, it) }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return bitmap
    }

    /**
     * 清理缓存
     */
    fun clearCache() {
        bitmapCache.evictAll()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        clearCache()
    }
}
