package sdk.chat.demo.robot.holder

import sdk.chat.core.dao.Message
import sdk.chat.core.types.MessageType
import java.util.concurrent.ConcurrentHashMap

object HolderProvider {
    public const val  GWMessageType: Int = 777
    private val messageHolders = ConcurrentHashMap<Message, MessageHolder>()

    fun getMessageHolder(message: Message?): MessageHolder? {
        if (message == null) return null

        messageHolders[message]?.let { return it }

        val holder = when {
            message.typeIs(MessageType.Text) -> TextHolder(message)
            message.typeIs(MessageType.Image) || message.typeIs(GWMessageType) -> ImageHolder(message)
            else -> null
        }

        holder?.let { messageHolders[message] = it }
        return holder

//        return messageHolders.getOrPut(message) {
//            if (message.typeIs(MessageType.Text)) {
//                return TextHolder(message)
//            }else if (message.typeIs(MessageType.Image)|| message.typeIs(GWMessageType)) {
//                return ImageHolder(message)
//            }else{
//                return null
//            }
////            ChatSDKUI.shared().messageRegistrationManager.onNewMessageHolder(message)
//        }
    }

    fun getExitsMessageHolder(message: Message?): MessageHolder? {
        return message?.let { messageHolders[it] }
    }

    fun removeMessageHolder(message: Message?) {
        message?.let { messageHolders.remove(it) }
    }

    fun clear() {
        messageHolders.clear()
    }
}