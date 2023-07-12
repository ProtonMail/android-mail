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

package ch.protonmail.android.uitest.e2e.mailbox.detail.authbadge

import ch.protonmail.android.uitest.robot.detail.section.MessageHeaderSection
import ch.protonmail.android.uitest.robot.detail.section.verify

internal interface AuthenticityBadgeDetailTests {

    fun MessageHeaderSection.verifyProtonSenderInMessageHeader(
        senderName: String = "Proton",
        shouldDisplayBadge: Boolean
    ) {
        verify {
            hasSenderName(senderName)
            hasAuthenticityBadge(shouldDisplayBadge)
        }

        expandHeader()

        verify {
            hasSenderName(senderName)
            hasAuthenticityBadge(shouldDisplayBadge)
        }
    }
}
