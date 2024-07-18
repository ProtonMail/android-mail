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

package ch.protonmail.android.mailcontact.presentation

import androidx.compose.ui.graphics.Color
import ch.protonmail.android.mailcommon.presentation.mapper.ColorMapper
import ch.protonmail.android.mailcontact.presentation.model.ContactGroupItemUiModel
import ch.protonmail.android.mailcontact.presentation.model.ContactGroupItemUiModelMapper
import ch.protonmail.android.maillabel.presentation.getHexStringFromColor
import ch.protonmail.android.test.utils.rule.MainDispatcherRule
import ch.protonmail.android.testdata.user.UserIdTestData
import me.proton.core.contact.domain.entity.Contact
import me.proton.core.contact.domain.entity.ContactEmail
import me.proton.core.contact.domain.entity.ContactEmailId
import me.proton.core.contact.domain.entity.ContactId
import me.proton.core.label.domain.entity.Label
import me.proton.core.label.domain.entity.LabelId
import me.proton.core.label.domain.entity.LabelType
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

class ContactGroupItemUiModelMapperTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val colorMapper = ColorMapper()
    private val contactGroupItemUiModelMapper = ContactGroupItemUiModelMapper(colorMapper)

    @Test
    fun `return correct contact groups`() {
        val contacts = listOf(
            Contact(
                UserIdTestData.userId,
                ContactId("1"),
                "first contact",
                listOf(
                    ContactEmail(
                        UserIdTestData.userId,
                        ContactEmailId("contact email id 1"),
                        "First contact email",
                        "firstcontact+alias@protonmail.com",
                        0,
                        0,
                        ContactId("1"),
                        "firstcontact@protonmail.com",
                        listOf("LabelId1"),
                        true,
                        lastUsedTime = 0
                    )
                )
            ),
            Contact(
                UserIdTestData.userId,
                ContactId("1.1"),
                "first contact bis",
                listOf(
                    ContactEmail(
                        UserIdTestData.userId,
                        ContactEmailId("contact email id 1.1"),
                        "First contact email bis",
                        "firstcontactbis@protonmail.com",
                        0,
                        1,
                        ContactId("1.1"),
                        "firstcontactbis@protonmail.com",
                        listOf("LabelId1"),
                        true,
                        lastUsedTime = 0
                    )
                )
            ),
            Contact(
                UserIdTestData.userId,
                ContactId("2"),
                "second contact",
                listOf(
                    ContactEmail(
                        UserIdTestData.userId,
                        ContactEmailId("contact email id 2"),
                        "Second contact email",
                        "secondcontact@protonmail.com",
                        0,
                        0,
                        ContactId("2"),
                        "secondcontact@protonmail.com",
                        listOf("LabelId1", "LabelId2"),
                        true,
                        lastUsedTime = 0
                    )
                )
            )
        )
        val contactGroupLabels = listOf(
            Label(
                userId = UserIdTestData.userId,
                labelId = LabelId("LabelId1"),
                parentId = null,
                name = "Label 1",
                type = LabelType.ContactGroup,
                path = "",
                color = Color.Red.getHexStringFromColor(),
                order = 0,
                isNotified = null,
                isExpanded = null,
                isSticky = null
            ),
            Label(
                userId = UserIdTestData.userId,
                labelId = LabelId("LabelId2"),
                parentId = null,
                name = "Label 2",
                type = LabelType.ContactGroup,
                path = "",
                color = Color.Blue.getHexStringFromColor(),
                order = 1,
                isNotified = null,
                isExpanded = null,
                isSticky = null
            )
        )

        val actual = contactGroupItemUiModelMapper.toContactGroupItemUiModel(
            contacts,
            contactGroupLabels
        )

        val expected = listOf(
            ContactGroupItemUiModel(
                labelId = LabelId("LabelId1"),
                name = "Label 1",
                memberCount = 3,
                color = Color.Red
            ),
            ContactGroupItemUiModel(
                labelId = LabelId("LabelId2"),
                name = "Label 2",
                memberCount = 1,
                color = Color.Blue
            )
        )

        assertEquals(actual, expected)
    }
}
