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

package ch.protonmail.android.uitest.e2e.mailbox.errors.append

import ch.protonmail.android.uitest.e2e.mailbox.errors.append.ScrollThreshold.FirstScrollThreshold
import ch.protonmail.android.uitest.e2e.mailbox.errors.append.ScrollThreshold.SecondScrollThreshold
import ch.protonmail.android.uitest.models.mailbox.MailboxListItemEntry
import ch.protonmail.android.uitest.robot.mailbox.mailboxRobot
import ch.protonmail.android.uitest.robot.mailbox.section.appendErrorSection
import ch.protonmail.android.uitest.robot.mailbox.section.appendLoadingSection
import ch.protonmail.android.uitest.robot.mailbox.section.listSection
import ch.protonmail.android.uitest.robot.mailbox.section.verify

internal interface MailboxAppendItemsTests {

    val lastExpectedMailboxItem: MailboxListItemEntry

    fun verifyAppendAdditionalItems() = verifyAppendItemsLoading(expectError = false)

    fun verifyAppendAdditionalItemsErrorAndRetry() = verifyAppendItemsLoading(expectError = true)

    private fun verifyAppendItemsLoading(expectError: Boolean) {
        mailboxRobot {
            listSection {
                scrollToItemAtIndex(FirstScrollThreshold)
                scrollToItemAtIndex(SecondScrollThreshold)
            }

            appendLoadingSection { verify { isShown() } }

            if (expectError) {
                appendErrorSection {
                    verify { isShown() }
                    tapRetryButton()
                    verify { isHidden() }
                }
            }

            appendLoadingSection { verify { isHidden() } }

            listSection {
                verify { listItemsAreShown(lastExpectedMailboxItem) }
            }
        }
    }
}

private object ScrollThreshold {

    const val FirstScrollThreshold = 75
    const val SecondScrollThreshold = 99
}
