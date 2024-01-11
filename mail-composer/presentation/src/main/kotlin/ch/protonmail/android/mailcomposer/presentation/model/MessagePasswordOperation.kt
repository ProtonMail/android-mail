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

package ch.protonmail.android.mailcomposer.presentation.model

import ch.protonmail.android.mailcomposer.domain.model.MessagePassword

sealed interface MessagePasswordOperation {
    sealed interface Action : MessagePasswordOperation {
        data class ValidatePassword(val password: String) : Action
        data class ValidateRepeatedPassword(val password: String, val repeatedPassword: String) : Action
        data class ApplyPassword(val password: String, val passwordHint: String?) : Action
        data class UpdatePassword(val password: String, val passwordHint: String?) : Action
        object RemovePassword : Action
    }
    sealed interface Event : MessagePasswordOperation {
        data class InitializeScreen(val messagePassword: MessagePassword?) : Event
        data class PasswordValidated(val hasMessagePasswordError: Boolean) : Event
        data class RepeatedPasswordValidated(val hasRepeatedMessagePasswordError: Boolean) : Event
        object ExitScreen : Event
    }
}
