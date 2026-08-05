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
    `kotlin-dsl`
}

repositories {
    google()
    mavenCentral()
}

kotlin {
    // Pin both compileJava and compileKotlin, which otherwise default to the JDK running the daemon.
    jvmToolchain(17)
}

dependencies {
    // Provided by the consuming build's buildscript classpath, so only needed to compile against.
    compileOnly(libs.android.tools.build)
}

gradlePlugin {
    plugins {
        create("versionPlugin") {
            id = "app-config-plugin"
            implementationClass = "ch.protonmail.android.mail.plugin.ApplicationConfigPlugin"
        }
        create("accountSdkPlugin") {
            id = "account-sdk-plugin"
            implementationClass = "ch.protonmail.android.mail.plugin.AccountSdkPlugin"
        }
    }
}
