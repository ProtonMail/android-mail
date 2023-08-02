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

import java.net.UnknownHostException
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.mapper.fromHttpCode
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.model.NetworkError
import me.proton.core.network.domain.ApiResult
import timber.log.Timber

fun <T : Any> ApiResult<T>.toEither(): Either<DataError.Remote, T> = when (this) {
    is ApiResult.Success -> value.right()

    is ApiResult.Error.Http -> {
        DataError.Remote.Http(NetworkError.fromHttpCode(httpCode), this.extractApiErrorInfo()).left()
    }

    is ApiResult.Error.Parse -> {
        Timber.e("Unexpected parse error, caused by: ${this.cause}")
        DataError.Remote.Http(NetworkError.Parse, this.cause.tryExtractError()).left()
    }

    is ApiResult.Error.Connection -> {
        DataError.Remote.Http(toNetworkError(this), this.cause.tryExtractError()).left()
    }
}

private fun Throwable?.tryExtractError() = this?.cause?.message ?: "No error message found"

private fun ApiResult.Error.Http.extractApiErrorInfo(): String = "${this.message} - ${this.proton?.error}"

private fun toNetworkError(apiResult: ApiResult.Error.Connection): NetworkError = when (apiResult) {
    is ApiResult.Error.NoInternet -> NetworkError.NoNetwork
    else -> if (apiResult.cause is UnknownHostException) {
        NetworkError.NoNetwork
    } else {
        NetworkError.Unreachable
    }
}
