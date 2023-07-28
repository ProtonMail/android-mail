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
import me.proton.core.data.room.db.Database
import me.proton.core.data.room.db.extension.dropTable
import me.proton.core.data.room.db.migration.DatabaseMigration

interface DraftStateDatabase : Database {

    fun draftStateDao(): DraftStateDao

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
    }
}
