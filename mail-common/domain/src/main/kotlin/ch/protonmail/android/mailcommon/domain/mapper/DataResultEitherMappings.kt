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

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.model.NetworkError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.transform
import me.proton.core.domain.arch.DataResult
import me.proton.core.util.kotlin.takeIfNotEmpty

fun <T> Flow<DataResult<T>>.mapToEither(): Flow<Either<DataError, T>> = transform { dataResult ->
    when (dataResult) {
        is DataResult.Error.Local -> emit(toLocalError(dataResult).left())
        is DataResult.Error.Remote -> emit(toRemoteDataError(dataResult).left())
        is DataResult.Processing -> Unit
        is DataResult.Success -> emit(dataResult.value.right())
    }
}

private fun toLocalError(dataResult: DataResult.Error.Local): DataError.Local {
    throw RuntimeException(
        "Unhandled local error $dataResult, message = ${messageFrom(dataResult)}",
        dataResult.cause
    )
}

private fun toRemoteDataError(dataResult: DataResult.Error.Remote): DataError.Remote {
    return when {
        dataResult.protonCode != INITIAL_ERROR_CODE -> toProtonDataError(dataResult)
        dataResult.httpCode != INITIAL_ERROR_CODE -> toHttpDataError(dataResult)
        else -> throw RuntimeException(
            "Unhandled remote error $dataResult, message = ${messageFrom(dataResult)}",
            dataResult.cause
        )
    }
}

private fun toHttpDataError(dataResult: DataResult.Error.Remote): DataError.Remote.Http {
    @Suppress("MagicNumber")
    val networkError = when (dataResult.httpCode) {
        401 -> NetworkError.Unauthorized
        403 -> NetworkError.Forbidden
        404 -> NetworkError.NotFound
        in 500 until 600 -> NetworkError.ServerError
        else -> throw RuntimeException(
            "Unhandled http error $dataResult, message = ${messageFrom(dataResult)}",
            dataResult.cause
        )
    }
    return DataError.Remote.Http(networkError)
}

private fun toProtonDataError(dataResult: DataResult.Error.Remote): DataError.Remote.Proton =
    throw RuntimeException(
        "Unhandled Proton error $dataResult, message = ${messageFrom(dataResult)}",
        dataResult.cause
    )

private fun messageFrom(dataResult: DataResult.Error): String =
    dataResult.message?.takeIfNotEmpty()
        ?: dataResult.cause?.message?.takeIfNotEmpty()
        ?: DATA_RESULT_NO_MESSAGE_PROVIDED

private const val INITIAL_ERROR_CODE = 0
internal const val DATA_RESULT_NO_MESSAGE_PROVIDED = "DataResult didn't provide any message"
