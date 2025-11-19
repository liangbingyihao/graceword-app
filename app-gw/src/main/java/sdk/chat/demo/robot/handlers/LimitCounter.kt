package sdk.chat.demo.robot.handlers
import android.content.Context
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors
import androidx.core.content.edit
import sdk.chat.demo.MainApp
import sdk.chat.demo.robot.api.model.ActionLimitConfig

object LimitCounter {
    private const val PREFS_NAME = "DailyLimitCounter"
    private val DATE_FORMAT = SimpleDateFormat("yyyyMMdd", Locale.US)

    // 单一后台线程池
    private val executor = Executors.newSingleThreadExecutor()

    // 内存缓存
    private val memoryCache = mutableMapOf<String, CounterItem>()
    private var isInitialized = false

    data class CounterItem(
        val key: String,
        var count: Int,
        var lastAccessDate: String
    )

    // 初始化：从 SharedPreferences 加载数据到内存
    fun initialize(context: Context, onComplete: (() -> Unit)? = null) {
        executor.execute {
            synchronized(this) {
                if (isInitialized) return@execute

                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val currentDate = getCurrentDate()

                prefs.all.forEach { (key, value) ->
                    val storedValue = value as? String ?: return@forEach
                    val parts = storedValue.split("|")
                    if (parts.size == 2) {
                        val count = parts[0].toIntOrNull() ?: 0
                        val date = parts[1]

                        // 只加载今天的数据
                        if (date == currentDate) {
                            memoryCache[key] = CounterItem(key, count, date)
                        }
                    }
                }

                isInitialized = true
            }
            onComplete?.invoke()
        }
    }

    // 获取当前日期
    private fun getCurrentDate(): String = DATE_FORMAT.format(Date())

    // 增加计数（使用配置中的限制）
    fun increment(actionName: String): Boolean {
        checkInitialized()

        if (!ActionLimitConfig.containsAction(actionName)) {
            return false
        }

        val currentDate = getCurrentDate()
        val item = memoryCache[actionName]

        if (item != null) {
            if (item.lastAccessDate == currentDate) {
                item.count++
            } else {
                item.count = 1
                item.lastAccessDate = currentDate
            }
        } else {
            memoryCache[actionName] = CounterItem(actionName, 1, currentDate)
        }

        Log.e("BillingManager","increment:$actionName")
        saveToStorage(MainApp.getContext(), actionName)
        return true
    }


    // 获取当前计数（同步）
    fun getCount(key: String): Int {
        checkInitialized()

        val item = memoryCache[key] ?: return 0
        val currentDate = getCurrentDate()

        return if (item.lastAccessDate == currentDate) item.count else 0
    }

    // 检查是否超过限制
    fun isWithinLimit(key: String, maxLimit: Int): Boolean {
        return getCount(key) < maxLimit
    }

    // 执行动作（检查限制）
    fun performAction(actionName: String): Boolean {
        if (!ActionLimitConfig.containsAction(actionName)) {
            return false
        }

        if(BillingManager.getInstance().hasSubscriptions()){
            return true
        }

        val limit = ActionLimitConfig.getLimit(actionName)
        if (isWithinLimit(actionName, limit)) {
            return increment(actionName)
        }
        return false
    }

//    // 获取剩余次数
//    fun getRemainingCount(key: String, maxLimit: Int): Int {
//        val currentCount = getCount(key)
//        return (maxLimit - currentCount).coerceAtLeast(0)
//    }

    // 使用配置中的限制检查
    fun canPerformAction(actionName: String): Boolean {
        if (!ActionLimitConfig.containsAction(actionName)) {
            return false
        }

        if(BillingManager.getInstance().hasSubscriptions()){
            return true
        }

        val limit = ActionLimitConfig.getLimit(actionName)
        return isWithinLimit(actionName, limit)
    }

    // 获取剩余次数
    fun getRemainingCount(actionName: String): Int {
        if (!ActionLimitConfig.containsAction(actionName)) {
            return 0
        }
        val limit = ActionLimitConfig.getLimit(actionName)
        val currentCount = getCount(actionName)
        Log.e("BillingManager","getRemainingCount:$actionName,$limit,$currentCount")
        return (limit - currentCount).coerceAtLeast(0)
    }

    // 重置计数
    fun reset(context: Context, key: String) {
        checkInitialized()
        memoryCache.remove(key)

        executor.execute {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit() { remove(key) }
        }
    }

    // 清除所有
    fun clearAll(context: Context) {
        checkInitialized()
        memoryCache.clear()

        executor.execute {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit() { clear() }
        }
    }

    // 获取今日所有计数
    fun getTodayCounts(): Map<String, Int> {
        checkInitialized()

        val currentDate = getCurrentDate()
        return memoryCache
            .filter { (_, item) -> item.lastAccessDate == currentDate }
            .mapValues { (_, item) -> item.count }
    }

    // 强制保存所有数据
    fun flushToStorage(context: Context, onComplete: (() -> Unit)? = null) {
        executor.execute {
            saveAllToStorage(context)
            onComplete?.invoke()
        }
    }

    // 清理过期数据
    fun cleanupExpiredData(context: Context) {
        executor.execute {
            val currentDate = getCurrentDate()
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit() {

                prefs.all.forEach { (key, value) ->
                    val storedValue = value as? String ?: return@forEach
                    val parts = storedValue.split("|")
                    if (parts.size == 2 && parts[1] != currentDate) {
                        remove(key)
                        memoryCache.remove(key)
                    }
                }

            }
        }
    }

    // 关闭线程池（应用退出时调用）
    fun shutdown() {
        executor.shutdown()
    }

    // 私有方法：检查初始化
    private fun checkInitialized() {
        if (!isInitialized) {
//            throw IllegalStateException("LimitCounter not initialized. Call initialize() first.")
        }
    }

    // 私有方法：异步保存单个key
    private fun saveToStorage(context: Context, key: String) {
        executor.execute {
            val item = memoryCache[key] ?: return@execute
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit() {
                    putString(key, "${item.count}|${item.lastAccessDate}")
            }
        }
    }

    // 私有方法：保存所有数据
    private fun saveAllToStorage(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit() {

            memoryCache.forEach { (_, item) ->
                putString(item.key, "${item.count}|${item.lastAccessDate}")
            }

        }
    }
}