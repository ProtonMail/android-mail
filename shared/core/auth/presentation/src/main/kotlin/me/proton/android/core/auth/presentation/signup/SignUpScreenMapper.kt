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

package me.proton.android.core.auth.presentation.signup

import me.proton.android.core.auth.presentation.LogTag
import me.proton.core.util.kotlin.CoreLogger
import uniffi.mail_account_uniffi.SimpleSignupState

fun SimpleSignupState.mapToNavigationRoute(): String {
    return when (this) {
        SimpleSignupState.WANT_USERNAME -> SignUpRoutes.Route.CreateUsername
        SimpleSignupState.WANT_PASSWORD -> SignUpRoutes.Route.CreatePassword
        SimpleSignupState.WANT_RECOVERY -> SignUpRoutes.Route.CreateRecovery
        SimpleSignupState.WANT_CREATE -> SignUpRoutes.Route.SignUpCreateUser
        SimpleSignupState.COMPLETE -> SignUpRoutes.Route.SignUpCongrats
        SimpleSignupState.INVALID -> {
            CoreLogger.e(LogTag.SIGNUP, "Received invalid state")
            throw IllegalStateException("Received invalid state from Rust.")
        }
    }.route
}
