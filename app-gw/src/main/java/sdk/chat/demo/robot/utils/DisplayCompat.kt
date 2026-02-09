package sdk.chat.demo.robot.utils

import android.content.Context
import android.content.res.Configuration
import android.graphics.Point
import android.os.Build
import android.util.DisplayMetrics
import android.view.Surface
import android.view.WindowManager
import androidx.annotation.RequiresApi

object DisplayCompat {

    /**
     * 获取屏幕尺寸（兼容所有 API 版本）
     */
    fun getScreenSize(context: Context): Pair<Int, Int> {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            getScreenSizeApi30(windowManager)
        } else {
            getScreenSizeLegacy(windowManager)
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun getScreenSizeApi30(windowManager: WindowManager): Pair<Int, Int> {
        return try {
            val windowMetrics = windowManager.currentWindowMetrics
            val bounds = windowMetrics.bounds
            Pair(bounds.width(), bounds.height())
        } catch (e: Exception) {
            // 回退到旧 API
            getScreenSizeLegacy(windowManager)
        }
    }

    @Suppress("DEPRECATION")
    private fun getScreenSizeLegacy(windowManager: WindowManager): Pair<Int, Int> {
        val display = windowManager.defaultDisplay
        val point = Point()

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
            display.getRealSize(point)  // 包括系统装饰
        } else {
            display.getSize(point)  // 不包括系统装饰
        }

        return Pair(point.x, point.y)
    }

    /**
     * 获取最大窗口尺寸
     */
    fun getMaximumWindowSize(context: Context): Pair<Int, Int> {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            getMaximumWindowSizeApi30(windowManager)
        } else {
            getMaximumWindowSizeLegacy(windowManager)
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun getMaximumWindowSizeApi30(windowManager: WindowManager): Pair<Int, Int> {
        val windowMetrics = windowManager.maximumWindowMetrics
        val bounds = windowMetrics.bounds
        return Pair(bounds.width(), bounds.height())
    }

    @Suppress("DEPRECATION")
    private fun getMaximumWindowSizeLegacy(windowManager: WindowManager): Pair<Int, Int> {
        val display = windowManager.defaultDisplay
        val point = Point()

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
            display.getRealSize(point)
        } else {
            display.getSize(point)
        }

        return Pair(point.x, point.y)
    }

    /**
     * 获取屏幕方向
     */
    fun getScreenOrientation(context: Context): Int {
        val (width, height) = getScreenSize(context)

        return if (width > height) {
            Configuration.ORIENTATION_LANDSCAPE
        } else {
            Configuration.ORIENTATION_PORTRAIT
        }
    }

    /**
     * 获取显示旋转
     */
    fun getDisplayRotation(context: Context): Int {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            val windowMetrics = windowManager.currentWindowMetrics
            val windowInsets = windowMetrics.windowInsets

            // 获取旋转
            val rotation = windowManager.defaultDisplay?.rotation ?: Surface.ROTATION_0

            rotation
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay?.rotation ?: Surface.ROTATION_0
        }
    }

    /**
     * 获取显示指标（包括密度等）
     */
    fun getDisplayMetrics(context: Context): DisplayMetrics {
        val resources = context.resources
        val metrics = DisplayMetrics()

        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            val windowMetrics = windowManager.currentWindowMetrics
            val bounds = windowMetrics.bounds

            // 设置尺寸
            metrics.widthPixels = bounds.width()
            metrics.heightPixels = bounds.height()

            // 从 Configuration 获取密度
            val config = resources.configuration
            metrics.densityDpi = config.densityDpi
            metrics.density = config.densityDpi / 160f
            metrics.scaledDensity = metrics.density
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay?.getMetrics(metrics)
        }

        return metrics
    }
}