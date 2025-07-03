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

package ch.protonmail.android.mailcomposer.presentation.usecase

import android.content.Context
import ch.protonmail.android.mailcomposer.presentation.R
import ch.protonmail.android.mailmessage.domain.model.SendingError
import dagger.hilt.android.qualifiers.ApplicationContext
import me.proton.core.util.kotlin.takeIfNotEmpty
import javax.inject.Inject

class FormatMessageSendingError @Inject constructor(
    @ApplicationContext private val context: Context
) {

    operator fun invoke(sendingError: SendingError): String? {

        val formattedRecipients = when (sendingError) {
            is SendingError.SendPreferences -> {
                sendingError.errors.toList().mapNotNull { (email, error) ->
                    when (error) {
                        SendingError.SendPreferencesError.TrustedKeysInvalid -> {
                            context.getString(R.string.message_sending_error_dialog_text_reason_no_trusted_keys)
                                .format(
                                    email
                                )
                        }

                        SendingError.SendPreferencesError.AddressDisabled -> {
                            context.getString(R.string.message_sending_error_dialog_text_reason_address_disabled)
                                .format(
                                    email
                                )
                        }

                        else -> null
                    }
                }
            }

            SendingError.Other -> emptyList()
            is SendingError.ExternalAddressSendDisabled -> emptyList()
            SendingError.MessageAlreadySent -> emptyList()
            is SendingError.GenericLocalized -> emptyList()
        }

        return formattedRecipients.takeIfNotEmpty()?.joinToString(separator = "\n\n")
    }
}
