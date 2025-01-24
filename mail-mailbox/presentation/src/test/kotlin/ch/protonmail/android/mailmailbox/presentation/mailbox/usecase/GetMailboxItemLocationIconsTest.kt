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

import androidx.compose.ui.graphics.Color
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.sample.LabelSample
import ch.protonmail.android.mailcommon.presentation.mapper.ColorMapper
import ch.protonmail.android.maillabel.domain.SelectedMailLabelId
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.maillabel.domain.usecase.GetRootLabel
import ch.protonmail.android.maillabel.presentation.R
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxItemLocationUiModel
import ch.protonmail.android.mailmailbox.presentation.mailbox.usecase.GetMailboxItemLocationIcons.Result
import ch.protonmail.android.mailsettings.domain.model.FolderColorSettings
import ch.protonmail.android.testdata.label.LabelTestData.buildLabel
import ch.protonmail.android.testdata.mailbox.MailboxTestData.buildMailboxItem
import ch.protonmail.android.testdata.user.UserIdTestData.userId
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import me.proton.core.label.domain.entity.LabelId
import me.proton.core.label.domain.entity.LabelType
import org.junit.Test
import kotlin.test.assertEquals

class GetMailboxItemLocationIconsTest {

    private val defaultFolderColorSettings = FolderColorSettings()
    private val colorMapper: ColorMapper = mockk {
        every { toColor(any()) } returns Color.Unspecified.right()
    }

    private val selectedMailLabelId = mockk<SelectedMailLabelId> {
        every { this@mockk.flow } returns MutableStateFlow<MailLabelId>(MailLabelId.System.Inbox)
    }
    private val getRootLabel = mockk<GetRootLabel> {
        coEvery { this@mockk.invoke(any(), any()) } returns LabelSample.Parent
    }

    private val getMailboxItemLocationIcons =
        GetMailboxItemLocationIcons(selectedMailLabelId, colorMapper, getRootLabel)

    @Test
    fun `location icons are displayed when current location is 'starred' 'all mail' or 'custom label'`() = runTest {
        givenCurrentLocationIs(MailLabelId.System.Inbox)
        val mailboxItem = buildMailboxItem()

        val actual = getMailboxItemLocationIcons(mailboxItem, defaultFolderColorSettings, false)

        assertEquals(Result.None, actual)
    }

    @Test
    fun `location icons are displayed when current location is 'almost all mail'`() = runTest {
        givenCurrentLocationIs(MailLabelId.System.AlmostAllMail)
        val itemLabels = listOf(MailLabelId.System.Archive.labelId, MailLabelId.System.Inbox.labelId)
        val mailboxItem = buildMailboxItem(labelIds = itemLabels)

        val actual = getMailboxItemLocationIcons(mailboxItem, defaultFolderColorSettings, false)

        val expected = Result.Icons(
            MailboxItemLocationUiModel(R.drawable.ic_proton_inbox),
            MailboxItemLocationUiModel(R.drawable.ic_proton_archive_box)
        )
        assertEquals(expected, actual)
    }

    @Test
    fun `location icons are displayed when showing search results independent from current location`() = runTest {
        givenCurrentLocationIs(MailLabelId.System.Sent)
        val itemLabels = listOf(MailLabelId.System.Drafts.labelId, MailLabelId.System.Sent.labelId)
        val mailboxItem = buildMailboxItem(labelIds = itemLabels)

        val actual = getMailboxItemLocationIcons(mailboxItem, defaultFolderColorSettings, true)

        val expected = Result.Icons(
            MailboxItemLocationUiModel(R.drawable.ic_proton_file_lines),
            MailboxItemLocationUiModel(R.drawable.ic_proton_paper_plane)
        )
        assertEquals(expected, actual)
    }

    @Test
    fun `when location is custom label and mailbox item is in inbox show Inbox icon`() = runTest {
        val customLabelId = LabelId("custom label")
        givenCurrentLocationIs(MailLabelId.Custom.Label(customLabelId))
        val itemLabels = listOf(customLabelId, MailLabelId.System.Inbox.labelId)
        val mailboxItem = buildMailboxItem(labelIds = itemLabels)

        val actual = getMailboxItemLocationIcons(mailboxItem, defaultFolderColorSettings, false)

        assertEquals(Result.Icons(MailboxItemLocationUiModel(R.drawable.ic_proton_inbox)), actual)
    }

    @Test
    fun `when location is custom label and mailbox item is in spam show Spam icon`() = runTest {
        val customLabelId = LabelId("custom label")
        givenCurrentLocationIs(MailLabelId.Custom.Label(customLabelId))
        val itemLabels = listOf(customLabelId, MailLabelId.System.Spam.labelId)
        val mailboxItem = buildMailboxItem(labelIds = itemLabels)

        val actual = getMailboxItemLocationIcons(mailboxItem, defaultFolderColorSettings, false)

        assertEquals(Result.Icons(MailboxItemLocationUiModel(R.drawable.ic_proton_fire)), actual)
    }

    @Test
    fun `when location is starred and item contains messages in inbox and archive show Inbox and Archive icons`() =
        runTest {
            givenCurrentLocationIs(MailLabelId.System.Starred)
            val itemLabels = listOf(MailLabelId.System.Archive.labelId, MailLabelId.System.Inbox.labelId)
            val mailboxItem = buildMailboxItem(labelIds = itemLabels)

            val actual = getMailboxItemLocationIcons(mailboxItem, defaultFolderColorSettings, false)

            val expected = Result.Icons(
                MailboxItemLocationUiModel(R.drawable.ic_proton_inbox),
                MailboxItemLocationUiModel(R.drawable.ic_proton_archive_box)
            )
            assertEquals(expected, actual)
        }

    @Test
    fun `when location is all mail and item contains messages in drafts and sent show Drafts and Sent icons`() =
        runTest {
            givenCurrentLocationIs(MailLabelId.System.AllMail)
            val itemLabels = listOf(MailLabelId.System.Drafts.labelId, MailLabelId.System.Sent.labelId)
            val mailboxItem = buildMailboxItem(labelIds = itemLabels)

            val actual = getMailboxItemLocationIcons(mailboxItem, defaultFolderColorSettings, false)

            val expected = Result.Icons(
                MailboxItemLocationUiModel(R.drawable.ic_proton_file_lines),
                MailboxItemLocationUiModel(R.drawable.ic_proton_paper_plane)
            )
            assertEquals(expected, actual)
        }

    @Test
    fun `when location is all mail and item contains messages in Spam, Custom Folder and Trash show those icons`() =
        runTest {
            val colorString = "#FF0000"
            every { colorMapper.toColor(colorString) } returns Color.Red.right()
            givenCurrentLocationIs(MailLabelId.System.AllMail)
            val folderId = "customFolder"
            val itemLabelIds = listOf(
                MailLabelId.System.Spam.labelId,
                MailLabelId.System.Trash.labelId,
                LabelId(folderId)
            )
            val labels = listOf(
                buildLabel(userId = userId, type = LabelType.MessageFolder, id = folderId, color = "#FF0000")
            )
            val mailboxItem = buildMailboxItem(labelIds = itemLabelIds, labels = labels)

            val actual = getMailboxItemLocationIcons(mailboxItem, defaultFolderColorSettings, false)

            val expected = Result.Icons(
                MailboxItemLocationUiModel(R.drawable.ic_proton_fire),
                MailboxItemLocationUiModel(R.drawable.ic_proton_folder_filled, Color.Red),
                MailboxItemLocationUiModel(R.drawable.ic_proton_trash)
            )
            assertEquals(expected, actual)
        }

    @Test
    fun `when location is all mail and item just contains message in custom folder show folder icon`() = runTest {
        val colorString = "#FF0000"
        every { colorMapper.toColor(colorString) } returns Color.Red.right()
        givenCurrentLocationIs(MailLabelId.System.AllMail)
        val folderId = "customFolder"
        val itemLabelIds = listOf(LabelId(folderId))
        val labels = listOf(
            buildLabel(userId = userId, type = LabelType.MessageFolder, id = folderId, color = "#FF0000")
        )
        val mailboxItem = buildMailboxItem(labelIds = itemLabelIds, labels = labels)

        val actual = getMailboxItemLocationIcons(mailboxItem, defaultFolderColorSettings, false)

        val expected = Result.Icons(
            MailboxItemLocationUiModel(R.drawable.ic_proton_folder_filled, Color.Red)
        )
        assertEquals(expected, actual)
    }

    @Test
    fun `when location is all mail and mailbox item contains no message for which to show icons return no icons`() =
        runTest {
            givenCurrentLocationIs(MailLabelId.System.AllMail)
            val mailboxItem = buildMailboxItem(labelIds = emptyList())

            val actual = getMailboxItemLocationIcons(mailboxItem, defaultFolderColorSettings, false)

            assertEquals(Result.None, actual)
        }

    private fun givenCurrentLocationIs(location: MailLabelId) {
        every { selectedMailLabelId.flow } returns MutableStateFlow(location)
    }
}
