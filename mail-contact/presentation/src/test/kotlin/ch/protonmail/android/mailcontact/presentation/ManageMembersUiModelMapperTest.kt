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
import androidx.compose.ui.graphics.toArgb
import ch.protonmail.android.mailcontact.presentation.model.ManageMembersUiModel
import ch.protonmail.android.mailcontact.presentation.model.ManageMembersUiModelMapper
import ch.protonmail.android.maillabel.presentation.getHexStringFromColor
import ch.protonmail.android.testdata.contact.ContactIdTestData
import ch.protonmail.android.testdata.contact.ContactTestData
import ch.protonmail.android.testdata.user.UserIdTestData
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import me.proton.core.contact.domain.entity.ContactEmail
import me.proton.core.contact.domain.entity.ContactEmailId
import org.junit.Test
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.assertEquals

class ManageMembersUiModelMapperTest {

    private val manageMembersUiModelMapper = ManageMembersUiModelMapper()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        mockkStatic(android.graphics.Color::class)
        every { android.graphics.Color.parseColor(Color.Red.getHexStringFromColor()) } returns Color.Red.toArgb()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `maps Contacts to ManageMembersUiModel`() {
        val contact = ContactTestData.buildContactWith(
            contactEmails = listOf(
                ContactEmail(
                    UserIdTestData.userId,
                    ContactEmailId("contact email id 1"),
                    "First name from contact email",
                    "test1+alias@protonmail.com",
                    0,
                    0,
                    ContactIdTestData.contactId1,
                    "test1@protonmail.com",
                    listOf("LabelId1"),
                    true
                ),
                ContactEmail(
                    UserIdTestData.userId,
                    ContactEmailId("contact email id 2"),
                    "First name from contact email",
                    "test2+alias@protonmail.com",
                    0,
                    0,
                    ContactIdTestData.contactId1,
                    "test2@protonmail.com",
                    emptyList(),
                    true
                )
            )
        )

        val expected = listOf(
            ManageMembersUiModel(
                id = ContactEmailId("contact email id 1"),
                name = "First name from contact email",
                email = "test1+alias@protonmail.com",
                initials = "FE",
                isSelected = true
            ),
            ManageMembersUiModel(
                id = ContactEmailId("contact email id 2"),
                name = "First name from contact email",
                email = "test2+alias@protonmail.com",
                initials = "FE",
                isSelected = false
            )
        )

        val actual = manageMembersUiModelMapper.toManageMembersUiModelList(
            listOf(contact),
            listOf(ContactEmailId("contact email id 1"))
        )

        assertEquals(expected, actual)
    }
}
