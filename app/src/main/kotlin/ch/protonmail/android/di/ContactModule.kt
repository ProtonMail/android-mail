/*
 * Copyright (c) 2021 Proton Technologies AG
 * This file is part of Proton Technologies AG and ProtonMail.
 *
 * ProtonMail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ProtonMail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ProtonMail.  If not, see <https://www.gnu.org/licenses/>.
 */

package ch.protonmail.android.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import me.proton.core.contact.data.api.ContactRemoteDataSourceImpl
import me.proton.core.contact.data.local.db.ContactLocalDataSourceImpl
import me.proton.core.contact.data.repository.ContactRepositoryImpl
import me.proton.core.contact.domain.repository.ContactLocalDataSource
import me.proton.core.contact.domain.repository.ContactRemoteDataSource
import me.proton.core.contact.domain.repository.ContactRepository

@Module
@InstallIn(SingletonComponent::class)
abstract class ContactModule {

    @Binds
    abstract fun bindContactLocalDataSource(impl: ContactLocalDataSourceImpl): ContactLocalDataSource

    @Binds
    abstract fun bindContactRemoteDataSource(impl: ContactRemoteDataSourceImpl): ContactRemoteDataSource

    @Binds
    abstract fun provideContactsRepository(impl: ContactRepositoryImpl): ContactRepository
}
