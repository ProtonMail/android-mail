/*
 * Copyright (c) 2021 Proton Technologies AG
 * This file is part of Proton Technologies AG and ProtonCore.
 *
 * ProtonCore is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ProtonCore is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ProtonCore.  If not, see <https://www.gnu.org/licenses/>.
 */

package ch.protonmail.android.uitests

import ch.protonmail.android.MainActivity
import ch.protonmail.android.di.AppDatabaseModule
import kotlinx.coroutines.runBlocking
import me.proton.core.test.android.instrumented.ProtonTest
import me.proton.core.test.android.plugins.data.User.Users
import org.junit.After
import timber.log.Timber

open class BaseTest(
    private val clearAppDatabaseOnTearDown: Boolean = true,
    defaultTimeout: Long = 20_000L
) : ProtonTest(MainActivity::class.java, defaultTimeout) {

    @After
    override fun tearDown() {
        super.tearDown()
        if (clearAppDatabaseOnTearDown) {
            runBlocking {
                appDatabase.accountDao().deleteAll()
            }
        }
        Timber.d("Finishing Testing: Clearing all database tables")
    }

    companion object {
        val users = Users("users.json")
        val appDatabase = AppDatabaseModule.provideAppDatabase(getTargetContext())
    }
}
