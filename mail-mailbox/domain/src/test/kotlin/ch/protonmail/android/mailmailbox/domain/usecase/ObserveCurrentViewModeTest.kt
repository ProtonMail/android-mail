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

package ch.protonmail.android.mailmailbox.domain.usecase

import app.cash.turbine.test
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.mailsettings.domain.usecase.ObserveMailSettings
import ch.protonmail.android.testdata.user.UserIdTestData.userId
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.type.IntEnum
import me.proton.core.label.domain.entity.LabelId
import me.proton.core.mailsettings.domain.entity.MailSettings
import me.proton.core.mailsettings.domain.entity.ViewMode
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
internal class ObserveCurrentViewModeTest(
    @Suppress("UNUSED_PARAMETER") testName: String,
    private val input: Params.Input,
    private val expected: ViewMode
) {

    private val observeMailSettings: ObserveMailSettings = mockk {
        every { this@mockk(userId) } returns
            flowOf(buildMailSettings(isConversationSettingEnabled = input.isConversationSettingEnabled))
    }
    private val observeCurrentViewMode = ObserveCurrentViewMode(observeMailSettings)

    @Test
    fun test() = runTest {
        observeCurrentViewMode(userId, input.selectedMailLabelId).test {
            assertEquals(expected, awaitItem())
            awaitComplete()
        }
    }

    data class Params(
        val testName: String,
        val input: Input,
        val expected: ViewMode
    ) {

        data class Input(
            val isConversationSettingEnabled: Boolean,
            val selectedMailLabelId: MailLabelId
        )
    }

    private companion object TestData {

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
            confirmLink = null,
            autoDeleteSpamAndTrashDays = null,
            mobileSettings = null,
            almostAllMail = null
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
                input = Params.Input(isConversationSettingEnabled = true, MailLabelId.System.Inbox),
                expected = ViewMode.ConversationGrouping
            ),

            Params(
                testName = "conversation enabled in Drafts",
                input = Params.Input(isConversationSettingEnabled = true, MailLabelId.System.Drafts),
                expected = ViewMode.NoConversationGrouping
            ),

            Params(
                testName = "conversation enabled in Sent",
                input = Params.Input(isConversationSettingEnabled = true, MailLabelId.System.Sent),
                expected = ViewMode.NoConversationGrouping
            ),

            Params(
                testName = "conversation enabled in Starred",
                input = Params.Input(isConversationSettingEnabled = true, MailLabelId.System.Starred),
                expected = ViewMode.ConversationGrouping
            ),

            Params(
                testName = "conversation enabled in Archive",
                input = Params.Input(isConversationSettingEnabled = true, MailLabelId.System.Archive),
                expected = ViewMode.ConversationGrouping
            ),

            Params(
                testName = "conversation enabled in Spam",
                input = Params.Input(isConversationSettingEnabled = true, MailLabelId.System.Spam),
                expected = ViewMode.ConversationGrouping
            ),

            Params(
                testName = "conversation enabled in Trash",
                input = Params.Input(isConversationSettingEnabled = true, MailLabelId.System.Trash),
                expected = ViewMode.ConversationGrouping
            ),

            Params(
                testName = "conversation enabled in AllMail",
                input = Params.Input(isConversationSettingEnabled = true, MailLabelId.System.AllMail),
                expected = ViewMode.ConversationGrouping
            ),

            Params(
                testName = "conversation enabled in CustomLabel",
                input = Params.Input(isConversationSettingEnabled = true, MailLabelId.Custom.Label(LabelId("0"))),
                expected = ViewMode.ConversationGrouping
            ),

            Params(
                testName = "conversation enabled in CustomFolder",
                input = Params.Input(isConversationSettingEnabled = true, MailLabelId.Custom.Folder(LabelId("0"))),
                expected = ViewMode.ConversationGrouping
            ),

            Params(
                testName = "conversation disabled in Inbox",
                input = Params.Input(isConversationSettingEnabled = false, MailLabelId.System.Inbox),
                expected = ViewMode.NoConversationGrouping
            ),

            Params(
                testName = "conversation disabled in Drafts",
                input = Params.Input(isConversationSettingEnabled = false, MailLabelId.System.Drafts),
                expected = ViewMode.NoConversationGrouping
            ),

            Params(
                testName = "conversation disabled in Sent",
                input = Params.Input(isConversationSettingEnabled = false, MailLabelId.System.Sent),
                expected = ViewMode.NoConversationGrouping
            ),

            Params(
                testName = "conversation disabled in Starred",
                input = Params.Input(isConversationSettingEnabled = false, MailLabelId.System.Starred),
                expected = ViewMode.NoConversationGrouping
            ),

            Params(
                testName = "conversation disabled in Archive",
                input = Params.Input(isConversationSettingEnabled = false, MailLabelId.System.Archive),
                expected = ViewMode.NoConversationGrouping
            ),

            Params(
                testName = "conversation disabled in Spam",
                input = Params.Input(isConversationSettingEnabled = false, MailLabelId.System.Spam),
                expected = ViewMode.NoConversationGrouping
            ),

            Params(
                testName = "conversation disabled in Trash",
                input = Params.Input(isConversationSettingEnabled = false, MailLabelId.System.Trash),
                expected = ViewMode.NoConversationGrouping
            ),

            Params(
                testName = "conversation disabled in AllMail",
                input = Params.Input(isConversationSettingEnabled = false, MailLabelId.System.AllMail),
                expected = ViewMode.NoConversationGrouping
            ),

            Params(
                testName = "conversation disabled in CustomLabel",
                input = Params.Input(isConversationSettingEnabled = false, MailLabelId.Custom.Label(LabelId("0"))),
                expected = ViewMode.NoConversationGrouping
            ),

            Params(
                testName = "conversation disabled in CustomFolder",
                input = Params.Input(isConversationSettingEnabled = false, MailLabelId.Custom.Folder(LabelId("0"))),
                expected = ViewMode.NoConversationGrouping
            )

        ).map { arrayOf(it.testName, it.input, it.expected) }
    }

}
