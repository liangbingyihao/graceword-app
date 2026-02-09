package sdk.chat.demo.robot.service

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.RectF
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.util.DisplayMetrics
import android.util.Log
import android.view.MotionEvent
import android.view.Surface
import android.view.SurfaceHolder
import android.view.WindowManager
import com.google.gson.Gson
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.Consumer
import org.json.JSONObject
import org.tinylog.Logger
import sdk.chat.core.events.EventType
import sdk.chat.core.events.NetworkEvent
import sdk.chat.core.session.ChatSDK
import sdk.chat.demo.MainApp
import sdk.chat.demo.pre.R
import sdk.chat.demo.robot.api.ImageApi
import sdk.chat.demo.robot.api.JsonCacheManager
import sdk.chat.demo.robot.api.model.BlessData
import sdk.chat.demo.robot.api.model.ImageDaily
import sdk.chat.demo.robot.extensions.DateLocalizationUtil.formatDayAgo
import sdk.chat.demo.robot.handlers.CardApiService
import sdk.chat.demo.robot.handlers.CardGenerator
import sdk.chat.demo.robot.handlers.WallpaperConfig
import java.util.concurrent.Executors
import sdk.chat.demo.robot.activities.MainDrawerActivity
import sdk.chat.demo.robot.utils.DisplayCompat

class BibleWallpaperService : WallpaperService() {
    private val TAG = "BibleWallpaperEngine"
    private var isClikable: Boolean = false
    private var wallpaperConfig: WallpaperConfig? = null
    private val dm = CompositeDisposable()
    private var lastOrientation: Int = -1

    override fun onCreateEngine(): Engine {
        return BibleWallpaperEngine()
    }

    override fun onCreate() {
        super.onCreate()
        Log.e(TAG, "onCreate")
        dm.add(
            ChatSDK.events().sourceOnMain()
                .filter(NetworkEvent.filterType(EventType.WallpaperConfigChange))
                .subscribe(Consumer {
                    Log.e(TAG, " WallpaperConfigChange")
                })
        )
    }

    private inner class BibleWallpaperEngine : Engine() {
        private val handler = Handler(Looper.getMainLooper())
        private val drawRunnable = Runnable { drawFrame() }
        private val executor = Executors.newSingleThreadExecutor()

        // 壁纸状态
        private var visible = false
        private var width = 0
        private var height = 0

        private var currentImageData: ImageDaily? = null

        // 时间控制
        private var lastImageChangeTime = System.currentTimeMillis()

        // 触摸区域
        private var verseRect: RectF? = null
        private var isVerseAreaTouched = false
        private var lastDate: String = ""
        private var isPortrait = false
        private val displayCompat = DisplayCompat

//        // 绘图工具
//        private val textPaint = Paint().apply {
//            isAntiAlias = true
//            color = Color.WHITE
//            textSize = 48f
//            typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
//            textAlign = Paint.Align.CENTER
//        }


        override fun onVisibilityChanged(visible: Boolean) {
            this.visible = visible

            Log.e(TAG, " onVisibilityChanged:${visible}")
            if (visible) {
                // 创建新截图
                initializeData()
                drawFrame()
            } else {
                handler.removeCallbacks(drawRunnable)
            }
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            this.width = width
            this.height = height
            this.isPortrait = this.height > this.width

            Log.e(TAG, "onSurfaceChanged:${this.width},${this.height},${this.isPortrait}")
//            updateVerseRect()
            super.onSurfaceChanged(holder, format, width, height)
        }

        override fun onTouchEvent(event: MotionEvent) {
            handleTouchEvent(event)
        }

        /**
         * 初始化数据
         */
        private var blessData: BlessData? = null
        private var gwImages: List<ImageDaily> = emptyList()

        private fun initializeData() {
            var lastConfig = wallpaperConfig

            wallpaperConfig = CardApiService.getWallPaperConfig();
            isClikable = wallpaperConfig?.isReadScriptureEnabled == true
            Log.e(TAG, "get wallpaperConfig: ${Gson().toJson(wallpaperConfig)}")
            if (lastConfig != null) {
                if (lastConfig.date != wallpaperConfig?.date || lastConfig.greeting != wallpaperConfig?.greeting) {
                    Log.e(TAG, "get wallpaperConfig and reset cache")
                    currentImageData = null
                }
            }

            var isBlessData = CardApiService.FROM_CARD.equals(wallpaperConfig?.from)
            var today = formatDayAgo(0)
            var lastBless = blessData?.daily?.lastOrNull()
            if (isBlessData && (lastBless == null || today > lastBless.date)) {
                dm.add(
                    CardApiService.getBlessData().subscribe(
                        { bless ->
                            blessData = bless
//                            Log.e(
//                                TAG,
//                                "${today} get blessdata: ${bless?.daily?.lastOrNull()?.date}"
//                            )
                            Logger.error { "${TAG}:${today} get blessdata: ${bless?.font}" }
                            if (currentImageData == null && blessData != null) {
                                drawFrame()
                            }
                        },
                        Consumer { e: Throwable? ->
                            Logger.error { "${TAG}:$today get blessdata error: ${e.toString()}" }
                        })
                )
            }

            dm.add(
                ImageApi.listImageDaily(today).subscribe(
                    { data ->
                        gwImages = data
                        Log.e(TAG, "$today get gwImages: ${gwImages.firstOrNull()?.date}")
                        Logger.error { "${TAG}:$today get gwImages: ${gwImages.firstOrNull()?.date}" }
                        if (currentImageData == null) {
                            drawFrame()
                        }
                    },
                    Consumer { e: Throwable? ->
//                        Log.e(TAG, "${today} get gwImages error: ${e.toString()}")
                        Logger.error { "${TAG}:$today get gwImages error: ${e.toString()}" }
//                        Log.e(TAG, "$today get gwImages error: ${e.toString()}")
//                        e?.printStackTrace()
                    })
            )
        }

//        private fun getStackTraceAsString(t: Throwable): String {
//            val sw: StringWriter = StringWriter()
//            val pw: PrintWriter = PrintWriter(sw)
//            t.printStackTrace(pw)
//            return sw.toString()
//        }

        /**
         * 处理触摸事件
         */
        private fun handleTouchEvent(event: MotionEvent) {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    val x = event.x
                    val y = event.y
                    isVerseAreaTouched = isClikable && verseRect?.contains(x, y) == true
                    Logger.info { "${TAG}:wallpaper ACTION_DOWN:$x,$y,$verseRect,$isVerseAreaTouched" }
                }

                MotionEvent.ACTION_UP -> {
                    if (isVerseAreaTouched) {
                        Logger.error { "${TAG}:wallpaper ACTION_UP" }
                        Log.e("isVerseAreaTouched", "isVerseAreaTouched...")
                        handleVerseClick()
                    }
                    isVerseAreaTouched = false
                }
            }
//            drawFrame()
        }

        /**
         * 处理经文点击
         */
        private fun handleVerseClick() {
            try {
                var reference = currentImageData?.reference ?: ""
                Log.e("bible_data", "handleVerseClick...$reference")
//                BibleActivity.start(applicationContext, reference = "太1:2", fullscreen = false, newTask = true)
                MainDrawerActivity.startBibleActivity(
                    applicationContext,
                    reference = reference,
                )
            } catch (e: Exception) {
                Log.e("bible_data", "handleVerseClick...$e")
                e.printStackTrace()
            }
        }

        private fun drawFrame() {
            handler.removeCallbacks(drawRunnable)
            if (!visible) return
            // 根据旋转计算实际方向
            this.isPortrait =
                displayCompat.getScreenOrientation(this@BibleWallpaperService) == Configuration.ORIENTATION_PORTRAIT
            if (!this.isPortrait) {
                Log.e(TAG, " !isPortrait in drawFrame:${this.width},${this.height}")
                handler.postDelayed(drawRunnable, 1000)
                return
            }

            val holder = surfaceHolder
            var canvas: Canvas? = null

            try {
                canvas = holder.lockCanvas()
                if (canvas != null) {
                    drawWallpaper(canvas)
                }
            } finally {
                if (canvas != null) {
                    holder.unlockCanvasAndPost(canvas)
                }
            }

            if (visible) {
                handler.postDelayed(drawRunnable, 1200000)
            }
        }

        private fun drawWallpaper(canvas: Canvas) {
//            checkAndChangeContent()
            if (this.isPortrait) {
                drawBackgroundImage(canvas)
            } else {
                Log.e(TAG, " !isPortrait:${this.width},${this.height},${visible}")
            }
        }


        private fun nextImageData(): ImageDaily? {
//            if (currentImageData != null && System.currentTimeMillis() - lastImageChangeTime <= 10000) {
//                Log.e(TAG, "get nextImageData:from cache:,got:${currentImageData?.date}")
//                return currentImageData
//            }
//            var ret: ImageDaily = blessData?.daily?.random()
//            if (ret == null && !gwImages.isNullOrEmpty()) {
//                ret = gwImages?.random()
//            }
            var ret: ImageDaily? = null
            var config = wallpaperConfig
            var bless = blessData
            var today = formatDayAgo(0)

            var latest = gwImages.firstOrNull()
            if (latest != null && latest.date != today) {
                initializeData()
            }


//            return this?.firstOrNull { "${it.date}-${today}" == date } ?: this?.lastOrNull()

            ret = config?.let { cfg ->
                when (cfg.from) {
                    CardApiService.FROM_CARD -> bless?.daily?.find { "${it.date}-${today}" == cfg.date }
                        ?: bless?.daily?.lastOrNull()

                    CardApiService.FROM_DAILY -> gwImages.find { "${it.date}-${today}" == cfg.date }
                        ?: gwImages.firstOrNull()

                    else -> null
                }
            }

            if (ret == null) {
                ret = gwImages.firstOrNull()
            }


            Logger.error { "$TAG, ${today},${config?.from} get new nextImageData: ${config?.date},got:${ret?.date}" }

            if (ret != null && wallpaperConfig != null) {
                ret.greeting = wallpaperConfig!!.greeting
                ret.fontUrl = wallpaperConfig!!.font
            }
            return ret
        }

        /**
         * 绘制背景图片
         */
        private fun drawBackgroundImage(canvas: Canvas) {
            var request = nextImageData()
//            Log.e(
//                TAG,
//                "generateBibleCard  ${Thread.currentThread().name} ,${request?.date}, ${request?.backgroundUrl}"
//            )
            if (request != null) {
                var resId =
                    if (!request.greeting.isNullOrEmpty() && !request.fontUrl.isNullOrEmpty()) R.layout.item_image_greeting else R.layout.item_image_gw

                var isUnderline = wallpaperConfig?.isReadScriptureEnabled == true
                var cacheKey = CardGenerator.getCacheKey(
                    resId, request, false,
                    isUnderline
                )
                val bitmap = CardGenerator.getCacheBitmap(cacheKey)

                if (bitmap != null && !bitmap.isRecycled) {
                    // 绘制缓存的图片
                    Log.e(
                        TAG,
                        "generateBibleCard  from cache ,${request.date}"
                    )
                    if (isClikable) {
                        val rect = CardGenerator.getCacheRect(cacheKey)
                        if (rect != null) {
                            verseRect = RectF(
                                rect.left - 10,
                                rect.top + 5,
                                rect.right + 10,
                                rect.bottom + 5
                            )
                        }
//                        Log.e(
//                            TAG,
//                            "generateBibleCard  from cache ,verseRect:${verseRect}"
//                        )
                    }
                    val scaledBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true)
                    canvas.drawBitmap(scaledBitmap, 0f, 0f, null)
                    scaledBitmap.recycle()
                    currentImageData = request
                    lastImageChangeTime = System.currentTimeMillis()
                    return
                }

                Log.e(
                    TAG,
                    "generateBibleCard  new ,${request.date},$width,$height,$isPortrait"
                )
                CardGenerator.generateBibleCard(
                    applicationContext,
                    resId,
                    request,
                    false,
                    isClikable,
                    { bitmap: Bitmap? ->
                        Log.e(
                            TAG,
                            "generateBibleCard callback ${request.date},$width,$height"
                        )
                        if (bitmap != null && !bitmap.isRecycled) {
                            handler.post(drawRunnable)
                        }
                        Unit
                    }, { err: Throwable? ->
                        Log.e(
                            TAG,
                            "generateBibleCard ${err.toString()}"
                        )
                        Unit
                    })
            }

        }


        override fun onDestroy() {
            super.onDestroy()

            Log.e(TAG, "onDestroy clear imageCache")
            handler.removeCallbacks(drawRunnable)
            executor.shutdown()

//            // 清理缓存
//            imageCache.clear()
//            loadingUrls.clear()
            dm.clear()
        }
    }

    /**
     * 圣经经文数据类
     */
    data class BibleVerse(
        val book: String,
        val chapter: Int,
        val verseStart: Int?,
        val verseEnd: Int?,
        val text: String,
        val translation: String
    ) {
        fun getDisplayText(): String = text

        fun toJsonString(): String {
            return JSONObject().apply {
                put("book", book)
                put("chapter", chapter)
                put("verseStart", verseStart)
                put("verseEnd", verseEnd)
                put("text", text)
                put("translation", translation)
            }.toString()
        }

        companion object {
            fun getDefaultVerse(): BibleVerse {
                return BibleVerse(
                    "诗篇", 23, 1, 1,
                    "耶和华是我的牧者，我必不至缺乏。",
                    "和合本"
                )
            }
        }
    }
}