package sdk.chat.demo.robot.ui

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView

class MaxWidthWrapImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    private var maxWidthPx: Int = 0

    fun setMaxWidthPx(maxWidth: Int) {
        this.maxWidthPx = maxWidth
        requestLayout()
    }

    fun setMaxWidthDp(maxWidthDp: Float) {
        this.maxWidthPx = (maxWidthDp * resources.displayMetrics.density).toInt()
        requestLayout()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var widthSpec = widthMeasureSpec

        if (maxWidthPx > 0) {
            val widthMode = MeasureSpec.getMode(widthMeasureSpec)

            when (widthMode) {
                MeasureSpec.AT_MOST, MeasureSpec.UNSPECIFIED -> {
                    // wrap_content 时是 UNSPECIFIED
                    widthSpec = MeasureSpec.makeMeasureSpec(maxWidthPx, MeasureSpec.AT_MOST)
                }
                // EXACTLY 时不处理
            }
        }

        // 先调用父类的测量
        super.onMeasure(widthSpec, heightMeasureSpec)

        val drawable = drawable
        if (drawable != null && adjustViewBounds) {
            // 获取图片原始尺寸
            val drawableWidth = drawable.intrinsicWidth
            val drawableHeight = drawable.intrinsicHeight

            if (drawableWidth > 0 && drawableHeight > 0) {
                // 计算等比例高度
                val width = measuredWidth
                val height = (drawableHeight * width / drawableWidth)

                // 设置精确的测量尺寸
                setMeasuredDimension(width, height)
            }
        }
    }
}