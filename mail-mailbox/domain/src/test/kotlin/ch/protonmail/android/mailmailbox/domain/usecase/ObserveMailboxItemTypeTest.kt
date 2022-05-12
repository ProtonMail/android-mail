/*
 * Copyright (c) 2021 Proton Technologies AG
 * This file is part of Proton Technologies AG and ProtonMail.
 *
 * ProtonMail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ProtonMail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ProtonMail.  If not, see <https://www.gnu.org/licenses/>.
 */

package ch.protonmail.android.mailmailbox.domain.usecase

import app.cash.turbine.test
import ch.protonmail.android.mailmailbox.domain.model.MailboxItemType
import ch.protonmail.android.mailmailbox.domain.model.SidebarLocation
import ch.protonmail.android.mailmailbox.domain.model.SidebarLocation.CustomFolder
import ch.protonmail.android.mailmailbox.domain.model.SidebarLocation.CustomLabel
import ch.protonmail.android.mailsettings.domain.ObserveMailSettings
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import me.proton.core.domain.type.IntEnum
import me.proton.core.label.domain.entity.LabelId
import me.proton.core.mailsettings.domain.entity.MailSettings
import me.proton.core.mailsettings.domain.entity.ViewMode
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
internal class ObserveMailboxItemTypeTest(
    @Suppress("UNUSED_PARAMETER") _testName: String,
    private val input: Params.Input,
    private val expected: MailboxItemType
) {

    private val observeMailSettings: ObserveMailSettings = mockk {
        every { this@mockk() } returns
            flowOf(buildMailSettings(isConversationSettingEnabled = input.isConversationSettingEnabled))
    }
    private val observeMailboxItemType = ObserveMailboxItemType(observeMailSettings)

    @Test
    fun test() = runTest {
        observeMailboxItemType(input.location).test {
            assertEquals(expected, awaitItem())
            awaitComplete()
        }
    }

    data class Params(
        val testName: String,
        val input: Input,
        val expected: MailboxItemType
    ) {

        data class Input(
            val isConversationSettingEnabled: Boolean,
            val location: SidebarLocation
        )
    }

    private companion object TestData {

        val userId = UserId("user")

        fun buildMailSettings(isConversationSettingEnabled: Boolean) = MailSettings(
            userId = userId,
            displayName = null,
            signature = null,
            autoSaveContacts = null,
            composerMode = null,
            messageButtons = null,
            showImages = null,
            showMoved = null,
            viewMode = buildViewModeEnum(isConversationSettingEnabled),
            viewLayout = null,
            swipeLeft = null,
            swipeRight = null,
            shortcuts = null,
            pmSignature = null,
            numMessagePerPage = null,
            draftMimeType = null,
            receiveMimeType = null,
            showMimeType = null,
            enableFolderColor = null,
            inheritParentFolderColor = null,
            rightToLeft = null,
            attachPublicKey = null,
            sign = null,
            pgpScheme = null,
            promptPin = null,
            stickyLabels = null,
            confirmLink = null
        )

        private fun buildViewModeEnum(isConversationSettingEnabled: Boolean): IntEnum<ViewMode> {
            val viewModel =
                if (isConversationSettingEnabled) ViewMode.ConversationGrouping
                else ViewMode.NoConversationGrouping

            return IntEnum(viewModel.value, viewModel)
        }

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() = listOf(

            Params(
                testName = "conversation enabled in Inbox",
                input = Params.Input(isConversationSettingEnabled = true, SidebarLocation.Inbox),
                expected = MailboxItemType.Conversation
            ),

            Params(
                testName = "conversation enabled in Drafts",
                input = Params.Input(isConversationSettingEnabled = true, SidebarLocation.Drafts),
                expected = MailboxItemType.Message
            ),

            Params(
                testName = "conversation enabled in Sent",
                input = Params.Input(isConversationSettingEnabled = true, SidebarLocation.Sent),
                expected = MailboxItemType.Message
            ),

            Params(
                testName = "conversation enabled in Starred",
                input = Params.Input(isConversationSettingEnabled = true, SidebarLocation.Starred),
                expected = MailboxItemType.Conversation
            ),

            Params(
                testName = "conversation enabled in Archive",
                input = Params.Input(isConversationSettingEnabled = true, SidebarLocation.Archive),
                expected = MailboxItemType.Conversation
            ),

            Params(
                testName = "conversation enabled in Spam",
                input = Params.Input(isConversationSettingEnabled = true, SidebarLocation.Spam),
                expected = MailboxItemType.Conversation
            ),

            Params(
                testName = "conversation enabled in Trash",
                input = Params.Input(isConversationSettingEnabled = true, SidebarLocation.Trash),
                expected = MailboxItemType.Conversation
            ),

            Params(
                testName = "conversation enabled in AllMail",
                input = Params.Input(isConversationSettingEnabled = true, SidebarLocation.AllMail),
                expected = MailboxItemType.Conversation
            ),

            Params(
                testName = "conversation enabled in CustomLabel",
                input = Params.Input(isConversationSettingEnabled = true, CustomLabel(LabelId("0"))),
                expected = MailboxItemType.Conversation
            ),

            Params(
                testName = "conversation enabled in CustomFolder",
                input = Params.Input(isConversationSettingEnabled = true, CustomFolder(LabelId("0"))),
                expected = MailboxItemType.Conversation
            ),

            Params(
                testName = "conversation disabled in Inbox",
                input = Params.Input(isConversationSettingEnabled = false, SidebarLocation.Inbox),
                expected = MailboxItemType.Message
            ),

            Params(
                testName = "conversation disabled in Drafts",
                input = Params.Input(isConversationSettingEnabled = false, SidebarLocation.Drafts),
                expected = MailboxItemType.Message
            ),

            Params(
                testName = "conversation disabled in Sent",
                input = Params.Input(isConversationSettingEnabled = false, SidebarLocation.Sent),
                expected = MailboxItemType.Message
            ),

            Params(
                testName = "conversation disabled in Starred",
                input = Params.Input(isConversationSettingEnabled = false, SidebarLocation.Starred),
                expected = MailboxItemType.Message
            ),

            Params(
                testName = "conversation disabled in Archive",
                input = Params.Input(isConversationSettingEnabled = false, SidebarLocation.Archive),
                expected = MailboxItemType.Message
            ),

            Params(
                testName = "conversation disabled in Spam",
                input = Params.Input(isConversationSettingEnabled = false, SidebarLocation.Spam),
                expected = MailboxItemType.Message
            ),

            Params(
                testName = "conversation disabled in Trash",
                input = Params.Input(isConversationSettingEnabled = false, SidebarLocation.Trash),
                expected = MailboxItemType.Message
            ),

            Params(
                testName = "conversation disabled in AllMail",
                input = Params.Input(isConversationSettingEnabled = false, SidebarLocation.AllMail),
                expected = MailboxItemType.Message
            ),

            Params(
                testName = "conversation disabled in CustomLabel",
                input = Params.Input(isConversationSettingEnabled = false, CustomLabel(LabelId("0"))),
                expected = MailboxItemType.Message
            ),

            Params(
                testName = "conversation disabled in CustomFolder",
                input = Params.Input(isConversationSettingEnabled = false, CustomFolder(LabelId("0"))),
                expected = MailboxItemType.Message
            )

        ).map { arrayOf(it.testName, it.input, it.expected) }
    }

}
