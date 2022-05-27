/*
 *  Copyright (c) 2021 Proton Technologies AG
 *  This file is part of Proton Technologies AG and ProtonCore.
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

plugins {
    id("com.android.library")
    kotlin("android")
}

android {
    compileSdk = Config.compileSdk

    defaultConfig {
        minSdk = Config.minSdk
        targetSdk = Config.targetSdk
    }
}

dependencies {
    implementation(Proton.Core.account)
    implementation(Proton.Core.domain)
    implementation(Proton.Core.featureFlag)
    implementation(Proton.Core.label)
    implementation(Proton.Core.mailSettings)
    implementation(Proton.Core.network)
    implementation(Proton.Core.user)
    implementation(Proton.Core.userSettings)

    implementation(project(":mail-common"))
    implementation(project(":mail-conversation"))
    implementation(project(":mail-label"))
    implementation(project(":mail-mailbox"))
    implementation(project(":mail-message"))
}
