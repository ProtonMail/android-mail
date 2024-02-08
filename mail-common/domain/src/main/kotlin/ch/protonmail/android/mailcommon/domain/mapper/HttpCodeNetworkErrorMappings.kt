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

package ch.protonmail.android.mailcommon.domain.mapper

import ch.protonmail.android.mailcommon.domain.model.NetworkError

@Suppress("MagicNumber")
fun NetworkError.Companion.fromHttpCode(httpCode: Int): NetworkError = when (httpCode) {
    400 -> NetworkError.BadRequest
    401 -> NetworkError.Unauthorized
    403 -> NetworkError.Forbidden
    404 -> NetworkError.NotFound
    422 -> NetworkError.UnprocessableEntity
    in 500..599 -> NetworkError.ServerError
    else -> NetworkError.Unknown
}
