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

import ch.protonmail.android.mailcommon.domain.sample.LabelSample
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
        listOf(LabelSample.GroupCoworkers.labelId.id),
        false,
        lastUsedTime = 0
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
        listOf(LabelSample.GroupCoworkers.labelId.id),
        false,
        lastUsedTime = 0
    )

    val contactEmail3 = ContactEmail(
        UserIdTestData.Primary,
        ContactIdTestData.contactEmailId3,
        "name (contact email 3)",
        "email (contact email 3)",
        0,
        2,
        ContactIdTestData.contactId3,
        "canonical email (contact email 3)",
        emptyList(),
        false,
        lastUsedTime = 0
    )

    val contactEmail4 = ContactEmail(
        UserIdTestData.Primary,
        ContactIdTestData.contactEmailId4,
        "name (contact email 4)",
        "email (contact email 4)",
        0,
        2,
        ContactIdTestData.contactId4,
        "canonical email (contact email 4)",
        emptyList(),
        false,
        lastUsedTime = 0
    )

    val contactEmailLastUsedRecently = ContactEmail(
        UserIdTestData.Primary,
        ContactIdTestData.contactEmailId5,
        "Testing LastUsedTime",
        "testing@last.used.time -- recently",
        0,
        2,
        ContactIdTestData.contactId5,
        "canonical testing@last.used.time",
        emptyList(),
        false,
        lastUsedTime = 100
    )

    val contactEmailLastUsedLongTimeAgo = ContactEmail(
        UserIdTestData.Primary,
        ContactIdTestData.contactEmailId6,
        "Testing LastUsedTime",
        "testing@last.used.time -- long time ago",
        0,
        2,
        ContactIdTestData.contactId6,
        "canonical testing@last.used.time",
        emptyList(),
        false,
        lastUsedTime = 1
    )
}
