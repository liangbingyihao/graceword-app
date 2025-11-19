package sdk.chat.demo.robot.utils

import java.util.concurrent.atomic.AtomicLong

object MemoryCounter {
    private val counters = mutableMapOf<String, AtomicLong>()

    // 增加计数
    fun increment(counterName: String, delta: Long = 1): Long {
        val counter = counters.getOrPut(counterName) { AtomicLong(0) }
        return counter.addAndGet(delta)
    }

    // 获取当前值
    fun getCount(counterName: String): Long {
        return counters[counterName]?.get() ?: 0
    }

    // 重置计数器
    fun reset(counterName: String): Long {
        val oldValue = counters[counterName]?.get() ?: 0
        counters[counterName] = AtomicLong(0)
        return oldValue
    }

    // 获取所有计数器状态
    fun getAllCounts(): Map<String, Long> {
        return counters.mapValues { it.value.get() }
    }

    // 删除计数器
    fun removeCounter(counterName: String): Boolean {
        return counters.remove(counterName) != null
    }

    // 清空所有计数器
    fun clearAll() {
        counters.clear()
    }

    // 获取计数器数量
    fun getCounterCount(): Int {
        return counters.size
    }
}