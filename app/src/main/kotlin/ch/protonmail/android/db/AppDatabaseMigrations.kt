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

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import ch.protonmail.android.composer.data.local.DraftStateDatabase
import ch.protonmail.android.mailconversation.data.local.ConversationDatabase
import ch.protonmail.android.mailmessage.data.local.MessageDatabase
import ch.protonmail.android.mailmessage.data.local.SearchResultsDatabase
import me.proton.core.account.data.db.AccountDatabase
import me.proton.core.auth.data.db.AuthDatabase
import me.proton.core.contact.data.local.db.ContactDatabase
import me.proton.core.eventmanager.data.db.EventMetadataDatabase
import me.proton.core.key.data.db.PublicAddressDatabase
import me.proton.core.keytransparency.data.local.KeyTransparencyDatabase
import me.proton.core.mailsettings.data.db.MailSettingsDatabase
import me.proton.core.notification.data.local.db.NotificationDatabase
import me.proton.core.payment.data.local.db.PaymentDatabase
import me.proton.core.push.data.local.db.PushDatabase
import me.proton.core.telemetry.data.db.TelemetryDatabase
import me.proton.core.user.data.db.AddressDatabase
import me.proton.core.user.data.db.UserDatabase
import me.proton.core.user.data.db.UserKeyDatabase
import me.proton.core.userrecovery.data.db.DeviceRecoveryDatabase
import me.proton.core.usersettings.data.db.OrganizationDatabase
import me.proton.core.usersettings.data.db.UserSettingsDatabase

object AppDatabaseMigrations {

    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            MessageDatabase.MIGRATION_0.migrate(db)
        }
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            OrganizationDatabase.MIGRATION_2.migrate(db)
        }
    }
    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            AddressDatabase.MIGRATION_4.migrate(db)
            PublicAddressDatabase.MIGRATION_2.migrate(db)
            KeyTransparencyDatabase.MIGRATION_0.migrate(db)
        }
    }

    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            MessageDatabase.MIGRATION_1.migrate(db)
        }

    }

    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            UserDatabase.MIGRATION_2.migrate(db)
        }
    }

    val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            NotificationDatabase.MIGRATION_0.migrate(db)
            NotificationDatabase.MIGRATION_1.migrate(db)
            PushDatabase.MIGRATION_0.migrate(db)
        }
    }

    val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            UserSettingsDatabase.MIGRATION_2.migrate(db)
        }
    }

    val MIGRATION_8_9 = object : Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) {
            MessageDatabase.MIGRATION_2.migrate(db)
        }
    }

    val MIGRATION_9_10 = object : Migration(9, 10) {
        override fun migrate(db: SupportSQLiteDatabase) {
            MessageDatabase.MIGRATION_3.migrate(db)
        }
    }

    val MIGRATION_10_11 = object : Migration(10, 11) {
        override fun migrate(db: SupportSQLiteDatabase) {
            DraftStateDatabase.MIGRATION_0.migrate(db)
        }
    }

    val MIGRATION_11_12 = object : Migration(11, 12) {
        override fun migrate(db: SupportSQLiteDatabase) {
            ContactDatabase.MIGRATION_1.migrate(db)
            EventMetadataDatabase.MIGRATION_1.migrate(db)
        }
    }

    val MIGRATION_12_13 = object : Migration(12, 13) {
        override fun migrate(db: SupportSQLiteDatabase) {
            MessageDatabase.MIGRATION_4.migrate(db)
            DraftStateDatabase.MIGRATION_1.migrate(db)
        }
    }

    val MIGRATION_13_14 = object : Migration(13, 14) {
        override fun migrate(db: SupportSQLiteDatabase) {
            UserDatabase.MIGRATION_3.migrate(db)
            AccountDatabase.MIGRATION_6.migrate(db)
        }
    }

    val MIGRATION_14_15 = object : Migration(14, 15) {
        override fun migrate(db: SupportSQLiteDatabase) {
            TelemetryDatabase.MIGRATION_0.migrate(db)
            UserSettingsDatabase.MIGRATION_3.migrate(db)
        }
    }

    val MIGRATION_15_16 = object : Migration(15, 16) {
        override fun migrate(db: SupportSQLiteDatabase) {
            DraftStateDatabase.MIGRATION_2.migrate(db)
        }
    }

    val MIGRATION_16_17 = object : Migration(16, 17) {
        override fun migrate(db: SupportSQLiteDatabase) {
            DraftStateDatabase.MIGRATION_3.migrate(db)
        }
    }

    val MIGRATION_17_18 = object : Migration(17, 18) {
        override fun migrate(db: SupportSQLiteDatabase) {
            MessageDatabase.MIGRATION_5.migrate(db)
        }

    }

    val MIGRATION_18_19 = object : Migration(18, 19) {
        override fun migrate(db: SupportSQLiteDatabase) {
            EventMetadataDatabase.MIGRATION_2.migrate(db)
        }
    }

    val MIGRATION_19_20 = object : Migration(19, 20) {
        override fun migrate(db: SupportSQLiteDatabase) {
            UserSettingsDatabase.MIGRATION_4.migrate(db)
        }
    }

    val MIGRATION_20_21 = object : Migration(20, 21) {
        override fun migrate(db: SupportSQLiteDatabase) {
            DraftStateDatabase.MIGRATION_4.migrate(db)
        }
    }

    val MIGRATION_21_22 = object : Migration(21, 22) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Empty migration as UnreadCountDatabase was deleted
            // when tables were distributed to message and conversation DBs (migration 23->24)
        }
    }

    val MIGRATION_22_23 = object : Migration(22, 23) {
        override fun migrate(db: SupportSQLiteDatabase) {
            DraftStateDatabase.MIGRATION_5.migrate(db)
        }
    }

    val MIGRATION_23_24 = object : Migration(23, 24) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Add UnreadMessageCount Table
            MessageDatabase.MIGRATION_6.migrate(db)
            // Add UnreadConversationsCount Table
            ConversationDatabase.MIGRATION_0.migrate(db)
        }
    }


    val MIGRATION_24_25 = object : Migration(24, 25) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Create SearchResults Table
            SearchResultsDatabase.MIGRATION_0.migrate(db)
        }
    }

    val MIGRATION_25_26 = object : Migration(25, 26) {
        override fun migrate(db: SupportSQLiteDatabase) {
            DraftStateDatabase.MIGRATION_6.migrate(db)
        }
    }

    val MIGRATION_26_27 = object : Migration(26, 27) {
        override fun migrate(db: SupportSQLiteDatabase) {
            DraftStateDatabase.MIGRATION_7.migrate(db)
        }
    }

    val MIGRATION_27_28 = object : Migration(27, 28) {
        override fun migrate(db: SupportSQLiteDatabase) {
            UserSettingsDatabase.MIGRATION_5.migrate(db)
            UserKeyDatabase.MIGRATION_0.migrate(db)
            UserDatabase.MIGRATION_4.migrate(db)
        }
    }

    val MIGRATION_28_29 = object : Migration(28, 29) {
        override fun migrate(db: SupportSQLiteDatabase) {
            DraftStateDatabase.MIGRATION_8.migrate(db)
        }
    }

    val MIGRATION_29_30 = object : Migration(29, 30) {
        override fun migrate(db: SupportSQLiteDatabase) {
            MessageDatabase.MIGRATION_7.migrate(db)
        }
    }

    val MIGRATION_30_31 = object : Migration(30, 31) {
        override fun migrate(db: SupportSQLiteDatabase) {
            UserDatabase.MIGRATION_5.migrate(db)
            AccountDatabase.MIGRATION_7.migrate(db)
        }
    }

    val MIGRATION_31_32 = object : Migration(31, 32) {
        override fun migrate(db: SupportSQLiteDatabase) {
            PaymentDatabase.MIGRATION_1.migrate(db)
        }
    }

    val MIGRATION_32_33 = object : Migration(32, 33) {
        override fun migrate(db: SupportSQLiteDatabase) {
            UserSettingsDatabase.MIGRATION_6.migrate(db)
        }
    }

    val MIGRATION_33_34 = object : Migration(33, 34) {
        override fun migrate(db: SupportSQLiteDatabase) {
            DeviceRecoveryDatabase.MIGRATION_0.migrate(db)
            DeviceRecoveryDatabase.MIGRATION_1.migrate(db)
            UserKeyDatabase.MIGRATION_1.migrate(db)
        }
    }

    val MIGRATION_34_35 = object : Migration(34, 35) {
        override fun migrate(db: SupportSQLiteDatabase) {
            AccountDatabase.MIGRATION_8.migrate(db)
            UserSettingsDatabase.MIGRATION_7.migrate(db)
            PublicAddressDatabase.MIGRATION_3.migrate(db)
            EventMetadataDatabase.MIGRATION_3.migrate(db)
        }
    }

    val MIGRATION_35_36 = object : Migration(35, 36) {
        override fun migrate(db: SupportSQLiteDatabase) {
            ContactDatabase.MIGRATION_2.migrate(db)
        }
    }

    val MIGRATION_36_37 = object : Migration(36, 37) {
        override fun migrate(db: SupportSQLiteDatabase) {
            AuthDatabase.MIGRATION_0.migrate(db)
            AuthDatabase.MIGRATION_1.migrate(db)
        }
    }

    val MIGRATION_37_38 = object : Migration(37, 38) {
        override fun migrate(db: SupportSQLiteDatabase) {
            AuthDatabase.MIGRATION_2.migrate(db)
            AuthDatabase.MIGRATION_3.migrate(db)
            AuthDatabase.MIGRATION_4.migrate(db)
            AuthDatabase.MIGRATION_5.migrate(db)
            UserDatabase.MIGRATION_6.migrate(db)
            AccountDatabase.MIGRATION_9.migrate(db)
            MailSettingsDatabase.MIGRATION_1.migrate(db)
        }
    }

    val MIGRATION_38_39 = object : Migration(38, 39) {
        override fun migrate(db: SupportSQLiteDatabase) {
            MailSettingsDatabase.MIGRATION_2.migrate(db)
            MailSettingsDatabase.MIGRATION_3.migrate(db)
        }
    }

    val MIGRATION_39_40 = object : Migration(39, 40) {
        override fun migrate(db: SupportSQLiteDatabase) {
            UserSettingsDatabase.MIGRATION_8.migrate(db)
        }
    }

    val MIGRATION_40_41 = object : Migration(40, 41) {
        override fun migrate(db: SupportSQLiteDatabase) {
            AccountDatabase.MIGRATION_10.migrate(db)
        }
    }
}
