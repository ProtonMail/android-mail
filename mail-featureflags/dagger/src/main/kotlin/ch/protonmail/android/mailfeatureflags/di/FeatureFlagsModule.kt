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

package ch.protonmail.android.mailfeatureflags.di

import ch.protonmail.android.mailfeatureflags.data.local.DataStoreFeatureFlagValueProvider
import ch.protonmail.android.mailfeatureflags.data.local.DefaultFeatureFlagValueProvider
import ch.protonmail.android.mailfeatureflags.data.local.UnleashFeatureFlagValueProvider
import ch.protonmail.android.mailfeatureflags.data.local.factory.BooleanFeatureFlagFactory
import ch.protonmail.android.mailfeatureflags.domain.FeatureFlagValueProvider
import ch.protonmail.android.mailfeatureflags.domain.annotation.ComposerAutoCollapseQuotedTextEnabled
import ch.protonmail.android.mailfeatureflags.domain.annotation.FeatureFlagsCoroutineScope
import ch.protonmail.android.mailfeatureflags.domain.annotation.IsBlackFridayWave1Enabled
import ch.protonmail.android.mailfeatureflags.domain.annotation.IsBlackFridayWave2Enabled
import ch.protonmail.android.mailfeatureflags.domain.annotation.IsDebugInspectDbEnabled
import ch.protonmail.android.mailfeatureflags.domain.annotation.IsFeatureSpotlightEnabled
import ch.protonmail.android.mailfeatureflags.domain.annotation.IsInjectCssOverrideEnabled
import ch.protonmail.android.mailfeatureflags.domain.annotation.IsLastMessageAutoExpandEnabled
import ch.protonmail.android.mailfeatureflags.domain.annotation.IsOnboardingUpsellEnabled
import ch.protonmail.android.mailfeatureflags.domain.annotation.IsPrivacyBundle2601Enabled
import ch.protonmail.android.mailfeatureflags.domain.annotation.IsRestrictMessageWebViewHeightEnabled
import ch.protonmail.android.mailfeatureflags.domain.annotation.IsShowRatingBoosterEnabled
import ch.protonmail.android.mailfeatureflags.domain.annotation.IsSpringOffer2026Enabled
import ch.protonmail.android.mailfeatureflags.domain.annotation.IsSpringOffer2026Wave2Enabled
import ch.protonmail.android.mailfeatureflags.domain.annotation.IsUpsellEnabled
import ch.protonmail.android.mailfeatureflags.domain.model.ComposerAutoCollapseQuotedText
import ch.protonmail.android.mailfeatureflags.domain.model.ConversationDetailAutoExpandLastMessageEnabled
import ch.protonmail.android.mailfeatureflags.domain.model.DebugInspectDbEnabled
import ch.protonmail.android.mailfeatureflags.domain.model.FeatureFlagDefinition
import ch.protonmail.android.mailfeatureflags.domain.model.FeatureSpotlight
import ch.protonmail.android.mailfeatureflags.domain.model.InjectDetailCssOverrideEnabled
import ch.protonmail.android.mailfeatureflags.domain.model.MailBlackFriday2025Enabled
import ch.protonmail.android.mailfeatureflags.domain.model.MailBlackFriday2025Wave2Enabled
import ch.protonmail.android.mailfeatureflags.domain.model.OnboardingUpsellingEnabled
import ch.protonmail.android.mailfeatureflags.domain.model.PrivacyBundle2601
import ch.protonmail.android.mailfeatureflags.domain.model.RestrictMessageWebViewHeight
import ch.protonmail.android.mailfeatureflags.domain.model.ShowRatingBoosterEnabled
import ch.protonmail.android.mailfeatureflags.domain.model.SpringOffer2026Enabled
import ch.protonmail.android.mailfeatureflags.domain.model.SpringOffer2026Wave2Enabled
import ch.protonmail.android.mailfeatureflags.domain.model.UpsellingEnabled
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
@Suppress("TooManyFunctions")
object FeatureFlagsModule {

    @Provides
    @IntoSet
    @Singleton
    fun provideAutoExpandLastMessageConvoDef(): FeatureFlagDefinition = ConversationDetailAutoExpandLastMessageEnabled

    @Provides
    @Singleton
    @IsLastMessageAutoExpandEnabled
    fun provideAutoExpandLastMessageConvoEnabled(factory: BooleanFeatureFlagFactory) =
        factory.create(ConversationDetailAutoExpandLastMessageEnabled.key, false)

    @Provides
    @IntoSet
    @Singleton
    fun provideDetailCssOverrideDefinitions(): FeatureFlagDefinition = InjectDetailCssOverrideEnabled

    @Provides
    @Singleton
    @IsInjectCssOverrideEnabled
    fun provideDetailCssOverrideEnabledOverride(factory: BooleanFeatureFlagFactory) =
        factory.create(InjectDetailCssOverrideEnabled.key, false)

    @Provides
    @Singleton
    @FeatureFlagsCoroutineScope
    fun provideFeatureFlagsCoroutineScope(): CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @Provides
    @Singleton
    @IsUpsellEnabled
    fun provideUpsellEnabled(factory: BooleanFeatureFlagFactory) = factory.create(key = UpsellingEnabled.key, false)

    @Provides
    @Singleton
    @IsOnboardingUpsellEnabled
    fun provideOnboardingUpsellEnabled(factory: BooleanFeatureFlagFactory) =
        factory.create(key = OnboardingUpsellingEnabled.key, false)

    @Provides
    @Singleton
    @IsBlackFridayWave1Enabled
    fun provideBlackFridayWave1Enabled(factory: BooleanFeatureFlagFactory) =
        factory.create(key = MailBlackFriday2025Enabled.key, false)

    @Provides
    @Singleton
    @IsBlackFridayWave2Enabled
    fun provideBlackFridayWave2Enabled(factory: BooleanFeatureFlagFactory) =
        factory.create(key = MailBlackFriday2025Wave2Enabled.key, false)

    @Provides
    @Singleton
    @IsSpringOffer2026Enabled
    fun provideSpringOffer2026Enabled(factory: BooleanFeatureFlagFactory) =
        factory.create(key = SpringOffer2026Enabled.key, false)

    @Provides
    @Singleton
    @IsSpringOffer2026Wave2Enabled
    fun provideSpringOffer2026Wave2Enabled(factory: BooleanFeatureFlagFactory) =
        factory.create(key = SpringOffer2026Wave2Enabled.key, false)

    @Provides
    @Singleton
    @ComposerAutoCollapseQuotedTextEnabled
    fun provideComposerAutoCollapseText(factory: BooleanFeatureFlagFactory) =
        factory.create(key = ComposerAutoCollapseQuotedText.key, false)

    @Provides
    @IntoSet
    @Singleton
    fun provideComposerAutoCollapseTextDef(): FeatureFlagDefinition = ComposerAutoCollapseQuotedText

    @Provides
    @IntoSet
    @Singleton
    fun provideDebugInspectDbEnabledDefinition(): FeatureFlagDefinition = DebugInspectDbEnabled

    @Provides
    @Singleton
    @IsDebugInspectDbEnabled
    fun provideIsDebugInspectDbEnabled(factory: BooleanFeatureFlagFactory) =
        factory.create(key = DebugInspectDbEnabled.key, false)

    @Provides
    @IntoSet
    @Singleton
    fun provideDefaultProvider(impl: DefaultFeatureFlagValueProvider): FeatureFlagValueProvider = impl

    @Provides
    @IntoSet
    @Singleton
    fun provideDataStoreProvider(impl: DataStoreFeatureFlagValueProvider): FeatureFlagValueProvider = impl

    @Provides
    @IntoSet
    @Singleton
    fun provideUnleashProvider(impl: UnleashFeatureFlagValueProvider): FeatureFlagValueProvider = impl

    @Provides
    @IntoSet
    @Singleton
    fun provideUpsellEnabledDefinitions(): FeatureFlagDefinition = UpsellingEnabled

    @Provides
    @IntoSet
    @Singleton
    fun provideObdnUpsellEnabledDefinition(): FeatureFlagDefinition = OnboardingUpsellingEnabled

    @Provides
    @IntoSet
    @Singleton
    fun provideRestrictMessageWebViewHeightDef(): FeatureFlagDefinition = RestrictMessageWebViewHeight

    @Provides
    @Singleton
    @IsRestrictMessageWebViewHeightEnabled
    fun provideRestrictMessageWebViewHeight(factory: BooleanFeatureFlagFactory) =
        factory.create(key = RestrictMessageWebViewHeight.key, false)

    @Provides
    @Singleton
    @IsShowRatingBoosterEnabled
    fun provideIsShowRatingBoosterEnabled(factory: BooleanFeatureFlagFactory) =
        factory.create(key = ShowRatingBoosterEnabled.key, false)

    @Provides
    @Singleton
    @IsPrivacyBundle2601Enabled
    fun providePrivacyBundle2601Enabled(factory: BooleanFeatureFlagFactory) =
        factory.create(PrivacyBundle2601.key, false)

    @Provides
    @IntoSet
    @Singleton
    fun providePrivacyBundle2601EnabledDef(): FeatureFlagDefinition = PrivacyBundle2601


    @Provides
    @Singleton
    @IsFeatureSpotlightEnabled
    fun provideFeatureSpotlightEnabled(factory: BooleanFeatureFlagFactory) = factory.create(FeatureSpotlight.key, false)

    @Provides
    @IntoSet
    @Singleton
    fun provideFeatureSpotlightEnabledDef(): FeatureFlagDefinition = FeatureSpotlight
}
