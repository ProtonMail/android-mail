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

package ch.protonmail.android.mailcommon.data.db.dao

import arrow.core.Either
import arrow.core.raise.either
import ch.protonmail.android.mailcommon.domain.model.DaoError
import me.proton.core.data.room.db.BaseDao
import timber.log.Timber

suspend fun <T> BaseDao<T>.upsertOrError(vararg entities: T): Either<DaoError.UpsertError, Unit> = either {
    runCatching {
        insertOrUpdate(*entities)
    }.onFailure {
        Timber.d("Error when performing upsertOrError - ${it::class.java} - ${it.message}")

        raise(DaoError.UpsertError(it))
    }
}
