package sdk.chat.demo.robot.holder

//import sdk.chat.ui.R
import android.graphics.drawable.Drawable
import android.text.util.Linkify
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.android.material.button.MaterialButton
import com.stfalcon.chatkit.messages.MessagesListAdapter
import com.stfalcon.chatkit.messages.MessagesListStyle
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.Consumer
import io.reactivex.functions.Predicate
import sdk.chat.core.dao.Keys
import sdk.chat.core.events.EventType
import sdk.chat.core.events.NetworkEvent
import sdk.chat.core.session.ChatSDK
import sdk.chat.core.types.MessageSendStatus
import sdk.chat.demo.pre.BuildConfig
import sdk.chat.demo.pre.R
import sdk.chat.demo.robot.adpter.data.AIExplore
import sdk.chat.demo.robot.api.model.MessageDetail
import sdk.chat.demo.robot.extensions.StateStorage
import sdk.chat.demo.robot.handlers.BillingManager
import sdk.chat.demo.robot.handlers.GWThreadHandler
import sdk.chat.demo.robot.ui.MarkdownRenderer
import sdk.chat.demo.robot.utils.MarkdownPreprocessor
import sdk.guru.common.DisposableMap
import sdk.guru.common.RX
import java.text.DateFormat

open class ChatTextViewHolder<T : MessageHolder>(itemView: View) :
    RecyclerView.ViewHolder(itemView),
//    MessageHolders.BaseMessageViewHolder<T>(itemView, null),
//    MessageHolders.DefaultMessageViewHolder,
    Consumer<Throwable> {
    //    open var root: View? = itemView.findViewById(R.id.root)
    open var bubble: ViewGroup? = itemView.findViewById(R.id.bubble)

    open var replyText: MaterialButton? = itemView.findViewById(R.id.replyText)
    open var text: TextView? = itemView.findViewById(R.id.messageText)
    open var feedback: TextView? = itemView.findViewById(R.id.feedback)
    open var sendErrorHint: TextView? = itemView.findViewById(R.id.send_error_hint)
    open var replyErrorHint: TextView? = itemView.findViewById(R.id.reply_error_hint)
    open var processContainer: View? = itemView.findViewById(R.id.process_container)
    open var time: TextView? = itemView.findViewById(R.id.messageTime)
    open var sessionContainer: View? =
        itemView.findViewById(R.id.session_container)
    open var sessionName: TextView? = itemView.findViewById(R.id.session_name)

    //    open val btnPlay: ImageView? =
//        itemView.findViewById(R.id.btn_play)
    open val feedbackMenu: View? = itemView.findViewById(R.id.feedback_menu)
    open val contentMenu: View? = itemView.findViewById(R.id.user_text_menu)

    open var format: DateFormat? = null

    open val dm = DisposableMap()
    open var image: ImageView? = itemView.findViewById(R.id.image)
    open var imageContainer: View? = itemView.findViewById(R.id.image_container)
    open var imageMenu: View? = itemView.findViewById(R.id.image_menu)
    open var bible: TextView? = itemView.findViewById(R.id.bible)
    open var vipBiblePic: View? = itemView.findViewById(R.id.vip_bible_pic)

    open var imageLikeAi: ImageView? = itemView.findViewById(R.id.btn_like_ai)
    open var imageLikeContent: ImageView? = itemView.findViewById(R.id.btn_like_user_text)
    open var cbUserText: CheckBox? = itemView.findViewById(R.id.cb_user_text)
    open var cbAiText: CheckBox? = itemView.findViewById(R.id.cb_ai_text)
    open var isMultiSelectMode: Boolean = false

//    open var userClickListener: MessagesListAdapter.UserClickListener? = null

    init {
//        itemView.let {
//            format = SimpleDateFormat("dd-M-yyyy hh:mm", CurrentLocale.get(it.context))
//        }
    }

    fun onBind(holder: T, isMultiSelectMode: Boolean, position: Int) {
        this.isMultiSelectMode = isMultiSelectMode
        holder.pos = position
        bindListeners(holder)
        bind(holder)
    }

    open fun bind(t: T) {
//        progressView?.actionButton?.setOnClickListener(View.OnClickListener {
//            actionButtonPressed(t)
//        })
//        progressView?.bringToFront()
//
//        bubble?.let {
//            it.isSelected = isSelected
//        }
        val threadHandler: GWThreadHandler = ChatSDK.thread() as GWThreadHandler
        replyText?.visibility = View.GONE
        cbUserText?.visibility = View.GONE
        var action = (t as? TextHolder)?.action

        if (t.message.messageStatus != MessageSendStatus.Deleted && action != AIExplore.ExploreItem.action_daily_pray && t.message.text != null && !t.message.text.isEmpty()) {
            var reply = t.message.stringForKey("reply");
            if (reply != null && !reply.isEmpty()) {
                replyText?.visibility = View.VISIBLE
                replyText?.text = reply
            }
            setText(
                t.message.text,
//                t.message.id.toString() + "," + t.message.messageStatus.name + ","+ (t as? TextHolder)?.aiFeedback?.status +"," + t.text,
                t.enableLinkify()
            )
            if (isMultiSelectMode) {
                cbUserText?.visibility = View.VISIBLE
                cbUserText?.isChecked = t.isUserSelected
            }
            var topic = threadHandler.getSessionName(t.message.threadId)
            if (topic != null) {
                sessionContainer?.visibility = View.VISIBLE
                sessionName?.let {
                    it.text = topic
                }
            } else if (BuildConfig.DEBUG) {
                sessionContainer?.visibility = View.VISIBLE
                sessionName?.let {
                    it.text = t.message.threadId?.toString() ?: ""
                }
            } else {
                sessionContainer?.visibility = View.GONE
            }

        } else {
            sessionContainer?.visibility = View.GONE
//            if (action != GWThreadHandler.action_daily_pray && !t.message.entityID.equals("welcome")) {
//                setText("deleted", t.enableLinkify())
//            } else {
//                setText("", false);
//            }
            if (action != AIExplore.ExploreItem.action_daily_pray && action != AIExplore.ExploreItem.action_local_bible_pic && !t.message.entityID.equals(
                    "welcome"
                )
            ) {
                setText(bubble?.context?.getString(R.string.message_deleted) ?: "", false)
            } else {
                setText("", false);
            }
        }


        if (StateStorage.getStateB(t.message.status)) {
            imageLikeAi?.setImageResource(R.mipmap.ic_dislike_black)
        } else {
            imageLikeAi?.setImageResource(R.mipmap.ic_like_black)
        }
        if (StateStorage.getStateA(t.message.status)) {
            imageLikeContent?.setImageResource(R.mipmap.ic_dislike_black)
        } else {
            imageLikeContent?.setImageResource(R.mipmap.ic_like_black)
        }

//        if (t.message.equals(TTSHelper.getPlayingMsg()) && !TTSHelper.isPlayerPaused()) {
//            btnPlay?.setImageResource(R.mipmap.ic_pause_black);
//        } else {
//            btnPlay?.setImageResource(R.mipmap.ic_play_black);
//        }

        bindSendStatus(t)

        var aiFeedback: MessageDetail? = (t as? TextHolder)?.getAiFeedback();

//        t.message.metaValuesAsMap
        var feedbackText = aiFeedback?.feedbackText ?: ""
        cbAiText?.visibility = View.GONE
        feedbackText = MarkdownPreprocessor.preprocessMultipleBoldMarkers(feedbackText)
        feedback?.let {
            if (!feedbackText.isEmpty() && isMultiSelectMode) {
                cbAiText?.visibility = View.VISIBLE
                cbAiText?.isChecked = t.isAiSelected
            }
            it.visibility = View.VISIBLE
            it.isClickable = true
            it.isFocusable = true
            MarkdownRenderer.markwon.setMarkdown(it, feedbackText);
        }

        if(feedbackText.isEmpty()&& action != AIExplore.ExploreItem.action_local_bible_pic&&t.message.messageStatus == MessageSendStatus.Sent){
            replyErrorHint?.visibility = View.VISIBLE
            replyErrorHint?.text = bubble?.context?.getString(R.string.no_cached_data) ?: "Retrieve the response data..."
        }


        var bibleText = aiFeedback?.feedback?.bible
        if (t.message.entityID.equals("welcome")) {
            contentMenu?.visibility = View.GONE
            bubble?.visibility = View.GONE
            if (isMultiSelectMode) {
                feedbackMenu?.visibility = View.GONE
            } else {
                feedbackMenu?.visibility = View.VISIBLE
            }
            sessionContainer?.visibility = View.GONE
            processContainer?.visibility = View.GONE
            sendErrorHint?.visibility = View.GONE
            showFeedbackMenus(feedbackMenu, View.GONE, bibleText)
        } else {
            if (feedbackText.isEmpty() || isMultiSelectMode) {
                feedbackMenu?.visibility = View.GONE
            } else {
                showFeedbackMenus(feedbackMenu, View.VISIBLE, bibleText)
            }
            if (t.message.text.isEmpty() || action == AIExplore.ExploreItem.action_daily_pray || isMultiSelectMode) {
                contentMenu?.visibility = View.GONE
            } else {
                contentMenu?.visibility = View.VISIBLE
            }
        }

        bindBiblePic(t, bibleText)

    }

    fun bindBiblePic(t: T, verses: String? = null) {
        var bibleText = verses
        if (bibleText == null) {
            var aiFeedback: MessageDetail? = (t as? TextHolder)?.getAiFeedback();
            bibleText = aiFeedback?.feedback?.bible
        }

        var imageUrl = t.message.stringForKey(Keys.ImageUrl)
        imageMenu?.visibility = View.GONE
        imageContainer?.visibility = View.GONE
        vipBiblePic?.visibility = View.GONE
        if (!imageUrl.isNullOrEmpty() && !bibleText.isNullOrEmpty()) {
            imageContainer?.visibility = View.VISIBLE
            if (!BillingManager.getInstance()
                    .hasSubscriptions() && t.message.integerForKey(Keys.Permission) == -1
            ) {
                image?.visibility == View.INVISIBLE
                vipBiblePic?.visibility = View.VISIBLE
            } else {
                imageMenu?.visibility = View.VISIBLE
                bible?.text = bibleText
                Glide.with(image!!)
                    .load(imageUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.icn_200_image_message_placeholder) // 占位图
                    .error(R.drawable.icn_200_image_message_error) // 错误图
                    .addListener(object : RequestListener<Drawable> {
                        override fun onResourceReady(
                            resource: Drawable,
                            model: Any,
                            target: Target<Drawable>,
                            dataSource: DataSource,
                            isFirstResource: Boolean
                        ): Boolean {
                            return false
                        }

                        override fun onLoadFailed(
                            e: GlideException?,
                            model: Any,
                            target: Target<Drawable>,
                            isFirstResource: Boolean
                        ): Boolean {
                            return false
                        }
                    })
                    .into(image!!)
            }
        }
    }

    fun showFeedbackMenus(v: View?, visible: Int, bibleText: String?) {
        if (v == null) {
            return
        }
        val ids: IntArray =
            intArrayOf(R.id.btn_like_ai, R.id.btn_share_text, R.id.btn_redo, R.id.btn_del)
        for (i in ids) {
            var sv = v.findViewById<View>(i)
            if (sv == null || sv.visibility == visible) {
                continue
            } else {
                sv.visibility = visible
            }
        }
        var bibleVisible = View.GONE
        if (bibleText != null && bibleText.isNotEmpty()) {
            bibleVisible = View.VISIBLE
        }
        //, R.id.btn_pray
        val id2s: IntArray = intArrayOf(R.id.btn_pic)
        for (i in id2s) {
            var sv = v.findViewById<View>(i)
            if (sv != null && sv.visibility != bibleVisible) {
                sv.visibility = bibleVisible
            }
        }
    }

    open fun setText(value: String, linkify: Boolean) {
        if (!value.isEmpty()) {
            bubble?.visibility = View.VISIBLE
            text?.let {
                if (linkify) {
                    it.autoLinkMask = Linkify.ALL
                } else {
                    it.autoLinkMask = 0
                }
                it.text = value
            }

        } else {
            bubble?.visibility = View.GONE
        }
    }

//    open fun bindReadStatus(t: T) {
//        readStatus?.let {
//            UIModule.shared().readStatusViewBinder.onBind(it, t)
//        }
//    }

    open fun bindSendStatus(holder: T): Boolean {
        var aiFeedback: MessageDetail? = (holder as? TextHolder)?.getAiFeedback();
        var status = holder.message.messageStatus
        Log.d("textviewholder", "bindSendStatus:" + status.name+","+holder.message.entityID)
        if (status.ordinal < MessageSendStatus.Replying.ordinal) {
            feedbackMenu?.visibility = View.GONE
            feedback?.visibility = View.GONE
            contentMenu?.visibility = View.GONE
            if (aiFeedback == null && status == MessageSendStatus.UploadFailed) {
                sendErrorHint?.visibility = View.VISIBLE
            } else {
                sendErrorHint?.visibility = View.GONE
            }

            if (status == MessageSendStatus.Uploading) {
                processContainer?.visibility = View.VISIBLE
            } else {
                processContainer?.visibility = View.GONE
            }
            replyErrorHint?.visibility = View.GONE
        } else {
            feedback?.visibility = View.VISIBLE
            processContainer?.visibility = View.GONE
            feedbackMenu?.visibility = View.GONE
            if (status == MessageSendStatus.Sent) {
                var feedbackText = aiFeedback?.feedbackText ?: ""
                if (!feedbackText.isEmpty()) {
                    feedbackMenu?.visibility = View.VISIBLE
                }
            } else if (status == MessageSendStatus.Replying) {
                processContainer?.visibility = View.VISIBLE
            }

            if (status == MessageSendStatus.Failed) {
                replyErrorHint?.visibility = View.VISIBLE
                replyErrorHint?.text = bubble?.context?.getString(R.string.ai_failed) ?: "Retrieve the response data..."
            } else {
                replyErrorHint?.visibility = View.GONE
            }
            sendErrorHint?.visibility = View.GONE
        }
        return true

    }

    open fun bindResend(holder: T) {
//        resendContainer?.let {
//            if (holder.canResend()) {
//                it.visibility = View.VISIBLE
//            } else {
//                it.visibility = View.GONE
//            }
//        }
    }

    open fun bindProgress(t: T) {
//        progressView?.bindProgress(t)
        bindResend(t)
    }

    open fun bindListeners(t: T) {
        dm.dispose()
        dm.add(
            ChatSDK.events().sourceOnSingle()
                .filter(
                    NetworkEvent.filterType(
                        EventType.MessageSendStatusUpdated,
                        EventType.MessageReadReceiptUpdated
                    )
                )
                .filter(filterById(t.message.id))
                .doOnError(this)
                .subscribe { networkEvent ->
                    RX.main().scheduleDirect {
//                        bindReadStatus(t)
                        bindSendStatus(t)
                    }
                })

        dm.add(
            ChatSDK.events().sourceOnSingle()
                .filter(NetworkEvent.filterType(EventType.MessageProgressUpdated))
                .filter(filterById(t.message.id))
                .doOnError(this)
                .subscribe {
                    RX.main().scheduleDirect {
                        bindProgress(t)
                    }
                })
        dm.add(
            ChatSDK.events().sourceOnSingle()
                .filter(NetworkEvent.filterType(EventType.BillChange))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(Consumer { networkEvent: NetworkEvent? ->
                    bindBiblePic(t)
                })
        )
        dm.add(
            ChatSDK.events().sourceOnSingle()
                .filter { networkEvent: NetworkEvent? ->
                    networkEvent!!.type == EventType.ThreadsUpdated
                            && networkEvent.threadId == t.message.threadId
                }
//                .filter { networkEvent: NetworkEvent? -> networkEvent!!.type == EventType.ThreadsUpdated }
                .doOnError(this)
                .subscribe {
                    RX.main().scheduleDirect {
                        val threadHandler: GWThreadHandler = ChatSDK.thread() as GWThreadHandler
                        var topic = threadHandler.getSessionName(t.message.threadId)
                        if (topic != null) {
                            sessionName?.let {
                                it.text = topic
                            }
                        }
                    }
                })

        dm.add(
            ChatSDK.events().sourceOnSingle()
                .filter(NetworkEvent.filterType(EventType.MessageUpdated))
                .filter(filterById(t.message.id))
                .doOnError(this)
                .subscribe {
                    RX.main().scheduleDirect {
                        (t as? TextHolder)?.aiFeedback = null
                        bind(t)
                    }
                })


    }

    fun filterById(id: Long?): Predicate<NetworkEvent?> {
        return Predicate { networkEvent: NetworkEvent? -> networkEvent?.message?.id == id }
    }

    fun applyStyle(style: MessagesListStyle) {
////        this.style = style
//        if (direction == MessageDirection.Incoming) {
//            applyIncomingStyle(style)
//        } else {
////            applyOutgoingStyle(style)
//        }
    }


//    open fun actionButtonPressed(holder: T) {
//        val payload = holder.payload
//        if (payload is DownloadablePayload) {
//            progressView?.let { view ->
//                dm.add(payload.startDownload().observeOn(RX.main()).subscribe({
//                    view.actionButton?.visibility = View.INVISIBLE
//                }, {
//                    it.printStackTrace()
//                    view.actionButton?.visibility = View.VISIBLE
//                }))
//            }
//        }
//    }

    fun setAvatarClickListener(l: MessagesListAdapter.UserClickListener?) {
//        userClickListener = l
    }

    override fun accept(t: Throwable?) {
        t?.printStackTrace()
    }

}