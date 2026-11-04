/*
 * Copyright (c) 2026 Proton Technologies AG
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

package ch.protonmail.android.mailbyoe.dagger

import ch.protonmail.android.mailbyoe.data.repository.ByoeGuidanceRepositoryImpl
import ch.protonmail.android.mailbyoe.data.repository.ByoeImportRepositoryImpl
import ch.protonmail.android.mailbyoe.domain.repository.ByoeGuidanceRepository
import ch.protonmail.android.mailbyoe.domain.repository.ByoeImportRepository
import dagger.Binds
import dagger.Module
import dagger.Reusable
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface ByoeModule {

    @Binds
    @Reusable
    fun bindByoeGuidanceRepository(impl: ByoeGuidanceRepositoryImpl): ByoeGuidanceRepository

    @Binds
    @Reusable
    fun bindByoeImportRepository(impl: ByoeImportRepositoryImpl): ByoeImportRepository
}
