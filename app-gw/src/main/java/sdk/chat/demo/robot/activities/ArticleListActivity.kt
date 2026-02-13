package sdk.chat.demo.robot.activities

import sdk.chat.demo.robot.api.model.MessagePage
import android.util.Log
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.Single
import io.reactivex.SingleSource
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.Consumer
import io.reactivex.functions.Function
import org.tinylog.Logger
import sdk.chat.core.dao.Message
import sdk.chat.core.events.EventType
import sdk.chat.core.events.NetworkEvent
import sdk.chat.core.session.ChatSDK
import sdk.chat.demo.MainApp
import sdk.chat.demo.pre.R
import sdk.chat.demo.robot.adpter.ArticleAdapter
import sdk.chat.demo.robot.adpter.GenericMenuPopupWindow
import sdk.chat.demo.robot.adpter.SessionPopupAdapter
import sdk.chat.demo.robot.adpter.data.Article
import sdk.chat.demo.robot.adpter.data.ArticleSession
import sdk.chat.demo.robot.api.model.KeyValuePair
import sdk.chat.demo.robot.api.model.MessageDetail
import sdk.chat.demo.robot.extensions.DateLocalizationUtil
import sdk.chat.demo.robot.extensions.FirebaseReport
import sdk.chat.demo.robot.extensions.showMaterialConfirmationDialog
import sdk.chat.demo.robot.handlers.GWMsgHandler
import sdk.chat.demo.robot.handlers.GWThreadHandler
import sdk.chat.demo.robot.handlers.LogUploader
import sdk.chat.demo.robot.ui.LoadMoreSwipeRefreshLayout
import sdk.chat.demo.robot.ui.PopupMenuHelper
import sdk.chat.demo.robot.utils.SoftHideKeyBoardUtil
import sdk.chat.demo.robot.utils.ToastHelper
import sdk.guru.common.RX


class ArticleListActivity : BaseActivity(), View.OnClickListener {
    private val threadHandler: GWThreadHandler = ChatSDK.thread() as GWThreadHandler
    private lateinit var articleAdapter: ArticleAdapter;
    private lateinit var swipeRefreshLayout: LoadMoreSwipeRefreshLayout
    private lateinit var menuPopup: GenericMenuPopupWindow<ArticleSession, SessionPopupAdapter.SessionItemViewHolder>
    private var sessionId: String = ""
    private var eldestMsg: Message? = null
    private lateinit var tvTitle: TextView
    private lateinit var vEdSummaryContainer: View
    private lateinit var bConversations: View
    private lateinit var vEdSummary: EditText
    private lateinit var vEmptyContainer: View
    private lateinit var vScrollTop: View

    //    private lateinit var vLineDash: View
    private lateinit var recyclerView: RecyclerView
    private lateinit var vMoreMenus: View
    private var editingMode = 0
    private val EDIT_SUMMARY = 1
    private val EDIT_TOPIC_NAME = 2

//    private val handler = Handler(Looper.getMainLooper())

    companion object {
        private const val EXTRA_INITIAL_DATA = "initial_data"

        // 提供静态启动方法（推荐）
        fun start(context: Context, topicId: String? = null) {
            val intent = Intent(context, ArticleListActivity::class.java).apply {
                putExtra(EXTRA_INITIAL_DATA, topicId)
            }
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_article_list)

        sessionId = intent.getStringExtra(EXTRA_INITIAL_DATA).toString()
        eldestMsg = null


        vEdSummaryContainer = findViewById(R.id.edSummaryContainer)
        vEdSummary = findViewById(R.id.edSummary)

        // 设置RecyclerView
        setupRecyclerView()
        setupRefreshLayout()

        findViewById<View>(R.id.home).setOnClickListener(this)
        tvTitle = findViewById<View>(R.id.title) as TextView
        tvTitle.setOnClickListener(this)
        findViewById<View>(R.id.edSummaryExit).setOnClickListener(this)
        findViewById<View>(R.id.edSummaryConfirm).setOnClickListener(this)
        vScrollTop = findViewById<View>(R.id.scrollTop)
        vScrollTop.setOnClickListener(this)
        vMoreMenus = findViewById<View>(R.id.more_menus)
        vMoreMenus.setOnClickListener(this)
        bConversations = findViewById<View>(R.id.conversations)
        bConversations.setOnClickListener(this)
        findViewById<View>(R.id.conversations1).setOnClickListener(this)

        vEmptyContainer = findViewById<View>(R.id.empty_container)
//        vLineDash = findViewById<View>(R.id.dash_line)

        dm.add(
            ChatSDK.events().sourceOnSingle()
                .filter(NetworkEvent.filterType(EventType.ThreadRemoved))
                .subscribe(Consumer { networkEvent: NetworkEvent? ->
                    finish()
                })
        )
        SoftHideKeyBoardUtil.assistActivity(findViewById<View>(R.id.main))

    }

//    override fun getRootView(): View? {
//        return findViewById<View>(R.id.main)
//    }

    override fun onResume() {
        super.onResume()
        loadSessions()
    }

    private fun setupRecyclerView() {
        recyclerView = findViewById<RecyclerView?>(R.id.recyclerView)
        articleAdapter = ArticleAdapter(
            onItemClick = { article ->
                // 处理普通点击
//                Toast.makeText(this, "点击了: ${article.localId}，${article.title}", Toast.LENGTH_SHORT).show()
                ChatActivity.start(ArticleListActivity@ this, article.id, "timeline");
                LogUploader.reportEvent(
                    "mod_timeline", mutableListOf(
                        KeyValuePair("timeline_action", "20"),
                    )
                )
            },
            onEditClick = { article ->
                // 处理编辑点击
//                Toast.makeText(this, "编辑: ${article.title}", Toast.LENGTH_SHORT).show()
                editingMode = EDIT_SUMMARY
                vEdSummary.setText(article.title)
                vEdSummaryContainer.visibility = View.VISIBLE
                showKeyboard(vEdSummary)
                LogUploader.reportEvent(
                    "mod_timeline", mutableListOf(
                        KeyValuePair("timeline_action", "40"),
                    )
                )
            },
            onLongClick = { v, article ->
                // 处理长按
//                Toast.makeText(this, "长按: ${article.title}", Toast.LENGTH_SHORT).show()
                showPopupMenu(v, article)
                LogUploader.reportEvent(
                    "mod_timeline", mutableListOf(
                        KeyValuePair("timeline_action", "30"),
                    )
                )
                true
            })
        recyclerView.adapter = articleAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)
    }

    private val hideLoadLatestRunnable: Runnable = object : Runnable {
        override fun run() {
            vScrollTop.visibility = View.GONE
        }
    }

    private fun setupRefreshLayout() {
        swipeRefreshLayout = findViewById<LoadMoreSwipeRefreshLayout?>(R.id.swiperefreshlayout)
        swipeRefreshLayout.apply {
            // 下拉刷新监听
            setOnRefreshListener {
                articleAdapter.isLoading = false
                eldestMsg = null
                setLoadingMore(false)
                loadArticles()
            }

            // 绑定RecyclerView
            setupWithRecyclerView(recyclerView)

//            // 上拉加载监听
            setOnLoadMoreListener(object : LoadMoreSwipeRefreshLayout.OnLoadMoreListener {
                override fun onLoadMore() {
                    Log.e("MessageService", "onLoadMore:" + articleAdapter.isLoading)
                    if (!articleAdapter.isLoading) {
                        articleAdapter.isLoading = true
                        setLoadingMore(true)
                        loadArticles()
                    }
                }

                override fun onLoadLatestActive() {
                    vScrollTop.visibility = View.VISIBLE
                    handler.removeCallbacks(hideLoadLatestRunnable)
                    handler.postDelayed(hideLoadLatestRunnable, 4000)
                }
            })
            setCanLoadMore(true)
        }

    }

    private fun setEmptyRecord(isEmpty: Boolean) {
        if (isEmpty) {
            vEmptyContainer.visibility = View.VISIBLE
//            vLineDash.visibility = View.INVISIBLE
            bConversations.visibility = View.INVISIBLE
        } else {
            vEmptyContainer.visibility = View.INVISIBLE
//            vLineDash.visibility = View.VISIBLE
            bConversations.visibility = View.VISIBLE
        }
    }

    private fun initMenuPopup(items: MutableList<ArticleSession>) {
        var selectedPosition = items.indexOfFirst { it.id == sessionId }.let {
            if (it < 0) 0 else it
        }
        var menuPopupAdapter = SessionPopupAdapter(this, items, selectedPosition)
        var s = items[selectedPosition]
        var title = s.title
        tvTitle.text = title
        if (s.isQA || s.id == GWThreadHandler.chatSessionId) {
            vMoreMenus.visibility = View.INVISIBLE
        } else {
            vMoreMenus.visibility = View.VISIBLE
        }
        menuPopup = GenericMenuPopupWindow(
            context = this,
            anchor = findViewById(R.id.home),
            adapter = menuPopupAdapter,
            onItemSelected = { item, position ->
                if (item != null) {
                    if (menuPopup.isEditModel) {
                        changeTopic(item.id.toLong())
                    } else {
                        tvTitle.text = item.title
                        sessionId = item.id
                        eldestMsg = null
                        menuPopup.setTitle(item.title)
                        loadArticles()

                        if (item.isQA || item.id == GWThreadHandler.chatSessionId) {
                            vMoreMenus.visibility = View.INVISIBLE
                        } else {
                            vMoreMenus.visibility = View.VISIBLE
                        }
                    }
                } else {
                    editingMode = 0
                    vEdSummary.setText("")
                    vEdSummaryContainer.visibility = View.VISIBLE
                    showKeyboard(vEdSummary)
                }
            }
        )
        menuPopup.setTitle(title)
    }

    override fun getLayout(): Int {
        return 0;
    }

    fun loadSessions() {
        dm.add(
            threadHandler.listSessions()
                .observeOn(RX.main())
                .subscribe(
                    { articleSessions ->
//                        menuPopup.updateMenuItems(articleSessions, 0)
                        initMenuPopup(articleSessions)
                        swipeRefreshLayout.setLoadingMore(true)
                        articleAdapter.isLoading = true
                        loadArticles()
                    },
                    { error -> // onError
                        Toast.makeText(
                            this@ArticleListActivity,
                            error.message,
                            Toast.LENGTH_SHORT
                        ).show()
                    })
        )
    }

    fun loadArticles() {
        if (eldestMsg == null) {
            articleAdapter.clearAll()
            swipeRefreshLayout.setCanLoadMore(true)
        }
        Logger.error { "loadArticles:$sessionId,${tvTitle.text},${eldestMsg}" }
        dm.add(
            threadHandler.loadMessagesBySession(sessionId, eldestMsg)
                .flatMap(Function { data: MessagePage ->
                    var messages = data.items
                    Logger.error { "loadArticles $sessionId,${tvTitle.text},${eldestMsg} got:${messages.size}" }
//                    var lastDay = articleAdapter.getLastArticle()?.day;

//                    var firstDay =
//                        DateLocalizationUtil.dateStr(messages.lastOrNull()?.date).split(" ")[0]
                    if (!data.isHasMore) {
                        swipeRefreshLayout.setCanLoadMore(false)
                    }
                    if (!messages.isEmpty()) {
                        eldestMsg = messages[messages.size - 1]
                    }
                    val articleList = messages.map { message ->
                        var dateStr = DateLocalizationUtil.dateStr(message.date)
                        val parts = dateStr.split(" ")
                        var thisDay = ""
                        var thisTime = ""
                        if (parts.size == 2) {
                            thisDay = parts[0];
                            thisTime = parts[1]
                        }

                        val aiFeedback: MessageDetail? = GWMsgHandler.getAiFeedback(message)
                        Article(
                            id = message.entityID,
                            localId = message.id,
                            content = message.text,
                            day = thisDay,
                            time = thisTime,
                            title = aiFeedback?.summary ?: message.stringForKey("summary"),
                            colorTag = runCatching {
                                aiFeedback?.feedback?.colorTag?.toColorInt()
                                    ?: message.stringForKey("colorTag").toColorInt()
                            }
                                .getOrElse { exception ->
                                    "#FFFBE8".toColorInt()
                                },
                            showDay = true,
                            isFirstDay = false,
                        )
                    }.filter { article ->
                        // 过滤条件：只保留符合条件的 message
                        !article.content.isEmpty()
                    }
                    Single.just(Pair(articleList, data.isHasMore))
                })
                .observeOn(RX.main())
                .doFinally {
                    articleAdapter.isLoading = false
                    swipeRefreshLayout.isRefreshing = false
                    swipeRefreshLayout.setLoadingMore(false)
                }
                .subscribe(
                    { articleList ->
                        val (messages, isHasMore) = articleList
                        var isFirst = articleAdapter.itemCount == 1
                        Logger.error { "loadArticles $sessionId,${tvTitle.text},${eldestMsg} got:${messages.size},isHasMore:${isHasMore}" }
                        articleAdapter.appendItems(messages, isHasMore) {
                            if (isFirst) {
                                recyclerView.scrollToPosition(0);
                            }
//                            Log.e(
//                                "MessageService",
//                                Thread.currentThread().name + " after refresh updatedList:${articleAdapter.itemCount}"
//                            )
                        }
                        if (!messages.isEmpty()) {
                            setEmptyRecord(false)
                        } else if (isFirst) {
                            setEmptyRecord(true)
                        }
                    },
                    { error -> // onError
                        Logger.error { "loadArticles $sessionId,${tvTitle.text},${eldestMsg} got:${error}" }
                        Toast.makeText(
                            this@ArticleListActivity,
                            error.message,
                            Toast.LENGTH_SHORT
                        ).show()
                    })
        )
    }


    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.home -> {
                LogUploader.reportEvent(
                    "mod_timeline", mutableListOf(
                        KeyValuePair("timeline_action", "80")
                    )
                )
                finish()
            }

            R.id.conversations, R.id.conversations1 -> {
                ChatSDK.events().source().accept(NetworkEvent(EventType.HideDrawer))
//                val intent =
//                    Intent(this@ArticleListActivity, MainDrawerActivity::class.java).apply {
//                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
//                    }
//                startActivity(intent)

                LogUploader.reportEvent(
                    "mod_timeline", mutableListOf(
                        KeyValuePair("timeline_action", "50"),
                    )
                )
                finish()
            }

            R.id.title -> {
                menuPopup.show(false)
            }

            R.id.edSummaryExit -> {
                vEdSummary.requestFocus()
                hideKeyboard()
                vEdSummaryContainer.visibility = View.GONE
            }

            R.id.edSummaryConfirm -> {
                if (editingMode == EDIT_SUMMARY) {
                    editSummary()
                } else if (editingMode == EDIT_TOPIC_NAME) {
                    editTopicName()
                } else {
                    newTopic()
                }
            }

            R.id.scrollTop -> {
                recyclerView.scrollToPosition(0)
            }

            R.id.more_menus -> {
                LogUploader.reportEvent(
                    "mod_timeline", mutableListOf(
                        KeyValuePair("timeline_action", "60"),
                    )
                )
                PopupMenuHelper(
                    context = this,
                    anchorView = v,
                    onItemSelected = { v ->
                        when (v.id) {
                            R.id.delTopic -> {
                                showMaterialConfirmationDialog(
                                    this@ArticleListActivity,
                                    getString(R.string.delete_confirm), null, null,
                                    positiveAction = {
                                        val disposable = threadHandler.deleteSession(sessionId)
                                            .observeOn(AndroidSchedulers.mainThread())
                                            .subscribe(
                                                Consumer { newState: Boolean? ->
                                                    ToastHelper.show(
                                                        this@ArticleListActivity,
                                                        getString(R.string.success)
                                                    )
                                                    finish()
                                                },
                                                Consumer { error: Throwable? ->
                                                    ToastHelper.show(
                                                        this@ArticleListActivity,
                                                        error?.message
                                                    );
                                                })
                                        dm.add(disposable)
                                    })
                                LogUploader.reportEvent(
                                    "mod_timeline", mutableListOf(
                                        KeyValuePair("timeline_action", "62"),
                                        KeyValuePair(
                                            "timeline_edit_entrance",
                                            "timeline_page_corner"
                                        ),
                                    )
                                )
                            }

                            R.id.rename -> {
                                editingMode = EDIT_TOPIC_NAME
                                vEdSummary.setText(tvTitle.text)
                                vEdSummaryContainer.visibility = View.VISIBLE
                                showKeyboard(vEdSummary)
                                LogUploader.reportEvent(
                                    "mod_timeline", mutableListOf(
                                        KeyValuePair("timeline_action", "61"),
                                        KeyValuePair(
                                            "timeline_edit_entrance",
                                            "timeline_page_corner"
                                        ),
                                    )
                                )
                            }
                        }
                    },
                    menuResId = R.layout.menu_article_topic,
                    clickableResIds = intArrayOf(
                        R.id.rename,
                        R.id.delTopic,
                    )
                ).show()
            }

        }
    }


    // 在Activity/Fragment中使用
    fun showPopupMenu(anchorView: View, selectedArticle: Article) {
        // 根据选中项目动态创建菜单项
        PopupMenuHelper(
            context = this,
            anchorView = anchorView,
            onItemSelected = { v ->
                when (v.id) {
                    R.id.delArticle -> {
                        showMaterialConfirmationDialog(
                            this@ArticleListActivity,
                            getString(R.string.delete_confirm), null, null,
                            positiveAction = {
                                changeTopic(-1)
                            })
                        LogUploader.reportEvent(
                            "mod_timeline", mutableListOf(
                                KeyValuePair("timeline_action", "38"),
                            )
                        )
                    }

                    R.id.changeTopic -> {
                        menuPopup.show(true)
                        LogUploader.reportEvent(
                            "mod_timeline", mutableListOf(
                                KeyValuePair("timeline_action", "32"),
                            )
                        )
                    }

                    R.id.copy -> {
                        LogUploader.reportEvent(
                            "mod_timeline", mutableListOf(
                                KeyValuePair("timeline_action", "31"),
                            )
                        )
                        val article = articleAdapter.selectArticle
                        if (article != null) {
                            val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("恩语", article.content)
                            clipboard.setPrimaryClip(clip)
                            ToastHelper.show(
                                MainApp.getContext(),
                                MainApp.getContext().getString(R.string.copied)
                            )
                        }
                    }
                }
            },
            menuResId = R.layout.menu_article_popup,
            clickableResIds = intArrayOf(
                R.id.copy,
                R.id.changeTopic,
                R.id.delArticle,
            )
        ).show()
    }

    fun changeTopic(topicId: Long) {
        val article = articleAdapter.selectArticle
        if (article == null) {
            return
        }
        dm.add(
            threadHandler.setMsgSession(article.id, topicId)
                .observeOn(RX.main())
                .subscribe(
                    { result ->
                        if (topicId <= 0) {
                            if (articleAdapter.itemCount == 2) {
                                setEmptyRecord(true)
                            }
                            articleAdapter.deleteById(article.id)
                        } else {
//                            sessionId = topicId.toString()
                            //FIXME
//                            loadSessions()
                            start(this@ArticleListActivity, topicId.toString())
                        }
                    },
                    { error -> // onError
                        Toast.makeText(
                            this@ArticleListActivity,
                            "修改失败: ${error.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    })
        )
    }

    fun hideEditDialog() {

        vEdSummary.requestFocus()
        hideKeyboard()
        vEdSummaryContainer.visibility = View.GONE
    }

    fun editSummary() {
        val newSummary = vEdSummary.text.toString()
        val article = articleAdapter.selectArticle
        if (article == null) {
            return
        }
        dm.add(
            threadHandler.setSummary(article.id, newSummary)
                .observeOn(RX.main())
                .subscribe(
                    { result ->
                        if (result) {
                            articleAdapter.updateSummaryById(article.id, newSummary)
                        }
                        hideEditDialog()
                    },
                    { error -> // onError
                        Toast.makeText(
                            this@ArticleListActivity,
                            "修改失败: ${error.message}",
                            Toast.LENGTH_SHORT
                        ).show()
//                        //FIXME
//                        var clipboard:ClipboardManager =
//                            getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager;
//                        val clip = ClipData.newPlainText("恩语", error.message)
//                        clipboard.setPrimaryClip(clip)
                        hideEditDialog()
                    })
        )
    }

    fun editTopicName() {
        if (sessionId.isEmpty()) {
            return
        }
        var title = vEdSummary.text.toString()
        dm.add(
            threadHandler.setSessionName(sessionId.toLong(), title)
                .observeOn(RX.main()).subscribe(
                    { result ->
                        if (!result) {
                            Toast.makeText(
                                this@ArticleListActivity,
                                getString(R.string.failed_and_retry),
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            menuPopup.updateMenuItemSelected(
                                ArticleSession(
                                    id = sessionId,
                                    title = title,
                                    false
                                )
                            )
                            menuPopup.setTitle(title)
                            tvTitle.text = title
                        }
                        hideEditDialog()
                    },
                    { error -> // onError
                        Toast.makeText(
                            this@ArticleListActivity,
                            "${getString(R.string.failed_and_retry)} ${error.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                        hideEditDialog()
                    })
        )
    }


    fun newTopic() {
        val newSummary = vEdSummary.text.toString()
        val article = articleAdapter.selectArticle
        if (article == null) {
            return
        }
        dm.add(
            threadHandler.newMsgSession(article.id, newSummary)
                .observeOn(RX.main())
                .subscribe(
                    { result ->
                        if (result > 0) {
//                            sessionId = result.toString()
//                            loadSessions()
                            start(this@ArticleListActivity, result.toString())
                        }
                        vEdSummary.requestFocus()
                        hideKeyboard()
                        vEdSummaryContainer.visibility = View.GONE
                    },
                    { error -> // onError
                        vEdSummary.requestFocus()
                        hideKeyboard()
                        vEdSummaryContainer.visibility = View.GONE
                        Toast.makeText(
                            this@ArticleListActivity,
                            "修改失败: ${error.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    })
        )
    }

    fun showKeyboard(view: View?) {
        if (view == null) return


        val context = view.getContext()
        view.requestFocus()

        view.post(Runnable {
            val imm = context.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager?
            if (imm != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    // Android 11+推荐方式
                    view.getWindowInsetsController()!!.show(WindowInsets.Type.ime())
                } else {
                    // 传统方式
                    imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
                }
            }

            var edv = view as? EditText
            if (edv != null) {
                edv.setSelection(edv.getText().length)
            }
        })
    }
}