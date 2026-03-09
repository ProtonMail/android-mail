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

import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import me.proton.android.core.auth.presentation.R
import me.proton.android.core.auth.presentation.challenge.toUserBehavior
import me.proton.android.core.auth.presentation.signup.CreateUsernameAction
import me.proton.android.core.auth.presentation.signup.CreateUsernameAction.CreateExternalAccount
import me.proton.android.core.auth.presentation.signup.CreateUsernameAction.CreateInternalAccount
import me.proton.android.core.auth.presentation.signup.CreateUsernameAction.CreateUsernameClosed
import me.proton.android.core.auth.presentation.signup.CreateUsernameAction.LoadData
import me.proton.android.core.auth.presentation.signup.CreateUsernameAction.Perform
import me.proton.android.core.auth.presentation.signup.CreateUsernameState
import me.proton.android.core.auth.presentation.signup.CreateUsernameState.Closed
import me.proton.android.core.auth.presentation.signup.CreateUsernameState.Creating
import me.proton.android.core.auth.presentation.signup.CreateUsernameState.Error
import me.proton.android.core.auth.presentation.signup.CreateUsernameState.LoadingComplete
import me.proton.android.core.auth.presentation.signup.CreateUsernameState.Success
import me.proton.android.core.auth.presentation.signup.CreateUsernameState.ValidationError.EmailEmpty
import me.proton.android.core.auth.presentation.signup.CreateUsernameState.ValidationError.InternalUsernameEmpty
import me.proton.android.core.auth.presentation.signup.CreateUsernameState.ValidationError.Other
import me.proton.android.core.auth.presentation.signup.CreateUsernameState.ValidationError.UsernameEmpty
import me.proton.android.core.auth.presentation.signup.SignUpState
import me.proton.android.core.auth.presentation.signup.ValidationField
import me.proton.android.core.auth.presentation.signup.mapToNavigationRoute
import me.proton.core.account.domain.entity.AccountType
import me.proton.core.challenge.domain.entity.ChallengeFrameDetails
import uniffi.mail_account_uniffi.SignupException
import uniffi.mail_account_uniffi.SignupFlow
import uniffi.mail_account_uniffi.SignupFlowAvailableDomainsResult
import uniffi.mail_account_uniffi.SignupFlowSubmitExternalUsernameResult
import uniffi.mail_account_uniffi.SignupFlowSubmitInternalUsernameResult

/**
 * Handler responsible for username-related actions during signup process.
 */
class UsernameHandler private constructor(
    private val getFlow: suspend () -> SignupFlow,
    private val getCurrentAccountType: () -> AccountType,
    private val getString: (resId: Int) -> String,
    private val getQuantityString: (resId: Int, quantity: Int, args: Int) -> String,
    private val updateAccountType: (AccountType) -> Unit
) : ErrorHandler {

    fun handleAction(action: CreateUsernameAction) = when (action) {
        is LoadData -> handleUsernameLoad(action.accountType)
        is CreateExternalAccount -> handleCreateExternalAccount()
        is CreateInternalAccount -> handleCreateInternalAccount()
        is Perform -> handleUsernameSubmit(
            accountType = action.accountType,
            username = action.value,
            domain = action.domain,
            usernameFrameDetails = action.usernameFrameDetails
        )

        is CreateUsernameClosed -> handleClose(action.back)
    }

    private fun handleUsernameLoad(accountType: AccountType) = flow {
        emit(CreateUsernameState.Idle(accountType = accountType, isLoading = true))
        emitAll(
            when (accountType) {
                AccountType.Internal -> handleCreateInternalAccount()
                AccountType.External -> handleCreateExternalAccount()
                AccountType.Username -> handleCreateUsernameAccount()
            }
        )
    }

    private fun handleCreateUsernameAccount() = flow {
        updateAccountType(AccountType.External)
        emit(LoadingComplete(AccountType.Username))
    }

    private fun handleCreateInternalAccount() = flow {
        updateAccountType(AccountType.Internal)
        when (val result = getFlow().availableDomains()) {
            is SignupFlowAvailableDomainsResult.Error -> Error(
                accountType = getCurrentAccountType(),
                isLoading = false,
                message = result.v1.getErrorMessage(getString, getQuantityString)
            )

            is SignupFlowAvailableDomainsResult.Ok -> emit(LoadingComplete(AccountType.Internal, domains = result.v1))
        }
    }

    private fun handleCreateExternalAccount() = flow {
        updateAccountType(AccountType.External)
        emit(LoadingComplete(AccountType.External))
    }

    @Suppress("ForbiddenComment")
    private fun handleUsernameSubmit(
        accountType: AccountType,
        username: String,
        domain: String?,
        usernameFrameDetails: ChallengeFrameDetails
    ) = flow {
        emit(Creating(accountType, isLoading = true))

        val result = when (accountType) {
            AccountType.Username, AccountType.Internal -> { // currently no difference between username and internal
                requireNotNull(domain) { "Domain must be set for Internal Account type." }
                if (username.isEmpty()) {
                    emit(InternalUsernameEmpty)
                    return@flow
                }
                handleInternalUsernameSubmission(accountType, username, domain, usernameFrameDetails)
            }

            AccountType.External -> {
                if (username.isEmpty()) {
                    emit(EmailEmpty)
                    return@flow
                }
                handleExternalUsernameSubmission(accountType, username, usernameFrameDetails)
            }
        }

        emit(result)
    }

    private suspend fun handleInternalUsernameSubmission(
        accountType: AccountType,
        username: String,
        domain: String,
        usernameFrameDetails: ChallengeFrameDetails
    ): CreateUsernameState {
        return when (
            val result = getFlow().submitInternalUsername(
                username = username,
                domain = domain,
                userBehavior = usernameFrameDetails.toUserBehavior()
            )
        ) {
            is SignupFlowSubmitInternalUsernameResult.Error -> {
                when (val signupException = result.v1) {
                    is SignupException.UsernameUnavailable -> {
                        if (signupException.v1 != null) {
                            Other(
                                accountType = accountType,
                                field = ValidationField.USERNAME,
                                message = signupException.getErrorMessage(getString, getQuantityString)
                            )
                        } else {
                            Error(
                                accountType = accountType,
                                isLoading = false,
                                message = getString(R.string.common_error_something_went_wrong)
                            )
                        }
                    }

                    else -> Error(
                        accountType = accountType,
                        isLoading = false,
                        message = signupException.getErrorMessage(getString, getQuantityString)
                    )
                }
            }

            is SignupFlowSubmitInternalUsernameResult.Ok ->
                Success(
                    accountType = accountType,
                    username = username,
                    domain = domain,
                    route = result.v1.mapToNavigationRoute()
                )
        }
    }

    private suspend fun handleExternalUsernameSubmission(
        accountType: AccountType,
        email: String,
        usernameFrameDetails: ChallengeFrameDetails
    ): CreateUsernameState {
        return when (
            val result = getFlow().submitExternalUsername(
                email = email,
                userBehavior = usernameFrameDetails.toUserBehavior()
            )
        ) {
            is SignupFlowSubmitExternalUsernameResult.Error -> {
                when (result.v1) {
                    is SignupException.UsernameUnavailable ->
                        Other(
                            accountType = accountType,
                            field = ValidationField.EMAIL,
                            message = result.v1.getErrorMessage(getString, getQuantityString)
                        )

                    is SignupException.UsernameEmpty -> UsernameEmpty

                    else -> Error(
                        accountType = accountType,
                        message = result.v1.getErrorMessage(getString, getQuantityString)
                    )
                }
            }

            is SignupFlowSubmitExternalUsernameResult.Ok ->
                Success(
                    accountType = accountType,
                    username = email,
                    route = result.v1.mapToNavigationRoute()
                )
        }
    }

    private fun handleClose(back: Boolean) = flow {
        if (back) {
            getFlow().stepBack()
        }
        emit(Closed(getCurrentAccountType()))
    }

    override fun handleError(throwable: Throwable): SignUpState =
        Error(accountType = getCurrentAccountType(), message = throwable.message)

    companion object {

        fun create(
            getFlow: suspend () -> SignupFlow,
            getCurrentAccountType: () -> AccountType,
            getString: (resId: Int) -> String,
            getQuantityString: (resId: Int, quantity: Int, args: Int) -> String,
            updateAccountType: (AccountType) -> Unit
        ): UsernameHandler =
            UsernameHandler(getFlow, getCurrentAccountType, getString, getQuantityString, updateAccountType)
    }
}
