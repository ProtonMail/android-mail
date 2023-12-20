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

package ch.protonmail.android.uitest.e2e.mailbox.errors.pulltorefresh

import ch.protonmail.android.uitest.models.avatar.AvatarInitial
import ch.protonmail.android.uitest.models.mailbox.MailboxListItemEntry
import ch.protonmail.android.uitest.models.mailbox.ParticipantEntry
import ch.protonmail.android.uitest.robot.common.section.snackbarSection
import ch.protonmail.android.uitest.robot.common.section.verify
import ch.protonmail.android.uitest.robot.mailbox.mailboxRobot
import ch.protonmail.android.uitest.robot.mailbox.model.snackbar.MailboxSnackbar
import ch.protonmail.android.uitest.robot.mailbox.section.emptyListSection
import ch.protonmail.android.uitest.robot.mailbox.section.fullScreenErrorSection
import ch.protonmail.android.uitest.robot.mailbox.section.listSection
import ch.protonmail.android.uitest.robot.mailbox.section.verify

internal interface MailboxPullToRefreshErrorTests {

    private val baseItem: MailboxListItemEntry
        get() = MailboxListItemEntry(
            index = 0,
            avatarInitial = AvatarInitial.WithText("M"),
            participants = listOf(ParticipantEntry.WithParticipant("mobileappsuitesting2")),
            subject = "Test message",
            date = "Mar 6, 2023"
        )

    fun verifyEmptyToContent() {
        mailboxRobot {
            emptyListSection {
                verify { isShown() }

                pullDownToRefresh()
            }

            listSection {
                verify { listItemsAreShown(baseItem) }
            }
        }
    }

    fun verifyErrorToEmpty() {
        mailboxRobot {
            fullScreenErrorSection {
                verify { isShown() }

                pullDownToRefresh()
            }

            emptyListSection {
                verify { isShown() }
            }
        }
    }

    fun verifyEmptyToError() {
        mailboxRobot {
            emptyListSection {
                verify { isShown() }

                pullDownToRefresh()
            }

            fullScreenErrorSection {
                verify { isShown() }
            }
        }
    }

    fun verifyErrorToContent() {
        mailboxRobot {
            fullScreenErrorSection {
                verify { isShown() }

                pullDownToRefresh()
            }

            listSection {
                verify { listItemsAreShown(baseItem) }
            }
        }
    }

    fun verifyContentToError() {
        mailboxRobot {
            listSection {
                verify { listItemsAreShown(baseItem) }

                pullDownToRefresh()
            }

            snackbarSection {
                verify { isDisplaying(MailboxSnackbar.FailedToLoadNewItems) }
            }

            listSection {
                verify { listItemsAreShown(baseItem) }
            }
        }
    }

    fun verifyErrorToError() {
        mailboxRobot {
            fullScreenErrorSection {
                verify { isShown() }
                pullDownToRefresh()
                verify { isShown() }
            }
        }
    }
}
