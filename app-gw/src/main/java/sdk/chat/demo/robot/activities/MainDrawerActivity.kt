package sdk.chat.demo.robot.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.core.content.edit
import androidx.core.view.GravityCompat
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers
import org.tinylog.Logger
import sdk.chat.core.events.EventType
import sdk.chat.core.events.NetworkEvent
import sdk.chat.core.session.ChatSDK
import sdk.chat.demo.MainApp
import sdk.chat.demo.pre.R
import sdk.chat.demo.robot.activities.ArticleListActivity
import sdk.chat.demo.robot.adpter.SessionAdapter
import sdk.chat.demo.robot.api.ImageApi
import sdk.chat.demo.robot.api.model.KeyValuePair
import sdk.chat.demo.robot.audio.AsrHelper
import sdk.chat.demo.robot.audio.TTSHelper
import sdk.chat.demo.robot.extensions.DateLocalizationUtil
import sdk.chat.demo.robot.extensions.LanguageUtils.updateContext
import sdk.chat.demo.robot.extensions.dpToPx
import sdk.chat.demo.robot.extensions.showMaterialConfirmationDialog
import sdk.chat.demo.robot.fragments.GWChatFragment
import sdk.chat.demo.robot.handlers.AuthService
import sdk.chat.demo.robot.handlers.BillingManager
import sdk.chat.demo.robot.handlers.CardApiService
import sdk.chat.demo.robot.handlers.CardApiService.LauncherStep
import sdk.chat.demo.robot.handlers.DailyTaskHandler
import sdk.chat.demo.robot.handlers.GWThreadHandler
import sdk.chat.demo.robot.handlers.LogUploader
import sdk.chat.demo.robot.holder.WelcomeHolder
import sdk.chat.demo.robot.ui.CustomDivider
import sdk.chat.demo.robot.ui.HighlightOverlayView
import sdk.chat.demo.robot.ui.hasShownGuideOverlay
import sdk.chat.demo.robot.ui.listener.GWClickListener
import sdk.chat.demo.robot.utils.ToastHelper
import sdk.chat.demo.robot.utils.WallpaperGuideUtil


class MainDrawerActivity : BaseActivity(), View.OnClickListener, GWClickListener.TTSSpeaker {
    open lateinit var drawerLayout: DrawerLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var vHomeMenu: View
    private lateinit var vTaskMenu: View
    private lateinit var vBillingMenu: View
    private lateinit var vRedDotTask: View
    private lateinit var vDgwMenu: TextView
    private lateinit var vUserMenu: TextView
    private lateinit var lottieAnimationView: LottieAnimationView
//    private lateinit var vErrorHint: TextView

    //    private lateinit var sessions: List<Thread>
    private var highlightOverlay: HighlightOverlayView? = null
    private lateinit var sessionAdapter: SessionAdapter
    private val threadHandler: GWThreadHandler = ChatSDK.thread() as GWThreadHandler
    private val chatTag = "tag_chat";
    private var toReloadSessions = false
    private var hasShownWelcome = false
    private var forceBilling = true
    private var cntShowBilling = 0
    private var isVipDisplayCrown = false
//    private lateinit var ttsCheckLauncher: ActivityResultLauncher<Intent>


    companion object {
        private const val ARG_REFERENCE = "reference"
        private var isTaskCleared = false
        fun startBibleActivity(
            context: Context,
            reference: String = "",
        ) {
            Logger.info { "startBibleActivity ${reference}." }
            val intent = Intent(context, MainDrawerActivity::class.java).apply {
                putExtra(ARG_REFERENCE, reference)
            }
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
//            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // 加载菜单资源
        menuInflater.inflate(R.menu.nav_menu, menu)
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(layout)
//        ImmersionBar.with(this).init()

        Log.e("MainApp", "mainactivity,${savedInstanceState == null}")
        try {
            require(ChatSDK.currentUser() != null)
        } catch (e: Exception) {
            Log.e("MainApp", "mainactivity,e:${e}")
            Logger.error(e, "currentUser error")
            val intent = Intent(this, SplashScreenActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
            finish()
            return
        }
        Log.e("MainApp", "mainactivity oncreated")

        var reference = intent.getStringExtra(ARG_REFERENCE)
        if (reference != null && savedInstanceState == null) {
            Log.e("MainApp", "isVerseAreaTouched..and start ${reference}.")
            BibleActivity.start(this@MainDrawerActivity, reference = reference, fullscreen = false)
        } else {
            reference = null
        }

        var isInitialized = (application as MainApp).isInitialized
        Logger.error { "MainDrawerActivity.onCreate,isInitialized:${isInitialized}" }
        drawerLayout = findViewById(R.id.root_container)
        highlightOverlay = findViewById(R.id.overlay)
        findViewById<View>(R.id.menu_favorites).setOnClickListener(this)
        vDgwMenu = findViewById<TextView>(R.id.menu_gw_daily)
        vDgwMenu.setOnClickListener(this)
        findViewById<View>(R.id.menu_search).setOnClickListener(this)
        findViewById<View>(R.id.menu_setting).setOnClickListener(this)
        vUserMenu = findViewById<TextView>(R.id.menu_user)
        vUserMenu.setOnClickListener(this)
        vHomeMenu = findViewById<View>(R.id.menu_home)
        vHomeMenu.setOnClickListener(this)
        vTaskMenu = findViewById<View>(R.id.menu_task)
        vTaskMenu.setOnClickListener(this)
        vRedDotTask = findViewById<View>(R.id.red_dot2)
        vBillingMenu = findViewById<View>(R.id.menu_vip)
        vBillingMenu.setOnClickListener(this)


//        KeyboardDrawerHelper.setup(drawerLayout)
        drawerLayout.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
                if (slideOffset > 0.3) {
                    hideKeyboard()
                }
            }

            override fun onDrawerOpened(drawerView: View) {
                if (toReloadSessions) {
                    threadHandler.triggerNetworkSync()
                }
                recyclerView.scrollToPosition(0);

            }

            override fun onDrawerClosed(drawerView: View) {
            }

            override fun onDrawerStateChanged(newState: Int) {
            }
        })
//        initViews()

        recyclerView = findViewById<RecyclerView>(R.id.nav_recycler)
        recyclerView.layoutManager = LinearLayoutManager(this)
        listSessions()


        dm.add(
            ChatSDK.events().sourceOnMain()
                .filter(NetworkEvent.filterType(EventType.ThreadsUpdated)).subscribe(Consumer {
                    listSessions()
                })
        )
        dm.add(
            ChatSDK.events().sourceOnSingle()
                .filter(NetworkEvent.filterType(EventType.BillChange))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(Consumer { networkEvent: NetworkEvent? ->
                    onBillChanged()
                })
        )

        dm.add(
            ChatSDK.events().sourceOnMain()
                .filter(NetworkEvent.filterType(EventType.HideDrawer)).subscribe(Consumer {
                    drawerLayout.closeDrawers()
                })
        )

        if (!hasShownGuideOverlay(this@MainDrawerActivity)) {
            getSharedPreferences("app_prefs", MODE_PRIVATE)
                .edit() {
                    putBoolean("has_shown_guide", true)
                }
            dm.add(
                ChatSDK.events().prioritySourceOnSingle()
                    .filter(
                        NetworkEvent.filterType(
                            EventType.MessageUpdated
                        )
                    )
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(Consumer { networkEvent: NetworkEvent? ->
                        highlightOverlay?.handleFirst(
                            this@MainDrawerActivity,
                            networkEvent?.message
                        )
                    }, this)
            )
            dm.add(
                ChatSDK.events().sourceOnSingle()
                    .filter(NetworkEvent.filterType(EventType.MessageAdded))
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(Consumer { networkEvent: NetworkEvent? ->
                        highlightOverlay?.finishGuideBeginner()
                        if (WelcomeHolder.isWelcomeMsg(networkEvent!!.getMessage())) {
//                            vHomeMenu.visibility = View.GONE
//                            vTaskMenu.visibility = View.GONE
                            findViewById<View>(R.id.red_dot).visibility = View.GONE
                            findViewById<View>(R.id.red_dot3).visibility = View.GONE
                        } else {
                            if (false) {
                                //FIXME
                                vTaskMenu.visibility = View.VISIBLE
                            }
                            vHomeMenu.visibility = View.VISIBLE
                            if (!hasShownWelcome) {
                                getSharedPreferences("app_prefs", MODE_PRIVATE)
                                    .edit() {
                                        putBoolean("has_shown_welcome", true)
                                    }
                                setRedDotView()
                                vHomeMenu.postDelayed({
                                    checkPreLaunchBill(force = forceBilling)
                                }, 200L)

                            }
                        }
                    })
            )
        } else {
            CardApiService.setLauncherStep(LauncherStep.READY)
        }


        val configs = ImageApi.getGwConfigs()
        if (configs != null) {
            isVipDisplayCrown = configs.isVipDisplayCrown == true
        }

        if (reference == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, GWChatFragment(), chatTag).commit()
        } else {
            recyclerView.postDelayed(
                {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, GWChatFragment(), chatTag).commit()
                }, 1000
            )
        }


        TTSHelper.initTTS(this@MainDrawerActivity)
        AsrHelper.initAsrEngine()

        hasShownWelcome = getSharedPreferences("app_prefs", MODE_PRIVATE)
            .getBoolean("has_shown_welcome", false)
        if ((reference == null || reference.isEmpty()) && savedInstanceState == null) {
            checkPreLaunchActivity()
            LogUploader.chatEntrance("app_launch")
        }
        checkWallPaper()

        highlightOverlay?.handleStatic(
            this@MainDrawerActivity,
            null
        )
        loadCampaignData()

    }

    private fun loadCampaignData() {
        lottieAnimationView = findViewById<LottieAnimationView>(R.id.ops)
//        CampaignInfoActivity.start(this@MainDrawerActivity,"mini")
        dm.add(
            ChatSDK.events().sourceOnMain()
                .filter(NetworkEvent.filterType(EventType.ShowOAMain)).subscribe(Consumer { event ->
                    CampaignInfoActivity.start(this@MainDrawerActivity, event.text)
                })
        )

        dm.add(
            CardApiService.getCampaignData().subscribe(
                { data ->
                    if (data != null) {
                        if (data.entryConfig != null && data.entryConfig.enable) {
                            setLottieAnimationView(data.entryConfig.iconUrl)
                        }
                    }
                },
                Consumer { e: Throwable? ->
//                    ToastHelper.show(this@EditCardActivity, e.toString())
                })
        )
    }

    private fun setLottieAnimationView(url: String) {
        lottieAnimationView.visibility = View.VISIBLE
        lottieAnimationView.setAnimationFromUrl(url)
        lottieAnimationView.repeatCount = 5
        lottieAnimationView.setOnClickListener(this)
    }

    private fun checkPreLaunchBill(force: Boolean = false) {
        if (force) {
            forceBilling = false
        }
        var isVip = BillingManager.getInstance().hasSubscriptions()
        if (!isVip) {
            cntShowBilling = getSharedPreferences("app_prefs", MODE_PRIVATE)
                .getInt("cnt_show_billing", -1)
            var from = "app_launch_nonfirst"
            if (cntShowBilling == -1) {
                from = "app_launch_first"
                cntShowBilling = 0
            }

            Log.e("BillingManager", "checkPreLaunchBill:$cntShowBilling")
            if (cntShowBilling % 3 == 1 || force) {
                Log.e("BillingManager", "checkPreLaunchBill.1,isvip:${isVip}")
                if (BillingActivity.start(this@MainDrawerActivity, from, true)) {
                    Log.e("BillingManager", "checkPreLaunchBill.2")
                    getSharedPreferences("app_prefs", MODE_PRIVATE)
                        .edit() {
                            putInt("cnt_show_billing", (cntShowBilling + 1) % 3)
                        }
                }
            } else {
                getSharedPreferences("app_prefs", MODE_PRIVATE)
                    .edit() {
                        putInt("cnt_show_billing", (cntShowBilling + 1) % 3)
                    }
            }
        }
    }

    private fun checkPreLaunchActivity() {
        vHomeMenu.postDelayed({
            checkPreLaunchBill()
        }, 2000)
        if (hasShownWelcome) {
            val today: String = DateLocalizationUtil.formatDayAgo(0)
            var showDate =
                getSharedPreferences("app_prefs", MODE_PRIVATE).getString("shown_gw_date", "")
            if (today != showDate) {
                ImageViewerActivity.start(this@MainDrawerActivity, "", "auto_launch");
            }

            getSharedPreferences("app_prefs", MODE_PRIVATE)
                .edit() {
                    putString("shown_gw_date", today)
                }
        }

    }

    private fun checkWallPaper() {
        if ((System.currentTimeMillis() - (application as MainApp).startTimeStamp) < 4000) {
            var config = CardApiService.getRawWallPaperConfig()
            if (config != null && config.isDynamic && config.cntSkip < 2) {
                showMaterialConfirmationDialog(
                    this@MainDrawerActivity,
                    getString(R.string.restore_wallpaper_message),
                    getString(R.string.one_click_setup),
                    getString(R.string.skip),
                    {
                        // 这里是positiveAction的逻辑
                        WallpaperGuideUtil(this@MainDrawerActivity).guideToDirectlySetLiveWallpaper()
                        Unit
                    },
                    {
                        // 这里是positiveAction的逻辑
                        config.cntSkip += 1
                        CardApiService.saveWallPaperConfig(config)
                        Unit
                    }
                )
            }
        } else {
            Log.e(
                "MainDrawerActivity1",
                "startime:${System.currentTimeMillis() - (application as MainApp).startTimeStamp}"
            )
        }
    }

    private fun onBillChanged() {
        if (BillingManager.getInstance().hasSubscriptions() && !isVipDisplayCrown) {
            vBillingMenu.visibility = View.GONE
        } else {
            vBillingMenu.visibility = View.VISIBLE
        }
    }


    private fun listSessions() {
        dm.add(
            threadHandler.listSessions()
                .subscribeOn(Schedulers.io()) // Specify database operations on IO thread
                .observeOn(AndroidSchedulers.mainThread()) // Results return to main thread
                .subscribe(
                    { data ->
                        if (data != null) {
//                            val sessionMenus: ArrayList<HistoryItem> = toMenuItems(data)
                            sessionAdapter = SessionAdapter(data, { changed, clickedItem ->
//                                toggleDrawer()
//                                if (changed) {
//                                    setCurrentSession(clickedItem)
                                LogUploader.reportEvent(
                                    "mod_timeline", mutableListOf(
                                        KeyValuePair("timeline_entrance", "sidebar"),
                                        KeyValuePair("timeline_action", "0"),
                                    )
                                )

                                ArticleListActivity.start(
                                    this@MainDrawerActivity,
                                    clickedItem.id
                                )
                            }, { clickedItem ->
//                                var item: ArticleSession? = sessionAdapter.getSelectItem()
//                                if (item != null && !dialogEditSingle.isShowing) {
//                                    dialogEditSingle.show()
//                                    dialogEditSingle.setEditDefault(item.title)
//                                }
                            })
                            recyclerView.adapter = sessionAdapter
                            if (recyclerView.itemDecorationCount == 0) {
                                recyclerView.addItemDecoration(
                                    CustomDivider(
                                        thickness = 1.dpToPx(this@MainDrawerActivity),  // 扩展函数转换 dp 到 px
                                        colorResId = R.color.gray_divider,
                                        insetStart = 12.dpToPx(this@MainDrawerActivity),
                                        insetEnd = 12.dpToPx(this@MainDrawerActivity)
                                    )
                                )
                            }
                        } else {
                            throw IllegalArgumentException("创建会话失败")
                        }
                    },
                    this
                )
        )

    }


    override fun speek(text: String, msgId: String) {
        TTSHelper.speek(text, msgId)
    }

    override fun getCurrentUtteranceId(): String? {
        return currentUtteranceId;
    }

    override fun stop() {
        TTSHelper.stop()
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                toggleDrawer()
                true
            }

            R.id.action_record -> {
                startActivity(
                    Intent(
                        this@MainDrawerActivity,
                        SpeechToTextActivity::class.java
                    )
                )
                true
            }

            R.id.action_prompt -> {
                startActivity(
                    Intent(
                        this@MainDrawerActivity,
                        SettingPromptActivity::class.java
                    )
                )
                true
            }

            R.id.action_share -> {
                BillingActivity.start(this@MainDrawerActivity, "main_page_right_corner")
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }


    fun toggleDrawer() {
        if (drawerLayout.isOpen) {
            drawerLayout.closeDrawers()
        } else {
            hideKeyboard()
            drawerLayout.openDrawer(GravityCompat.START)
            LogUploader.reportEvent(
                "mod_sidebar", listOf<KeyValuePair?>(
                    KeyValuePair("sidebar_action", "0")
                )
            )
        }
    }


    override fun onResume() {
        super.onResume()
        updateContext(this)
        if (threadHandler.isCustomPrompt) {
            toolbar?.title = "自定义提示语中"
        } else {
            toolbar?.title = getString(R.string.app_name)
        }
        setRedDotView()
        threadHandler.reloadTimeoutMsg()
        BillingManager.getInstance().checkSubscriptions()
        refreshMembership()

//        lottieAnimationView.clearAnimation()
        lottieAnimationView.playAnimation()

        var lastUser = AuthService.getLastLoginUser()
        if (lastUser != null && !lastUser.isGuest) {
            vUserMenu.text = getString(R.string.my_account)
//            if (!AuthService.isAuthenticated() && (System.currentTimeMillis() - (application as MainApp).startTimeStamp) < 4000) {
//                Handler(Looper.getMainLooper()).postDelayed({
//                    showMaterialConfirmationDialog(
//                        this@MainDrawerActivity,
//                        getString(R.string.login_hint_offline),
//                        getString(R.string.login),
//                        getString(R.string.skip),
//                        positiveAction = {
//                            startActivity(
//                                Intent(
//                                    this@MainDrawerActivity,
//                                    LoginActivity::class.java
//                                )
//                            )
//                        })
//                }, 1000)
//            }
        } else {
            vUserMenu.text = getString(R.string.login)

            var hasLoginHint =
                getSharedPreferences("app_prefs", MODE_PRIVATE).getBoolean(
                    "shown_login_hint",
                    false
                )

            var loginHintRes = 0
            if (!hasLoginHint && hasShownGuideOverlay(this@MainDrawerActivity)) {
                getSharedPreferences("app_prefs", MODE_PRIVATE)
                    .edit() {
                        putBoolean("shown_login_hint", true)
                    }
                loginHintRes = R.string.login_hint
            } else if (AuthService.hasLoginBefore() && (System.currentTimeMillis() - (application as MainApp).startTimeStamp) < 4000) {
                loginHintRes = R.string.login_hint_offline
            }
            if (loginHintRes > 0) {
                Handler(Looper.getMainLooper()).postDelayed({
                    showMaterialConfirmationDialog(
                        this@MainDrawerActivity,
                        getString(loginHintRes),
                        getString(R.string.login),
                        getString(R.string.skip),
                        positiveAction = {
                            startActivity(
                                Intent(
                                    this@MainDrawerActivity,
                                    LoginActivity::class.java
                                )
                            )
                        })
                }, 1000)
            }
        }
    }

    override fun getLayout(): Int {
        return R.layout.activity_main_coze_drawer;
    }

    override fun onClick(v: View?) {
        var vid = v?.id
        val excludedIds = setOf(R.id.menu_task, R.id.menu_vip, R.id.ops, R.id.menu_user)
        if (vid !in excludedIds) {
            toggleDrawer()
        }
        when (vid) {
            R.id.menu_home -> {
                true
            }

            R.id.menu_search -> {
                startActivity(
                    Intent(
                        this@MainDrawerActivity,
                        SearchActivity::class.java
                    )
                )

                LogUploader.reportEvent(
                    "mod_sidebar", listOf<KeyValuePair?>(
                        KeyValuePair("sidebar_action", "10")
                    )
                )
            }

            R.id.menu_favorites -> {
                startActivity(
                    Intent(
                        this@MainDrawerActivity,
                        FavoriteListActivity::class.java
                    )
                )
                LogUploader.reportEvent(
                    "mod_sidebar", listOf<KeyValuePair?>(
                        KeyValuePair("sidebar_action", "20")
                    )
                )
            }

            R.id.menu_gw_daily -> {
                hasShownWelcome = true
                ImageViewerActivity.start(this@MainDrawerActivity, "", "sidebar");
                LogUploader.reportEvent(
                    "mod_sidebar", listOf<KeyValuePair?>(
                        KeyValuePair("sidebar_action", "30")
                    )
                )
            }

            R.id.menu_setting -> {
                startActivity(
                    Intent(
                        this@MainDrawerActivity,
                        SettingsActivity::class.java
                    )
                )
                LogUploader.reportEvent(
                    "mod_sidebar", listOf<KeyValuePair?>(
                        KeyValuePair("sidebar_action", "40")
                    )
                )
            }

            R.id.menu_user -> {
                var lastUser = AuthService.getLastLoginUser()
                if (lastUser != null && !lastUser.isGuest) {
                    startActivity(
                        Intent(
                            this@MainDrawerActivity,
                            AccountActivity::class.java
                        )
                    )
                } else {
                    startActivity(
                        Intent(
                            this@MainDrawerActivity,
                            LoginActivity::class.java
                        )
                    )
                }
            }

            R.id.menu_vip -> {
                BillingActivity.start(this@MainDrawerActivity, "main_page_right_corner")
            }


            R.id.menu_task -> {
                startActivity(
                    Intent(
                        this@MainDrawerActivity,
                        TaskActivity::class.java
                    )
                )
                true
            }

            R.id.ops -> {
//                WebViewActivity.launchWithUrl(this@MainDrawerActivity,"https://api-test.kolacdn.xyz/public/testops.html","ops")
//                startActivity(
//                    Intent(
//                        this@MainDrawerActivity,
//                        EditCardActivity::class.java
//                    )
//                )
//                startActivity(Intent(Intent.ACTION_VIEW, "graceword://open/card".toUri()))
                CardApiService.handleJoinCampaign(this@MainDrawerActivity)
                true
            }

        }
    }

    override fun onDestroy() {
        super.onDestroy()
        TTSHelper.clear()
    }

    fun refreshMembership() {
        dm.add(BillingManager.getInstance().getMembership().subscribe { data ->
            onBillChanged()
        })
    }

    fun setRedDotView() {
        setGwdRedDotView()
        setTaskRedDotView()
    }

    fun setTaskRedDotView() {
        dm.add(
            DailyTaskHandler.getTaskProgress(false)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread()) // Results return to main thread
                .subscribe(
                    { data ->
                    },
                    Consumer { error: Throwable? ->
//                        vRedDotTask.visibility = View.GONE
                    }
                )
        )
    }

    fun setGwdRedDotView() {
        // 获取红点视图
        val redDot: View = findViewById<View>(R.id.red_dot)
        val redDot3: View = findViewById<View>(R.id.red_dot3)
        if (!hasShownWelcome) {
//            redDot.visibility = vHomeMenu.visibility
            redDot3.visibility = View.VISIBLE
//            vDgwMenu.post({
//                val drawables: Array<Drawable?> = vDgwMenu.getCompoundDrawables()
//                val leftDrawable: Drawable? = drawables[0]
//                if (leftDrawable != null) {
//
//                    // 创建布局参数
//                    val params: FrameLayout.LayoutParams =
//                        redDot3.layoutParams as FrameLayout.LayoutParams
//
//
//                    // 设置红点位置（菜单图标右上角）
//                    params.gravity = Gravity.START or Gravity.TOP
//                    params.leftMargin =
//                        vDgwMenu.left + vDgwMenu.paddingLeft + leftDrawable.intrinsicWidth - redDot3.width / 2
//                    params.topMargin = vDgwMenu.top + vDgwMenu.paddingTop - redDot3.height / 2
//                    redDot3.setLayoutParams(params)
//                }
//
//            })
//// 在视图布局完成后调整位置
//            vHomeMenu.post({
//                redDot.visibility = vHomeMenu.visibility
//                // 获取菜单图标的宽高
//                val menuWidth: Int = vHomeMenu.width
//
//
//                // 创建布局参数
//                val params: FrameLayout.LayoutParams =
//                    redDot.layoutParams as FrameLayout.LayoutParams
//
//
//                // 设置红点位置（菜单图标右上角）
//                params.gravity = Gravity.START or Gravity.TOP
//                params.leftMargin =
//                    vHomeMenu.left + menuWidth - vHomeMenu.paddingRight - redDot.width / 2
//                params.topMargin = vHomeMenu.top + vHomeMenu.paddingTop - redDot.height / 2
//                redDot.setLayoutParams(params)
//            })

            // 保存已经显示过引导页的状态
        } else {
            redDot.visibility = View.GONE
            redDot3.visibility = View.GONE
        }
    }
}