/*
 * Copyright (C) 2025 Proton AG
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.proton.android.core.payment.data

import android.content.Context
import ch.protonmail.android.mailsession.domain.repository.UserSessionRepository
import ch.protonmail.android.mailsession.domain.repository.getPrimarySession
import dagger.hilt.android.qualifiers.ApplicationContext
import me.proton.android.core.payment.data.extension.getErrorMessage
import me.proton.android.core.payment.data.extension.isForbiddenError
import me.proton.android.core.payment.data.extension.toObservabilityValue
import me.proton.android.core.payment.data.model.toProductOfferDetail
import me.proton.android.core.payment.data.model.toSubscriptionDetail
import me.proton.android.core.payment.domain.PaymentErrorCode
import me.proton.android.core.payment.domain.PaymentException
import me.proton.android.core.payment.domain.PaymentMetricsTracker
import me.proton.android.core.payment.domain.SubscriptionManager
import me.proton.android.core.payment.domain.model.PaymentObservabilityMetric.CREATE_SUBSCRIPTION
import me.proton.android.core.payment.domain.model.PaymentObservabilityMetric.GET_PAYMENTS_STATUS
import me.proton.android.core.payment.domain.model.PaymentObservabilityMetric.GET_PLANS
import me.proton.android.core.payment.domain.model.PaymentObservabilityMetric.GET_SUBSCRIPTION
import me.proton.android.core.payment.domain.model.PaymentObservabilityMetric.SEND_PAYMENT_TOKEN
import me.proton.android.core.payment.domain.model.ProductOfferDetail
import me.proton.android.core.payment.domain.model.Purchase
import me.proton.android.core.payment.domain.model.SubscriptionDetail
import uniffi.mail_uniffi.GetPaymentsPlansOptions
import uniffi.mail_uniffi.GoogleRecurringReceiptDetails
import uniffi.mail_uniffi.MailUserSession
import uniffi.mail_uniffi.MailUserSessionGetPaymentsPlansResult
import uniffi.mail_uniffi.MailUserSessionGetPaymentsStatusResult
import uniffi.mail_uniffi.MailUserSessionGetPaymentsSubscriptionResult
import uniffi.mail_uniffi.MailUserSessionPostPaymentsSubscriptionResult
import uniffi.mail_uniffi.MailUserSessionPostPaymentsTokensResult
import uniffi.mail_uniffi.NewSubscription
import uniffi.mail_uniffi.NewSubscriptionValues
import uniffi.mail_uniffi.PaymentReceipt
import uniffi.mail_uniffi.PaymentToken
import uniffi.mail_uniffi.PaymentVendorState
import uniffi.mail_uniffi.UserSessionError
import javax.inject.Inject
import javax.inject.Singleton

//region Constants

private const val VENDOR_ANDROID_GOOGLE: String = "google"

//endregion

@Singleton
class SubscriptionManagerRust @Inject constructor(
    @ApplicationContext private val context: Context,
    private val metricsTracker: PaymentMetricsTracker,
    private val sessionRepository: UserSessionRepository
) : SubscriptionManager {

    private fun UserSessionError.throwException(): Nothing {
        throw PaymentException(
            errorCode = PaymentErrorCode.DEVELOPER_ERROR.value,
            message = getErrorMessage(context)
        )
    }

    private suspend fun getProtonToken(
        product: ProductOfferDetail,
        purchase: Purchase,
        session: MailUserSession
    ): PaymentToken {
        val result = session.postPaymentsTokens(
            amount = product.offer.current.amount.toULong(),
            currency = product.offer.current.currency,
            payment = PaymentReceipt.Google(
                details = GoogleRecurringReceiptDetails(
                    orderId = purchase.orderId,
                    productId = purchase.productId,
                    customerId = purchase.customerId,
                    packageName = purchase.clientId,
                    token = purchase.receipt
                )
            )
        )

        metricsTracker.track(SEND_PAYMENT_TOKEN, result.toObservabilityValue())
        return when (result) {
            is MailUserSessionPostPaymentsTokensResult.Error -> result.v1.throwException()
            is MailUserSessionPostPaymentsTokensResult.Ok -> result.v1
        }
    }

    private suspend fun createOrUpdateSubscription(
        session: MailUserSession,
        product: ProductOfferDetail,
        paymentToken: PaymentToken
    ) {
        val result = session.postPaymentsSubscription(
            subscription = NewSubscription(
                cycle = product.offer.current.cycle.toUByte(),
                currency = product.offer.current.currency,
                currencyId = null,
                plans = mapOf(product.metadata.planName to 1),
                planIds = null,
                codes = null,
                couponCode = null,
                giftCode = null
            ),
            newValues = NewSubscriptionValues(
                amount = product.offer.current.amount.toULong(),
                payments = null,
                paymentToken = paymentToken.token
            )
        )

        metricsTracker.track(CREATE_SUBSCRIPTION, result.toObservabilityValue())
        when (result) {
            is MailUserSessionPostPaymentsSubscriptionResult.Error -> result.v1.throwException()
            is MailUserSessionPostPaymentsSubscriptionResult.Ok -> Unit
        }
    }

    override suspend fun getCurrent(): List<SubscriptionDetail> {
        val session = sessionRepository.getPrimarySession() ?: return emptyList()
        val result = session.getPaymentsSubscription()

        metricsTracker.track(GET_SUBSCRIPTION, result.toObservabilityValue())
        when (result) {
            is MailUserSessionGetPaymentsSubscriptionResult.Error -> {
                val error = result.v1

                when (error.isForbiddenError()) {
                    true -> throw PaymentException(
                        errorCode = PaymentErrorCode.FORBIDDEN.value,
                        message = error.getErrorMessage(context)
                    )
                    false -> error.throwException()
                }
            }
            is MailUserSessionGetPaymentsSubscriptionResult.Ok -> {
                return result.v1.current.map { it.toSubscriptionDetail() }
            }
        }
    }

    override suspend fun getAvailable(): List<ProductOfferDetail> {
        val session = sessionRepository.getPrimarySession() ?: return emptyList()
        val options = GetPaymentsPlansOptions(
            vendor = VENDOR_ANDROID_GOOGLE,
            currency = null,
            state = null,
            timestamp = null,
            fallback = null
        )
        val result = session.getPaymentsPlans(options)

        metricsTracker.track(GET_PLANS, result.toObservabilityValue())
        when (result) {
            is MailUserSessionGetPaymentsPlansResult.Error -> result.v1.throwException()
            is MailUserSessionGetPaymentsPlansResult.Ok -> {
                return result.v1.plans.map { plan ->
                    plan.instances.map { instance -> plan.toProductOfferDetail(instance) }
                }.flatten()
            }
        }
    }

    override suspend fun subscribe(product: ProductOfferDetail, purchase: Purchase) {
        val session = checkNotNull(sessionRepository.getPrimarySession())
        val protonToken = getProtonToken(product, purchase, session)
        createOrUpdateSubscription(session, product, protonToken)
    }

    override suspend fun areInAppPurchasesEnabled(): Boolean {
        val session = checkNotNull(sessionRepository.getPrimarySession())
        val result = session.getPaymentsStatus(VENDOR_ANDROID_GOOGLE)

        metricsTracker.track(GET_PAYMENTS_STATUS, result.toObservabilityValue())
        when (result) {
            is MailUserSessionGetPaymentsStatusResult.Error -> {
                result.v1.throwException()
            }
            is MailUserSessionGetPaymentsStatusResult.Ok -> {
                return result.v1.paymentMethods.inApp.state == PaymentVendorState.ENABLED
            }
        }
    }
}
