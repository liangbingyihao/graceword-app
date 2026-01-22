package sdk.chat.demo.robot.fragments
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import sdk.chat.demo.pre.R

class PlaceholderFragment : Fragment() {

    companion object {
        private const val ARG_POSITION = "position"

        fun newInstance(position: Int): PlaceholderFragment {
            val fragment = PlaceholderFragment()
            val args = Bundle()
            args.putInt(ARG_POSITION, position)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // 返回一个简单的加载视图
        return inflater.inflate(R.layout.fragment_placeholder, container, false)
    }

    override fun onResume() {
        super.onResume()
        // 当PlaceholderFragment变为可见时，尝试替换为真实Fragment
        val position = arguments?.getInt(ARG_POSITION, 0) ?: 0
        (parentFragment as? FragmentPlaceholderCallback)?.onPlaceholderBecomeVisible(position)
    }
}

interface FragmentPlaceholderCallback {
    fun onPlaceholderBecomeVisible(position: Int)
}