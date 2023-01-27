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

import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies

// It's very easy to forget something when adding Hilt and finding the cause can be
// dreadful. Therefore, this little helper module ensure everything is setup correctly
fun Project.setAsHiltModule() {
    apply(plugin = "dagger.hilt.android.plugin")

    dependencies {
        implementation(listOf(Dagger.hiltAndroid))
        kapt(listOf(Dagger.hiltDaggerCompiler, AndroidX.Hilt.compiler))
        androidTestImplementation(listOf(Dagger.hiltAndroidTesting))
        kaptAndroidTest(listOf(Dagger.hiltDaggerCompiler))
    }
}
