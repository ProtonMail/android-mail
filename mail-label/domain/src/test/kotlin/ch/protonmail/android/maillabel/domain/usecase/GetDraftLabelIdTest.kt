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

package ch.protonmail.android.maillabel.domain.usecase

import java.net.SocketTimeoutException
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.model.NetworkError
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.testdata.mailsettings.MailSettingsTestData.buildMailSettings
import io.mockk.coEvery
import io.mockk.mockk
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.arch.ResponseSource
import me.proton.core.domain.entity.UserId
import me.proton.core.domain.type.IntEnum
import me.proton.core.mailsettings.domain.entity.ShowMoved
import me.proton.core.mailsettings.domain.repository.MailSettingsRepository
import org.junit.Before
import kotlin.test.Test
import kotlin.test.assertEquals

class GetDraftLabelIdTest {
    private val mailSettingsRepository: MailSettingsRepository = mockk()
    private val observePrimaryUserId: ObservePrimaryUserId = mockk()
    private lateinit var sut: GetDraftLabelId

    private val userId = UserId("user-1")

    private val settingsWithMovedBoth = buildMailSettings(
        showMoved = IntEnum(3, ShowMoved.Both)
    )

    private val settingsWithoutMoved = buildMailSettings(
        showMoved = IntEnum(3, ShowMoved.None)
    )

    private fun isMovedEnabled(isMovedEnabld: Boolean) {
        coEvery { mailSettingsRepository.getMailSettingsFlow(userId) } returns
            flowOf(
                DataResult.Success(
                    ResponseSource.Remote,
                    value = if (isMovedEnabld) settingsWithMovedBoth else settingsWithoutMoved
                )
            )
    }

    @Before
    fun setUp() {
        sut = GetDraftLabelId(mailSettingsRepository, observePrimaryUserId)
    }

    @Test
    fun `invoke returns AllDrafts when showMoved is Both`() = runTest {
        // given
        coEvery { observePrimaryUserId.invoke() } returns flowOf(userId)
        isMovedEnabled(true)

        // when
        val actual = sut()

        // then
        assertTrue(actual.isRight())
        assertEquals(MailLabelId.System.AllDrafts, actual.getOrNull())
    }

    @Test
    fun `invoke returns Drafts when showMoved is None`() = runTest {
        // given
        coEvery { observePrimaryUserId.invoke() } returns flowOf(userId)
        isMovedEnabled(false)

        // when
        val actual = sut()

        // then
        assertTrue(actual.isRight())
        assertEquals(MailLabelId.System.Drafts, actual.getOrNull())
    }

    @Test
    fun `invoke maps SocketTimeoutException to Unreachable`() = runTest {
        // given
        coEvery { observePrimaryUserId.invoke() } returns flowOf(userId)
        coEvery { mailSettingsRepository.getMailSettingsFlow(userId) } throws SocketTimeoutException()

        // when
        val result = sut()

        // then
        assertTrue(result.isLeft())
        val error = result.swap().getOrNull()!!
        assertTrue(error is DataError.Remote.Http)
        assertEquals(NetworkError.Unreachable, (error as DataError.Remote.Http).networkError)
    }
}
