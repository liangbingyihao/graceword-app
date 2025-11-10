package sdk.chat.demo.robot.activities

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.edit
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers
import org.tinylog.Logger
import sdk.chat.core.events.EventType
import sdk.chat.core.events.NetworkEvent
import sdk.chat.core.session.ChatSDK
import sdk.chat.demo.MainApp
import sdk.chat.demo.pre.R
import sdk.chat.demo.robot.adpter.SessionAdapter
import sdk.chat.demo.robot.api.model.KeyValuePair
import sdk.chat.demo.robot.audio.AsrHelper
import sdk.chat.demo.robot.audio.TTSHelper
import sdk.chat.demo.robot.extensions.DateLocalizationUtil
import sdk.chat.demo.robot.extensions.LanguageUtils.updateContext
import sdk.chat.demo.robot.extensions.dpToPx
import sdk.chat.demo.robot.fragments.GWChatFragment
import sdk.chat.demo.robot.handlers.DailyTaskHandler
import sdk.chat.demo.robot.handlers.GWThreadHandler
import sdk.chat.demo.robot.handlers.LogUploader
import sdk.chat.demo.robot.holder.WelcomeHolder
import sdk.chat.demo.robot.ui.CustomDivider
import sdk.chat.demo.robot.ui.HighlightOverlayView
import sdk.chat.demo.robot.ui.hasShownGuideOverlay
import sdk.chat.demo.robot.ui.listener.GWClickListener
import androidx.core.view.isVisible


class MainDrawerActivity : BaseActivity(), View.OnClickListener, GWClickListener.TTSSpeaker {
    open lateinit var drawerLayout: DrawerLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var vHomeMenu: View
    private lateinit var vTaskMenu: View
    private lateinit var vRedDotTask: View
    private lateinit var vDgwMenu: TextView
//    private lateinit var vErrorHint: TextView

    //    private lateinit var sessions: List<Thread>
    private var highlightOverlay: HighlightOverlayView? = null
    private lateinit var sessionAdapter: SessionAdapter
    private val threadHandler: GWThreadHandler = ChatSDK.thread() as GWThreadHandler
    private val chatTag = "tag_chat";
    private var toReloadSessions = false
    private var hasShownWelcome = false
//    private lateinit var ttsCheckLauncher: ActivityResultLauncher<Intent>

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // 加载菜单资源
        menuInflater.inflate(R.menu.nav_menu, menu)
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(layout)
//        ImmersionBar.with(this).init()

        try {
            ChatSDK.currentUser()
        } catch (e: Exception) {
            Logger.error(e, "currentUser error")
            val intent = Intent(this, SplashScreenActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
            finish()
            return
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
        vHomeMenu = findViewById<View>(R.id.menu_home)
        vHomeMenu.setOnClickListener(this)
        vTaskMenu = findViewById<View>(R.id.menu_task)
        vTaskMenu.setOnClickListener(this)
        vRedDotTask = findViewById<View>(R.id.red_dot2)


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
                        if (WelcomeHolder.isWelcomeMsg(networkEvent!!.getMessage())) {
                            vHomeMenu.visibility = View.GONE
                            vTaskMenu.visibility = View.GONE
                            findViewById<View>(R.id.red_dot).visibility = View.GONE
                            findViewById<View>(R.id.red_dot3).visibility = View.GONE
                            highlightOverlay?.finishGuideBeginner()
                        } else {
                            vHomeMenu.visibility = View.VISIBLE
                            vTaskMenu.visibility = View.VISIBLE
                            if(!hasShownWelcome){
                                getSharedPreferences("app_prefs", MODE_PRIVATE)
                                    .edit() {
                                        putBoolean("has_shown_welcome", true)
                                    }
                                setRedDotView()
                            }
                        }
                    })
            )
//            dm.add(
//                threadHandler.welcomeMsg
//                    .delay(2, TimeUnit.SECONDS)
//                    .subscribeOn(RX.io())
//                    .observeOn(RX.main())
//                    .subscribe(
//                        {
//                        },
//                        this
//                    )
//            )
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, GWChatFragment(), chatTag).commit()


        TTSHelper.initTTS(this@MainDrawerActivity)
        AsrHelper.initAsrEngine()
        checkTaskDetail()

        highlightOverlay?.handleStatic(
            this@MainDrawerActivity,
            null
        )
    }

    private fun checkTaskDetail() {
        hasShownWelcome = getSharedPreferences("app_prefs", MODE_PRIVATE)
            .getBoolean("has_shown_welcome", false)
        if (hasShownWelcome) {
            val today: String = DateLocalizationUtil.formatDayAgo(0)
            var showDate =
                getSharedPreferences("app_prefs", MODE_PRIVATE).getString("shown_gw_date", "")
            if (today != showDate) {
                ImageViewerActivity.start(this@MainDrawerActivity);
            }

            getSharedPreferences("app_prefs", MODE_PRIVATE)
                .edit() {
                    putString("shown_gw_date", today)
                }
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
                startActivity(
                    Intent(
                        this@MainDrawerActivity,
                        TaskActivity::class.java
                    )
                )
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
    }

    override fun getLayout(): Int {
        return R.layout.activity_main_coze_drawer;
    }

    override fun onClick(v: View?) {
        if (v?.id != R.id.menu_task) {
            toggleDrawer()
        }
        when (v?.id) {
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
            }

            R.id.menu_favorites -> {
                startActivity(
                    Intent(
                        this@MainDrawerActivity,
                        FavoriteListActivity::class.java
                    )
                )
            }

            R.id.menu_gw_daily -> {
                hasShownWelcome = true
                startActivity(
                    Intent(
                        this@MainDrawerActivity,
                        ImageViewerActivity::class.java
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
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        TTSHelper.clear()
    }

    fun setRedDotView() {
        setGwdRedDotView()
        setTaskRedDotView()

    }

    fun setTaskRedDotView() {

        dm.add(
            DailyTaskHandler.getTaskProgress()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread()) // Results return to main thread
                .subscribe(
                    { data ->
                        if (data != null && !data.taskDetail.isAllUserTaskCompleted&&vTaskMenu.isVisible) {
                            // 获取菜单图标的宽高
                            val menuWidth: Int = vTaskMenu.width

                            // 创建布局参数
                            val params: FrameLayout.LayoutParams =
                                vRedDotTask.layoutParams as FrameLayout.LayoutParams


                            // 设置红点位置（菜单图标右上角）
                            params.gravity = Gravity.START or Gravity.TOP
                            params.leftMargin =
                                vTaskMenu.left + menuWidth - vTaskMenu.paddingRight - vRedDotTask.width / 2
                            params.topMargin =
                                vTaskMenu.top + vTaskMenu.paddingTop - vRedDotTask.height / 2
                            vRedDotTask.setLayoutParams(params)
                        } else {
                            vRedDotTask.visibility = View.GONE
                        }
                    },
                    Consumer { error: Throwable? ->
                        vRedDotTask.visibility = View.GONE
                    }
                )
        )
    }

    fun setGwdRedDotView() {
        // 获取红点视图
        val redDot: View = findViewById<View>(R.id.red_dot)
        val redDot3: View = findViewById<View>(R.id.red_dot3)
        if (!hasShownWelcome) {
            vDgwMenu.post({
                val drawables: Array<Drawable?> = vDgwMenu.getCompoundDrawables()
                val leftDrawable: Drawable? = drawables[0]
                if (leftDrawable != null) {

                    // 创建布局参数
                    val params: FrameLayout.LayoutParams =
                        redDot3.layoutParams as FrameLayout.LayoutParams


                    // 设置红点位置（菜单图标右上角）
                    params.gravity = Gravity.START or Gravity.TOP
                    params.leftMargin =
                        vDgwMenu.left + vDgwMenu.paddingLeft + leftDrawable.intrinsicWidth - redDot3.width / 2
                    params.topMargin = vDgwMenu.top + vDgwMenu.paddingTop - redDot3.height / 2
                    redDot3.setLayoutParams(params)
                }

            })
// 在视图布局完成后调整位置
            vHomeMenu.post({
                redDot.visibility = vHomeMenu.visibility
                // 获取菜单图标的宽高
                val menuWidth: Int = vHomeMenu.width


                // 创建布局参数
                val params: FrameLayout.LayoutParams =
                    redDot.layoutParams as FrameLayout.LayoutParams


                // 设置红点位置（菜单图标右上角）
                params.gravity = Gravity.START or Gravity.TOP
                params.leftMargin =
                    vHomeMenu.left + menuWidth - vHomeMenu.paddingRight - redDot.width / 2
                params.topMargin = vHomeMenu.top + vHomeMenu.paddingTop - redDot.height / 2
                redDot.setLayoutParams(params)
            })

            // 保存已经显示过引导页的状态
        } else {
            redDot.visibility = View.GONE
            redDot3.visibility = View.GONE
        }
    }
}