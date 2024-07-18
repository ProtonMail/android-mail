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
import ch.protonmail.android.mailcontact.domain.model.ContactGroup
import ch.protonmail.android.mailcontact.presentation.model.ContactGroupItemUiModel
import ch.protonmail.android.mailcontact.presentation.model.ContactSearchUiModelMapper
import ch.protonmail.android.maillabel.presentation.getHexStringFromColor
import ch.protonmail.android.test.utils.rule.MainDispatcherRule
import ch.protonmail.android.testdata.contact.ContactIdTestData
import ch.protonmail.android.testdata.label.LabelTestData
import ch.protonmail.android.testdata.user.UserIdTestData
import me.proton.core.contact.domain.entity.ContactEmail
import me.proton.core.contact.domain.entity.ContactEmailId
import me.proton.core.label.domain.entity.LabelId
import me.proton.core.label.domain.entity.LabelType
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

class ContactSearchUiModelMapperTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val colorMapper = ColorMapper()
    private val contactSearchUiModelMapper = ContactSearchUiModelMapper(colorMapper)

    @Test
    fun `maps list of ContactGroups to list of UiModel`() {
        val label = LabelTestData.buildLabel(
            "LabelId1",
            UserIdTestData.userId,
            LabelType.ContactGroup,
            "Label 1",
            color = Color.Red.getHexStringFromColor()
        )

        val contactGroups = listOf(
            ContactGroup(
                UserIdTestData.userId,
                label.labelId,
                label.name,
                label.color,
                listOf(
                    ContactEmail(
                        UserIdTestData.userId,
                        ContactEmailId(""),
                        "",
                        "",
                        0,
                        0,
                        ContactIdTestData.contactId1,
                        "",
                        emptyList(),
                        true,
                        lastUsedTime = 0
                    )
                )
            )
        )

        val actual = contactSearchUiModelMapper.contactGroupsToContactSearchUiModelList(contactGroups)

        val expected = listOf(
            ContactGroupItemUiModel(
                labelId = LabelId("LabelId1"),
                name = label.name,
                color = Color.Red,
                memberCount = contactGroups[0].members.size
            )
        )

        assertEquals(expected, actual)
    }
}
