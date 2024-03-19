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

package ch.protonmail.android.uitest.screen.mailbox

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import ch.protonmail.android.mailmailbox.presentation.mailbox.UnreadItemsFilter
import ch.protonmail.android.mailmailbox.presentation.mailbox.UnreadItemsFilterTestTags
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.UnreadFilterState
import ch.protonmail.android.test.annotations.suite.SmokeTest
import ch.protonmail.android.uitest.util.HiltInstrumentedTest
import dagger.hilt.android.testing.HiltAndroidTest
import me.proton.core.compose.theme.ProtonTheme
import org.junit.Test

@SmokeTest
@HiltAndroidTest
internal class MailboxUnreadFiltersTest : HiltInstrumentedTest() {

    private val unreadFilterNode by lazy {
        composeTestRule
            .onNodeWithTag(UnreadItemsFilterTestTags.UnreadFilterChip)
    }

    @Test
    fun unreadFilterItemDisplaysZeroCounterWhenZero() {
        // When
        setupCountersItem(CounterZeroValue)

        // Then
        unreadFilterNode.assertTextEquals(CounterZeroValueText)
    }

    @Test
    fun unreadFilterItemDisplaysCounterValueWhenBelowThreshold() {
        // When
        setupCountersItem(CounterValue)

        // Then
        unreadFilterNode.assertTextEquals(CounterValueText)
    }

    @Test
    fun unreadFilterItemDisplaysCappedCounterValueWhenAboveThreshold() {
        // When
        setupCountersItem(CounterValueAboveThreshold)

        // Then
        unreadFilterNode.assertTextEquals(CounterValueTextCapped)
    }

    private fun setupCountersItem(count: Int) {
        composeTestRule.setContent {
            ProtonTheme {
                UnreadItemsFilter(
                    state = UnreadFilterState.Data(
                        numUnread = count,
                        isFilterEnabled = false
                    ),
                    onFilterDisabled = {},
                    onFilterEnabled = {}
                )
            }
        }
    }

    private companion object {

        const val CounterZeroValue = 0
        const val CounterZeroValueText = "$CounterZeroValue unread"
        const val CounterValue = 10
        const val CounterValueText = "$CounterValue unread"
        const val CounterValueAboveThreshold = 10_000
        const val CounterValueTextCapped = "9999+ unread"
    }
}
