/*
 * Copyright (c) 2025 Proton Technologies AG
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

package ch.protonmail.android.mail.plugin

import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Selects the Account integration to compile, driven by the `accountNew` Gradle property.
 *
 * When enabled, the Account SDK dependencies and the `src/accountNew` sources are used; otherwise
 * neither is part of the build and the legacy auth flow in `src/accountOld` is compiled instead.
 *
 * The Account SDK is only published from the `external/clients-monorepo` submodule, so enabling this
 * also requires `buildFromSource=true`; `settings.gradle.kts` enforces that.
 */
class AccountSdkPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        val enabled = target.providers.gradleProperty(ACCOUNT_NEW_PROPERTY).orNull.toBoolean()

        val mainSources = if (enabled) "src/accountNew/kotlin" else "src/accountOld/kotlin"
        val testSources = if (enabled) "src/testAccountNew/kotlin" else "src/testAccountOld/kotlin"

        target.pluginManager.withPlugin(ANDROID_APPLICATION_PLUGIN) {
            val android = target.extensions.getByType(ApplicationExtension::class.java)
            android.sourceSets.getByName("main").java.srcDir(mainSources)
            android.sourceSets.getByName("test").java.srcDir(testSources)
        }

        if (!enabled) return

        // Declared without a version on purpose: the `account` and `core` included builds substitute
        // these coordinates for local projects, so there is no version to resolve.
        ACCOUNT_SDK_DEPENDENCIES.forEach { coordinates ->
            target.dependencies.add("implementation", coordinates)
        }
    }

    private companion object {

        const val ACCOUNT_NEW_PROPERTY = "accountNew"
        const val ANDROID_APPLICATION_PLUGIN = "com.android.application"

        val ACCOUNT_SDK_DEPENDENCIES = listOf(
            "me.proton.android.account:account-api",
            "me.proton.android.account:account-crux",
            "me.proton.android.account:account-entry",
            "me.proton.android.account:account-logging",
            "me.proton.android.core:core-env",
            "me.proton.android.core:core-fido-google",
            "me.proton.android.core:core-logging-sentry"
        )
    }
}
