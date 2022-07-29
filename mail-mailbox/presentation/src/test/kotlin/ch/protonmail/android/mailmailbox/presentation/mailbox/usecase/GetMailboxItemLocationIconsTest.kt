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

package ch.protonmail.android.mailmailbox.presentation.mailbox.usecase

import ch.protonmail.android.maillabel.domain.SelectedMailLabelId
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.maillabel.presentation.R
import ch.protonmail.android.mailmailbox.presentation.mailbox.usecase.GetMailboxItemLocationIcons.Result
import ch.protonmail.android.testdata.mailbox.MailboxTestData.buildMailboxItem
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import me.proton.core.label.domain.entity.LabelId
import org.junit.Test
import kotlin.test.assertEquals

class GetMailboxItemLocationIconsTest {

    private val selectedMailLabelId = mockk<SelectedMailLabelId> {
        every { this@mockk.flow } returns MutableStateFlow<MailLabelId>(MailLabelId.System.Inbox)
    }

    private val getMailboxItemLocationIcons = GetMailboxItemLocationIcons(selectedMailLabelId)

    @Test
    fun `location icons are only displayed when current location is 'starred' 'all mail' or 'custom label'`() {
        givenCurrentLocationIs(MailLabelId.System.Inbox)
        val mailboxItem = buildMailboxItem()

        val actual = getMailboxItemLocationIcons(mailboxItem)

        assertEquals(Result.None, actual)
    }

    @Test
    fun `when location is custom label and mailbox item is in inbox show Inbox icon`() {
        val customLabelId = "custom label"
        givenCurrentLocationIs(MailLabelId.Custom.Label(LabelId(customLabelId)))
        val itemLabels = listOf(customLabelId, MailLabelId.System.Inbox.labelId.id)
        val mailboxItem = buildMailboxItem(labelIds = itemLabels)

        val actual = getMailboxItemLocationIcons(mailboxItem)

        assertEquals(Result.Icons(R.drawable.ic_proton_inbox), actual)
    }

    private fun givenCurrentLocationIs(location: MailLabelId) {
        every { selectedMailLabelId.flow } returns MutableStateFlow(location)
    }
}
