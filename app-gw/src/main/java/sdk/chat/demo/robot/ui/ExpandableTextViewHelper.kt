package sdk.chat.demo.robot.ui
import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ImageSpan
import android.view.View
import android.view.ViewTreeObserver
import android.widget.TextView
import androidx.core.content.ContextCompat

class ExpandableTextViewHelper(
    private val textView: TextView,
    private val maxLinesCollapsed: Int = 3,
    private val expandDrawable: Drawable? = null,
    private val collapseDrawable: Drawable? = null
) {

    private var originalText: CharSequence = ""
    private var isExpanded = false

    // 默认图标尺寸
    private val iconSize = textView.textSize.toInt()

    private val preDrawListener = object : ViewTreeObserver.OnPreDrawListener {
        override fun onPreDraw(): Boolean {
            textView.viewTreeObserver.removeOnPreDrawListener(this)

            if (textView.lineCount > maxLinesCollapsed) {
                setupExpandableText()
            } else {
                textView.text = originalText
            }
            return true
        }
    }

    // 构造函数重载：使用资源ID
    constructor(
        textView: TextView,
        maxLinesCollapsed: Int = 3,
        expandIconResId: Int,
        collapseIconResId: Int
    ) : this(
        textView,
        maxLinesCollapsed,
        ContextCompat.getDrawable(textView.context, expandIconResId),
        ContextCompat.getDrawable(textView.context, collapseIconResId)
    )

    fun setText(text: CharSequence) {
        originalText = text
        textView.text = text
        isExpanded = false
        textView.maxLines = Integer.MAX_VALUE

        textView.post {
            setupTextView()
        }
    }

    private fun setupTextView() {
        try {
            textView.viewTreeObserver.removeOnPreDrawListener(preDrawListener)
        } catch (e: Exception) {
            // Ignore
        }

        if (originalText.length > 50) {
            textView.viewTreeObserver.addOnPreDrawListener(preDrawListener)
        } else {
            textView.text = originalText
        }
    }

    private fun setupExpandableText() {
        if (isExpanded) {
            // 展开状态：显示收起图标
            textView.text = buildExpandedText()
            textView.maxLines = Integer.MAX_VALUE
        } else {
            // 收起状态：显示展开图标
            textView.text = buildCollapsedText()
            textView.maxLines = maxLinesCollapsed
        }
    }

    private fun buildCollapsedText(): SpannableString {
        val layout = textView.layout ?: return SpannableString(originalText)

        // 计算截断位置
        val lineEndIndex = maxLinesCollapsed - 1
        if (lineEndIndex >= layout.lineCount) {
            return SpannableString(originalText)
        }

        val lineEnd = layout.getLineEnd(lineEndIndex)
        val safeEnd = lineEnd.coerceAtMost(originalText.length)

        // 找到合适的截断位置
        var truncatedLength = safeEnd
        while (truncatedLength > 0) {
            // 测量文本 + "... " 的宽度
            val testText = originalText.subSequence(0, truncatedLength).toString() + "... "
            if (textView.paint.measureText(testText) <= layout.width) {
                break
            }
            truncatedLength--
        }

        if (truncatedLength <= 0) {
            return SpannableString(originalText)
        }

        val truncatedText = originalText.subSequence(0, truncatedLength)
        val spannable = SpannableString("$truncatedText... ")

        // 添加展开图标
        val expandIcon = expandDrawable ?: getDefaultExpandIcon(textView.context)
        expandIcon.setBounds(0, 0, iconSize, iconSize)

        // 使用可点击的ImageSpan
        val expandSpan = ClickableImageSpan(expandIcon, isExpand = true)
        spannable.setSpan(
            expandSpan,
            spannable.length - 1,
            spannable.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        // 使整个图标区域可点击
        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                toggleExpand()
            }

            override fun updateDrawState(ds: TextPaint) {
                // 清除下划线
                ds.isUnderlineText = false
            }
        }

        spannable.setSpan(
            clickableSpan,
            spannable.length - 1,
            spannable.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        textView.movementMethod = LinkMovementMethod.getInstance()
        return spannable
    }

    private fun buildExpandedText(): SpannableString {
        val spannable = SpannableString("$originalText ")

        // 添加收起图标
        val collapseIcon = collapseDrawable ?: getDefaultCollapseIcon(textView.context)
        collapseIcon.setBounds(0, 0, iconSize, iconSize)

        val collapseSpan = ClickableImageSpan(collapseIcon, isExpand = false)
        spannable.setSpan(
            collapseSpan,
            spannable.length - 1,
            spannable.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        // 使图标可点击
        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                toggleExpand()
            }

            override fun updateDrawState(ds: TextPaint) {
                ds.isUnderlineText = false
            }
        }

        spannable.setSpan(
            clickableSpan,
            spannable.length - 1,
            spannable.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        textView.movementMethod = LinkMovementMethod.getInstance()
        return spannable
    }

    private fun getDefaultExpandIcon(context: Context): Drawable {
        // 创建一个默认的展开图标（向下箭头）
        return ContextCompat.getDrawable(context, android.R.drawable.arrow_down_float)?.apply {
            setTint(Color.BLUE)
        } ?: run {
            // 如果找不到，创建一个简单的图形
            val drawable = context.getDrawable(android.R.drawable.ic_menu_more)
            drawable?.setTint(Color.BLUE)
            drawable!!
        }
    }

    private fun getDefaultCollapseIcon(context: Context): Drawable {
        // 创建一个默认的收起图标（向上箭头）
        return ContextCompat.getDrawable(context, android.R.drawable.arrow_up_float)?.apply {
            setTint(Color.BLUE)
        } ?: run {
            val drawable = context.getDrawable(android.R.drawable.ic_menu_more)
            drawable?.setTint(Color.BLUE)
            drawable!!
        }
    }

    private fun toggleExpand() {
        isExpanded = !isExpanded
        setupExpandableText()
    }

    // 自定义 ImageSpan，支持点击
    private inner class ClickableImageSpan(
        private val drawable: Drawable,
        private val isExpand: Boolean
    ) : ImageSpan(drawable) {

        override fun getSize(
            paint: Paint,
            text: CharSequence?,
            start: Int,
            end: Int,
            fm: Paint.FontMetricsInt?
        ): Int {
            // 设置图标与文本的垂直对齐
            val size = super.getSize(paint, text, start, end, fm)
            val metrics = paint.fontMetricsInt
            val iconHeight = drawable.bounds.height()

            // 垂直居中
            if (fm != null) {
                fm.ascent = metrics.ascent - (iconHeight - (metrics.descent - metrics.ascent)) / 2
                fm.descent = metrics.descent + (iconHeight - (metrics.descent - metrics.ascent)) / 2
                fm.top = fm.ascent
                fm.bottom = fm.descent
            }

            return size
        }
    }

    fun dispose() {
        try {
            textView.viewTreeObserver.removeOnPreDrawListener(preDrawListener)
        } catch (e: Exception) {
            // Ignore
        }
    }
}