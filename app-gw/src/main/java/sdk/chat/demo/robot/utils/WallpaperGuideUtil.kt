package sdk.chat.demo.robot.utils
import android.app.WallpaperManager
import android.content.*
import android.provider.Settings
import android.widget.Toast
import sdk.chat.demo.robot.service.BibleWallpaperService

class WallpaperGuideUtil(private val context: Context) {

    companion object {
        // 检查设备是否支持动态壁纸
        fun isLiveWallpaperSupported(context: Context): Boolean {
            val wallpaperManager = WallpaperManager.getInstance(context)
            return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
                wallpaperManager.isWallpaperSupported
            } else {
                true // 旧版本默认支持
            }
        }
    }

    /**
     * 引导用户设置动态壁纸（完整流程）
     */
    fun guideToSetLiveWallpaper() {
        if (!isLiveWallpaperSupported(context)) {
            showUnsupportedDialog()
            return
        }

        showWallpaperSelectionDialog()
    }

    /**
     * 引导用户设置动态壁纸（完整流程）
     */
    fun guideToDirectlySetLiveWallpaper() {
        if (!isLiveWallpaperSupported(context)) {
            showUnsupportedDialog()
            return
        }

        setDirectly()
    }

    /**
     * 显示壁纸选择对话框
     */
    private fun showWallpaperSelectionDialog() {
        val options = arrayOf("直接设置动态壁纸", "打开壁纸库选择", "查看设置教程")

        androidx.appcompat.app.AlertDialog.Builder(context)
            .setTitle("设置动态壁纸")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> setDirectly()  // 直接设置
                    1 -> openWallpaperPicker() // 打开选择器
                    2 -> showTutorial() // 显示教程
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    /**
     * 方法1：直接设置动态壁纸
     */
    private fun setDirectly() {
        try {
            val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                putExtra(
                    WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                    ComponentName(context, BibleWallpaperService::class.java)
                )
                // 添加预览选项（部分设备支持）
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            // 如果直接设置失败，尝试通用方法
            openWallpaperPicker()
        } catch (e: Exception) {
            Toast.makeText(context, "设置失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 方法2：打开壁纸选择器
     */
    fun openWallpaperPicker() {
        try {
            // 尝试多种Intent，兼容不同设备
            val intents = listOf(
                Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER),
                Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER),
                Intent().apply {
                    action = "android.service.wallpaper.LIVE_WALLPAPER_CHOOSER"
                }
            )

            var success = false
            for (intent in intents) {
                try {
                    context.startActivity(intent)
                    success = true
                    break
                } catch (e: ActivityNotFoundException) {
                    continue
                }
            }

            if (!success) {
                // 所有Intent都失败，打开系统设置
                openSystemWallpaperSettings()
            }

        } catch (e: Exception) {
            Toast.makeText(context, "无法打开壁纸设置", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 方法3：打开系统壁纸设置
     */
    private fun openSystemWallpaperSettings() {
        try {
            val intent = Intent().apply {
                action = Settings.ACTION_SETTINGS
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)

            // 提示用户手动操作
            Toast.makeText(context, "请在设置中搜索'壁纸'进行设置", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            showManualGuide()
        }
    }

    /**
     * 显示手动设置教程
     */
    private fun showTutorial() {
        val tutorial = """
        设置动态壁纸步骤：
        1. 长按桌面空白处
        2. 选择"壁纸"或"Wallpaper"
        3. 选择"动态壁纸"或"Live Wallpaper"  
        4. 找到并选择我们的壁纸
        5. 点击"设置壁纸"
        """.trimIndent()

        androidx.appcompat.app.AlertDialog.Builder(context)
            .setTitle("动态壁纸设置教程")
            .setMessage(tutorial)
            .setPositiveButton("立即设置") { _, _ -> openWallpaperPicker() }
            .setNegativeButton("取消", null)
            .show()
    }

    /**
     * 设备不支持提示
     */
    private fun showUnsupportedDialog() {
        androidx.appcompat.app.AlertDialog.Builder(context)
            .setTitle("不支持动态壁纸")
            .setMessage("您的设备不支持动态壁纸功能")
            .setPositiveButton("确定", null)
            .show()
    }

    /**
     * 显示手动设置指南
     */
    private fun showManualGuide() {
        val message = """
        请手动设置动态壁纸：
        
        方法一：
        1. 长按桌面空白处
        2. 选择"壁纸"
        3. 选择"动态壁纸" 
        4. 选择我们的应用
        
        方法二：
        1. 打开"设置"应用
        2. 搜索"壁纸"
        3. 进入壁纸设置
        4. 选择动态壁纸
        """.trimIndent()

        androidx.appcompat.app.AlertDialog.Builder(context)
            .setTitle("手动设置指南")
            .setMessage(message)
            .setPositiveButton("知道了", null)
            .show()
    }
}