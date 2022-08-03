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
import ch.protonmail.android.testdata.label.LabelTestData.buildLabel
import ch.protonmail.android.testdata.mailbox.MailboxTestData.buildMailboxItem
import ch.protonmail.android.testdata.user.UserIdTestData.userId
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import me.proton.core.label.domain.entity.LabelId
import me.proton.core.label.domain.entity.LabelType
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
        val customLabelId = LabelId("custom label")
        givenCurrentLocationIs(MailLabelId.Custom.Label(customLabelId))
        val itemLabels = listOf(customLabelId, MailLabelId.System.Inbox.labelId)
        val mailboxItem = buildMailboxItem(labelIds = itemLabels)

        val actual = getMailboxItemLocationIcons(mailboxItem)

        assertEquals(Result.Icons(R.drawable.ic_proton_inbox), actual)
    }

    @Test
    fun `when location is custom label and mailbox item is in spam show Spam icon`() {
        val customLabelId = LabelId("custom label")
        givenCurrentLocationIs(MailLabelId.Custom.Label(customLabelId))
        val itemLabels = listOf(customLabelId, MailLabelId.System.Spam.labelId)
        val mailboxItem = buildMailboxItem(labelIds = itemLabels)

        val actual = getMailboxItemLocationIcons(mailboxItem)

        assertEquals(Result.Icons(R.drawable.ic_proton_fire), actual)
    }

    @Test
    fun `when location is starred and mailbox item contains messages in inbox and archive show Inbox and Archive icons`() {
        givenCurrentLocationIs(MailLabelId.System.Starred)
        val itemLabels = listOf(MailLabelId.System.Archive.labelId, MailLabelId.System.Inbox.labelId)
        val mailboxItem = buildMailboxItem(labelIds = itemLabels)

        val actual = getMailboxItemLocationIcons(mailboxItem)

        val expected = Result.Icons(R.drawable.ic_proton_inbox, R.drawable.ic_proton_archive_box)
        assertEquals(expected, actual)
    }

    @Test
    fun `when location is all mail and mailbox item contains messages in drafts and sent show Drafts and Sent icons`() {
        givenCurrentLocationIs(MailLabelId.System.AllMail)
        val itemLabels = listOf(MailLabelId.System.Drafts.labelId, MailLabelId.System.Sent.labelId)
        val mailboxItem = buildMailboxItem(labelIds = itemLabels)

        val actual = getMailboxItemLocationIcons(mailboxItem)

        val expected = Result.Icons(R.drawable.ic_proton_file_lines, R.drawable.ic_proton_paper_plane)
        assertEquals(expected, actual)
    }

    @Test
    fun `when location is all mail and mailbox item contains messages in Spam, Custom Folder and Trash show Spam Custom Folder and Trash icons`() {
        givenCurrentLocationIs(MailLabelId.System.AllMail)
        val folderId = "customFolder"
        val itemLabelIds = listOf(
            MailLabelId.System.Spam.labelId,
            MailLabelId.System.Trash.labelId,
            LabelId(folderId)
        )
        val labels = listOf(buildLabel(userId, LabelType.MessageFolder, id = folderId))
        val mailboxItem = buildMailboxItem(labelIds = itemLabelIds, labels = labels)

        val actual = getMailboxItemLocationIcons(mailboxItem)

        val expected = Result.Icons(R.drawable.ic_proton_fire, R.drawable.ic_proton_folder, R.drawable.ic_proton_trash)
        assertEquals(expected, actual)
    }

    @Test
    fun `when location is all mail and mailbox item contains no message for which to show icons return no icons`() {
        givenCurrentLocationIs(MailLabelId.System.AllMail)
        val mailboxItem = buildMailboxItem(labelIds = emptyList())

        val actual = getMailboxItemLocationIcons(mailboxItem)

        assertEquals(Result.None, actual)
    }

    private fun givenCurrentLocationIs(location: MailLabelId) {
        every { selectedMailLabelId.flow } returns MutableStateFlow(location)
    }
}
