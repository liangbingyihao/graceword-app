package sdk.chat.demo.robot.api.model

import com.google.gson.annotations.SerializedName

// 经文章节模型
data class Product(
    @SerializedName("header_image")
    val headerImage: String,
    @SerializedName("privacy_agreement")
    val privacyAgreement: String,
    @SerializedName("term_of_service")
    val termOfService: String,
    val plans: List<Plan>
)


//{
//    "product_id": "com.graceword.sub.yearly",
//    "title": "{formattedPrice}/年",
//    "subtitle": "约 {priceAmountPerDay}/天",
//    "start_button": "立刻开始使用",
//    "promotion": "",
//    "offer_title": "7 天免费试用",
//    "offer_subtitle": "之后 {formattedPrice} 每年，只需 {priceAmountPerWeek}/周",
//    "offer_start_button": "开始免费试用",
//    "offer_promotion": "Save 25%"
//}

//priceCurrencyCode
//formattedPrice
//
//priceAmountPerDay = round(priceAmountMicros / 1000000 / 365, 2)
//priceAmountPerWeek = round(priceAmountMicros / 1000000 / 52, 2)
//priceAmountPerMonth = round(priceAmountMicros / 1000000 / 12, 2)
//
//取 offer 价格：
//offerFormattedPrice
//offerPriceAmountPerDay = round(priceAmountMicros / 1000000 / 365, 2)
//offerPriceAmountPerWeek = round(priceAmountMicros / 1000000 / 52, 2)
//offerPriceAmountPerMonth = round(priceAmountMicros / 1000000 / 12, 2)


// 经文章节模型
data class Plan(
    @SerializedName("product_id")
    val productId: String,
    val title: String,
    val subtitle: String,
    val promotion: String,

    @SerializedName("offer_title")
    val offerTitle: String,
    @SerializedName("offer_subtitle")
    val offerSubtitle:String,
    @SerializedName("offer_promotion")
    val offerPromotion: String,

    @SerializedName("offer_start_button")
    val offerStartButton:String,
)
