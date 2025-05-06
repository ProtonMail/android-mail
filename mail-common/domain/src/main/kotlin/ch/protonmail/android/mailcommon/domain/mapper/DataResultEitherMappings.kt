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
@file:Suppress("TooGenericExceptionThrown")

package ch.protonmail.android.mailcommon.domain.mapper

import java.net.UnknownHostException
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.model.NetworkError
import ch.protonmail.android.mailcommon.domain.model.ProtonError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.transform
import me.proton.core.domain.arch.DataResult
import me.proton.core.network.data.ProtonErrorException
import me.proton.core.network.domain.ApiException
import retrofit2.HttpException
import timber.log.Timber

fun <T> Flow<DataResult<T>>.mapToEither(): Flow<Either<DataError, T>> = transform { dataResult ->
    when (dataResult) {
        is DataResult.Error.Local -> emit(toLocalDataError(dataResult).left())
        is DataResult.Error.Remote -> emit(toRemoteDataError(dataResult).left())
        is DataResult.Processing -> Unit
        is DataResult.Success -> emit(dataResult.value.right())
    }
}

private fun toLocalDataError(dataResult: DataResult.Error.Local): DataError.Local = unhandledLocalError(dataResult)

private fun toRemoteDataError(dataResult: DataResult.Error.Remote): DataError.Remote {
    return when {
        dataResult.protonCode != INITIAL_ERROR_CODE -> toProtonDataError(dataResult)
        dataResult.httpCode != INITIAL_ERROR_CODE -> toHttpDataError(dataResult)
        else -> handleRemoteError(dataResult)
    }
}

private fun toProtonDataError(dataResult: DataResult.Error.Remote): DataError.Remote.Proton {
    val protonError = ProtonError.fromProtonCode(dataResult.protonCode)
    if (protonError == ProtonError.Unknown) {
        Timber.e("UNHANDLED PROTON ERROR caused by result: $dataResult")
    }
    return DataError.Remote.Proton(protonError, apiMessage = dataResult.message)
}

private fun toHttpDataError(dataResult: DataResult.Error.Remote): DataError.Remote.Http {
    val networkError = NetworkError.fromHttpCode(dataResult.httpCode)
    if (networkError == NetworkError.Unknown) {
        Timber.e("UNHANDLED NETWORK ERROR caused by result: $dataResult")
    }
    return DataError.Remote.Http(networkError, dataResult.tryExtractErrorMessage())
}

private fun handleRemoteError(dataResult: DataResult.Error.Remote): DataError.Remote =
    when (val cause = dataResult.cause) {
        is ApiException -> handleApiException(cause, dataResult)
        else -> unhandledRemoteError(dataResult)
    }

private fun handleApiException(cause: ApiException, dataResult: DataResult.Error.Remote) =
    when (val innerCause = cause.cause) {
        is UnknownHostException -> DataError.Remote.Http(NetworkError.NoNetwork, dataResult.tryExtractErrorMessage())
        is HttpException -> toHttpDataError(dataResult.copy(httpCode = innerCause.code()))
        is ProtonErrorException -> toProtonDataError(dataResult.copy(protonCode = innerCause.protonData.code))
        else -> unhandledRemoteError(dataResult)
    }

private fun DataResult.Error.Remote.tryExtractErrorMessage() = this.message ?: "No error message found"

private fun unhandledRemoteError(dataResult: DataResult.Error.Remote): DataError.Remote.Unknown {
    Timber.e("UNHANDLED REMOTE ERROR caused by result: $dataResult")
    return DataError.Remote.Unknown
}

private fun unhandledLocalError(dataResult: DataResult.Error.Local): DataError.Local.Unknown {
    Timber.e("UNHANDLED LOCAL ERROR caused by result: $dataResult")
    return DataError.Local.Unknown
}

private const val INITIAL_ERROR_CODE = 0
