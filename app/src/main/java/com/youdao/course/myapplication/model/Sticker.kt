package com.youdao.course.myapplication.model

import androidx.annotation.DrawableRes

/**
 * 贴纸类型枚举
 */
enum class StickerType {
    NONE,           // 无贴纸
    GLASSES,        // 眼镜
    HAT,            // 帽子
    MUSTACHE,       // 胡子
    CAT_EARS,       // 猫耳朵
    DOG_NOSE,       // 狗鼻子
    CROWN,          // 皇冠
    MASK            // 面具
}

/**
 * 贴纸数据类
 * @param id 贴纸唯一标识
 * @param type 贴纸类型
 * @param drawableRes 贴纸图片资源ID
 * @param name 贴纸名称
 * @param offsetYRatio 垂直位置偏移比例（相对于人脸高度，0为人脸顶部，1为人脸底部）
 * @param scaleRatio 贴纸缩放比例（相对于人脸宽度）
 */
data class Sticker(
    val id: Int,
    val type: StickerType,
    @DrawableRes val drawableRes: Int,
    val name: String,
    val offsetYRatio: Float = 0.5f,
    val scaleRatio: Float = 1.0f
)

/**
 * 人脸检测结果数据类
 * 包含人脸边界框和关键点信息
 */
data class FaceDetectionResult(
    val boundingBox: FaceBoundingBox,
    val keypoints: List<FaceKeypoint> = emptyList(),
    val confidence: Float = 0f
)

/**
 * 人脸边界框
 */
data class FaceBoundingBox(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
    val centerX: Float get() = (left + right) / 2
    val centerY: Float get() = (top + bottom) / 2
}

/**
 * 人脸关键点类型
 */
enum class FaceKeypointType {
    LEFT_EYE,
    RIGHT_EYE,
    NOSE_TIP,
    MOUTH_CENTER,
    LEFT_EAR_TRAGION,
    RIGHT_EAR_TRAGION
}

/**
 * 人脸关键点
 */
data class FaceKeypoint(
    val type: FaceKeypointType,
    val x: Float,
    val y: Float
)
