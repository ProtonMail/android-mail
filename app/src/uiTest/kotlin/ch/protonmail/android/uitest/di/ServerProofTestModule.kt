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

package ch.protonmail.android.uitest.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.mockk.every
import io.mockk.mockk
import me.proton.core.auth.domain.usecase.ValidateServerProof

/**
 * A test module that does not override, but rather defines a provider for the [ValidateServerProof] use case.
 *
 * The provided [ValidateServerProof] instance is just a mock that allows to skip SRP validation
 * when running UI Tests against a mocked environment (as otherwise it will always fail).
 *
 * Note that this module needs to be uninstalled for E2E test suites running against real pre-production environments.
 */
@Module
@InstallIn(SingletonComponent::class)
object ServerProofTestModule {

    @Provides
    fun provideServerProofValidation(): ValidateServerProof = mockk {
        every { this@mockk.invoke(any(), any(), any()) } returns Unit
    }
}
