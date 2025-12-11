package sdk.chat.demo.robot.utils
import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.Toast

class WallpaperUtils(private val context: Context) {

    // 设置锁屏壁纸
    fun setLockScreenWallpaper(bitmap: Bitmap): Boolean {
        return try {
            val wallpaperManager = WallpaperManager.getInstance(context)
            wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK)
            Toast.makeText(context, "锁屏壁纸设置成功", Toast.LENGTH_SHORT).show()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "设置失败: ${e.message}", Toast.LENGTH_SHORT).show()
            false
        }
    }

    // 设置主屏幕壁纸
    fun setHomeScreenWallpaper(bitmap: Bitmap): Boolean {
        return try {
            val wallpaperManager = WallpaperManager.getInstance(context)
            wallpaperManager.setBitmap(bitmap)
            Toast.makeText(context, "主屏壁纸设置成功", Toast.LENGTH_SHORT).show()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "设置失败: ${e.message}", Toast.LENGTH_SHORT).show()
            false
        }
    }

    // 同时设置锁屏和主屏壁纸
    fun setBothWallpapers(bitmap: Bitmap): Boolean {
        return try {
            val wallpaperManager = WallpaperManager.getInstance(context)
            wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK)
            Toast.makeText(context, "壁纸设置成功", Toast.LENGTH_SHORT).show()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "设置失败: ${e.message}", Toast.LENGTH_SHORT).show()
            false
        }
    }
}