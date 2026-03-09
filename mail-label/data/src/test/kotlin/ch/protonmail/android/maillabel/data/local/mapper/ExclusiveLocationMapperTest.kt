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

package ch.protonmail.android.maillabel.data.local.mapper

import ch.protonmail.android.mailcommon.data.mapper.LocalExclusiveLocationCustom
import ch.protonmail.android.mailcommon.data.mapper.LocalExclusiveLocationSystem
import ch.protonmail.android.maillabel.data.mapper.toExclusiveLocation
import ch.protonmail.android.maillabel.domain.model.ExclusiveLocation
import ch.protonmail.android.maillabel.domain.model.LabelId
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import org.junit.Test
import uniffi.mail_uniffi.Id
import uniffi.mail_uniffi.LabelColor
import uniffi.mail_uniffi.SystemLabel
import kotlin.test.assertEquals

class ExclusiveLocationMapperTest {

    @Test
    fun `should map LocalExclusiveLocation System correctly`() {
        // Given
        val localSystemLocation = LocalExclusiveLocationSystem(
            name = SystemLabel.TRASH,
            id = Id(100uL)
        )
        val expectedSystemLocation = ExclusiveLocation.System(
            systemLabelId = SystemLabelId.Trash,
            labelId = LabelId("100")
        )

        // When
        val result = localSystemLocation.toExclusiveLocation()

        // Then
        assertEquals(expectedSystemLocation, result)
    }

    @Test
    fun `should map LocalExclusiveLocation Custom to ExclusiveLocation Custom`() {
        // Given
        val localCustomLocation = LocalExclusiveLocationCustom(
            name = "Custom Label",
            id = Id(2uL),
            color = LabelColor("#FF5733")
        )
        val expectedCustomLocation = ExclusiveLocation.Folder(
            name = "Custom Label",
            labelId = LabelId("2"),
            color = "#FF5733"
        )

        // When
        val result = localCustomLocation.toExclusiveLocation()

        // Then
        assertEquals(expectedCustomLocation, result)
    }
}
