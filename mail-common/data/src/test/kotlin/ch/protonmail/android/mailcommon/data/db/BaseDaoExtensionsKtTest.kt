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

package ch.protonmail.android.mailcommon.data.db

import android.database.sqlite.SQLiteConstraintException
import ch.protonmail.android.mailcommon.data.db.dao.upsertOrError
import ch.protonmail.android.mailcommon.domain.model.DaoError
import io.mockk.coEvery
import io.mockk.spyk
import kotlinx.coroutines.test.runTest
import me.proton.core.data.room.db.BaseDao
import org.junit.Test
import kotlin.test.assertIs

internal class BaseDaoExtensionsKtTest {

    private val dao = spyk<BaseDao<Int>>()

    @Test
    fun `when the underlying calls throw, should wrap the throwable into an error`() = runTest {
        coEvery { dao.insertOrUpdate(1) } throws SQLiteConstraintException()

        // When
        val error = dao.upsertOrError(entities = arrayOf(1)).leftOrNull()

        // Then
        assertIs<DaoError.UpsertError>(error)
        assertIs<SQLiteConstraintException>(error.throwable)
    }
}
