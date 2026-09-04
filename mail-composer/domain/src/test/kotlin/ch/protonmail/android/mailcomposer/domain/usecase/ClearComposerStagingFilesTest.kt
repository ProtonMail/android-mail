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

package ch.protonmail.android.mailcomposer.domain.usecase

import ch.protonmail.android.mailcomposer.domain.repository.CameraTempImageRepository
import ch.protonmail.android.mailcomposer.domain.repository.PastedImageRepository
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.AfterTest
import kotlin.test.Test
import io.mockk.unmockkAll

class ClearComposerStagingFilesTest {

    private val pastedImageRepository = mockk<PastedImageRepository>(relaxUnitFun = true)
    private val cameraTempImageRepository = mockk<CameraTempImageRepository>(relaxUnitFun = true)

    private val clearComposerStagingFiles = ClearComposerStagingFiles(
        pastedImageRepository,
        cameraTempImageRepository
    )

    @AfterTest
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `given staging repositories, when invoked, then clears both staging areas`() {
        // When
        clearComposerStagingFiles()

        // Then
        verify(exactly = 1) { pastedImageRepository.clearStagingArea() }
        verify(exactly = 1) { cameraTempImageRepository.clearStagingArea() }
    }
}
