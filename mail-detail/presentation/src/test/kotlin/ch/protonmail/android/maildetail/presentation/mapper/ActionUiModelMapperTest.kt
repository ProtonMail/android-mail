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

package ch.protonmail.android.maildetail.presentation.mapper

import ch.protonmail.android.mailcommon.domain.model.Action
import ch.protonmail.android.mailcommon.presentation.model.ActionUiModel
import me.proton.core.presentation.R
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
class ActionUiModelMapperTest(
    @Suppress("UNUSED_PARAMETER") private val testName: String,
    private val testInput: TestParams.TestInput
) {

    private val mapper = ActionUiModelMapper()

    @Test
    fun `map action to action ui model`() {
        // Given
        val action = testInput.action
        // When
        val actual = mapper.toUiModel(action)
        // Then
        assertEquals(testInput.expected, actual)
    }

    companion object {

        private val actions = listOf(
            TestParams(
                "Map Reply action to UiModel",
                TestParams.TestInput(
                    action = Action.Reply,
                    expected = ActionUiModel(Action.Reply, R.drawable.ic_proton_arrow_up_and_left)
                )
            ),
            TestParams(
                "Map ReplyAll action to UiModel",
                TestParams.TestInput(
                    action = Action.ReplyAll,
                    expected = ActionUiModel(Action.ReplyAll, R.drawable.ic_proton_arrows_up_and_left)
                )
            ),
            TestParams(
                "Map Forward action to UiModel",
                TestParams.TestInput(
                    action = Action.Forward,
                    expected = ActionUiModel(Action.Forward, R.drawable.ic_arrow_forward)
                )
            ),
            TestParams(
                "Map MarkRead action to UiModel",
                TestParams.TestInput(
                    action = Action.MarkRead,
                    expected = ActionUiModel(Action.MarkRead, R.drawable.ic_proton_envelope)
                )
            ),
            TestParams(
                "Map MarkUnread action to UiModel",
                TestParams.TestInput(
                    action = Action.MarkUnread,
                    expected = ActionUiModel(Action.MarkUnread, R.drawable.ic_proton_envelope_dot)
                )
            ),
            TestParams(
                "Map Star action to UiModel",
                TestParams.TestInput(
                    action = Action.Star,
                    expected = ActionUiModel(Action.Star, R.drawable.ic_proton_star)
                )
            ),
            TestParams(
                "Map Unstar action to UiModel",
                TestParams.TestInput(
                    action = Action.Unstar,
                    expected = ActionUiModel(Action.Unstar, R.drawable.ic_proton_star_filled)
                )
            ),
            TestParams(
                "Map Label action to UiModel",
                TestParams.TestInput(
                    action = Action.Label,
                    expected = ActionUiModel(Action.Label, R.drawable.ic_proton_tag)
                )
            ),
            TestParams(
                "Map Move action to UiModel",
                TestParams.TestInput(
                    action = Action.Move,
                    expected = ActionUiModel(Action.Move, R.drawable.ic_proton_folder_arrow_in)
                )
            ),
            TestParams(
                "Map Trash action to UiModel",
                TestParams.TestInput(
                    action = Action.Trash,
                    expected = ActionUiModel(Action.Trash, R.drawable.ic_proton_trash)
                )
            ),
            TestParams(
                "Map Archive action to UiModel",
                TestParams.TestInput(
                    action = Action.Archive,
                    expected = ActionUiModel(Action.Archive, R.drawable.ic_proton_archive_box)
                )
            ),
            TestParams(
                "Map Spam action to UiModel",
                TestParams.TestInput(
                    action = Action.Spam,
                    expected = ActionUiModel(Action.Spam, R.drawable.ic_proton_fire)
                )
            ),
            TestParams(
                "Map ViewInLightMode action to UiModel",
                TestParams.TestInput(
                    action = Action.ViewInLightMode,
                    expected = ActionUiModel(Action.ViewInLightMode, R.drawable.ic_proton_circle)
                )
            ),
            TestParams(
                "Map ViewInDarkMode action to UiModel",
                TestParams.TestInput(
                    action = Action.ViewInDarkMode,
                    expected = ActionUiModel(Action.ViewInDarkMode, R.drawable.ic_proton_circle_filled)
                )
            ),
            TestParams(
                "Map Print action to UiModel",
                TestParams.TestInput(
                    action = Action.Print,
                    expected = ActionUiModel(Action.Print, R.drawable.ic_proton_printer)
                )
            ),
            TestParams(
                "Map ViewHeaders action to UiModel",
                TestParams.TestInput(
                    action = Action.ViewHeaders,
                    expected = ActionUiModel(Action.ViewHeaders, R.drawable.ic_proton_file_lines)
                )
            ),
            TestParams(
                "Map ViewHtml action to UiModel",
                TestParams.TestInput(
                    action = Action.ViewHtml,
                    expected = ActionUiModel(Action.ViewHtml, R.drawable.ic_proton_code)
                )
            ),
            TestParams(
                "Map ReportPhishing action to UiModel",
                TestParams.TestInput(
                    action = Action.ReportPhishing,
                    expected = ActionUiModel(Action.ReportPhishing, R.drawable.ic_proton_hook)
                )
            ),
            TestParams(
                "Map Remind action to UiModel",
                TestParams.TestInput(
                    action = Action.Remind,
                    expected = ActionUiModel(Action.Remind, R.drawable.ic_proton_clock)
                )
            ),
            TestParams(
                "Map SavePdf action to UiModel",
                TestParams.TestInput(
                    action = Action.SavePdf,
                    expected = ActionUiModel(Action.SavePdf, R.drawable.ic_proton_arrow_down_line)
                )
            ),
            TestParams(
                "Map SenderEmails action to UiModel",
                TestParams.TestInput(
                    action = Action.SenderEmails,
                    expected = ActionUiModel(Action.SenderEmails, R.drawable.ic_proton_envelope)
                )
            ),
            TestParams(
                "Map SaveAttachments action to UiModel",
                TestParams.TestInput(
                    action = Action.SaveAttachments,
                    expected = ActionUiModel(Action.SaveAttachments, R.drawable.ic_proton_arrow_down_to_square)
                )
            )
        )

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() = actions
            .map { arrayOf(it.testName, it.testInput) }
    }

    data class TestParams(
        val testName: String,
        val testInput: TestInput
    ) {

        data class TestInput(
            val action: Action,
            val expected: ActionUiModel
        )
    }
}
