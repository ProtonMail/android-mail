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

package ch.protonmail.android.mailcommon.domain.model

/**
 * Errors related to Network
 */
sealed interface NetworkError {

    /**
     * Request is forbidden
     * 403 error
     */
    object Forbidden : NetworkError

    /**
     * Network connectivity is not available
     */
    object NoNetwork : NetworkError

    /**
     * Requested url cannot be found.
     * 404 error
     */
    object NotFound : NetworkError

    /**
     * Server has encountered an error.
     * 500 error
     */
    object Internal : NetworkError

    /**
     * Request is not authorized
     * 401 error
     */
    object Unauthorized : NetworkError

    /**
     * Requested host is not reachable
     */
    object Unreachable : NetworkError

    data class Other(val httpCode: Int) : NetworkError
}
