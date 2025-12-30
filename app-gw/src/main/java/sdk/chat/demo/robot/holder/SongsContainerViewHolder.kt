package sdk.chat.demo.robot.holder

import android.text.util.Linkify
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import io.noties.markwon.Markwon
import io.reactivex.functions.Consumer
import io.reactivex.functions.Predicate
import sdk.chat.core.events.EventType
import sdk.chat.core.events.NetworkEvent
import sdk.chat.core.session.ChatSDK
import sdk.chat.core.types.MessageSendStatus
import sdk.chat.demo.pre.BuildConfig
import sdk.chat.demo.pre.R
import sdk.chat.demo.robot.api.model.MessageDetail
import sdk.chat.demo.robot.api.model.Song
import sdk.chat.demo.robot.extensions.StateStorage
import sdk.chat.demo.robot.handlers.GWThreadHandler
import sdk.chat.demo.robot.ui.SongsContainerView
import sdk.guru.common.DisposableMap
import sdk.guru.common.RX

open class SongsContainerViewHolder<T : MessageHolder>(itemView: View) :
    RecyclerView.ViewHolder(itemView), Consumer<Throwable> {
    open var bubble: ViewGroup? = itemView.findViewById(R.id.bubble)
    open var text: TextView? = itemView.findViewById(R.id.messageText)
    open var feedback: TextView? = itemView.findViewById(R.id.feedback)
    open var sendErrorHint: TextView? = itemView.findViewById(R.id.send_error_hint)
    open var replyErrorHint: TextView? = itemView.findViewById(R.id.reply_error_hint)
    open var processContainer: View? = itemView.findViewById(R.id.process_container)
    open val feedbackMenu: View? = itemView.findViewById(R.id.feedback_menu)
    open val contentMenu: View? = itemView.findViewById(R.id.user_text_menu)
    open var sessionContainer: View? =
        itemView.findViewById(R.id.session_container)
    open var sessionName: TextView? = itemView.findViewById(R.id.session_name)

    private val songsContainer: SongsContainerView = itemView.findViewById(R.id.songsContainer)
    private val onSongClickListener: SongsContainerView.OnSongClickListener? = null
    open val dm = DisposableMap()

    open var imageLikeAi: ImageView? = itemView.findViewById(R.id.btn_like_ai)
    open var imageLikeContent: ImageView? = itemView.findViewById(R.id.btn_like_user_text)
    open var cbUserText: CheckBox? = itemView.findViewById(R.id.cb_user_text)
    open var cbAiText: CheckBox? = itemView.findViewById(R.id.cb_ai_text)
    open var isMultiSelectMode: Boolean = false

    open fun bindSendStatus(holder: T): Boolean {
        var aiFeedback: MessageDetail? = (holder as? TextHolder)?.getAiFeedback();
        var status = holder.message.messageStatus
//        Log.d("sending", "bindSendStatus:" + status.name)
        if (status.ordinal < MessageSendStatus.Replying.ordinal) {
            feedbackMenu?.visibility = View.GONE
            songsContainer.visibility = View.GONE
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
            songsContainer.visibility = View.VISIBLE
            processContainer?.visibility = View.GONE
            feedbackMenu?.visibility = View.GONE
            if (status == MessageSendStatus.Sent) {
//                var songs: List<Song> = aiFeedback?.feedback?.hymns ?: listOf()
                var songs: List<Song> = (holder as? TextHolder)?.songs ?: listOf();
//                var feedbackText = aiFeedback?.feedbackText ?: ""
                if (!songs.isEmpty()) {
                    feedbackMenu?.visibility = View.VISIBLE
                }
            } else if (status == MessageSendStatus.Replying) {
                processContainer?.visibility = View.VISIBLE
            }

            if (status == MessageSendStatus.Failed) {
                replyErrorHint?.visibility = View.VISIBLE
            } else {
                replyErrorHint?.visibility = View.GONE
            }
            sendErrorHint?.visibility = View.GONE
        }
        return true

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

    fun bind(t: T) {
//        fun bind(songs: List<Song>) {
        setText(
            t.message.text,
            t.enableLinkify()
        )
        cbUserText?.visibility = View.GONE
        cbAiText?.visibility = View.GONE
        var aiFeedback: MessageDetail? = (t as? TextHolder)?.getAiFeedback();
        var feedbackText = aiFeedback?.feedback?.response ?: aiFeedback?.feedbackText ?: ""
        feedback?.let {
            if (!feedbackText.isEmpty() && isMultiSelectMode) {
                cbAiText?.visibility = View.VISIBLE
                cbAiText?.isChecked = t.isAiSelected
            }
            it.visibility = View.VISIBLE
            Markwon.create(it.context)
                .setMarkdown(it, feedbackText)
        }

//        var songs: List<Song> = aiFeedback?.feedback?.hymns ?: listOf()
        var songs: List<Song> = (t as? TextHolder)?.songs ?: listOf();
//        Log.d("sending", "MessageUpdated.songs:" + songs.size)
        songsContainer.setSongs(songs, isMultiSelectMode)
        songsContainer.setOnSongClickListener(object : SongsContainerView.OnSongClickListener {
            override fun onListenClick(song: Song) {
                onSongClickListener?.onListenClick(song)
            }

            override fun onSheetMusicClick(song: Song) {
                onSongClickListener?.onSheetMusicClick(song)
            }

            override fun onSongClick(song: Song) {
                onSongClickListener?.onSongClick(song)
            }
        })

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


        val threadHandler: GWThreadHandler = ChatSDK.thread() as GWThreadHandler
        var topic = threadHandler.getSessionName(t.message.threadId)
        if (topic != null) {
            sessionContainer?.visibility = View.VISIBLE
            sessionName?.let {
                it.text = topic
            }
        } else {
            sessionContainer?.visibility = View.GONE
        }

        bindSendStatus(t)
    }

    fun onBind(holder: T, isMultiSelectMode: Boolean, position: Int) {
        this.isMultiSelectMode = isMultiSelectMode
        holder.pos = position
        bindListeners(holder)
        bind(holder)
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
                        bindSendStatus(t)
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
                        Log.d("sending", "MessageUpdated:" + t.message.id)
                        bind(t)
                    }
                })


    }

    fun filterById(id: Long?): Predicate<NetworkEvent?> {
//        Log.e("AIExplore", "ExploreViewHolder.MessageUpdated:" + id)
        return Predicate { networkEvent: NetworkEvent? -> networkEvent?.message?.id == id }
    }

    open fun bindListeners(t: ExploreHolder) {
    }

    override fun accept(t: Throwable?) {
        t?.printStackTrace()
    }
}