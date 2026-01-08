package sdk.chat.demo.robot.extensions

import android.content.Context
import android.os.Build
import android.text.format.DateUtils
import androidx.annotation.RequiresApi
import com.vojtkovszky.billinghelper.BillingHelper
import sdk.chat.demo.MainApp
import sdk.chat.demo.pre.R
import java.text.ParseException
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone


object DateLocalizationUtil {
    val dayFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    var sdf: SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")

    // 常见的 UTC 时间格式
    private val utcPatterns = listOf(
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd HH:mm:ss",
        "yyyy/MM/dd HH:mm:ss",
        "EEE, dd MMM yyyy HH:mm:ss 'GMT'",  // HTTP 日期格式
        "yyyy-MM-dd"
    )

    /**
     * 通用 UTC 时间解析
     */
    fun parseUTCString(utcString: String): Date? {
//        // 1. 尝试使用 Instant.parse（最准确）
//        try {
//            val instant = Instant.parse(utcString)
//            return Date.from(instant)
//        } catch (e: DateTimeParseException) {
//            // 继续尝试其他方法
//        }

        // 2. 尝试各种日期格式
        for (pattern in utcPatterns) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.getDefault())
                sdf.timeZone = TimeZone.getTimeZone("UTC")
                sdf.isLenient = false
                return sdf.parse(utcString)
            } catch (e: Exception) {
                // 继续尝试下一个格式
            }
        }

        // 3. 尝试解析为时间戳
        try {
            val timestamp = utcString.toLong()
            return Date(timestamp)
        } catch (e: Exception) {
            // 不是时间戳
        }

        return null
    }

    /**
     * 安全解析，带默认值
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun parseUTCStringOrDefault(
        utcString: String?,
        default: Date = Date()
    ): Date {
        return if (utcString.isNullOrBlank()) {
            default
        } else {
            parseUTCString(utcString) ?: default
        }
    }


    fun getFriendlyDate(context: Context, date: Date): String {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply { time = date }

        return when {
            isToday(now, target) -> context.getString(R.string.today)
            isYesterday(now, target) -> context.getString(R.string.yesterday)
            isWithinDays(now, target, 7) -> getDaysAgoText(context, now, target)
            isSameYear(now, target) -> formatDate(
                date,
                context.getString(R.string.this_year_format)
            )

            else -> formatDate(date, context.getString(R.string.default_format))
        }
    }

    fun dateStr(date: Date?): String {
        if (date == null) {
            return ""
        }
        return dayFormat.format(date)
    }

    fun toDate(dateStr: String): Date {
        try {
            return sdf.parse(dateStr)
        } catch (e: ParseException) {
        }
        return Date();
    }

    private fun isToday(cal1: Calendar, cal2: Calendar): Boolean {
        return DateUtils.isToday(cal2.timeInMillis)
    }

    private fun isYesterday(now: Calendar, target: Calendar): Boolean {
        val yesterday = Calendar.getInstance().apply {
            add(Calendar.DATE, -1)
        }
        return isSameDay(yesterday, target)
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun isWithinDays(now: Calendar, target: Calendar, days: Int): Boolean {
        val diff = now.timeInMillis - target.timeInMillis
        return diff > 0 && diff < days * 24 * 60 * 60 * 1000L
    }

    private fun isSameYear(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR)
    }

    private fun getDaysAgoText(context: Context, now: Calendar, target: Calendar): String {
        val diffDays = ((now.timeInMillis - target.timeInMillis) / (24 * 60 * 60 * 1000)).toInt()
        return context.getString(R.string.days_ago, diffDays)
    }

    private fun formatTime(date: Date): String {
        return SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
    }

    private fun formatDate(date: Date, pattern: String): String {
        return SimpleDateFormat(pattern, Locale.getDefault()).format(date)
    }


    public fun formatDayAgo(dateAgo: Int): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -1 * dateAgo) // 减去dataAgo
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateFormat.format(calendar.time)
    }

    fun getDateBefore(dateStr: String?, dateAgo: Int): String {
        if (dateStr == null || dateStr.isEmpty()) {
            return formatDayAgo(dateAgo)
        }
        // 1. 解析输入日期
        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val parsedDate =
            inputFormat.parse(dateStr) ?: throw IllegalArgumentException("Invalid date format")

        // 2. 计算指定天数前的日期
        val calendar = Calendar.getInstance().apply {
            time = parsedDate
            add(Calendar.DAY_OF_YEAR, -dateAgo) // 减去指定天数
        }

        // 3. 格式化为字符串
        return inputFormat.format(calendar.time)
    }


    fun getCurrentFormattedDate(timestamp:Long,formatStr:String?="yyyy/MM/dd"): String {
        val sdf = SimpleDateFormat(formatStr, Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}