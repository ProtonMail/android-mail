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

import android.content.Context
import androidx.room.RoomDatabase
import ch.protonmail.android.composer.data.local.DraftStateDatabase
import ch.protonmail.android.db.AppDatabase
import ch.protonmail.android.mailconversation.data.local.ConversationDatabase
import ch.protonmail.android.mailmessage.data.local.MessageDatabase
import ch.protonmail.android.mailmessage.data.local.SearchResultsDatabase
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import me.proton.core.account.data.db.AccountDatabase
import me.proton.core.auth.data.db.AuthDatabase
import me.proton.core.challenge.data.db.ChallengeDatabase
import me.proton.core.contact.data.local.db.ContactDatabase
import me.proton.core.eventmanager.data.db.EventMetadataDatabase
import me.proton.core.featureflag.data.db.FeatureFlagDatabase
import me.proton.core.humanverification.data.db.HumanVerificationDatabase
import me.proton.core.key.data.db.KeySaltDatabase
import me.proton.core.key.data.db.PublicAddressDatabase
import me.proton.core.keytransparency.data.local.KeyTransparencyDatabase
import me.proton.core.label.data.local.LabelDatabase
import me.proton.core.mailsettings.data.db.MailSettingsDatabase
import me.proton.core.notification.data.local.db.NotificationDatabase
import me.proton.core.observability.data.db.ObservabilityDatabase
import me.proton.core.payment.data.local.db.PaymentDatabase
import me.proton.core.push.data.local.db.PushDatabase
import me.proton.core.telemetry.data.db.TelemetryDatabase
import me.proton.core.user.data.db.AddressDatabase
import me.proton.core.user.data.db.UserDatabase
import me.proton.core.userrecovery.data.db.DeviceRecoveryDatabase
import me.proton.core.usersettings.data.db.OrganizationDatabase
import me.proton.core.usersettings.data.db.UserSettingsDatabase
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppDatabaseModule {
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase = AppDatabase.buildDatabase(context)
}

@Module
@InstallIn(SingletonComponent::class)
@Suppress("TooManyFunctions")
abstract class AppDatabaseBindsModule {
    @Binds
    abstract fun provideRoomDatabase(appDatabase: AppDatabase): RoomDatabase

    @Binds
    abstract fun provideAccountDatabase(appDatabase: AppDatabase): AccountDatabase

    @Binds
    abstract fun provideUserDatabase(appDatabase: AppDatabase): UserDatabase

    @Binds
    abstract fun provideAddressDatabase(appDatabase: AppDatabase): AddressDatabase

    @Binds
    abstract fun provideKeySaltDatabase(appDatabase: AppDatabase): KeySaltDatabase

    @Binds
    abstract fun providePublicAddressDatabase(appDatabase: AppDatabase): PublicAddressDatabase

    @Binds
    abstract fun provideHumanVerificationDatabase(appDatabase: AppDatabase): HumanVerificationDatabase

    @Binds
    abstract fun provideMailSettingsDatabase(appDatabase: AppDatabase): MailSettingsDatabase

    @Binds
    abstract fun provideUserSettingsDatabase(appDatabase: AppDatabase): UserSettingsDatabase

    @Binds
    abstract fun provideOrganizationDatabase(appDatabase: AppDatabase): OrganizationDatabase

    @Binds
    abstract fun provideContactDatabase(appDatabase: AppDatabase): ContactDatabase

    @Binds
    abstract fun provideEventMetadataDatabase(appDatabase: AppDatabase): EventMetadataDatabase

    @Binds
    abstract fun provideLabelDatabase(appDatabase: AppDatabase): LabelDatabase

    @Binds
    abstract fun provideFeatureFlagDatabase(appDatabase: AppDatabase): FeatureFlagDatabase

    @Binds
    abstract fun provideChallengeDatabase(appDatabase: AppDatabase): ChallengeDatabase

    @Binds
    abstract fun provideMessageDatabase(appDatabase: AppDatabase): MessageDatabase

    @Binds
    abstract fun provideConversationDatabase(appDatabase: AppDatabase): ConversationDatabase

    @Binds
    abstract fun providePaymentDatabase(appDatabase: AppDatabase): PaymentDatabase

    @Binds
    abstract fun provideObservabilityDatabase(appDatabase: AppDatabase): ObservabilityDatabase

    @Binds
    abstract fun provideKeyTransparencyDatabase(appDatabase: AppDatabase): KeyTransparencyDatabase

    @Binds
    abstract fun provideNotificationDatabase(appDatabase: AppDatabase): NotificationDatabase

    @Binds
    abstract fun providePushDatabase(appDatabase: AppDatabase): PushDatabase

    @Binds
    abstract fun provideTelemetryDatabase(appDatabase: AppDatabase): TelemetryDatabase

    @Binds
    abstract fun provideDraftStateDatabase(appDatabase: AppDatabase): DraftStateDatabase

    @Binds
    abstract fun provideSearchResultsDatabase(appDatabase: AppDatabase): SearchResultsDatabase

    @Binds
    abstract fun provideDeviceRecoveryDatabase(appDatabase: AppDatabase): DeviceRecoveryDatabase

    @Binds
    abstract fun provideAuthDatabase(appDatabase: AppDatabase): AuthDatabase
}
