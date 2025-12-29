package sdk.chat.demo.robot.service

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.util.Log
import android.view.MotionEvent
import android.view.SurfaceHolder
import com.google.gson.Gson
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.Consumer
import org.json.JSONObject
import org.tinylog.Logger
import sdk.chat.core.events.EventType
import sdk.chat.core.events.NetworkEvent
import sdk.chat.core.session.ChatSDK
import sdk.chat.demo.pre.R
import sdk.chat.demo.robot.activities.BibleActivity
import sdk.chat.demo.robot.activities.EditCardActivity
import sdk.chat.demo.robot.activities.MainDrawerActivity
import sdk.chat.demo.robot.api.ImageApi
import sdk.chat.demo.robot.api.model.BlessData
import sdk.chat.demo.robot.api.model.ImageDaily
import sdk.chat.demo.robot.extensions.DateLocalizationUtil.formatDayAgo
import sdk.chat.demo.robot.handlers.CardApiService
import sdk.chat.demo.robot.handlers.CardGenerator
import sdk.chat.demo.robot.handlers.WallpaperConfig
import java.util.concurrent.Executors

class BibleWallpaperService : WallpaperService() {
    private val TAG = "BibleWallpaperEngine"
    private var isClikable: Boolean = false
    private var wallpaperConfig: WallpaperConfig? = null
    private val dm = CompositeDisposable()

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
//        private var lastSourceChangeTime = System.currentTimeMillis()
//        private val imageChangeInterval = 60 * 1000L // 1分钟切换图片
//        private val sourceChangeInterval = 10 * 60 * 1000L // 10分钟切换图片源

//        // 图片缓存
//        private val imageCache = mutableMapOf<String, Bitmap>()
//        private val loadingUrls = mutableSetOf<String>()
//        private val imageDailyCache = mutableMapOf<String, ImageDaily>()
//
//        // 经文数据
//        private val bibleVerses = mutableListOf<BibleVerse>()
//        private var currentVerse: BibleVerse? = null

        // 触摸区域
        private var verseRect: RectF? = null
        private var isVerseAreaTouched = false
        private var lastDate: String = ""

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
                initializeData()
                drawFrame()
            } else {
                handler.removeCallbacks(drawRunnable)
            }
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            this.width = width
            this.height = height
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

            var today = formatDayAgo(0)
            var lastBless = blessData?.daily?.lastOrNull()
            if (lastBless == null || today > lastBless.date) {
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
//                        Log.e(TAG, "$today get gwImages: ${gwImages?.firstOrNull()?.date}")
                        Logger.error { "${TAG}:$today get gwImages: ${gwImages.firstOrNull()?.date}" }
                        if (currentImageData == null) {
                            drawFrame()
                        }
                    },
                    Consumer { e: Throwable? ->
//                        Log.e(TAG, "${today} get gwImages error: ${e.toString()}")
                        Logger.error { "${TAG}:$today get gwImages error: ${e.toString()}" }
                    })
            )

//            val imageDailyList = ImageApi.getImageDailyListCache()
//            if (imageDailyList != null) {
//                var imgs: MutableList<ImageDaily> = imageDailyList.imgs
//                var bgList: MutableList<String>? = null
//                imgs.forEachIndexed { index, element ->
//                    if (index % 3 == 0) {
//                        bgList = mutableListOf<String>()
//                        imageSources.add(bgList)
//                    }
//                    var reference = BibleData.parseScriptureReference(element.reference)
//                    bibleVerses.add(
//                        BibleVerse(
//                            book = reference.bookName,
//                            verseStart = reference.verseStart,
//                            verseEnd = reference.verseEnd,
//                            chapter = reference.chapterStart,
//                            text = element.scripture,
//                            translation = ""
//                        )
//                    )
//                    bgList?.add(element.backgroundUrl)
//                    imageDailyCache[element.backgroundUrl] = element
//                }
//            }
////            loadBibleVerses()
//            selectRandomVerse()
//            currentImageUrls = imageSources[currentImageSourceIndex]
//            preloadCurrentSourceImages()
        }

//        /**
//         * 加载圣经经文
//         */
//        private fun loadBibleVerses() {
//            try {
//                val inputStream = assets.open("bible_verses.json")
//                val jsonString = inputStream.bufferedReader().use { it.readText() }
//                val jsonArray = JSONArray(jsonString)
//
//                for (i in 0 until jsonArray.length()) {
//                    val jsonObject = jsonArray.getJSONObject(i)
//                    val verse = BibleVerse(
//                        book = jsonObject.getString("book"),
//                        chapter = jsonObject.getInt("chapter"),
//                        verseStart = jsonObject.getInt("verseStart"),
//                        verseEnd = jsonObject.getInt("verseEnd"),
//                        text = jsonObject.getString("text"),
//                        translation = jsonObject.getString("translation")
//                    )
//                    bibleVerses.add(verse)
//                }
//
//                selectRandomVerse()
//
//            } catch (e: Exception) {
//                e.printStackTrace()
//                currentVerse = BibleVerse.getDefaultVerse()
//            }
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
                MainDrawerActivity.startBibleActivity(
                    applicationContext,
                    reference = currentImageData?.reference ?: "",
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        /**
         * 更新经文区域
         */
        private fun updateVerseRect() {
            val padding = 80f
            val rectWidth = width - 2 * padding
            val rectHeight = 200f

//            verseRect = RectF(
//                padding,
//                height - rectHeight - padding - 200,
//                padding + rectWidth,
//                height - padding - 200
//            )
        }

        private fun drawFrame() {
            if (!visible) return

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

            handler.removeCallbacks(drawRunnable)
            if (visible) {
                handler.postDelayed(drawRunnable, 600000)
            }
        }

        private fun drawWallpaper(canvas: Canvas) {
//            checkAndChangeContent()
            drawBackgroundImage(canvas)
//            drawBibleVerse(canvas)
//            drawInfoText(canvas)

//            if (isVerseAreaTouched) {
//                drawTouchFeedback(canvas)
//            }
        }


        //        /**
//         * 随机选择经文
//         */
//        private fun selectRandomVerse() {
//            if (bibleVerses.isNotEmpty()) {
//                currentVerse = bibleVerses[Random.nextInt(bibleVerses.size)]
//            } else {
//                currentVerse = BibleVerse.getDefaultVerse()
//            }
//        }

//        private fun List<ImageDaily>?.findByDate(date: String, today: String): ImageDaily? {
//            return this?.firstOrNull { "${it.date}-${today}" == date } ?: this?.lastOrNull()
//        }

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

                var isUnderline = wallpaperConfig?.isReadScriptureEnabled ?: false
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
                        verseRect = rect
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

                CardGenerator.generateBibleCard(
                    applicationContext,
                    resId,
                    request,
                    false,
                    isClikable,
                    { bitmap: Bitmap? ->
                        Log.e(
                            TAG,
                            "generateBibleCard callback ${Thread.currentThread().name} ,${bitmap != null && !bitmap.isRecycled},$width,$height"
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

//            val disposable = PermissionRequestHandler
//                .requestWriteExternalStorage(this@SettingWallpaperActivity)
//                .andThen<Bitmap?>( // After permission is granted, execute the following operations
//                    Observable.create<Bitmap?>(ObservableOnSubscribe { emitter: ObservableEmitter<Bitmap?>? ->
//                        getInstance()
//                            .generateBibleCard(
//                                applicationContext,
//                                R.layout.item_image_greeting,
//                                request,
//                                false,
//                                { result: Bitmap? ->
//                                    emitter!!.onNext(result!!) // 发送成功结果
//                                    emitter.onComplete() // 完成
//                                    Unit
//                                }, { err: Throwable? ->
//                                    emitter!!.onError(err!!)
//                                    Unit
//                                })
//                    })
//                        .subscribeOn(Schedulers.io())
//                )
//                .observeOn(AndroidSchedulers.mainThread())
//                .subscribe(
//                    Consumer { bitmap: Bitmap? ->
//                        if (bitmap != null && !bitmap.isRecycled) {
//                            // 绘制缓存的图片
//                            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true)
//                            canvas.drawBitmap(scaledBitmap, 0f, 0f, null)
//                            scaledBitmap.recycle()
//                        }
//                    },
//                    Consumer { e: Throwable? ->
//                        if (e != null) {
//                            Log.e(
//                                TAG,
//                                "drawBackgroundImage getCachedImage ${currentUrl} ${bitmap == null},${bitmap?.isRecycled}"
//                            )
//                        }
//                    }
//                )
//            dm.add(disposable)


//            val currentUrl = currentImageUrls.getOrNull(currentImageIndex) ?: return
//            val bitmap = getCachedImage(currentUrl)
//
//            if (bitmap != null && !bitmap.isRecycled) {
//                // 绘制缓存的图片
//                val scaledBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true)
//                canvas.drawBitmap(scaledBitmap, 0f, 0f, null)
//                scaledBitmap.recycle()
//            } else {
//                Log.e(
//                    "biblewallGlide",
//                    "drawBackgroundImage getCachedImage ${currentUrl} ${bitmap == null},${bitmap?.isRecycled}"
//                )
//                // 绘制默认背景
//                drawDefaultBackground(canvas)
//
//                // 异步加载图片
//                if (!loadingUrls.contains(currentUrl)) {
//                    loadImageWithGlide(currentUrl, false)
//                }
//            }
        }

//        /**
//         * 绘制默认背景
//         */
//        private fun drawDefaultBackground(canvas: Canvas) {
////            val gradient = LinearGradient(
////                0f, 0f, width.toFloat(), height.toFloat(),
////                Color.parseColor("#2C3E50"), Color.parseColor("#3498DB"),
////                Shader.TileMode.CLAMP
////            )
////            val paint = Paint().apply {
////                shader = gradient
////            }
////            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
////
////            // 显示加载信息
////            textPaint.color = Color.WHITE
////            textPaint.textSize = 36f
////            canvas.drawText("加载精美图片中...", width / 2f, height / 2f, textPaint)
//            try {
//
//                val bitmap = BitmapFactory.decodeResource(
//                    resources,
//                    R.mipmap.bg_default
//                )
//
//                // 缩放图片以适应屏幕
//                val scaledBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true)
//
//                // 绘制图片
//                canvas.drawBitmap(scaledBitmap, 0f, 0f, null)
//
//                // 添加半透明遮罩，让文字更清晰
//                canvas.drawColor(Color.argb(50, 0, 0, 0))
//
//                // 回收bitmap
//                scaledBitmap.recycle()
//                bitmap.recycle()
//            } catch (e: Exception) {
//                // 如果图片加载失败，绘制纯色背景
//                canvas.drawColor(getBackgroundColorByIndex(currentImageIndex))
//            }
//        }

        private fun getBackgroundColorByIndex(index: Int): Int {
            return when (index % 5) {
                0 -> Color.parseColor("#FF6B6B")
                1 -> Color.parseColor("#4ECDC4")
                2 -> Color.parseColor("#45B7D1")
                3 -> Color.parseColor("#96CEB4")
                4 -> Color.parseColor("#FECA57")
                else -> Color.BLUE
            }
        }

//        /**
//         * 绘制圣经经文
//         */
//        private fun drawBibleVerse(canvas: Canvas) {
//            val verse = currentVerse ?: return
//            val rect = verseRect ?: return
//
//            // 绘制背景
//            canvas.drawRoundRect(rect, 20f, 20f, backgroundPaint)
//            canvas.drawRoundRect(rect, 20f, 20f, borderPaint)
//
//            // 绘制经文
//            textPaint.color = Color.WHITE
//            textPaint.textSize = 36f
//            textPaint.textAlign = Paint.Align.CENTER
//
//            drawMultilineText(canvas, verse.getDisplayText(), rect)
//
////            // 绘制引用
////            textPaint.textSize = 24f
////            textPaint.color = Color.LTGRAY
////            canvas.drawText(
////                "${verse.book} ${verse.chapter}:${verse.verseStart}-${verse.verseEnd} (${verse.translation})",
////                rect.centerX(),
////                rect.bottom - 480f,
////                textPaint
////            )
//        }

//        /**
//         * 绘制信息文本
//         */
//        private fun drawInfoText(canvas: Canvas) {
//            val currentTime = System.currentTimeMillis()
//            val nextImageTime = (imageChangeInterval - (currentTime - lastImageChangeTime)) / 1000
//            val nextSourceTime =
//                (sourceChangeInterval - (currentTime - lastSourceChangeTime)) / 1000
//
//            val infoText = "图片源 ${currentImageSourceIndex + 1}/${imageSources.size} | " +
//                    "下张图片: ${nextImageTime}s | " +
//                    "切换主题: ${nextSourceTime / 60}m"
//
//            infoPaint.color = Color.argb(150, 255, 255, 255)
//            canvas.drawText(infoText, width - 20f, 40f, infoPaint.apply {
//                textAlign = Paint.Align.RIGHT
//            })
//        }
//
//        /**
//         * 绘制多行文本
//         */
//        private fun drawMultilineText(canvas: Canvas, text: String, rect: RectF) {
//            val maxWidth = rect.width() - 40f
//            val lines = breakTextIntoLines(text, maxWidth)
//            val lineHeight = 50f
//            val startY = rect.top + 60f
//
//            for ((index, line) in lines.withIndex()) {
//                if (index >= 3) break
//                canvas.drawText(line, rect.centerX(), startY + index * lineHeight, textPaint)
//            }
//        }

//        /**
//         * 文本自动换行
//         */
//        private fun breakTextIntoLines(text: String, maxWidth: Float): List<String> {
//            val lines = mutableListOf<String>()
//            val words = text.split(" ")
//            var currentLine = StringBuilder()
//
//            for (word in words) {
//                val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
//                val testWidth = textPaint.measureText(testLine)
//
//                if (testWidth <= maxWidth) {
//                    currentLine.append(if (currentLine.isEmpty()) word else " $word")
//                } else {
//                    if (currentLine.isNotEmpty()) {
//                        lines.add(currentLine.toString())
//                    }
//                    currentLine = StringBuilder(word)
//                }
//            }
//
//            if (currentLine.isNotEmpty()) {
//                lines.add(currentLine.toString())
//            }
//
//            return lines
//        }

//        /**
//         * 绘制触摸反馈
//         */
//        private fun drawTouchFeedback(canvas: Canvas) {
//            val rect = verseRect ?: return
//            val highlightPaint = Paint().apply {
//                color = Color.argb(80, 255, 255, 255)
//                style = Paint.Style.FILL
//                isAntiAlias = true
//            }
//            canvas.drawRoundRect(rect, 20f, 20f, highlightPaint)
//        }

//        /**
//         * 获取缓存的图片
//         */
//        private fun getCachedImage(url: String): Bitmap? {
//            return imageCache[url]
//        }
//
//        /**
//         * 检查图片是否已缓存
//         */
//        private fun isImageCached(url: String): Boolean {
//            return imageCache.containsKey(url)
//        }

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