package sdk.chat.demo.robot.api.model

data class ActionConfig(
    val actionName: String,
    val dailyLimit: Int
) {
    companion object {
        // 定义常量，方便外部通过变量名访问
        const val DAILY_MSG = "msg"
        const val DAILY_PIC = "pic"

        // 默认配置
        val DEFAULT_CONFIGS = listOf(
            ActionConfig(DAILY_MSG, 5),
            ActionConfig(DAILY_PIC, 1),
        )
    }
}

// 配置管理器
object ActionLimitConfig {
    // 内存中的配置映射
    private var actionLimits: Map<String, Int> = emptyMap()
    private var actionConfigs: Map<String, ActionConfig> = emptyMap()

    // 初始化默认配置
    init {
        loadDefaultConfigs()
    }

    // 加载默认配置
    fun loadDefaultConfigs() {
        actionConfigs = ActionConfig.DEFAULT_CONFIGS.associateBy { it.actionName }
        actionLimits = ActionConfig.DEFAULT_CONFIGS.associate { it.actionName to it.dailyLimit }
    }

    // 从外部配置加载（JSON、XML等）
    fun loadFromList(configs: List<ActionConfig>) {
        actionConfigs = configs.associateBy { it.actionName }
        actionLimits = configs.associate { it.actionName to it.dailyLimit }
    }

    // 动态添加配置
    fun addConfig(config: ActionConfig) {
        val newConfigs = actionConfigs.toMutableMap()
        newConfigs[config.actionName] = config
        actionConfigs = newConfigs

        val newLimits = actionLimits.toMutableMap()
        newLimits[config.actionName] = config.dailyLimit
        actionLimits = newLimits
    }

    // 更新配置
    fun updateConfig(actionName: String, newLimit: Int) {
        val config = actionConfigs[actionName] ?: return
        val updatedConfig = config.copy(dailyLimit = newLimit)
        addConfig(updatedConfig)
    }

    // 获取配置
    fun getLimit(actionName: String): Int {
        return actionLimits[actionName] ?: 0
    }

    fun getConfig(actionName: String): ActionConfig? {
        return actionConfigs[actionName]
    }

    fun getAllConfigs(): List<ActionConfig> {
        return actionConfigs.values.toList()
    }

    fun getAllLimits(): Map<String, Int> {
        return actionLimits
    }

    // 检查动作是否存在
    fun containsAction(actionName: String): Boolean {
        return actionLimits.containsKey(actionName)
    }

}