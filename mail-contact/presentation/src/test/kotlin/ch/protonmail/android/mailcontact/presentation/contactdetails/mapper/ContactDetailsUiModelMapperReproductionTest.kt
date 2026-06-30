/*
 * Copyright (c) 2026 Proton Technologies AG
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

package ch.protonmail.android.mailcontact.presentation.contactdetails.mapper

import androidx.compose.ui.graphics.Color
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.AvatarInformation
import ch.protonmail.android.mailcommon.presentation.mapper.ColorMapper
import ch.protonmail.android.mailcontact.domain.model.ContactDetailCard
import ch.protonmail.android.mailcontact.domain.model.ContactDetailEmail
import ch.protonmail.android.mailcontact.domain.model.ContactField
import ch.protonmail.android.mailcontact.domain.model.ContactGroup
import ch.protonmail.android.mailcontact.domain.model.ExtendedName
import ch.protonmail.android.mailcontact.presentation.contactdetails.model.ContactDetailsItemBadgeUiModel
import ch.protonmail.android.testdata.contact.ContactIdTestData
import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals

class ContactDetailsUiModelMapperReproductionTest {

    private val colorMapper = mockk<ColorMapper> {
        every { toColor(any()) } returns Color.Blue.right()
    }

    private val mapper = ContactDetailsUiModelMapper(colorMapper)

    @Test
    fun `should deduplicate contact groups when mapping to ui model`() {
        // Given
        val group = ContactGroup(name = "group", color = "color")
        val contactDetailCard = ContactDetailCard(
            id = ContactIdTestData.contactId1,
            remoteId = "id",
            avatarInformation = AvatarInformation(
                initials = "P",
                color = "color"
            ),
            extendedName = ExtendedName(
                last = "Mail",
                first = "Proton"
            ),
            fields = listOf(
                ContactField.Emails(
                    list = listOf(
                        ContactDetailEmail(
                            email = "pm@pm.me",
                            emailType = emptyList(),
                            groups = listOf(group, group) // Duplicate group
                        )
                    )
                )
            )
        )

        // When
        val result = mapper.toUiModel(contactDetailCard)

        // Then
        val emailItem = result.contactDetailsItemGroupUiModels.first().contactDetailsItemUiModels.first()
        val expectedBadges = listOf(ContactDetailsItemBadgeUiModel(name = "group", color = Color.Blue))
        assertEquals(expectedBadges, emailItem.badges, "Badges should be deduplicated")
    }
}
