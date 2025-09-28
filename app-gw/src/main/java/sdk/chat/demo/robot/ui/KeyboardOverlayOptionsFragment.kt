package sdk.chat.demo.robot.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import sdk.chat.core.interfaces.ChatOption
import sdk.chat.core.session.ChatSDK
import sdk.chat.core.ui.AbstractKeyboardOverlayFragment
import sdk.chat.core.ui.Sendable
import sdk.chat.demo.pre.R
import smartadapter.SmartRecyclerAdapter
import smartadapter.viewevent.listener.OnClickEventListener

open class KeyboardOverlayOptionsFragment(): AbstractKeyboardOverlayFragment() {

    companion object {
        const val key = "options"
    }

    open lateinit var smartRecyclerAdapter: SmartRecyclerAdapter
    open lateinit var recyclerView: RecyclerView
    open lateinit var rootView: View

    open var screenWidth: Int = 0
    open var screenHeight: Int = 0

    open var itemHeight: Int? = null
    open var itemWidth: Int? = null

    open fun getLayout(): Int {
        return R.layout.fragment_smart_recycler
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        rootView = inflater.inflate(getLayout(), container, false)

        recyclerView = rootView.findViewById(R.id.recyclerView)

//        smartRecyclerAdapter = SmartRecyclerAdapter
//            .items(items())
//            .map(ChatOptionModel::class, ChatOptionViewHolder::class)
//            .add(OnClickEventListener {
//
//                var model = smartRecyclerAdapter.getItem(it.position)
//                if (model is ChatOptionModel) {
//                    model.click()
//                }
//            })
//            .into(recyclerView)

        if (isPortrait) {
            recyclerView.layoutManager = GridLayoutManager(context, 3, RecyclerView.VERTICAL, false)
        } else {
            recyclerView.layoutManager = GridLayoutManager(context, 2, RecyclerView.HORIZONTAL, false)
        }

        return rootView
    }

    override fun onStart() {
        super.onStart()
    }

    override fun onResume() {
        super.onResume()
    }

    override fun key(): String {
        return key
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        updateViewLayout()
    }

    open fun updateViewLayout() {
        if (::smartRecyclerAdapter.isInitialized) {

            if (isPortrait) {
                itemWidth = null
                itemHeight = screenWidth / 3 + 30
            } else {
                itemHeight = (screenHeight - 70) / 2
                itemWidth = (screenHeight - 70) / 2 - 40
//                itemHeight = null;
//                itemWidth = screenHeight / 2
            }

            if (::smartRecyclerAdapter.isInitialized) {
                smartRecyclerAdapter.setItems(items())
            }

        }
    }

    override fun setViewSize(width: Int, height: Int, context: Context) {
        screenWidth = width
        screenHeight = height
//        context.let {
            updateViewLayout()
//        }
    }

    open fun items(): MutableList<Any> {
        var items = arrayListOf<Any>()

//        // Get the items
//        val options = ChatSDK.ui().chatOptions
//        for (option in options) {
//            items.add(ChatOptionModel(resources.getString(option.title), option.image, itemWidth, itemHeight, Runnable {
//                this.executeOption(option)
//            }))
//        }

        return items
    }

    open fun executeOption(option: ChatOption) {
        if (option.hasOverlay()) {
            keyboardOverlayHandler.get()?.showOverlay(option.getOverlay(keyboardOverlayHandler.get()))
        } else {
            keyboardOverlayHandler.get()?.send(Sendable { activity, launcher, thread ->
                option.execute(activity, launcher, thread)
            })
        }
    }

}