package sdk.chat.demo.robot.utils
import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.Toast
import sdk.chat.demo.pre.R

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

    // 设置屏幕壁纸
    fun setScreenWallpaper(bitmap: Bitmap,which:Int): Boolean {
        return try {
            val wallpaperManager = WallpaperManager.getInstance(context)
            wallpaperManager.setBitmap(bitmap, null, true,which)
            Toast.makeText(context, context.getString(R.string.set_wallpaper_success), Toast.LENGTH_SHORT).show()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, context.getString(R.string.failed_and_retry), Toast.LENGTH_SHORT).show()
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