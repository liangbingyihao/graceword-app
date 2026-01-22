package sdk.chat.demo.robot.adpter

import androidx.viewpager2.widget.ViewPager2
import android.os.Handler
import android.os.Looper

class SmartViewPagerHelper(
    private val viewPager: ViewPager2,
    private val adapter: ExpandableFragmentStateAdapter
) {

    private var isExtendingForward = false
    private var isExtendingBackward = false
    private val threshold = 1
    private val handler = Handler(Looper.getMainLooper())

    fun setup() {
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
                // 检测是否需要向前扩展
                checkForwardExtend(position)

                // 检测是否需要向后扩展
                checkBackwardExtend(position)
            }

            override fun onPageSelected(position: Int) {
                // 重置扩展标志
                isExtendingForward = false
                isExtendingBackward = false
            }
        })
    }

    private fun checkForwardExtend(currentPosition: Int) {
        val (minPos, maxPos) = adapter.getCurrentRange()

        // 如果接近右边界且还未扩展
        if (currentPosition >= maxPos - threshold && !isExtendingForward) {
            isExtendingForward = true
            extendForward(maxPos)
        }
    }

    private fun checkBackwardExtend(currentPosition: Int) {
        val (minPos, maxPos) = adapter.getCurrentRange()

        // 如果接近左边界且还未扩展
        if (currentPosition <= minPos + threshold && !isExtendingBackward) {
            isExtendingBackward = true
            extendBackward(minPos)
        }
    }

    private fun extendForward(currentMaxPos: Int) {
        val newMax = if (currentMaxPos + 2 < adapter.itemCount) currentMaxPos + 2 else adapter.itemCount - 1

        if (newMax > currentMaxPos) {
            handler.post {
                adapter.expandRange(maxPosition = newMax)
            }
        }
    }

    private fun extendBackward(currentMinPos: Int) {
        val newMin = if (currentMinPos - 2 > 0) currentMinPos - 2 else 0

        if (newMin < currentMinPos) {
            handler.post {
                adapter.expandRange(minPosition = newMin)
            }
        }
    }
}