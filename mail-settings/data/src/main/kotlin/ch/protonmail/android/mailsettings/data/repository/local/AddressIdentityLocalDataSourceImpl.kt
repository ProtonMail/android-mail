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

package ch.protonmail.android.mailsettings.data.repository.local

import androidx.datastore.preferences.core.booleanPreferencesKey
import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.raise.either
import arrow.core.right
import ch.protonmail.android.mailcommon.data.mapper.safeData
import ch.protonmail.android.mailcommon.data.mapper.safeEdit
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.model.PreferencesError
import ch.protonmail.android.mailsettings.data.MailSettingsDataStoreProvider
import ch.protonmail.android.mailsettings.domain.model.DisplayName
import ch.protonmail.android.mailsettings.domain.model.Signature
import ch.protonmail.android.mailsettings.domain.model.SignaturePreference
import ch.protonmail.android.mailsettings.domain.model.SignatureValue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import me.proton.core.user.data.db.AddressDatabase
import me.proton.core.user.domain.entity.AddressId
import javax.inject.Inject

class AddressIdentityLocalDataSourceImpl @Inject constructor(
    private val db: AddressDatabase,
    private val dataStoreProvider: MailSettingsDataStoreProvider
) : AddressIdentityLocalDataSource {

    private val addressDao by lazy { db.addressDao() }

    override fun observeDisplayName(addressId: AddressId): Flow<Either<DataError, DisplayName>> =
        addressDao.observeByAddressId(addressId).mapLatest {
            it ?: return@mapLatest DataError.AddressNotFound.left()
            return@mapLatest DisplayName(it.displayName ?: "").right()
        }

    override fun observeSignatureValue(addressId: AddressId): Flow<Either<DataError, SignatureValue>> =
        addressDao.observeByAddressId(addressId).mapLatest {
            it ?: return@mapLatest DataError.AddressNotFound.left()
            return@mapLatest SignatureValue(it.signature ?: "").right()
        }

    override fun observeSignaturePreference(addressId: AddressId): Flow<Either<PreferencesError, SignaturePreference>> =
        dataStoreProvider.addressDisplayInfoDataStore.safeData.map { preferences ->
            preferences.map {
                val isEnabled = it[getSignatureEnabledPrefKey(addressId)] ?: true
                SignaturePreference(isEnabled)
            }
        }

    override suspend fun updateSignatureEnabledState(
        addressId: AddressId,
        enabled: Boolean
    ): Either<PreferencesError, Unit> {
        return dataStoreProvider.addressDisplayInfoDataStore.safeEdit { preferences ->
            preferences[getSignatureEnabledPrefKey(addressId)] = enabled
        }.map { Unit.right() }
    }

    override suspend fun updateAddressIdentity(
        addressId: AddressId,
        displayName: DisplayName,
        signature: Signature
    ): Either<DataError, Unit> = either {
        updateSignatureEnabledState(addressId, signature.enabled).getOrElse {
            raise(DataError.Local.Unknown)
        }

        val address = addressDao.getByAddressId(addressId) ?: raise(DataError.AddressNotFound)
        val updatedAddress = address.copy(displayName = displayName.value, signature = signature.value.text)
        addressDao.insertOrUpdate(updatedAddress)
    }

    private fun getSignatureEnabledPrefKey(addressId: AddressId) = booleanPreferencesKey(
        "${addressId.id}-signatureEnabledPrefKey"
    )
}
