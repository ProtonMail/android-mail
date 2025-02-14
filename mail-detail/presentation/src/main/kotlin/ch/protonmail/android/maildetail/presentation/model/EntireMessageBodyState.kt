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

package ch.protonmail.android.maildetail.presentation.model

import android.net.Uri
import ch.protonmail.android.mailcommon.presentation.Effect

data class EntireMessageBodyState(
    val messageBodyState: MessageBodyState,
    val subject: String = "",
    val requestLinkConfirmation: Boolean = false,
    val requestPhishingLinkConfirmation: Boolean = false,
    val openMessageBodyLinkEffect: Effect<Uri> = Effect.empty()
) {

    companion object {

        val Initial = EntireMessageBodyState(
            messageBodyState = MessageBodyState.Loading,
            subject = "",
            requestLinkConfirmation = false,
            requestPhishingLinkConfirmation = false,
            openMessageBodyLinkEffect = Effect.empty()
        )
    }
}
