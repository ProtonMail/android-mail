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

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import ch.protonmail.android.MainActivity
import ch.protonmail.android.di.AppDatabaseModule
import kotlinx.coroutines.runBlocking
import me.proton.core.test.android.instrumented.ProtonTest.Companion.getTargetContext
import me.proton.core.test.android.plugins.data.User.Users
import org.junit.After
import org.junit.Rule
import org.junit.rules.RuleChain
import org.junit.rules.TestName
import timber.log.Timber

open class BaseTest(
    private val clearAppDatabaseOnTearDown: Boolean = true
) {

    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Rule
    @JvmField
    val ruleChain: RuleChain = RuleChain
        .outerRule(TestName())
        .around(composeTestRule)

    @After
    fun tearDown() {
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
