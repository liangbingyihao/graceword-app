package sdk.chat.demo.robot.ui

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.text.style.ReplacementSpan

class CustomDashedUnderlineSpan(
    private val color: Int = Color.RED,
    private val strokeWidth: Float = 2f,
    private val dashWidth: Float = 8f,
    private val dashGap: Float = 4f
) : ReplacementSpan() {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = this@CustomDashedUnderlineSpan.color
        this.strokeWidth = this@CustomDashedUnderlineSpan.strokeWidth
        this.style = Paint.Style.STROKE
        this.pathEffect = android.graphics.DashPathEffect(floatArrayOf(dashWidth, dashGap), 0f)
    }

    override fun getSize(
        paint: Paint,
        text: CharSequence?,
        start: Int,
        end: Int,
        fm: Paint.FontMetricsInt?
    ): Int {
        return paint.measureText(text, start, end).toInt()
    }

    override fun draw(
        canvas: Canvas,
        text: CharSequence?,
        start: Int,
        end: Int,
        x: Float,
        top: Int,
        y: Int,
        bottom: Int,
        originalPaint: Paint
    ) {
        // 创建文本绘制的Paint
        val textPaint = Paint(originalPaint)

        // 如果指定了颜色，应用到文本和下划线
        color.let { color ->
            textPaint.color = color
        }

        // 绘制文本
        canvas.drawText(text.toString(), start, end, x, y.toFloat(), textPaint)

        // 计算下划线的位置
        val underlineY = y + textPaint.descent() + 2f

        // 计算下划线的宽度
        val underlineWidth = textPaint.measureText(text, start, end)

        // 绘制虚线下划线
        canvas.drawLine(x, underlineY, x + underlineWidth, underlineY, this.paint)
    }
}
