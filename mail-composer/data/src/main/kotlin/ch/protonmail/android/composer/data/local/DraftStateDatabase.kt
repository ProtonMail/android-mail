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

package ch.protonmail.android.composer.data.local

import androidx.sqlite.db.SupportSQLiteDatabase
import ch.protonmail.android.composer.data.local.dao.DraftStateDao
import ch.protonmail.android.composer.data.local.dao.MessageExpirationTimeDao
import ch.protonmail.android.composer.data.local.dao.MessagePasswordDao
import ch.protonmail.android.mailmessage.data.local.dao.AttachmentStateDao
import me.proton.core.data.room.db.Database
import me.proton.core.data.room.db.extension.addTableColumn
import me.proton.core.data.room.db.extension.dropTable
import me.proton.core.data.room.db.migration.DatabaseMigration

interface DraftStateDatabase : Database {

    fun draftStateDao(): DraftStateDao
    fun attachmentStateDao(): AttachmentStateDao
    fun messagePasswordDao(): MessagePasswordDao
    fun messageExpirationTimeDao(): MessageExpirationTimeDao

    companion object {

        val MIGRATION_0: DatabaseMigration = object : DatabaseMigration {
            @Suppress("MaxLineLength")
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create DraftState table
                database.execSQL("CREATE TABLE IF NOT EXISTS `DraftStateEntity` (`userId` TEXT NOT NULL, `messageId` TEXT NOT NULL, `apiMessageId` TEXT, `state` INTEGER NOT NULL, `action` TEXT NOT NULL, PRIMARY KEY(`userId`, `messageId`), FOREIGN KEY(`userId`, `messageId`) REFERENCES `MessageEntity`(`userId`,`messageId`) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(`userId`)  REFERENCES `UserEntity`(`userId`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_DraftStateEntity_userId` ON `DraftStateEntity` (`userId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_DraftStateEntity_userId_messageId` ON `DraftStateEntity` (`userId`, `messageId`)")
            }
        }

        val MIGRATION_1: DatabaseMigration = object : DatabaseMigration {
            @Suppress("MaxLineLength")
            override fun migrate(database: SupportSQLiteDatabase) {
                // Re-create draft state table without ForeignKey on messageId
                database.dropTable("DraftStateEntity")
                database.execSQL("CREATE TABLE IF NOT EXISTS `DraftStateEntity` (`userId` TEXT NOT NULL, `messageId` TEXT NOT NULL, `apiMessageId` TEXT, `state` INTEGER NOT NULL, `action` TEXT NOT NULL, PRIMARY KEY(`userId`, `messageId`), FOREIGN KEY(`userId`)  REFERENCES `UserEntity`(`userId`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_DraftStateEntity_userId` ON `DraftStateEntity` (`userId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_DraftStateEntity_userId_messageId` ON `DraftStateEntity` (`userId`, `messageId`)")
            }
        }

        val MIGRATION_2: DatabaseMigration = object : DatabaseMigration {
            @Suppress("MaxLineLength")
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS `AttachmentStateEntity` (`userId` TEXT NOT NULL, `messageId` TEXT NOT NULL, `attachmentId` TEXT NOT NULL, `apiAttachmentId` TEXT, `state` INTEGER NOT NULL, PRIMARY KEY(`userId`, `messageId`), FOREIGN KEY(`userId`, `messageId`) REFERENCES `MessageEntity`(`userId`,`messageId`) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(`userId`)  REFERENCES `UserEntity`(`userId`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_AttachmentStateEntity_userId` ON `AttachmentStateEntity` (`userId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_AttachmentStateEntity_userId_messageId` ON `AttachmentStateEntity` (`userId`, `messageId`)")
            }
        }

        val MIGRATION_3: DatabaseMigration = object : DatabaseMigration {
            @Suppress("MaxLineLength")
            override fun migrate(database: SupportSQLiteDatabase) {
                database.dropTable("AttachmentStateEntity")
                database.execSQL("CREATE TABLE IF NOT EXISTS `AttachmentStateEntity` (`userId` TEXT NOT NULL, `messageId` TEXT NOT NULL, `attachmentId` TEXT NOT NULL, `state` INTEGER NOT NULL, PRIMARY KEY(`userId`, `messageId`, `attachmentId`), FOREIGN KEY(`userId`, `messageId`) REFERENCES `MessageEntity`(`userId`,`messageId`) ON UPDATE CASCADE ON DELETE CASCADE, FOREIGN KEY(`userId`) REFERENCES `UserEntity`(`userId`) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(`userId`, `messageId`, `attachmentId`) REFERENCES MessageAttachmentEntity(`userId`, `messageId`, `attachmentId`) ON UPDATE CASCADE ON DELETE CASCADE )")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_AttachmentStateEntity_userId` ON `AttachmentStateEntity` (`userId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_AttachmentStateEntity_userId_messageId` ON `AttachmentStateEntity` (`userId`, `messageId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_AttachmentStateEntity_userId_messageId_attachmentId` ON `AttachmentStateEntity` (`userId`, `messageId`, `attachmentId`)")
            }
        }

        val MIGRATION_4: DatabaseMigration = object : DatabaseMigration {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.addTableColumn("DraftStateEntity", "sendingError", "TEXT") // nullable by default
            }
        }

        val MIGRATION_5: DatabaseMigration = object : DatabaseMigration {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.addTableColumn("DraftStateEntity", "sendingStatusConfirmed", "INTEGER NOT NULL", "0")
            }
        }

        val MIGRATION_6: DatabaseMigration = object : DatabaseMigration {
            @Suppress("MaxLineLength")
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS `MessagePasswordEntity` (`userId` TEXT NOT NULL, `messageId` TEXT NOT NULL, `password` TEXT NOT NULL, `passwordHint` TEXT, PRIMARY KEY(`userId`, `messageId`), FOREIGN KEY(`userId`)  REFERENCES `UserEntity`(`userId`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_MessagePasswordEntity_userId` ON `MessagePasswordEntity` (`userId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_MessagePasswordEntity_userId_messageId` ON `MessagePasswordEntity` (`userId`, `messageId`)")
            }
        }

        val MIGRATION_7: DatabaseMigration = object : DatabaseMigration {
            @Suppress("MaxLineLength")
            override fun migrate(database: SupportSQLiteDatabase) {
                database.dropTable("MessagePasswordEntity")
                database.execSQL("CREATE TABLE IF NOT EXISTS `MessagePasswordEntity` (`userId` TEXT NOT NULL, `messageId` TEXT NOT NULL, `password` TEXT NOT NULL, `passwordHint` TEXT, PRIMARY KEY(`userId`, `messageId`), FOREIGN KEY(`userId`, `messageId`) REFERENCES `MessageEntity`(`userId`,`messageId`) ON UPDATE CASCADE ON DELETE CASCADE, FOREIGN KEY(`userId`)  REFERENCES `UserEntity`(`userId`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_MessagePasswordEntity_userId` ON `MessagePasswordEntity` (`userId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_MessagePasswordEntity_userId_messageId` ON `MessagePasswordEntity` (`userId`, `messageId`)")
            }
        }

        val MIGRATION_8: DatabaseMigration = object : DatabaseMigration {
            @Suppress("MaxLineLength")
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS `MessageExpirationTimeEntity` (`userId` TEXT NOT NULL, `messageId` TEXT NOT NULL, `expiresInSeconds` INTEGER NOT NULL, PRIMARY KEY(`userId`, `messageId`), FOREIGN KEY(`userId`, `messageId`) REFERENCES `MessageEntity`(`userId`,`messageId`) ON UPDATE CASCADE ON DELETE CASCADE, FOREIGN KEY(`userId`)  REFERENCES `UserEntity`(`userId`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_MessageExpirationTimeEntity_userId` ON `MessageExpirationTimeEntity` (`userId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_MessageExpirationTimeEntity_userId_messageId` ON `MessageExpirationTimeEntity` (`userId`, `messageId`)")
            }
        }
    }
}
