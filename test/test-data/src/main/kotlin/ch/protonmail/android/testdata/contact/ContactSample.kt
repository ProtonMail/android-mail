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

package ch.protonmail.android.testdata.contact

import ch.protonmail.android.testdata.user.UserIdTestData
import me.proton.core.contact.domain.entity.Contact

object ContactSample {

    val Doe = Contact(
        contactEmails = emptyList(),
        id = ContactIdSample.Doe,
        name = "Doe",
        userId = UserIdTestData.Primary
    )

    val John = Contact(
        contactEmails = emptyList(),
        id = ContactIdSample.John,
        name = "John",
        userId = UserIdTestData.Primary
    )

    val Mario = Contact(
        contactEmails = listOf(
            ContactEmailSample.contactEmail1,
            ContactEmailSample.contactEmail2
        ),
        id = ContactIdSample.Mario,
        name = "Mario",
        userId = UserIdTestData.Primary
    )

    val Stefano = Contact(
        contactEmails = listOf(ContactEmailSample.contactEmail3),
        id = ContactIdSample.Stefano,
        name = "Stefano",
        userId = UserIdTestData.Primary
    )

    val Francesco = Contact(
        contactEmails = listOf(ContactEmailSample.contactEmail4),
        id = ContactIdSample.Francesco,
        name = "Francesco",
        userId = UserIdTestData.Primary
    )
}
