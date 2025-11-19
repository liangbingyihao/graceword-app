package sdk.chat.demo.robot.api.model

import com.google.gson.annotations.SerializedName

data class GWConfigs(
    @SerializedName("configs")
    val configs: GWConfigItem? = null,

    @SerializedName("default_msg")
    val defaultMsg: MessageDetail? = null,

    @SerializedName("text_to_speech_voices")
    val dbVoiceTypes: List<DBVoiceType>? = null,

    @SerializedName("tts_voice")
    val defaultVoiceTypes: Map<String, String>? = null,

    @SerializedName("stt_config")
    val voiceBaseConfigs: VoiceBaseConfigs? = null,

    @SerializedName("hymns_params")
    val hymnsParams: List<HymnParam>? = null,

    @SerializedName("welcome_survey")
    val welcomeSurvey: WelcomeSurvey? = null,
    val billingInfo: BillingInfo? = null

) {

    data class GWConfigItem(
        @SerializedName("generating_hint")
        val generatingHint: List<String>? = null
    )

    data class VoiceBaseConfigs(
        val token: String? = null,

        @SerializedName("app_id")
        val appId: String? = null
    )

    data class DBVoiceType(
        val name: String? = null,

        @SerializedName("voice_type")
        val voiceType: String? = null
    )

    data class HymnParam(
        val field: String? = null,
        val choices: List<String>? = null
    )

    data class WelcomeSurvey(
        val question: String? = null,
        val title: String? = null,
        val options: List<WelcomeSurveyOption>? = null
    ) {
        fun findOptionByValue(value: String?): WelcomeSurveyOption? {
            if (value == null) return null
            return options?.find { it.value == value }
        }
    }

    data class WelcomeSurveyOption(
        val value: String? = null,
        val prompts: List<String>? = null,
        val response: String? = null
    )

    data class BillingInfo(
        val productSubscriptions: List<String>? = null
    )
}
