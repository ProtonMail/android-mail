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

package ch.protonmail.android.mailcommon.data.mapper

import ch.protonmail.android.mailcommon.domain.model.DataError
import uniffi.mail_uniffi.ProtonError
import uniffi.mail_uniffi_common.UserApiServiceError
import kotlin.test.Test
import kotlin.test.assertEquals

internal class RustErrorMapperTest {

    @Test
    fun `should map a bad request to a remote bad request`() {
        val error = ProtonError.ServerError(UserApiServiceError.BadRequest("bad query"))

        assertEquals(DataError.Remote.BadRequest, error.toDataError())
    }

    @Test
    fun `should map an unprocessable entity to a remote unprocessable entity`() {
        val error = ProtonError.ServerError(UserApiServiceError.UnprocessableEntity("bad query"))

        assertEquals(DataError.Remote.UnprocessableEntity, error.toDataError())
    }

    @Test
    fun `should map any other server error to a remote server error`() {
        val error = ProtonError.ServerError(UserApiServiceError.InternalServerError("down"))

        assertEquals(DataError.Remote.ServerError, error.toDataError())
    }
}
