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

package ch.protonmail.android.mailsettings.domain.usecase

import ch.protonmail.android.mailsettings.domain.model.SwipeActionDirection
import ch.protonmail.android.testdata.mailsettings.MailSettingsTestData
import ch.protonmail.android.testdata.user.UserIdTestData.userId
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.core.mailsettings.domain.entity.SwipeAction
import me.proton.core.mailsettings.domain.repository.MailSettingsRepository
import kotlin.test.Test

internal class UpdateSwipeActionPreferenceTest {

    private val mailSettingsRepository: MailSettingsRepository = mockk {
        coEvery { updateSwipeLeft(any(), any(), any()) } returns MailSettingsTestData.mailSettings
        coEvery { updateSwipeRight(any(), any(), any()) } returns MailSettingsTestData.mailSettings
    }
    private val updateSwipeActionPreference = UpdateSwipeActionPreference(mailSettingsRepository)

    @Test
    fun `correctly calls repository for update left with none action`() = runTest {
        // given
        val swipeAction = SwipeAction.None

        // when
        updateSwipeActionPreference(
            userId = userId,
            swipeActionDirection = SwipeActionDirection.LEFT,
            swipeAction = swipeAction
        )

        // then
        coVerify {
            mailSettingsRepository.updateSwipeLeft(userId = userId, swipeAction = swipeAction, syncWithRemote = true)
        }
    }

    @Test
    fun `correctly calls repository for update right with none action`() = runTest {
        // given
        val swipeAction = SwipeAction.None

        // when
        updateSwipeActionPreference(
            userId = userId,
            swipeActionDirection = SwipeActionDirection.RIGHT,
            swipeAction = swipeAction
        )

        // then
        coVerify {
            mailSettingsRepository.updateSwipeRight(userId = userId, swipeAction = swipeAction, syncWithRemote = true)
        }
    }

    @Test
    fun `correctly calls repository for update left with trash action`() = runTest {
        // given
        val swipeAction = SwipeAction.Trash

        // when
        updateSwipeActionPreference(
            userId = userId,
            swipeActionDirection = SwipeActionDirection.LEFT,
            swipeAction = swipeAction
        )

        // then
        coVerify {
            mailSettingsRepository.updateSwipeLeft(userId = userId, swipeAction = swipeAction, syncWithRemote = true)
        }
    }

    @Test
    fun `correctly calls repository for update right with trash action`() = runTest {
        // given
        val swipeAction = SwipeAction.Trash

        // when
        updateSwipeActionPreference(
            userId = userId,
            swipeActionDirection = SwipeActionDirection.RIGHT,
            swipeAction = swipeAction
        )

        // then
        coVerify {
            mailSettingsRepository.updateSwipeRight(userId = userId, swipeAction = swipeAction, syncWithRemote = true)
        }
    }

    @Test
    fun `correctly calls repository for update left with spam action`() = runTest {
        // given
        val swipeAction = SwipeAction.Spam

        // when
        updateSwipeActionPreference(
            userId = userId,
            swipeActionDirection = SwipeActionDirection.LEFT,
            swipeAction = swipeAction
        )

        // then
        coVerify {
            mailSettingsRepository.updateSwipeLeft(userId = userId, swipeAction = swipeAction, syncWithRemote = true)
        }
    }

    @Test
    fun `correctly calls repository for update right with spam action`() = runTest {
        // given
        val swipeAction = SwipeAction.Spam

        // when
        updateSwipeActionPreference(
            userId = userId,
            swipeActionDirection = SwipeActionDirection.RIGHT,
            swipeAction = swipeAction
        )

        // then
        coVerify {
            mailSettingsRepository.updateSwipeRight(userId = userId, swipeAction = swipeAction, syncWithRemote = true)
        }
    }

    @Test
    fun `correctly calls repository for update right with star action`() = runTest {
        // given
        val swipeAction = SwipeAction.Star

        // when
        updateSwipeActionPreference(
            userId = userId,
            swipeActionDirection = SwipeActionDirection.RIGHT,
            swipeAction = swipeAction
        )

        // then
        coVerify {
            mailSettingsRepository.updateSwipeRight(userId = userId, swipeAction = swipeAction, syncWithRemote = true)
        }
    }

    @Test
    fun `correctly calls repository for update left with start action`() = runTest {
        // given
        val swipeAction = SwipeAction.Star

        // when
        updateSwipeActionPreference(
            userId = userId,
            swipeActionDirection = SwipeActionDirection.LEFT,
            swipeAction = swipeAction
        )

        // then
        coVerify {
            mailSettingsRepository.updateSwipeLeft(userId = userId, swipeAction = swipeAction, syncWithRemote = true)
        }
    }

    @Test
    fun `correctly calls repository for update left with archive action`() = runTest {
        // given
        val swipeAction = SwipeAction.Archive

        // when
        updateSwipeActionPreference(
            userId = userId,
            swipeActionDirection = SwipeActionDirection.LEFT,
            swipeAction = swipeAction
        )

        // then
        coVerify {
            mailSettingsRepository.updateSwipeLeft(userId = userId, swipeAction = swipeAction, syncWithRemote = true)
        }
    }

    @Test
    fun `correctly calls repository for update right with archive action`() = runTest {
        // given
        val swipeAction = SwipeAction.Archive

        // when
        updateSwipeActionPreference(
            userId = userId,
            swipeActionDirection = SwipeActionDirection.RIGHT,
            swipeAction = swipeAction
        )

        // then
        coVerify {
            mailSettingsRepository.updateSwipeRight(userId = userId, swipeAction = swipeAction, syncWithRemote = true)
        }
    }

    @Test
    fun `correctly calls repository for update left with mark read action`() = runTest {
        // given
        val swipeAction = SwipeAction.MarkRead

        // when
        updateSwipeActionPreference(
            userId = userId,
            swipeActionDirection = SwipeActionDirection.LEFT,
            swipeAction = swipeAction
        )

        // then
        coVerify {
            mailSettingsRepository.updateSwipeLeft(userId = userId, swipeAction = swipeAction, syncWithRemote = true)
        }
    }

    @Test
    fun `correctly calls repository for update right with mark read action`() = runTest {
        // given
        val swipeAction = SwipeAction.MarkRead

        // when
        updateSwipeActionPreference(
            userId = userId,
            swipeActionDirection = SwipeActionDirection.RIGHT,
            swipeAction = swipeAction
        )

        // then
        coVerify {
            mailSettingsRepository.updateSwipeRight(userId = userId, swipeAction = swipeAction, syncWithRemote = true)
        }
    }

    @Test
    fun `correctly calls repository for update left with label as action`() = runTest {
        // given
        val swipeAction = SwipeAction.LabelAs

        // when
        updateSwipeActionPreference(
            userId = userId,
            swipeActionDirection = SwipeActionDirection.LEFT,
            swipeAction = swipeAction
        )

        // then
        coVerify {
            mailSettingsRepository.updateSwipeLeft(userId = userId, swipeAction = swipeAction, syncWithRemote = true)
        }
    }

    @Test
    fun `correctly calls repository for update right with label as action`() = runTest {
        // given
        val swipeAction = SwipeAction.LabelAs

        // when
        updateSwipeActionPreference(
            userId = userId,
            swipeActionDirection = SwipeActionDirection.RIGHT,
            swipeAction = swipeAction
        )

        // then
        coVerify {
            mailSettingsRepository.updateSwipeRight(userId = userId, swipeAction = swipeAction, syncWithRemote = true)
        }
    }

    @Test
    fun `correctly calls repository for update left with move to action`() = runTest {
        // given
        val swipeAction = SwipeAction.MoveTo

        // when
        updateSwipeActionPreference(
            userId = userId,
            swipeActionDirection = SwipeActionDirection.LEFT,
            swipeAction = swipeAction
        )

        // then
        coVerify {
            mailSettingsRepository.updateSwipeLeft(userId = userId, swipeAction = swipeAction, syncWithRemote = true)
        }
    }

    @Test
    fun `correctly calls repository for update right with move to action`() = runTest {
        // given
        val swipeAction = SwipeAction.MoveTo

        // when
        updateSwipeActionPreference(
            userId = userId,
            swipeActionDirection = SwipeActionDirection.RIGHT,
            swipeAction = swipeAction
        )

        // then
        coVerify {
            mailSettingsRepository.updateSwipeRight(userId = userId, swipeAction = swipeAction, syncWithRemote = true)
        }
    }
}
