package com.aiyifan.app.feature.video

import kotlin.math.roundToInt

data class FloatingWindowSize(
    val width: Int,
    val height: Int,
)

object FloatingWindowSizePolicy {
    fun resize(requestedWidth: Int, minWidth: Int, maxWidth: Int): FloatingWindowSize {
        val width = requestedWidth.coerceIn(minWidth, maxWidth)
        return FloatingWindowSize(
            width = width,
            height = (width * HEIGHT_RATIO / WIDTH_RATIO).roundToInt(),
        )
    }

    private const val WIDTH_RATIO = 16f
    private const val HEIGHT_RATIO = 9f
}
