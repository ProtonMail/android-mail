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

package ch.protonmail.android.mailupselling.dagger

import android.content.Context
import ch.protonmail.android.mailupselling.data.UpsellingDataStoreProvider
import ch.protonmail.android.mailupselling.data.repository.UpsellingVisibilityRepositoryImpl
import ch.protonmail.android.mailupselling.domain.annotations.ForceOneClickUpsellingDetailsOverride
import ch.protonmail.android.mailupselling.domain.repository.UpsellingVisibilityRepository
import ch.protonmail.android.mailupselling.domain.usecase.featureflags.IsSignupPaidPlanSupportEnabled
import ch.protonmail.android.mailupselling.domain.usecase.featureflags.IsUpgradePaidPlanSupportEnabled
import ch.protonmail.android.mailupselling.domain.usecase.featureflags.IsUpsellingOneClickOverrideEnabled
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.Reusable
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import me.proton.core.plan.domain.ClientPlanFilter
import me.proton.core.plan.domain.ProductOnlyPaidPlans
import me.proton.core.plan.domain.SupportSignupPaidPlans
import me.proton.core.plan.domain.SupportUpgradePaidPlans
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UpsellingModule {

    @Provides
    @SupportSignupPaidPlans
    fun provideSupportSignupPaidPlans(isEnabled: IsSignupPaidPlanSupportEnabled) = isEnabled(null)

    @Provides
    @SupportUpgradePaidPlans
    fun provideSupportUpgradePaidPlans(isEnabled: IsUpgradePaidPlanSupportEnabled) = isEnabled(null)

    @Provides
    @ProductOnlyPaidPlans
    fun provideProductOnlyPaidPlans() = false

    @Provides
    @ForceOneClickUpsellingDetailsOverride
    fun provideOneClickOverrideEnabled(isEnabled: IsUpsellingOneClickOverrideEnabled) = isEnabled(null)

    @Provides
    fun provideClientPlansFilterPredicate(): ClientPlanFilter? = null
}

@Module
@InstallIn(SingletonComponent::class)
interface UpsellingLocalDataModule {

    @Binds
    @Reusable
    fun provideUpsellingVisibilityRepository(impl: UpsellingVisibilityRepositoryImpl): UpsellingVisibilityRepository

    @Module
    @InstallIn(SingletonComponent::class)
    object Providers {

        @Provides
        @Singleton
        fun provideDataStoreProvider(@ApplicationContext context: Context): UpsellingDataStoreProvider =
            UpsellingDataStoreProvider(context)
    }
}
