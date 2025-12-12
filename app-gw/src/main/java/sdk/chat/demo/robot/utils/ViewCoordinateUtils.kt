package sdk.chat.demo.robot.utils

import android.graphics.RectF
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import androidx.recyclerview.widget.RecyclerView

/**
 * 增强版坐标获取工具
 */
object ViewCoordinateUtils {

    /**
     * 获取View在Bitmap中的精确坐标（支持嵌套布局）
     */
    fun getViewPositionInBitmap(targetView: View, rootView: View): ViewPosition {
        // 计算相对于根视图的坐标
        val relativePosition = calculateRelativePosition(targetView, rootView)

        // 考虑Padding和Margin
        val adjustedPosition = adjustForLayoutParams(targetView, relativePosition)

        return adjustedPosition
    }

    /**
     * 计算相对位置
     */
    private fun calculateRelativePosition(targetView: View, rootView: View): ViewPosition {
        var currentView: View? = targetView
        var totalX = 0f
        var totalY = 0f

        // 从目标View向上遍历到根View
        while (currentView != null && currentView != rootView) {
            totalX += currentView.left
            totalY += currentView.top

            // 考虑父容器的Scroll
            if (currentView.parent is View) {
                val parentView = currentView.parent as View
                if (parentView is ScrollView || parentView is RecyclerView) {
                    totalX -= parentView.scrollX
                    totalY -= parentView.scrollY
                }
            }

            currentView = if (currentView.parent is View) {
                currentView.parent as View
            } else {
                null
            }
        }

        return ViewPosition(
            x = totalX,
            y = totalY,
            width = targetView.measuredWidth.toFloat(),
            height = targetView.measuredHeight.toFloat(),
            view = targetView
        )
    }

    /**
     * 调整布局参数（Padding、Margin等）
     */
    private fun adjustForLayoutParams(targetView: View, position: ViewPosition): ViewPosition {
        // 考虑目标View的Margin
        val layoutParams = targetView.layoutParams as? ViewGroup.MarginLayoutParams
        val marginLeft = layoutParams?.leftMargin ?: 0
        val marginTop = layoutParams?.topMargin ?: 0

        // 考虑父容器的Padding
        val parent = targetView.parent as? ViewGroup
        val parentPaddingLeft = parent?.paddingLeft ?: 0
        val parentPaddingTop = parent?.paddingTop ?: 0

        return position.copy(
            x = position.x + marginLeft + parentPaddingLeft,
            y = position.y + marginTop + parentPaddingTop
        )
    }

    /**
     * 获取View的边界矩形（在Bitmap坐标系中）
     */
    fun getViewBoundsInBitmap(targetView: View, rootView: View): RectF {
        val position = getViewPositionInBitmap(targetView, rootView)
        return RectF(
            position.x,
            position.y,
            position.x + position.width,
            position.y + position.height
        )
    }

    /**
     * 验证坐标是否在Bitmap范围内
     */
    fun validatePositionInBitmap(position: ViewPosition, bitmapWidth: Int, bitmapHeight: Int): Boolean {
        return position.x >= 0 &&
                position.y >= 0 &&
                position.x + position.width <= bitmapWidth &&
                position.y + position.height <= bitmapHeight
    }
}

/**
 * 视图位置信息
 */
data class ViewPosition(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val view: View
) {
    val centerX: Float get() = x + width / 2
    val centerY: Float get() = y + height / 2
    val right: Float get() = x + width
    val bottom: Float get() = y + height
}