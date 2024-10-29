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

plugins {
    id("kotlin")
    kotlin("kapt")
}

dependencies {
    kapt(libs.bundles.app.annotationProcessors)

    api(libs.mockwebserver)
    implementation(libs.dagger.hilt.core)
    implementation(libs.kotlin.coroutines.core)
    implementation(libs.okhttp.tls)

    testImplementation(libs.kotlin.coroutines.test)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.okhttp)
}
