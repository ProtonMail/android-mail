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

package ch.protonmail.android.uitest.filters

import androidx.test.filters.AbstractFilter
import ch.protonmail.android.test.annotations.suite.CoreLibraryTest
import org.junit.runner.Description

@Suppress("unused") // Used as CLI parameter
internal class CoreLibraryTestFilter : AbstractFilter() {

    override fun shouldRun(description: Description): Boolean = super.shouldRun(description)

    override fun describe(): String = "Filters core library tests only"

    override fun evaluateTest(description: Description): Boolean {
        return description.hasAnnotation(CoreLibraryTest::class.java)
    }
}
