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

import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.testdata.mailsettings.MailSettingsTestData.buildMailSettings
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import me.proton.core.domain.type.IntEnum
import me.proton.core.mailsettings.domain.entity.ShowMoved
import me.proton.core.mailsettings.domain.repository.MailSettingsRepository
import org.junit.Before
import kotlin.test.Test
import kotlin.test.assertEquals

class GetDraftLabelIdTest {
    private val mailSettingsRepository: MailSettingsRepository = mockk()
    private lateinit var sut: GetDraftLabelId

    private val userId = UserId("user-1")

    private val settingsWithMovedBoth = buildMailSettings(
        showMoved = IntEnum(3, ShowMoved.Both)
    )

    private val settingsWithoutMoved = buildMailSettings(
        showMoved = IntEnum(3, ShowMoved.None)
    )

    private fun isMovedEnabled(isMovedEnabld: Boolean) {
        coEvery { mailSettingsRepository.getMailSettings(userId) } returns
            if (isMovedEnabld) settingsWithMovedBoth else settingsWithoutMoved
    }

    @Before
    fun setUp() {
        sut = GetDraftLabelId(mailSettingsRepository)
    }

    @Test
    fun `invoke returns AllDrafts when showMoved is Both`() = runTest {
        // given
        isMovedEnabled(true)

        // when
        val actual = sut(userId)

        // then
        assertEquals(MailLabelId.System.AllDrafts, actual)
    }

    @Test
    fun `invoke returns Drafts when showMoved is None`() = runTest {
        // given
        isMovedEnabled(false)

        // when
        val actual = sut(userId)

        // then
        assertEquals(MailLabelId.System.Drafts, actual)
    }
}
