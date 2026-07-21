package com.aiyifan.app.core.ui

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import kotlin.math.max

class KeywordFlowLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : ViewGroup(context, attrs) {
    private val horizontalGap = context.dp(8)
    private val verticalGap = context.dp(8)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val availableWidth = MeasureSpec.getSize(widthMeasureSpec) - paddingLeft - paddingRight
        var rowWidth = 0
        var rowHeight = 0
        var contentHeight = 0
        var maxRowWidth = 0

        forEachVisibleChild { child ->
            child.measure(
                MeasureSpec.makeMeasureSpec(availableWidth.coerceAtLeast(0), MeasureSpec.AT_MOST),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
            )
            if (rowWidth > 0 && rowWidth + horizontalGap + child.measuredWidth > availableWidth) {
                contentHeight += rowHeight + verticalGap
                maxRowWidth = max(maxRowWidth, rowWidth)
                rowWidth = 0
                rowHeight = 0
            }
            if (rowWidth > 0) rowWidth += horizontalGap
            rowWidth += child.measuredWidth
            rowHeight = max(rowHeight, child.measuredHeight)
        }
        if (rowHeight > 0) {
            contentHeight += rowHeight
            maxRowWidth = max(maxRowWidth, rowWidth)
        }
        setMeasuredDimension(
            resolveSize(maxRowWidth + paddingLeft + paddingRight, widthMeasureSpec),
            resolveSize(contentHeight + paddingTop + paddingBottom, heightMeasureSpec),
        )
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val availableWidth = width - paddingLeft - paddingRight
        var x = paddingLeft
        var y = paddingTop
        var rowHeight = 0

        forEachVisibleChild { child ->
            if (x > paddingLeft && x + horizontalGap + child.measuredWidth > paddingLeft + availableWidth) {
                x = paddingLeft
                y += rowHeight + verticalGap
                rowHeight = 0
            }
            if (x > paddingLeft) x += horizontalGap
            child.layout(x, y, x + child.measuredWidth, y + child.measuredHeight)
            x += child.measuredWidth
            rowHeight = max(rowHeight, child.measuredHeight)
        }
    }

    override fun generateDefaultLayoutParams(): LayoutParams =
        LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)

    override fun generateLayoutParams(attrs: AttributeSet?): LayoutParams = LayoutParams(context, attrs)

    override fun checkLayoutParams(params: LayoutParams?): Boolean = params != null

    private inline fun forEachVisibleChild(block: (View) -> Unit) {
        for (index in 0 until childCount) {
            getChildAt(index).takeIf { it.visibility != GONE }?.let(block)
        }
    }

    private fun Context.dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
