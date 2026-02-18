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

package ch.protonmail.android.testdata.composer

import uniffi.mail_uniffi.ComposerRecipient
import uniffi.mail_uniffi.ComposerRecipientGroup
import uniffi.mail_uniffi.ComposerRecipientSingle
import uniffi.mail_uniffi.ComposerRecipientValidState
import uniffi.mail_uniffi.PrivacyLock

object LocalComposerRecipientTestData {

    val Doe = build(
        address = "doe@pm.me",
        name = "Doe"
    )

    val Alice = build(
        address = "alice@pm.me",
        name = "Alice"
    )

    val Bob = build(
        address = "bob@pm.me",
        name = "Bob"
    )

    val Billing = build(
        address = "billing@company.com",
        name = "Billing Service"
    )

    val John = build(
        address = "john@pm.me",
        name = "John"
    )

    fun buildGroup(name: String, emails: List<String>) = ComposerRecipient.Group(
        ComposerRecipientGroup(
            displayName = name,
            recipients = emails.map { email ->
                ComposerRecipientSingle(email, email, ComposerRecipientValidState.Validating, null)
            },
            totalContactsInGroup = emails.size.toULong()
        )
    )

    private fun build(
        address: String,
        name: String,
        privacyLock: PrivacyLock? = null
    ) = ComposerRecipient.Single(
        ComposerRecipientSingle(name, address, ComposerRecipientValidState.Validating, privacyLock)
    )
}
