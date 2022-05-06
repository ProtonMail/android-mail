/*
 * Copyright (c) 2021 Proton Technologies AG
 * This file is part of Proton Technologies AG and ProtonMail.
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
 * along with ProtonMail.  If not, see <https://www.gnu.org/licenses/>.
 */

package ch.protonmail.android.di

import ch.protonmail.android.BuildConfig
import ch.protonmail.android.mailcommon.domain.MailFeatureDefault
import ch.protonmail.android.mailcommon.domain.MailFeatureId
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FeatureModule {

    private const val isAlphaOrDev = BuildConfig.FLAVOR == "dev" || BuildConfig.FLAVOR == "alpha"

    @Provides
    @Singleton
    fun provideMailFeatureFlag(): MailFeatureDefault = MailFeatureDefault(
        mapOf(
            MailFeatureId.ConversationMode to isAlphaOrDev,
            MailFeatureId.ShowSettings to isAlphaOrDev
        )
    )
}
