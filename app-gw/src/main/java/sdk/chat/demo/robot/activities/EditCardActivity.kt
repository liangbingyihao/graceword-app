package sdk.chat.demo.robot.activities

import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.Spannable
import android.text.SpannableString
import android.text.TextWatcher
import android.text.style.UnderlineSpan
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.gson.Gson
import com.gyf.immersionbar.ImmersionBar
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.ObservableOnSubscribe
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import sdk.chat.core.utils.PermissionRequestHandler
import sdk.chat.demo.pre.R
import sdk.chat.demo.robot.adpter.ImagePagerAdapter
import sdk.chat.demo.robot.api.model.BlessData
import sdk.chat.demo.robot.api.model.ImageDaily
import sdk.chat.demo.robot.extensions.ImageSaveUtils
import sdk.chat.demo.robot.handlers.CardApiService
import sdk.chat.demo.robot.handlers.CardApiService.shareCardWithBitmap
import sdk.chat.demo.robot.handlers.CardGenerator
import sdk.chat.demo.robot.utils.AdvancedChineseEnglishFilter
import sdk.chat.demo.robot.utils.FontManager
import sdk.chat.demo.robot.utils.SocialShareUtils
import sdk.chat.demo.robot.utils.ToastHelper

class EditCardActivity : BaseActivity(), View.OnClickListener {
    private val TAG: String = "EditCardActivity"
    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var adapter: ImagePagerAdapter
    private lateinit var inputContainer: View
    private lateinit var messageInput: TextView
    private lateinit var messageInput1: EditText
    private lateinit var editHint: TextView
    private var lastHeight = 0

    //    private var greetings: List<String>? = null
    private var lastIndex = 0
    private var blessData: BlessData? = null
    private val buttonList = mutableListOf<MaterialButton>()
//    private lateinit var root: KeyboardAwareFrameLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        ImmersionBar.with(this).init()
        setContentView(R.layout.activity_edit_card)
//        root = findViewById<KeyboardAwareFrameLayout>(R.id.main)
//        changeGreetings = findViewById<View>(R.id.change_greetings)


        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ImmersionBar.with(this)
                .titleBar(findViewById<View>(R.id.title_bar))
                .init()
        } else {
            ImmersionBar.with(this).init()
        }
        fontManager = FontManager.getInstance(applicationContext)
        messageInput = findViewById<TextView>(R.id.messageInput)
        messageInput1 = findViewById<EditText>(R.id.messageInput1)


        findViewById<View>(R.id.back).setOnClickListener(this)
        findViewById<View>(R.id.btn_download).setOnClickListener(this)
        findViewById<View>(R.id.wallpaper).setOnClickListener(this)
        findViewById<View>(R.id.switch_greeting).setOnClickListener(this)
        findViewById<View>(R.id.confirm).setOnClickListener(this)
        findViewById<View>(R.id.btn_share_image).setOnClickListener(this)

        inputContainer = findViewById<View>(R.id.edGreetingContainer)


        editHint = findViewById<TextView>(R.id.editHint)
        editHint.setOnClickListener(this)
        val text = getString(R.string.customize_blessings)
        val spannable = SpannableString(text)
        spannable.setSpan(
            UnderlineSpan(),
            0, // 开始位置
            text.length, // 结束位置
            Spannable.SPAN_INCLUSIVE_INCLUSIVE
        )
        editHint.text = spannable
        //3､给Activity的xml布局设置View树监听，当布局有变化，如键盘弹出或收起时，都会回调此监听
        //4､软键盘弹起会使GlobalLayout发生变化
        inputContainer.viewTreeObserver.addOnGlobalLayoutListener(OnGlobalLayoutListener {
//            if (isfirst) {
//                contentHeight = mChildOfContent.getHeight() //兼容华为等机型
//                //                if (BuildConfig.CUT_DEBUG)
//                Log.d(
//                    "SoftHideKeyBoardUtil",
//                    "SoftHideKeyBoardUtil: contentHeight = " + contentHeight
//                )
//                isfirst = false
//            }
            //5､当前布局发生变化时，对Activity的xml布局进行重绘
            var b = computeUsableHeight()
            if (lastHeight != b) {
                lastHeight = b
                if (lastHeight > 200) {
                    (inputContainer.layoutParams as FrameLayout.LayoutParams).bottomMargin =
                        lastHeight - 12
                    inputContainer.visibility = View.VISIBLE
                    inputContainer.requestLayout()
                    editHint.visibility = View.GONE
                } else {
                    inputContainer.visibility = View.GONE
                    editHint.visibility = View.VISIBLE
                }
                Log.e(TAG, "computeUsableHeight:${b}")
            }
        })

        viewPager = findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.tabLayout)
        setAdapter()

        loadData()
        setupTextWatcher()

    }


    private fun setAdapter() {
        // 绑定 TabLayout 指示器
        adapter = ImagePagerAdapter(lifecycle)
        viewPager.adapter = adapter
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = "${position + 1}/${adapter.itemCount}"
        }.attach()

//        viewPager.setPageTransformer { page, position ->
//            page.translationX = -position * page.width // 关键：取负值实现反向
//        }
        // 预加载相邻页面（优化性能）
        viewPager.offscreenPageLimit = 1

// 3. 监听页面滑动，预加载下一页图片
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                if (position < adapter.itemCount - 1 && position > 0) {
                    lifecycleScope.launch {
                        Glide.with(this@EditCardActivity)
                            .downloadOnly()
                            .load(adapter.getUrlAt(position - 2)?.backgroundUrl) // 预加载下一页
                            .preload()
                    }
                }
            }
        })
    }

    private fun setupTextWatcher() {
        messageInput1.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // 文本变化前调用
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // 文本变化时调用

                // 实时更新TextView
                messageInput.text = s.toString()
            }

            override fun afterTextChanged(s: Editable?) {
                // 文本变化后调用
            }
        })

        var maxWordCount = 25
        val filter = AdvancedChineseEnglishFilter(maxWordCount) { totalCount ->
            if (totalCount >= maxWordCount) {
                ToastHelper.show(this@EditCardActivity, "total:$totalCount")
            }
        }

        messageInput1.filters = arrayOf(filter)
    }

    private fun loadData() {
        dm.add(
            CardApiService.getBlessData().subscribe(
                { data ->
                    if (data != null) {
                        blessData = data
                        downloadFont(messageInput, data.font)

//                        val items: List<ImageDaily> = data.daily.map { dailyItem ->
//                            ImageDaily(
//                                dailyItem.background,
//                                dailyItem.date,
//                                dailyItem.reference,
//                                dailyItem.verse
//                            )
//                        }

                        adapter.replaceData(data.daily)
                        viewPager.setCurrentItem(adapter.itemCount - 1, false)

                        switchGreetings()
                    }
                },
                Consumer { e: Throwable? ->
                    ToastHelper.show(this@EditCardActivity, e.toString())
                })
        )

    }

    private fun computeUsableHeight(): Int {
        val r = Rect()
        window.decorView.rootView.getWindowVisibleDisplayFrame(r)
        Log.e(TAG, "root.height:${window.decorView.rootView.height}")
//        return (r.bottom - r.top)
        return window.decorView.rootView.height - r.bottom
    }

    override fun getLayout(): Int {
        return 0
    }

    override fun onClick(v: View?) {
        var vid = v?.id
        when (vid) {

            R.id.back -> {
//                LogUploader.reportEvent(
//                    "mod_daily", listOf<KeyValuePair?>(
//                        KeyValuePair("daily_action", "50"),
//                        KeyValuePair("daily_entrance", from)
//                    )
//                )
                finish()
            }

            R.id.editHint -> {
                inputContainer.visibility = View.VISIBLE
                messageInput1.requestFocus()
                messageInput1.postDelayed({
                    Log.e(TAG, "editHint:showKeyboard()")
                    showKeyboard()
                }, 50)
            }

            R.id.switch_greeting -> {
                switchGreetings()
            }

            R.id.confirm -> {
                hideKeyboard()
            }

            R.id.wallpaper, R.id.btn_download, R.id.btn_share_image -> {

                var imageDaily = adapter.getUrlAt(viewPager.currentItem)
                if (imageDaily == null) {
                    return
                }
                imageDaily.greeting = messageInput.text.toString()
                imageDaily.fontUrl = blessData?.font

                if (vid == R.id.wallpaper) {
                    SettingWallpaperActivity.start(
                        this@EditCardActivity,
                        CardApiService.FROM_CARD,
                        (Gson()).toJson(imageDaily, ImageDaily::class.java)
                    )
                } else if (vid == R.id.btn_share_image) {
                    share(imageDaily)
                } else {
                    download(imageDaily)
                }
            }
        }

    }

    private fun switchGreetings() {
        val currentGreetings = blessData?.choices
        if (!currentGreetings.isNullOrEmpty()) {
            lastIndex = (lastIndex + 1) % currentGreetings.size
            var g = currentGreetings[lastIndex]
            messageInput1.setText(currentGreetings[lastIndex])
//            messageInput.setText(currentGreetings[lastIndex])
            messageInput1.setSelection(g.length)
        }
    }

    private lateinit var fontManager: FontManager
    private var downloadJob: Job? = null
    private fun downloadFont(
        textView: TextView,
        fontUrl: String
    ) {
        downloadJob?.cancel()
        showProgressDialog("开始加载字体库")

        downloadJob = fontManager.loadFont(fontUrl, object : FontManager.FontCallback {
            override fun onSuccess(typeface: Typeface) {
                runOnUiThread {
                    dismissProgressDialog()
//                progressText.text = "字体加载成功！"
//
                    // 应用到TextView
                    textView.typeface = typeface
//                    textView.text = ""
//
//                // 3秒后隐藏进度文本
//                Handler(Looper.getMainLooper()).postDelayed({
//                    progressText.visibility = View.GONE
//                }, 3000)
                }
            }

            override fun onError(exception: Exception) {
                showProgressDialog("加载字体库失败")
                runOnUiThread {
                    Handler(Looper.getMainLooper()).postDelayed({
                        dismissProgressDialog()
                    }, 3000)
//                progressBar.visibility = View.GONE
//                progressText.text = "字体加载失败: ${exception.message}"
//                progressText.setTextColor(Color.RED)
                }
            }

            override fun onProgress(progress: Int) {
                runOnUiThread {
                    showProgressDialog("加载字体库中:${progress}")
                }
            }
        })
    }


    private fun download(imageDaily: ImageDaily) {
        val disposable = PermissionRequestHandler
            .requestWriteExternalStorage(this@EditCardActivity)
            .andThen<Bitmap?>( // After permission is granted, execute the following operations
                Observable.create<Bitmap?>(ObservableOnSubscribe { emitter: ObservableEmitter<Bitmap?>? ->
                    CardGenerator.generateBibleCard(
                        this@EditCardActivity,
                        R.layout.item_image_greeting,
                        imageDaily,
                        true,
                        false,
                        { result: Bitmap? ->
                            emitter!!.onNext(result!!) // 发送成功结果
                            emitter.onComplete() // 完成
                            Unit
                        }, { err: Throwable? ->
                            emitter!!.onError(err!!)
                            Unit
                        })
                })
                    .subscribeOn(Schedulers.io())
            )
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                Consumer { bitmap: Bitmap? ->
                    if (bitmap != null) {
                        val bitmapURL = ImageSaveUtils.saveBitmapToGallery(
                            this@EditCardActivity,  // context
                            bitmap,
                            "img_" + System.currentTimeMillis(),
                            Bitmap.CompressFormat.JPEG
                        )
                        if (bitmapURL != null) {
                            ToastHelper.show(
                                this@EditCardActivity,
                                getString(R.string.image_saved)
                            )

                        } else {
                            ToastHelper.show(
                                this@EditCardActivity,
                                getString(R.string.image_save_failed)
                            )
                        }
                    } else {
                        ToastHelper.show(
                            this@EditCardActivity,
                            getString(R.string.image_save_failed)
                        )
                    }
                    bitmap?.recycle()
                }
            )
        dm.add(disposable)
    }

    private fun share(imageDaily: ImageDaily) {
        val disposable =
            Observable.create<Bitmap> { emitter ->
                CardGenerator.generateBibleCard(
                    this@EditCardActivity,
                    R.layout.item_image_greeting,
                    imageDaily,
                    false,
                    false,
                    { result: Bitmap? ->
                        if (result != null && !result.isRecycled) {
                            emitter.onNext(result) // 发送成功结果
                            emitter.onComplete() // 完成
                        } else {
                            emitter.onError(IllegalStateException("generate card failed"))
                        }
                        Unit
                    }, { err: Throwable? ->
                        emitter.onError(err ?: IllegalStateException("generate card failed"))
                        Unit
                    })
            }.flatMap { bitmap: Bitmap ->
                shareCardWithBitmap(bitmap, imageDaily.greeting).toObservable().doFinally {
                    // 关键：在 finally 中回收 Bitmap
                    if (!bitmap.isRecycled) {
                        bitmap.recycle()
                        Log.d(TAG, "Bitmap 已回收")
                    }
                }
            }
                .subscribe(
                    { result ->
                        Log.e(TAG, result.url)

                        SocialShareUtils.showCustomShareDialog(
                            this,
                            SocialShareUtils.targetApps,
                            result.shareText,
                            null,
                            result.url
                        )
                        //                        if (id == R.id.btn_share_image) {
//                            val shareIntent = Intent(Intent.ACTION_SEND)
//                            shareIntent.setType("image/*") // 或具体类型如 "image/jpeg"
//                            shareIntent.putExtra(Intent.EXTRA_STREAM, bitmapURL)
//                            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) // 临时权限
//
//                            weakContext.get()
//                                .startActivity(Intent.createChooser(shareIntent, "分享图片"))
//                        } else {
//                            WallpaperUtils(this@EditCardActivity).setHomeScreenWallpaper(bitmap)
//                        }
                    },
                    { error -> ToastHelper.show(this@EditCardActivity, error.toString()) }
                )
        dm.add(disposable)
    }
}