/*
 * Copyright (c) 2026 Proton Technologies AG
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

package ch.protonmail.android.mailevents.data.remote.model

import ch.protonmail.android.mailevents.domain.model.InstallAttribution
import ch.protonmail.android.mailevents.domain.model.InstallReferrer
import ch.protonmail.android.mailevents.domain.model.MetaInstallReferrer
import uniffi.mail_uniffi.MeasurementValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.time.Instant

internal class EventPayloadMapperTest {

    private val metadata = EventPayloadMetadata(
        asid = "asid",
        appPackageName = "ch.protonmail.android",
        openUri = null,
        osVersion = "35",
        appVersion = "7.0.0",
        locale = "en_US",
        platform = "android",
        make = "Google",
        model = "Pixel",
        languageCode = "en",
        appIdentifier = "ch.protonmail.android"
    )

    @Test
    fun `should map install attribution fields on install event`() {
        // Given
        val payload = installPayload(
            InstallAttribution(
                playInstallReferrer = InstallReferrer(
                    referrerUrl = "utm_source=google&gclid=abc",
                    referrerClickTimestampMs = 1_550_420_123_000L,
                    installBeginTimestampMs = 1_550_420_200_000L,
                    isGooglePlayInstant = false
                ),
                metaInstallReferrer = MetaInstallReferrer(
                    installReferrer = "encrypted",
                    isClickThrough = true,
                    actualTimestamp = Instant.fromEpochSeconds(1_550_420_000L)
                ),
                currentDeviceTime = Instant.fromEpochMilliseconds(1_550_420_454_906L)
            )
        )

        // When
        val fields = payload.toMeasurementFields()

        // Then
        val expected = MeasurementValue.Object(
            mapOf(
                "go_click_timestamp_seconds" to MeasurementValue.Int(1_550_420_123UL),
                "go_install_begin_timestamp_seconds" to MeasurementValue.Int(1_550_420_200UL),
                "go_current_device_time" to MeasurementValue.Int(1_550_420_454_906UL),
                "me_install_referrer" to MeasurementValue.String("encrypted"),
                "me_is_ct" to MeasurementValue.Int(1UL),
                "me_actual_timestamp" to MeasurementValue.Int(1_550_420_000UL)
            )
        )
        assertEquals(MeasurementValue.String("utm_source=google&gclid=abc"), fields["install_ref"])
        assertEquals(expected, fields[INSTALL_REF_PARAMETERS])
    }

    @Test
    fun `should not add install ref parameters without attribution`() {
        // Given
        val payload = installPayload(null)

        // When
        val fields = payload.toMeasurementFields()

        // Then
        assertFalse(fields.containsKey(INSTALL_REF_PARAMETERS))
    }

    @Test
    fun `should only add device time when no referrer is available`() {
        // Given
        val payload = installPayload(
            InstallAttribution(
                playInstallReferrer = null,
                metaInstallReferrer = null,
                currentDeviceTime = Instant.fromEpochMilliseconds(1_000L)
            )
        )

        // When
        val fields = payload.toMeasurementFields()

        // Then
        val expected = MeasurementValue.Object(mapOf("go_current_device_time" to MeasurementValue.Int(1_000UL)))
        assertEquals(expected, fields[INSTALL_REF_PARAMETERS])
    }

    private fun installPayload(installAttribution: InstallAttribution?) = EventPayload.Install(
        metadata = metadata,
        isReinstall = false,
        installRef = "utm_source=google&gclid=abc",
        installReceipt = null,
        installAttribution = installAttribution
    )

    private companion object {

        const val INSTALL_REF_PARAMETERS = "install_ref_parameters"
    }
}
