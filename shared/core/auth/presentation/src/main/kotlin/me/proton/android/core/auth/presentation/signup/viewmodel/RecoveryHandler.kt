/*
 * Copyright (c) 2022 Proton Technologies AG
 * This file is part of Proton Technologies AG and Proton Mail.
 *
 * Proton Mail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Mail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Mail. If not, see <https://www.gnu.org/licenses/>.
 */

package me.proton.android.core.auth.presentation.signup.viewmodel

import kotlinx.coroutines.flow.flow
import me.proton.android.core.auth.presentation.challenge.toUserBehavior
import me.proton.android.core.auth.presentation.signup.CreateRecoveryAction
import me.proton.android.core.auth.presentation.signup.CreateRecoveryAction.CreateRecoveryClosed
import me.proton.android.core.auth.presentation.signup.CreateRecoveryAction.DialogAction.CountryPicked
import me.proton.android.core.auth.presentation.signup.CreateRecoveryAction.DialogAction.CountryPickerClosed
import me.proton.android.core.auth.presentation.signup.CreateRecoveryAction.DialogAction.PickCountry
import me.proton.android.core.auth.presentation.signup.CreateRecoveryAction.DialogAction.RecoverySkipped
import me.proton.android.core.auth.presentation.signup.CreateRecoveryAction.DialogAction.WantSkipDialogClosed
import me.proton.android.core.auth.presentation.signup.CreateRecoveryAction.DialogAction.WantSkipRecovery
import me.proton.android.core.auth.presentation.signup.CreateRecoveryAction.SelectRecoveryMethod
import me.proton.android.core.auth.presentation.signup.CreateRecoveryAction.SubmitRecoveryEmail
import me.proton.android.core.auth.presentation.signup.CreateRecoveryAction.SubmitRecoveryPhone
import me.proton.android.core.auth.presentation.signup.CreateRecoveryState.Closed
import me.proton.android.core.auth.presentation.signup.CreateRecoveryState.CountryPickerFailed
import me.proton.android.core.auth.presentation.signup.CreateRecoveryState.Creating
import me.proton.android.core.auth.presentation.signup.CreateRecoveryState.Error
import me.proton.android.core.auth.presentation.signup.CreateRecoveryState.Idle
import me.proton.android.core.auth.presentation.signup.CreateRecoveryState.OnCountryPicked
import me.proton.android.core.auth.presentation.signup.CreateRecoveryState.SkipFailed
import me.proton.android.core.auth.presentation.signup.CreateRecoveryState.SkipSuccess
import me.proton.android.core.auth.presentation.signup.CreateRecoveryState.Success
import me.proton.android.core.auth.presentation.signup.CreateRecoveryState.ValidationError.Email
import me.proton.android.core.auth.presentation.signup.CreateRecoveryState.ValidationError.Phone
import me.proton.android.core.auth.presentation.signup.CreateRecoveryState.WantCountryPicker
import me.proton.android.core.auth.presentation.signup.CreateRecoveryState.WantSkip
import me.proton.android.core.auth.presentation.signup.RecoveryMethod
import me.proton.android.core.auth.presentation.signup.mapToNavigationRoute
import me.proton.android.core.auth.presentation.signup.ui.Country
import me.proton.core.challenge.domain.entity.ChallengeFrameDetails
import uniffi.mail_account_uniffi.SignupFlow
import uniffi.mail_account_uniffi.SignupFlowAvailableCountriesResult
import uniffi.mail_account_uniffi.SignupFlowSkipRecoveryResult
import uniffi.mail_account_uniffi.SignupFlowSubmitRecoveryEmailResult
import uniffi.mail_account_uniffi.SignupFlowSubmitRecoveryPhoneResult
import uniffi.mail_account_uniffi.Country as RustCountry

/**
 * Handler responsible for account recovery actions during signup process.
 */
class RecoveryHandler private constructor(
    private val getFlow: suspend () -> SignupFlow,
    private val getString: (resId: Int) -> String,
    private val getQuantityString: (resId: Int, quantity: Int, args: Int) -> String
) : ErrorHandler {

    @Volatile
    private var cachedCountries: List<Country>? = null

    @Volatile
    private var defaultCountry: Country? = null

    @Volatile
    private var selectedRecoveryMethod: RecoveryMethod = RecoveryMethod.Email

    @Volatile
    private var selectedCountry: Country? = null

    @Volatile
    private var recoveryFrameDetails: ChallengeFrameDetails? = null

    fun handleAction(action: CreateRecoveryAction) = when (action) {
        is SelectRecoveryMethod ->
            handleSelectRecoveryMethod(action.recoveryMethod, action.locale)

        is SubmitRecoveryEmail -> handleSubmitRecoveryEmail(
            email = action.email,
            recoveryFrameDetails = action.recoveryFrameDetails
        )

        is SubmitRecoveryPhone -> handleSubmitRecoveryPhone(
            callingCode = action.callingCode,
            phoneNumber = action.phoneNumber,
            recoveryFrameDetails = action.recoveryFrameDetails
        )

        is PickCountry -> handleCountryPicker()
        is CountryPicked -> handleCountryPicked(action.country)
        is CountryPickerClosed -> handleCountryDialogClose()

        is WantSkipRecovery -> handleWantSkipRecovery(action.recoveryFrameDetails)
        is RecoverySkipped -> handleRecoverySkipped()
        is WantSkipDialogClosed -> handleSkipDialogClose()

        is CreateRecoveryClosed -> handleClose(action.back)
    }

    private fun handleSelectRecoveryMethod(recoveryMethod: RecoveryMethod, localeFilter: String) = flow {
        if (recoveryMethod == RecoveryMethod.Phone) {
            if (cachedCountries.isNullOrEmpty()) {
                when (val result = getFlow().availableCountries(localeFilter)) {
                    is SignupFlowAvailableCountriesResult.Error -> {}
                    is SignupFlowAvailableCountriesResult.Ok -> {
                        cachedCountries = result.v1.countries.map { it.toCountry() }
                        defaultCountry = result.v1.defaultCountry?.toCountry()
                    }
                }

                selectedCountry = defaultCountry
            }
        }
        selectedRecoveryMethod = recoveryMethod
        emit(Idle(selectedRecoveryMethod, cachedCountries, selectedCountry))
    }

    private fun handleSubmitRecoveryEmail(email: String, recoveryFrameDetails: ChallengeFrameDetails) = flow {
        this@RecoveryHandler.recoveryFrameDetails = recoveryFrameDetails

        if (email.isBlank()) {
            emit(WantSkip(selectedRecoveryMethod))
            return@flow
        }

        emit(Creating(selectedRecoveryMethod))

        when (val result = getFlow().submitRecoveryEmail(email, recoveryFrameDetails.toUserBehavior())) {
            is SignupFlowSubmitRecoveryEmailResult.Error -> {
                emit(Email(message = result.v1.getErrorMessage(getString, getQuantityString)))
            }

            is SignupFlowSubmitRecoveryEmailResult.Ok -> {
                val route = result.v1.mapToNavigationRoute()
                emit(Success(selectedRecoveryMethod, email, route))
            }
        }
    }

    private fun handleSubmitRecoveryPhone(
        callingCode: String,
        phoneNumber: String,
        recoveryFrameDetails: ChallengeFrameDetails
    ) = flow {
        this@RecoveryHandler.recoveryFrameDetails = recoveryFrameDetails

        if (phoneNumber.isBlank()) {
            emit(WantSkip(selectedRecoveryMethod))
            return@flow
        }

        emit(Creating(selectedRecoveryMethod))

        val fullPhoneNumber = "$callingCode$phoneNumber"
        when (val result = getFlow().submitRecoveryPhone(fullPhoneNumber, recoveryFrameDetails.toUserBehavior())) {
            is SignupFlowSubmitRecoveryPhoneResult.Error -> {
                emit(Phone(message = result.v1.getErrorMessage(getString, getQuantityString)))
            }

            is SignupFlowSubmitRecoveryPhoneResult.Ok -> {
                val route = result.v1.mapToNavigationRoute()
                emit(Success(selectedRecoveryMethod, fullPhoneNumber, route))
            }
        }
    }

    private fun handleCountryPicker() = flow {
        emit(WantCountryPicker(selectedRecoveryMethod, cachedCountries ?: emptyList()))
    }

    private fun handleCountryPicked(country: Country) = flow {
        selectedCountry = country
        emit(OnCountryPicked(selectedRecoveryMethod, country))
    }

    private fun handleWantSkipRecovery(recoveryFrameDetails: ChallengeFrameDetails) = flow {
        this@RecoveryHandler.recoveryFrameDetails = recoveryFrameDetails
        emit(WantSkip(selectedRecoveryMethod))
    }

    private fun handleRecoverySkipped() = flow {
        when (val result = getFlow().skipRecovery(recoveryFrameDetails?.toUserBehavior())) {
            is SignupFlowSkipRecoveryResult.Error -> {
                emit(
                    Error(
                        recoveryMethod = selectedRecoveryMethod,
                        message = result.v1.getErrorMessage(getString, getQuantityString)
                    )
                )
            }

            is SignupFlowSkipRecoveryResult.Ok -> {
                val route = result.v1.mapToNavigationRoute()
                emit(SkipSuccess(recoveryMethod = selectedRecoveryMethod, route = route))
            }
        }
    }

    private fun handleSkipDialogClose() = flow {
        emit(SkipFailed(recoveryMethod = selectedRecoveryMethod))
    }

    private fun handleCountryDialogClose() = flow {
        emit(
            CountryPickerFailed(
                recoveryMethod = selectedRecoveryMethod,
                country = selectedCountry
            )
        )
    }

    private fun handleClose(back: Boolean) = flow {
        if (back) {
            getFlow().stepBack()
        }
        emit(Closed)
    }

    override fun handleError(throwable: Throwable) =
        Error(recoveryMethod = selectedRecoveryMethod, message = throwable.message)

    companion object {

        fun create(
            getFlow: suspend () -> SignupFlow,
            getString: (resId: Int) -> String,
            getQuantityString: (resId: Int, quantity: Int, args: Int) -> String
        ): RecoveryHandler = RecoveryHandler(getFlow, getString, getQuantityString)
    }
}

fun RustCountry.toCountry() = Country(
    countryCode = countryCode,
    callingCode = phoneCode.toInt(),
    name = countryEn
)
