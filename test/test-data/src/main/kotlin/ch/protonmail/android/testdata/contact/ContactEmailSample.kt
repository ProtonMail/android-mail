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
import me.proton.core.contact.domain.entity.ContactEmail

object ContactEmailSample {

    val contactEmail1 = ContactEmail(
        UserIdTestData.Primary,
        ContactIdTestData.contactEmailId1,
        "name (contact email 1)",
        "email (contact email 1)",
        0,
        0,
        ContactIdTestData.contactId1,
        "canonical email (contact email 1)",
        emptyList(),
        false
    )

    val contactEmail2 = ContactEmail(
        UserIdTestData.Primary,
        ContactIdTestData.contactEmailId2,
        "name (contact email 2)",
        "email (contact email 2)",
        0,
        1,
        ContactIdTestData.contactId1,
        "canonical email (contact email 2)",
        emptyList(),
        false
    )

    val contactEmails = listOf(
        contactEmail1,
        contactEmail2
    )
}
