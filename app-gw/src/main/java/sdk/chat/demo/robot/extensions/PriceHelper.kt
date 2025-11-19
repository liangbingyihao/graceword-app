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

//    for (i in 0 until this.pricingPhaseList.size) {
//        var phase = this.pricingPhaseList[i].toReadableString(context)
//        if (i > 0) {
//            phase = "${context.getString(R.string.then_prefix)} $phase"
//        }
//        phases.add(phase)
//    }


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

data class PlaceholderConfig(val placeholder: String, val day: Int)

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

