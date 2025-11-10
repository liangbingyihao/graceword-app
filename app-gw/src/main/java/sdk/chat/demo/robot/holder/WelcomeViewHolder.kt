package sdk.chat.demo.robot.holder

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import sdk.chat.demo.pre.R

open class WelcomeViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    open var replyText: MaterialButton? = itemView.findViewById(R.id.replyText)
    open var text: TextView? = itemView.findViewById(R.id.messageText)
    open var feedback: TextView? = itemView.findViewById(R.id.feedback)

    fun bind(h: WelcomeHolder) {
//        replyText?.text = h.question
//        text?.text = h.text
        feedback?.text = h.option.response
    }
}