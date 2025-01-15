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

package ch.protonmail.android.mailmailbox.presentation.mailbox.mapper

import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.mailmailbox.presentation.R
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.SwipeActionsUiModel
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.SwipeUiModel
import ch.protonmail.android.mailsettings.domain.model.SwipeActionsPreference
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.label.domain.entity.LabelId
import me.proton.core.mailsettings.domain.entity.SwipeAction
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
class SwipeActionsMapperTest(
    private val testInput: TestInput
) {

    private val swipeActionsMapper = SwipeActionsMapper()

    @Test
    fun `should produce the expected new state`() = with(testInput) {
        val actual = swipeActionsMapper(testInput.input.first, testInput.input.second)

        assertEquals(expected, actual)
    }

    companion object {

        private val transitions = listOf(
            TestInput(
                input = SystemLabelId.Inbox.labelId to SwipeActionsPreference(
                    swipeLeft = SwipeAction.Spam,
                    swipeRight = SwipeAction.Archive
                ),
                expected = SwipeActionsUiModel(
                    end = SwipeUiModel(
                        swipeAction = SwipeAction.Spam,
                        icon = R.drawable.ic_proton_fire,
                        descriptionRes = R.string.mail_settings_swipe_action_spam_description,
                        getColor = { ProtonTheme.colors.notificationNorm },
                        staysDismissed = true
                    ),
                    start = SwipeUiModel(
                        swipeAction = SwipeAction.Archive,
                        icon = R.drawable.ic_proton_archive_box,
                        descriptionRes = R.string.mail_settings_swipe_action_archive_description,
                        getColor = { ProtonTheme.colors.notificationNorm },
                        staysDismissed = true
                    )
                )
            ),
            TestInput(
                input = SystemLabelId.Spam.labelId to SwipeActionsPreference(
                    swipeLeft = SwipeAction.Spam,
                    swipeRight = SwipeAction.Archive
                ),
                expected = SwipeActionsUiModel(
                    end = SwipeUiModel(
                        swipeAction = SwipeAction.Spam,
                        icon = R.drawable.ic_proton_fire,
                        descriptionRes = R.string.mail_settings_swipe_action_spam_description,
                        getColor = { ProtonTheme.colors.notificationNorm },
                        staysDismissed = false
                    ),
                    start = SwipeUiModel(
                        swipeAction = SwipeAction.Archive,
                        icon = R.drawable.ic_proton_archive_box,
                        descriptionRes = R.string.mail_settings_swipe_action_archive_description,
                        getColor = { ProtonTheme.colors.notificationNorm },
                        staysDismissed = true
                    )
                )
            ),
            TestInput(
                input = SystemLabelId.Inbox.labelId to SwipeActionsPreference(
                    swipeLeft = SwipeAction.Star,
                    swipeRight = SwipeAction.MarkRead
                ),
                expected = SwipeActionsUiModel(
                    end = SwipeUiModel(
                        swipeAction = SwipeAction.Star,
                        icon = R.drawable.ic_proton_star,
                        descriptionRes = R.string.mail_settings_swipe_action_star_description,
                        getColor = { ProtonTheme.colors.notificationWarning },
                        staysDismissed = false
                    ),
                    start = SwipeUiModel(
                        swipeAction = SwipeAction.MarkRead,
                        icon = R.drawable.ic_proton_envelope_dot,
                        descriptionRes = R.string.mail_settings_swipe_action_read_description,
                        getColor = { ProtonTheme.colors.brandNorm },
                        staysDismissed = false
                    )
                )
            ),
            TestInput(
                input = SystemLabelId.Trash.labelId to SwipeActionsPreference(
                    swipeLeft = SwipeAction.Trash,
                    swipeRight = SwipeAction.MarkRead
                ),
                expected = SwipeActionsUiModel(
                    end = SwipeUiModel(
                        swipeAction = SwipeAction.Trash,
                        icon = R.drawable.ic_proton_trash,
                        descriptionRes = R.string.mail_settings_swipe_action_trash_description,
                        getColor = { ProtonTheme.colors.notificationError },
                        staysDismissed = false
                    ),
                    start = SwipeUiModel(
                        swipeAction = SwipeAction.MarkRead,
                        icon = R.drawable.ic_proton_envelope_dot,
                        descriptionRes = R.string.mail_settings_swipe_action_read_description,
                        getColor = { ProtonTheme.colors.brandNorm },
                        staysDismissed = false
                    )
                )
            ),
            TestInput(
                input = SystemLabelId.Archive.labelId to SwipeActionsPreference(
                    swipeLeft = SwipeAction.Archive,
                    swipeRight = SwipeAction.Spam
                ),
                expected = SwipeActionsUiModel(
                    end = SwipeUiModel(
                        swipeAction = SwipeAction.Archive,
                        icon = R.drawable.ic_proton_archive_box,
                        descriptionRes = R.string.mail_settings_swipe_action_archive_description,
                        getColor = { ProtonTheme.colors.notificationNorm },
                        staysDismissed = false
                    ),
                    start = SwipeUiModel(
                        swipeAction = SwipeAction.Spam,
                        icon = R.drawable.ic_proton_fire,
                        descriptionRes = R.string.mail_settings_swipe_action_spam_description,
                        getColor = { ProtonTheme.colors.brandNorm },
                        staysDismissed = true
                    )
                )
            ),
            TestInput(
                input = SystemLabelId.Starred.labelId to SwipeActionsPreference(
                    swipeLeft = SwipeAction.None,
                    swipeRight = SwipeAction.MarkRead
                ),
                expected = SwipeActionsUiModel(
                    end = SwipeUiModel(
                        swipeAction = SwipeAction.None,
                        icon = R.drawable.ic_proton_cross_circle,
                        descriptionRes = R.string.mail_settings_swipe_action_none_description,
                        getColor = { ProtonTheme.colors.notificationNorm },
                        staysDismissed = false
                    ),
                    start = SwipeUiModel(
                        swipeAction = SwipeAction.MarkRead,
                        icon = R.drawable.ic_proton_envelope_dot,
                        descriptionRes = R.string.mail_settings_swipe_action_read_description,
                        getColor = { ProtonTheme.colors.brandNorm },
                        staysDismissed = false
                    )
                )
            ),
            TestInput(
                input = SystemLabelId.Drafts.labelId to SwipeActionsPreference(
                    swipeLeft = SwipeAction.LabelAs,
                    swipeRight = SwipeAction.None
                ),
                expected = SwipeActionsUiModel(
                    end = SwipeUiModel(
                        swipeAction = SwipeAction.LabelAs,
                        icon = R.drawable.ic_proton_tag,
                        descriptionRes = R.string.mail_settings_swipe_action_label_as_description,
                        getColor = { ProtonTheme.colors.notificationNorm },
                        staysDismissed = false
                    ),
                    start = SwipeUiModel(
                        swipeAction = SwipeAction.None,
                        icon = R.drawable.ic_proton_cross_circle,
                        descriptionRes = R.string.mail_settings_swipe_action_none_description,
                        getColor = { ProtonTheme.colors.notificationNorm },
                        staysDismissed = false
                    )
                )
            ),
            TestInput(
                input = SystemLabelId.AllMail.labelId to SwipeActionsPreference(
                    swipeLeft = SwipeAction.MoveTo,
                    swipeRight = SwipeAction.Star
                ),
                expected = SwipeActionsUiModel(
                    end = SwipeUiModel(
                        swipeAction = SwipeAction.MoveTo,
                        icon = R.drawable.ic_proton_folder_arrow_in,
                        descriptionRes = R.string.mail_settings_swipe_action_move_to_description,
                        getColor = { ProtonTheme.colors.notificationNorm },
                        staysDismissed = false
                    ),
                    start = SwipeUiModel(
                        swipeAction = SwipeAction.Star,
                        icon = R.drawable.ic_proton_star,
                        descriptionRes = R.string.mail_settings_swipe_action_star_description,
                        getColor = { ProtonTheme.colors.notificationWarning },
                        staysDismissed = false
                    )
                )
            )
        )

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data(): List<Array<Any>> = transitions.map { arrayOf(it) }
    }

    data class TestInput(
        val input: Pair<LabelId, SwipeActionsPreference>,
        val expected: SwipeActionsUiModel
    )

}
