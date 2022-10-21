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

package ch.protonmail.android.db

import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import ch.protonmail.android.mailcommon.data.sample.AccountEntitySample
import ch.protonmail.android.mailcommon.data.sample.AddressEntitySample
import ch.protonmail.android.mailcommon.data.sample.SessionEntitySample
import ch.protonmail.android.mailcommon.data.sample.UserEntitySample
import kotlin.test.AfterTest

@Suppress("UnnecessaryAbstractClass", "MemberVisibilityCanBePrivate")
abstract class BaseDatabaseTest {

    protected val database by lazy {
        Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).build()
    }
    protected val accountDao by lazy { database.accountDao() }
    protected val addressDao by lazy { database.addressDao() }
    protected val messageDao by lazy { database.messageDao() }
    protected val messageLabelDao by lazy { database.messageLabelDao() }
    protected val labelDao by lazy { database.labelDao() }
    protected val sessionDao by lazy { database.sessionDao() }
    protected val userDao by lazy { database.userDao() }

    @AfterTest
    fun teardown() {
        database.close()
    }

    protected suspend fun insertPrimaryUser() {
        accountDao.insertOrIgnore(AccountEntitySample.PrimaryNotReady)
        database.withTransaction {
            sessionDao.insertOrIgnore(SessionEntitySample.Primary)
            accountDao.insertOrUpdate(AccountEntitySample.Primary)
            userDao.insertOrIgnore(UserEntitySample.Primary)
        }
        addressDao.insertOrIgnore(AddressEntitySample.Primary)
    }
}
