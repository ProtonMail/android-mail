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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.transform
import me.proton.core.domain.arch.DataResult
import me.proton.core.network.domain.ApiException
import me.proton.core.util.kotlin.takeIfNotEmpty
import retrofit2.HttpException

fun <T> Flow<DataResult<T>>.mapToEither(): Flow<Either<DataError, T>> = transform { dataResult ->
    when (dataResult) {
        is DataResult.Error.Local -> emit(toLocalError(dataResult).left())
        is DataResult.Error.Remote -> emit(toRemoteDataError(dataResult).left())
        is DataResult.Processing -> Unit
        is DataResult.Success -> emit(dataResult.value.right())
    }
}

private fun toLocalError(dataResult: DataResult.Error.Local): DataError.Local {
    when (val cause = dataResult.cause) {
        null -> throw dataResult.asWrappedException()
        else -> throw cause
    }
}

private fun toRemoteDataError(dataResult: DataResult.Error.Remote): DataError.Remote {
    return when {
        dataResult.protonCode != INITIAL_ERROR_CODE -> toProtonDataError(dataResult)
        dataResult.httpCode != INITIAL_ERROR_CODE -> toHttpDataError(dataResult)
        else -> toNetworkDataError(dataResult)
    }
}

private fun toProtonDataError(dataResult: DataResult.Error.Remote): DataError.Remote.Proton {
    when (val cause = dataResult.cause) {
        null -> throw dataResult.asWrappedException()
        else -> throw cause
    }
}

private fun toHttpDataError(dataResult: DataResult.Error.Remote): DataError.Remote.Http {
    val networkError = NetworkError.fromHttpCodeOrNull(dataResult.httpCode)
        ?: throw dataResult.asWrappedException()
    return DataError.Remote.Http(networkError)
}

@Suppress("ThrowsCount")
private fun toNetworkDataError(dataResult: DataResult.Error.Remote): DataError.Remote.Http {
    return when (val cause = dataResult.cause) {
        null -> throw dataResult.asWrappedException()
        is ApiException -> when (val innerCause = cause.cause) {
            is UnknownHostException -> DataError.Remote.Http(NetworkError.NoNetwork)
            is HttpException -> toHttpDataError(dataResult.copy(httpCode = innerCause.code()))
            null -> throw cause
            else -> throw innerCause
        }
        else -> throw cause
    }
}

private fun DataResult.Error.asWrappedException(): MappingRuntimeException {
    val message = when (this) {
        is DataResult.Error.Local -> "Unhandled local error $this, message = ${messageFrom(this)}"
        is DataResult.Error.Remote -> "Unhandled remote error $this, message = ${messageFrom(this)}"
    }
    return MappingRuntimeException(message, cause)
}

private fun messageFrom(dataResult: DataResult.Error): String =
    dataResult.message?.takeIfNotEmpty()
        ?: dataResult.cause?.message?.takeIfNotEmpty()
        ?: DATA_RESULT_NO_MESSAGE_PROVIDED

private const val INITIAL_ERROR_CODE = 0
internal const val DATA_RESULT_NO_MESSAGE_PROVIDED = "DataResult didn't provide any message"

/**
 * This type overrides [message] and [cause] which prevents the coroutines stacktrace recovery
 * from copying this exception (and break referential equality).
 *
 * See StackTrace Recovery machinery:
 * https://github.com/Kotlin/kotlinx.coroutines/blob/master/docs/topics/debugging.md#stacktrace-recovery-machinery
 */
class MappingRuntimeException(
    override val message: String?,
    override val cause: Throwable? = null
) : RuntimeException(message, cause)
