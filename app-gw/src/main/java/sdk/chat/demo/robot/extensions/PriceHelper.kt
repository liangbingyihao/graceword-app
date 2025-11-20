package sdk.chat.demo.robot.extensions

import android.content.Context
import android.util.Log
import com.android.billingclient.api.ProductDetails.PricingPhase
import com.android.billingclient.api.ProductDetails.PricingPhases
import sdk.chat.demo.pre.R
import sdk.chat.demo.robot.api.model.Plan
import kotlin.collections.listOf

/**
 * PricingPhases的扩展函数
 */
fun PricingPhases.toPriceInfo(context: Context): List<String> {
    val phases = mutableListOf<String>()

    for (i in 0 until this.pricingPhaseList.size) {
        var phase = this.pricingPhaseList[i].toReadableString(context)
        if (i > 0) {
            phase = "${context.getString(R.string.then_prefix)} $phase"
        }
        phases.add(phase)
    }


    return phases
}

fun PricingPhases.toPriceInfo(product: Plan): List<String> {
    //this.pricingPhaseList 0:offer,1:base。或0:base
    if (this.pricingPhaseList.size == 1) {
        return this.pricingPhaseList[0].toReadableString(product, false)
    } else if (this.pricingPhaseList.size > 1) {
        return this.pricingPhaseList[0].toReadableString(product, true)
    }

    return emptyList()
}

fun PricingPhase.toReadableString(
    product: Plan,
    isOffer: Boolean
): MutableList<String> {
    val phases = mutableListOf<String>()
    if (!isOffer) {
        phases.add(product.title.toMeaningfulStr(this))
        phases.add(product.subtitle.toMeaningfulStr(this))
    } else {
        phases.add(product.offerTitle.toMeaningfulStr(this))
        phases.add(product.offerSubtitle.toMeaningfulStr(this))
    }
    return phases
}

data class PlaceholderConfig(
    val placeholder: String,
    val day: Int,
    var pricePhase: PricingPhase? = null
)


fun Plan.toMeaningfulStr(pricingPhases: PricingPhases): List<String> {
    val phases = mutableListOf<String>()
    if (pricingPhases.pricingPhaseList.size == 1) {
        //no offer
        phases.add(this.title.getPriceTitle(pricingPhases.pricingPhaseList[0]))
        phases.add(this.subtitle.getPriceTitle(pricingPhases.pricingPhaseList[0]))
    } else if (pricingPhases.pricingPhaseList.size > 1) {
        phases.add(
            this.offerTitle.getPriceTitle(
                pricingPhases.pricingPhaseList[1],
                pricingPhases.pricingPhaseList[0]
            )
        )
        phases.add(
            this.offerSubtitle.getPriceTitle(
                pricingPhases.pricingPhaseList[1],
                pricingPhases.pricingPhaseList[0]
            )
        )
    }
    return phases
}

fun String.getPriceTitle(mainPrice: PricingPhase, offerPrice: PricingPhase? = null): String {
//    var currency = mainPrice.formattedPrice.replace(Regex("[0-9.]"), "")
//    var tmp = this.replace("{formattedPrice}", mainPrice.formattedPrice)
//    if (offerPrice != null) {
//        tmp = tmp.replace("{offerFormattedPrice}", offerPrice.formattedPrice)
//    }
//
////    var price = pricingPhase.priceAmountMicros / 1000000.0
//    listOf(
//        PlaceholderConfig("{priceAmountPerDay}", 365, mainPrice),
//        PlaceholderConfig("{priceAmountPerWeek}", 52, mainPrice),
//        PlaceholderConfig("{priceAmountPerMonth}", 12, mainPrice),
//        PlaceholderConfig("{offerPriceAmountPerDay}", 365, offerPrice),
//        PlaceholderConfig("{offerPriceAmountPerWeek}", 52, offerPrice),
//        PlaceholderConfig("{offerPriceAmountPerMonth}", 12, offerPrice),
//    ).forEach { config ->
//        if (config.pricePhase != null) {
//            var p = currency + "%.2f".format(
//                config.pricePhase!!.priceAmountMicros / 1000000.0 / config.day,
//                2
//            )
//            tmp = tmp.replace(config.placeholder, p)
//        }
//    }
//
//
//    val input =
//        "商品价格: {price/3}, 商品价格2: {price/6}, 优惠价: {offerPrice/2}, 优惠价2: {offerPrice/3}"

    var currency = mainPrice.formattedPrice.replace(Regex("[0-9.]"), "")
    var dMainPrice = mainPrice.priceAmountMicros / 1000000.0
    var dOfferPrice: Double = 0.0
    if (offerPrice != null) {
        dOfferPrice = offerPrice.priceAmountMicros / 1000000.0
    }

    val result = replaceTemplates(this) { key, value ->
        // 根据不同的key计算具体值
        when (key) {
            "price" -> currency+"%.2f".format(dMainPrice / value.toDouble(), 2)
            "offerPrice" -> currency+"%.2f".format(dOfferPrice / value.toDouble(), 2)  // 优惠价计算逻辑
            else -> "?"  // 默认处理
        }
    }

    return result
}

fun replaceTemplates(input: String, calculator: (String, Int) -> String): String {
    val regex = """\{([^}/]+)/(\d+)\}""".toRegex()

    return regex.replace(input) { matchResult ->
        val (key, valueStr) = matchResult.destructured
        val value = valueStr.toInt()
        val calculatedValue = calculator(key, value)
        calculatedValue.toString()
    }
}

fun String.getPriceTitle1(mainPrice: PricingPhase, offerPrice: PricingPhase? = null): String {
    var currency = mainPrice.formattedPrice.replace(Regex("[0-9.]"), "")
    var tmp = this.replace("{formattedPrice}", mainPrice.formattedPrice)
    if (offerPrice != null) {
        tmp = tmp.replace("{offerFormattedPrice}", offerPrice.formattedPrice)
    }

//    var price = pricingPhase.priceAmountMicros / 1000000.0
    listOf(
        PlaceholderConfig("{priceAmountPerDay}", 365, mainPrice),
        PlaceholderConfig("{priceAmountPerWeek}", 52, mainPrice),
        PlaceholderConfig("{priceAmountPerMonth}", 12, mainPrice),
        PlaceholderConfig("{offerPriceAmountPerDay}", 365, offerPrice),
        PlaceholderConfig("{offerPriceAmountPerWeek}", 52, offerPrice),
        PlaceholderConfig("{offerPriceAmountPerMonth}", 12, offerPrice),
    ).forEach { config ->
        if (config.pricePhase != null) {
            var p = currency + "%.2f".format(
                config.pricePhase!!.priceAmountMicros / 1000000.0 / config.day,
                2
            )
            tmp = tmp.replace(config.placeholder, p)
        }
    }

    return tmp
}

fun String.toMeaningfulStr(pricingPhase: PricingPhase): String {
    var currency = pricingPhase.formattedPrice.replace(Regex("[0-9.]"), "")
    var tmp = this.replace("{formattedPrice}", pricingPhase.formattedPrice)
    tmp = tmp.replace("{formattedPrice}", pricingPhase.formattedPrice)

    var price = pricingPhase.priceAmountMicros / 1000000.0
    listOf(
        PlaceholderConfig("{priceAmountPerDay}", 365),
        PlaceholderConfig("{offerPriceAmountPerDay}", 365),
        PlaceholderConfig("{priceAmountPerWeek}", 52),
        PlaceholderConfig("{offerPriceAmountPerWeek}", 52),
        PlaceholderConfig("{priceAmountPerMonth}", 12),
        PlaceholderConfig("{offerPriceAmountPerMonth}", 12),
    ).forEach { config ->
        var p = currency + "%.2f".format(price / config.day, 2)
        tmp = tmp.replace(config.placeholder, p)
    }

    return tmp
}

fun PricingPhase.toReadableString(context: Context): String {
    return when {
        priceAmountMicros == 0L -> {
            val period = billingPeriod.toLocalizedPeriod(context, true)
            if (period.isNotEmpty()) {
                "$period ${context.getString(R.string.free_trial)}"
            } else {
                context.getString(R.string.free_trial)
            }
        }

        else -> {
            val period = billingPeriod.toLocalizedPeriod(context)
            if (period.isNotEmpty()) {
                context.getString(R.string.price_format, formattedPrice, period)
            } else {
                formattedPrice
            }
        }
    }
}

fun String.toLocalizedPeriod(context: Context, pre: Boolean = false): String {
    return when {
        contains("P1D") -> context.getString(R.string.period_day)
        contains("P7D") || contains("P1W") -> {
            if (pre) {
                context.getString(R.string.period_7_days)
            } else {
                context.getString(R.string.period_week)
            }
        }

        contains("P1M") -> context.getString(R.string.period_month)
        contains("P3M") -> context.getString(R.string.period_quarter)
        contains("P6M") -> context.getString(R.string.period_half_year)
        contains("P1Y") -> context.getString(R.string.period_year)
        else -> ""
    }
}

