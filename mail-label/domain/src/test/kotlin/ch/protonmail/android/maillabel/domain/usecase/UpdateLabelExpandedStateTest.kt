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
import ch.protonmail.android.testdata.label.LabelTestData.buildLabel
import ch.protonmail.android.testdata.user.UserIdTestData
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.core.label.domain.entity.LabelId
import me.proton.core.label.domain.entity.LabelType
import me.proton.core.label.domain.repository.LabelRepository
import org.junit.Before
import org.junit.Test

class UpdateLabelExpandedStateTest {

    private val userId = UserIdTestData.userId
    private val folderId = MailLabelId.Custom.Folder(LabelId("1"))

    private val labelRepository = mockk<LabelRepository> {
        coEvery { getLabel(any(), any(), any()) } returns buildLabel(
            userId = userId,
            type = LabelType.MessageFolder,
            id = folderId.labelId.id,
            isExpanded = false
        )
        coEvery { updateLabelIsExpanded(any(), any(), any(), any()) } returns Unit
    }

    private lateinit var updateLabelExpandedState: UpdateLabelExpandedState

    @Before
    fun setUp() {
        updateLabelExpandedState = UpdateLabelExpandedState(labelRepository)
    }

    @Test
    fun `invoke call labelRepository`() = runTest {
        // When
        updateLabelExpandedState.invoke(userId, folderId, true)

        // Then
        coVerify { labelRepository.updateLabelIsExpanded(userId, LabelType.MessageFolder, folderId.labelId, true) }
    }
}
