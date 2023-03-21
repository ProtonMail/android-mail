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

package ch.protonmail.android.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import me.proton.core.auth.domain.usecase.ValidateServerProof

/**
 * This module is an explicit provider for [ValidateServerProof] that is needed to perform the required
 * overrides when running UI Tests.
 *
 * There's no need to have this definition in the production code, as the dependency is still automatically provided.
 */
@Module
@InstallIn(SingletonComponent::class)
object ServerProofModule {

    @Provides
    fun provideServerProofValidation(): ValidateServerProof = ValidateServerProof()
}
