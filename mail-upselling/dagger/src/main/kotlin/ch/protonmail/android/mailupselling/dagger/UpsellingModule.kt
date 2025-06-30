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
import ch.protonmail.android.mailupselling.data.repository.DriveSpotlightVisibilityRepositoryImpl
import ch.protonmail.android.mailupselling.data.repository.UpsellingVisibilityRepositoryImpl
import ch.protonmail.android.mailupselling.domain.annotations.DriveSpotlightEnabled
import ch.protonmail.android.mailupselling.domain.annotations.ForceOneClickUpsellingDetailsOverride
import ch.protonmail.android.mailupselling.domain.annotations.HeaderUpsellSocialProofLayoutEnabled
import ch.protonmail.android.mailupselling.domain.annotations.HeaderUpsellVariantLayoutEnabled
import ch.protonmail.android.mailupselling.domain.annotations.OneClickUpsellingAlwaysShown
import ch.protonmail.android.mailupselling.domain.annotations.OneClickUpsellingTelemetryEnabled
import ch.protonmail.android.mailupselling.domain.annotations.SidebarUpsellingEnabled
import ch.protonmail.android.mailupselling.domain.annotations.UpsellingAutodeleteEnabled
import ch.protonmail.android.mailupselling.domain.annotations.UpsellingMobileSignatureEnabled
import ch.protonmail.android.mailupselling.domain.annotations.UpsellingOnboardingEnabled
import ch.protonmail.android.mailupselling.domain.repository.DriveSpotlightTelemetryRepository
import ch.protonmail.android.mailupselling.domain.repository.DriveSpotlightTelemetryRepositoryImpl
import ch.protonmail.android.mailupselling.domain.repository.DriveSpotlightVisibilityRepository
import ch.protonmail.android.mailupselling.domain.repository.NPSFeedbackTelemetryRepository
import ch.protonmail.android.mailupselling.domain.repository.NPSFeedbackTelemetryRepositoryImpl
import ch.protonmail.android.mailupselling.domain.repository.PostSubscriptionTelemetryRepository
import ch.protonmail.android.mailupselling.domain.repository.PostSubscriptionTelemetryRepositoryImpl
import ch.protonmail.android.mailupselling.domain.repository.UpsellingTelemetryRepository
import ch.protonmail.android.mailupselling.domain.repository.UpsellingTelemetryRepositoryImpl
import ch.protonmail.android.mailupselling.domain.repository.UpsellingVisibilityRepository
import ch.protonmail.android.mailupselling.domain.usecase.featureflags.AlwaysShowOneClickUpselling
import ch.protonmail.android.mailupselling.domain.usecase.featureflags.IsDriveSpotlightEnabled
import ch.protonmail.android.mailupselling.domain.usecase.featureflags.IsHeaderUpsellSocialProofLayoutEnabled
import ch.protonmail.android.mailupselling.domain.usecase.featureflags.IsHeaderUpsellVariantLayoutEnabled
import ch.protonmail.android.mailupselling.domain.usecase.featureflags.IsOneClickUpsellingTelemetryEnabled
import ch.protonmail.android.mailupselling.domain.usecase.featureflags.IsSidebarUpsellingEnabled
import ch.protonmail.android.mailupselling.domain.usecase.featureflags.IsSignupPaidPlanSupportEnabled
import ch.protonmail.android.mailupselling.domain.usecase.featureflags.IsUpgradePaidPlanSupportEnabled
import ch.protonmail.android.mailupselling.domain.usecase.featureflags.IsUpsellingAutodeleteEnabled
import ch.protonmail.android.mailupselling.domain.usecase.featureflags.IsUpsellingMobileSignatureEnabled
import ch.protonmail.android.mailupselling.domain.usecase.featureflags.IsUpsellingOneClickOverrideEnabled
import ch.protonmail.android.mailupselling.domain.usecase.featureflags.IsUpsellingPostOnboardingEnabled
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
    @OneClickUpsellingTelemetryEnabled
    fun provideOneClickUpsellingTelemetryEnabled(isEnabled: IsOneClickUpsellingTelemetryEnabled) = isEnabled(null)

    @Provides
    @HeaderUpsellVariantLayoutEnabled
    fun provideHeaderUpsellVariantLayoutEnabled(isEnabled: IsHeaderUpsellVariantLayoutEnabled) = isEnabled(null)

    @Provides
    @DriveSpotlightEnabled
    fun provideDriveSpotlightEnabled(isEnabled: IsDriveSpotlightEnabled) = isEnabled(null)

    @Provides
    @HeaderUpsellSocialProofLayoutEnabled
    fun provideUpsellSocialProofLayoutEnabled(isEnabled: IsHeaderUpsellSocialProofLayoutEnabled) = isEnabled(null)

    @Provides
    @OneClickUpsellingAlwaysShown
    fun provideOneClickUpsellingAlwaysShown(isEnabled: AlwaysShowOneClickUpselling) = isEnabled(null)

    @Provides
    @UpsellingMobileSignatureEnabled
    fun provideUpsellingMobileSignatureEnabled(isEnabled: IsUpsellingMobileSignatureEnabled) = isEnabled(null)

    @Provides
    @UpsellingOnboardingEnabled
    fun provideUpsellingOnboardingEnabled(isEnabled: IsUpsellingPostOnboardingEnabled) = isEnabled(null)

    @Provides
    @UpsellingAutodeleteEnabled
    fun provideUpsellingAutodeleteEnabled(isEnabled: IsUpsellingAutodeleteEnabled) = isEnabled(null)

    @Provides
    @SidebarUpsellingEnabled
    fun provideSidebarUpsellingEnabled(isEnabled: IsSidebarUpsellingEnabled) = isEnabled(null)

    @Provides
    fun provideClientPlansFilterPredicate(): ClientPlanFilter? = null
}

@Module
@InstallIn(SingletonComponent::class)
interface UpsellingModuleBindings {

    @Binds
    @Reusable
    fun provideTelemetryRepository(impl: UpsellingTelemetryRepositoryImpl): UpsellingTelemetryRepository

    @Binds
    @Reusable
    fun provideDriveSpotlightTelemetryRepository(
        impl: DriveSpotlightTelemetryRepositoryImpl
    ): DriveSpotlightTelemetryRepository

    @Binds
    @Reusable
    fun provideNPSFeedbackTelemetryRepository(impl: NPSFeedbackTelemetryRepositoryImpl): NPSFeedbackTelemetryRepository

    @Binds
    @Reusable
    fun providePostSubscriptionTelemetryRepo(
        impl: PostSubscriptionTelemetryRepositoryImpl
    ): PostSubscriptionTelemetryRepository
}

@Module
@InstallIn(SingletonComponent::class)
interface UpsellingLocalDataModule {

    @Binds
    @Reusable
    fun provideUpsellingVisibilityRepository(impl: UpsellingVisibilityRepositoryImpl): UpsellingVisibilityRepository

    @Binds
    @Reusable
    fun provideDriveSpotlightRepository(
        impl: DriveSpotlightVisibilityRepositoryImpl
    ): DriveSpotlightVisibilityRepository

    @Module
    @InstallIn(SingletonComponent::class)
    object Providers {

        @Provides
        @Singleton
        fun provideDataStoreProvider(@ApplicationContext context: Context): UpsellingDataStoreProvider =
            UpsellingDataStoreProvider(context)
    }
}
