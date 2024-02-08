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
     * 5xx error
     */
    object ServerError : NetworkError

    /**
     * Request is not authorized
     * 401 error
     */
    object Unauthorized : NetworkError

    /**
     * Requested host is not reachable
     */
    object Unreachable : NetworkError

    /**
     * Failed to parse the given response
     */
    object Parse : NetworkError

    /**
     * Request is not in the correct format
     */
    object BadRequest : NetworkError

    /**
     * Request is correct and understood but cannot be processed
     */
    object UnprocessableEntity : NetworkError

    /**
     * This object is not meant to be actively used.
     * Its purpose is to notify the logging tool that a case that should be handled
     * is not and to allow dedicated handling to be put in place.
     */
    object Unknown : NetworkError

    companion object
}
