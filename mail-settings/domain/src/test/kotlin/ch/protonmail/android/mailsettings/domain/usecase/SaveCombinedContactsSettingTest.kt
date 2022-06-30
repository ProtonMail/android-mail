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

import ch.protonmail.android.mailsettings.domain.model.CombinedContactsPreference
import ch.protonmail.android.mailsettings.domain.repository.CombinedContactsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class SaveCombinedContactsSettingTest {

    private val combinedContactsRepository: CombinedContactsRepository = mockk {
        coEvery { save(any()) } just runs
    }

    private val saveCombinedContactsSetting = SaveCombinedContactsSetting(combinedContactsRepository)

    @Test
    fun `should call save method from repository when use case is invoked`() = runTest {
        // given
        val combinedContactsPreference = CombinedContactsPreference(isEnabled = true)

        // when
        saveCombinedContactsSetting(combinedContactsPreference.isEnabled)

        // then
        coVerify { combinedContactsRepository.save(combinedContactsPreference) }
    }
}
