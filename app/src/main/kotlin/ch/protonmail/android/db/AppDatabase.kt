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

package ch.protonmail.android.db

import android.content.Context
import androidx.room.Database
import androidx.room.TypeConverters
import ch.protonmail.android.composer.data.local.DraftStateDatabase
import ch.protonmail.android.composer.data.local.converters.AttachmentStateConverters
import ch.protonmail.android.composer.data.local.converters.DraftStateConverters
import ch.protonmail.android.composer.data.local.entity.AttachmentStateEntity
import ch.protonmail.android.composer.data.local.entity.DraftStateEntity
import ch.protonmail.android.mailconversation.data.local.ConversationDatabase
import ch.protonmail.android.mailconversation.data.local.converters.ConversationConverters
import ch.protonmail.android.mailconversation.data.local.converters.MapConverters
import ch.protonmail.android.mailconversation.data.local.entity.ConversationEntity
import ch.protonmail.android.mailconversation.data.local.entity.ConversationLabelEntity
import ch.protonmail.android.mailmessage.data.local.MessageConverters
import ch.protonmail.android.mailmessage.data.local.MessageDatabase
import ch.protonmail.android.mailmessage.data.local.converters.AttachmentWorkerStatusConverters
import ch.protonmail.android.mailmessage.data.local.converters.UriConverter
import ch.protonmail.android.mailmessage.data.local.entity.MessageAttachmentEntity
import ch.protonmail.android.mailmessage.data.local.entity.MessageAttachmentMetadataEntity
import ch.protonmail.android.mailmessage.data.local.entity.MessageBodyEntity
import ch.protonmail.android.mailmessage.data.local.entity.MessageEntity
import ch.protonmail.android.mailmessage.data.local.entity.MessageLabelEntity
import ch.protonmail.android.mailpagination.data.local.PageIntervalDatabase
import ch.protonmail.android.mailpagination.data.local.entity.PageIntervalEntity
import me.proton.core.account.data.db.AccountConverters
import me.proton.core.account.data.db.AccountDatabase
import me.proton.core.account.data.entity.AccountEntity
import me.proton.core.account.data.entity.AccountMetadataEntity
import me.proton.core.account.data.entity.SessionDetailsEntity
import me.proton.core.account.data.entity.SessionEntity
import me.proton.core.challenge.data.db.ChallengeConverters
import me.proton.core.challenge.data.db.ChallengeDatabase
import me.proton.core.challenge.data.entity.ChallengeFrameEntity
import me.proton.core.contact.data.local.db.ContactConverters
import me.proton.core.contact.data.local.db.ContactDatabase
import me.proton.core.contact.data.local.db.entity.ContactCardEntity
import me.proton.core.contact.data.local.db.entity.ContactEmailEntity
import me.proton.core.contact.data.local.db.entity.ContactEmailLabelEntity
import me.proton.core.contact.data.local.db.entity.ContactEntity
import me.proton.core.crypto.android.keystore.CryptoConverters
import me.proton.core.data.room.db.BaseDatabase
import me.proton.core.data.room.db.CommonConverters
import me.proton.core.eventmanager.data.db.EventManagerConverters
import me.proton.core.eventmanager.data.db.EventMetadataDatabase
import me.proton.core.eventmanager.data.entity.EventMetadataEntity
import me.proton.core.featureflag.data.db.FeatureFlagDatabase
import me.proton.core.featureflag.data.entity.FeatureFlagEntity
import me.proton.core.humanverification.data.db.HumanVerificationConverters
import me.proton.core.humanverification.data.db.HumanVerificationDatabase
import me.proton.core.humanverification.data.entity.HumanVerificationEntity
import me.proton.core.key.data.db.KeySaltDatabase
import me.proton.core.key.data.db.PublicAddressDatabase
import me.proton.core.key.data.entity.KeySaltEntity
import me.proton.core.key.data.entity.PublicAddressEntity
import me.proton.core.key.data.entity.PublicAddressKeyEntity
import me.proton.core.keytransparency.data.local.KeyTransparencyDatabase
import me.proton.core.keytransparency.data.local.entity.AddressChangeEntity
import me.proton.core.keytransparency.data.local.entity.SelfAuditResultEntity
import me.proton.core.label.data.local.LabelConverters
import me.proton.core.label.data.local.LabelDatabase
import me.proton.core.label.data.local.LabelEntity
import me.proton.core.mailsettings.data.db.MailSettingsDatabase
import me.proton.core.mailsettings.data.entity.MailSettingsEntity
import me.proton.core.notification.data.local.db.NotificationConverters
import me.proton.core.notification.data.local.db.NotificationDatabase
import me.proton.core.notification.data.local.db.NotificationEntity
import me.proton.core.observability.data.db.ObservabilityDatabase
import me.proton.core.observability.data.entity.ObservabilityEventEntity
import me.proton.core.payment.data.local.db.PaymentDatabase
import me.proton.core.payment.data.local.entity.GooglePurchaseEntity
import me.proton.core.push.data.local.db.PushConverters
import me.proton.core.push.data.local.db.PushDatabase
import me.proton.core.push.data.local.db.PushEntity
import me.proton.core.telemetry.data.db.TelemetryDatabase
import me.proton.core.telemetry.data.entity.TelemetryEventEntity
import me.proton.core.user.data.db.AddressDatabase
import me.proton.core.user.data.db.UserConverters
import me.proton.core.user.data.db.UserDatabase
import me.proton.core.user.data.entity.AddressEntity
import me.proton.core.user.data.entity.AddressKeyEntity
import me.proton.core.user.data.entity.UserEntity
import me.proton.core.user.data.entity.UserKeyEntity
import me.proton.core.usersettings.data.db.OrganizationDatabase
import me.proton.core.usersettings.data.db.UserSettingsConverters
import me.proton.core.usersettings.data.db.UserSettingsDatabase
import me.proton.core.usersettings.data.entity.OrganizationEntity
import me.proton.core.usersettings.data.entity.OrganizationKeysEntity
import me.proton.core.usersettings.data.entity.UserSettingsEntity

@Database(
    entities = [
        // account-data
        AccountEntity::class,
        AccountMetadataEntity::class,
        SessionEntity::class,
        SessionDetailsEntity::class,
        // user-data
        UserEntity::class,
        UserKeyEntity::class,
        AddressEntity::class,
        AddressKeyEntity::class,
        // key-data
        KeySaltEntity::class,
        PublicAddressEntity::class,
        PublicAddressKeyEntity::class,
        // human-verification
        HumanVerificationEntity::class,
        // mail-settings
        MailSettingsEntity::class,
        // user-settings
        UserSettingsEntity::class,
        // organization
        OrganizationEntity::class,
        OrganizationKeysEntity::class,
        // contact
        ContactEntity::class,
        ContactCardEntity::class,
        ContactEmailEntity::class,
        ContactEmailLabelEntity::class,
        // event-manager
        EventMetadataEntity::class,
        // label
        LabelEntity::class,
        // feature-flag
        FeatureFlagEntity::class,
        // challenge
        ChallengeFrameEntity::class,
        // notification
        NotificationEntity::class,
        // push
        PushEntity::class,
        // mail-pagination
        PageIntervalEntity::class,
        // mail-message
        MessageEntity::class,
        MessageLabelEntity::class,
        MessageBodyEntity::class,
        MessageAttachmentEntity::class,
        MessageAttachmentMetadataEntity::class,
        // mail-conversation
        ConversationEntity::class,
        ConversationLabelEntity::class,
        // in app purchase
        GooglePurchaseEntity::class,
        // observability
        ObservabilityEventEntity::class,
        // telemetry
        TelemetryEventEntity::class,
        // key transparency
        AddressChangeEntity::class,
        SelfAuditResultEntity::class,
        // draft state
        DraftStateEntity::class,
        AttachmentStateEntity::class
    ],
    version = AppDatabase.version,
    exportSchema = true
)
@TypeConverters(
    CommonConverters::class,
    AccountConverters::class,
    UserConverters::class,
    CryptoConverters::class,
    HumanVerificationConverters::class,
    UserSettingsConverters::class,
    ContactConverters::class,
    EventManagerConverters::class,
    LabelConverters::class,
    ChallengeConverters::class,
    NotificationConverters::class,
    PushConverters::class,
    MessageConverters::class,
    ConversationConverters::class,
    MapConverters::class,
    AttachmentWorkerStatusConverters::class,
    UriConverter::class,
    DraftStateConverters::class,
    AttachmentStateConverters::class
)
@Suppress("UnnecessaryAbstractClass")
abstract class AppDatabase :
    BaseDatabase(),
    AccountDatabase,
    UserDatabase,
    AddressDatabase,
    KeySaltDatabase,
    HumanVerificationDatabase,
    PublicAddressDatabase,
    MailSettingsDatabase,
    UserSettingsDatabase,
    OrganizationDatabase,
    ContactDatabase,
    EventMetadataDatabase,
    LabelDatabase,
    FeatureFlagDatabase,
    ChallengeDatabase,
    PageIntervalDatabase,
    MessageDatabase,
    ConversationDatabase,
    PaymentDatabase,
    ObservabilityDatabase,
    KeyTransparencyDatabase,
    NotificationDatabase,
    PushDatabase,
    TelemetryDatabase,
    DraftStateDatabase {

    companion object {

        const val name = "db-mail"
        const val version = 19

        internal val migrations = listOf(
            AppDatabaseMigrations.MIGRATION_1_2,
            AppDatabaseMigrations.MIGRATION_2_3,
            AppDatabaseMigrations.MIGRATION_3_4,
            AppDatabaseMigrations.MIGRATION_4_5,
            AppDatabaseMigrations.MIGRATION_5_6,
            AppDatabaseMigrations.MIGRATION_6_7,
            AppDatabaseMigrations.MIGRATION_7_8,
            AppDatabaseMigrations.MIGRATION_8_9,
            AppDatabaseMigrations.MIGRATION_9_10,
            AppDatabaseMigrations.MIGRATION_10_11,
            AppDatabaseMigrations.MIGRATION_11_12,
            AppDatabaseMigrations.MIGRATION_12_13,
            AppDatabaseMigrations.MIGRATION_13_14,
            AppDatabaseMigrations.MIGRATION_14_15,
            AppDatabaseMigrations.MIGRATION_15_16,
            AppDatabaseMigrations.MIGRATION_16_17,
            AppDatabaseMigrations.MIGRATION_17_18,
            AppDatabaseMigrations.MIGRATION_18_19,
        )

        fun buildDatabase(context: Context): AppDatabase = databaseBuilder<AppDatabase>(context, name)
            .apply { migrations.forEach { addMigrations(it) } }
            .build()
    }
}
