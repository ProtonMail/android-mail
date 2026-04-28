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

@file:OptIn(ExperimentalMaterial3Api::class)

package ch.protonmail.android.mailcomposer.presentation.ui

import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Calendar
import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.getSelectedDate
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.tooling.preview.Preview
import ch.protonmail.android.design.compose.component.ProtonAlertDialog
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.design.compose.theme.bodyLargeNorm
import ch.protonmail.android.mailcomposer.presentation.R
import ch.protonmail.android.mailcomposer.presentation.model.ExpirationTimeOption
import ch.protonmail.android.mailcomposer.presentation.model.ExpirationTimeUiModel
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toKotlinMonth
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

@Composable
fun CustomExpirationDateTimePicker(
    onTimePicked: (ExpirationTimeUiModel) -> Unit,
    onDismiss: () -> Unit,
    initialTime: Instant
) {

    val timeZone = remember { TimeZone.currentSystemDefault() }
    val localDateTime = remember(initialTime) {
        val instant = Instant.fromEpochMilliseconds(initialTime.toEpochMilliseconds())
        instant.toLocalDateTime(timeZone)
    }
    val shownDialogType = rememberSaveable { mutableStateOf(DialogType.SelectDate) }

    val timePickerState = rememberTimePickerState(localDateTime.hour, localDateTime.minute)
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = localDateTime.toInstant(UtcOffset.ZERO).toEpochMilliseconds(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val timeZone = TimeZone.currentSystemDefault()
                val instant = Instant.fromEpochMilliseconds(utcTimeMillis)

                val today = Clock.System.now().toLocalDateTime(timeZone).date
                val maxDate = today.plus(MAX_EXPIRATION_DAYS, DateTimeUnit.DAY)
                val dateOfTimestamp = instant.toLocalDateTime(timeZone).date
                return dateOfTimestamp in today..maxDate
            }

            override fun isSelectableYear(year: Int): Boolean {
                val timeZone = TimeZone.currentSystemDefault()
                val today = Clock.System.now().toLocalDateTime(timeZone).date
                val maxDate = today.plus(MAX_EXPIRATION_DAYS, DateTimeUnit.DAY)
                return year in today.year..maxDate.year
            }
        }
    )

    when (shownDialogType.value) {
        DialogType.SelectDate -> ExpirationDatePickerDialog(
            datePickerState = datePickerState,
            onDismiss = onDismiss,
            onDateSelected = { shownDialogType.value = DialogType.SetExpiration }
        )

        DialogType.SelectTime -> ExpirationTimePickerDialog(
            timePickerState = timePickerState,
            onDismiss = onDismiss,
            onTimeSelected = { shownDialogType.value = DialogType.SetExpiration }
        )
        DialogType.SetExpiration -> SetCustomExpirationDialog(
            timePickerState = timePickerState,
            datePickerState = datePickerState,
            onDismiss = { onDismiss() },
            onSetExpiration = {
                datePickerState.getSelectedDate()?.let { date ->
                    val dateTime = LocalDateTime(
                        year = date.year,
                        month = date.month.toKotlinMonth(),
                        day = date.dayOfMonth,
                        hour = timePickerState.hour,
                        minute = timePickerState.minute,
                        second = 0,
                        nanosecond = 0
                    )
                    val timeZone = TimeZone.currentSystemDefault()
                    val timestamp = Instant.fromEpochSeconds(dateTime.toInstant(timeZone).epochSeconds)
                    val expirationTime = ExpirationTimeUiModel(ExpirationTimeOption.Custom, timestamp)
                    onTimePicked(expirationTime)
                }
            },
            onPickDate = { shownDialogType.value = DialogType.SelectDate },
            onPickTime = { shownDialogType.value = DialogType.SelectTime }
        )
    }
}

@Suppress("UseComposableActions")
@Composable
fun SetCustomExpirationDialog(
    timePickerState: TimePickerState,
    datePickerState: DatePickerState,
    onDismiss: () -> Unit,
    onSetExpiration: () -> Unit,
    onPickDate: () -> Unit,
    onPickTime: () -> Unit,
    modifier: Modifier = Modifier
) {

    val context = LocalContext.current
    val dateFormatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL) }
    val formattedDate = remember { datePickerState.getSelectedDate()?.format(dateFormatter) ?: "" }
    val is24HoursFormat = remember { DateFormat.is24HourFormat(context) }
    val timePattern = if (is24HoursFormat) "HH:mm" else "hh:mm a"
    val timeFormatter = remember { SimpleDateFormat(timePattern, Locale.current.platformLocale) }
    val formattedTime = remember { mutableStateOf("") }
    val calendar = remember { Calendar.getInstance() }

    LaunchedEffect(timePickerState.hour, timePickerState.minute) {
        calendar.set(Calendar.HOUR_OF_DAY, timePickerState.hour)
        calendar.set(Calendar.MINUTE, timePickerState.minute)
        calendar.isLenient = false
        formattedTime.value = timeFormatter.format(calendar.time)
    }

    ProtonAlertDialog(
        modifier = modifier,
        onDismissRequest = {},
        confirmButton = {
            TextButton(onClick = onSetExpiration) {
                Text(stringResource(R.string.composer_expiration_set_expiration))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.presentation_alert_cancel))
            }
        },
        title = {
            Text(stringResource(R.string.expiration_time_set_time_and_date))
        },
        text = {
            Column {
                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = ProtonTheme.colors.backgroundNorm,
                            shape = ProtonTheme.shapes.medium
                        )
                        .clickable(role = Role.Button, onClick = onPickDate),
                    value = formattedDate,
                    onValueChange = {},
                    enabled = false,
                    readOnly = true,
                    textStyle = ProtonTheme.typography.bodyLargeNorm,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ProtonTheme.colors.shade40,
                        unfocusedBorderColor = ProtonTheme.colors.shade40
                    ),
                    trailingIcon = {
                        Icon(
                            modifier = Modifier.size(ProtonDimens.IconSize.Small),
                            painter = painterResource(id = R.drawable.ic_proton_chevron_down_filled),
                            contentDescription = null,
                            tint = ProtonTheme.colors.iconNorm
                        )
                    }
                )

                Spacer(modifier = Modifier.size(ProtonDimens.Spacing.Large))

                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = ProtonTheme.colors.backgroundNorm,
                            shape = ProtonTheme.shapes.medium
                        )
                        .clickable(role = Role.Button, onClick = onPickTime),
                    value = formattedTime.value,
                    onValueChange = {},
                    enabled = false,
                    readOnly = true,
                    textStyle = ProtonTheme.typography.bodyLargeNorm,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ProtonTheme.colors.shade40,
                        unfocusedBorderColor = ProtonTheme.colors.shade40
                    ),
                    trailingIcon = {
                        Icon(
                            modifier = Modifier.size(ProtonDimens.IconSize.Small),
                            painter = painterResource(id = R.drawable.ic_proton_chevron_down_filled),
                            contentDescription = null,
                            tint = ProtonTheme.colors.iconNorm
                        )
                    }
                )
            }
        }
    )
}

@Composable
private fun ExpirationDatePickerDialog(
    datePickerState: DatePickerState,
    onDismiss: () -> Unit,
    onDateSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pickedDate = remember { mutableStateOf("") }
    val formatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }

    LaunchedEffect(datePickerState.getSelectedDate()) {
        pickedDate.value = datePickerState.getSelectedDate()?.format(formatter) ?: ""
    }

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDateSelected) {
                Text(stringResource(R.string.presentation_alert_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.presentation_alert_cancel))
            }
        },
        shape = ProtonTheme.shapes.extraLarge,
        colors = DatePickerDefaults.colors().copy(
            containerColor = ProtonTheme.colors.backgroundNorm,
            dividerColor = ProtonTheme.colors.backgroundInvertedNorm
        )
    ) {
        Column {
            DatePicker(
                modifier = modifier.verticalScroll(rememberScrollState()),
                state = datePickerState,
                showModeToggle = false,
                colors = DatePickerDefaults.colors().copy(
                    containerColor = ProtonTheme.colors.backgroundNorm,
                    dividerColor = ProtonTheme.colors.backgroundInvertedNorm
                )
            )
        }

    }
}

@Composable
private fun ExpirationTimePickerDialog(
    timePickerState: TimePickerState,
    onDismiss: () -> Unit,
    onTimeSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    TimePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onTimeSelected) {
                Text(stringResource(R.string.presentation_alert_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.presentation_alert_cancel))
            }
        },
        shape = ProtonTheme.shapes.extraLarge,
        title = { Text(stringResource(R.string.composer_expiration_pick_time_title)) }
    ) {
        Column {
            TimePicker(
                modifier = modifier.verticalScroll(rememberScrollState()),
                state = timePickerState,
                colors = TimePickerDefaults.colors().copy(
                    containerColor = ProtonTheme.colors.backgroundInvertedNorm,
                    timeSelectorSelectedContentColor = ProtonTheme.colors.textAccent,
                    timeSelectorSelectedContainerColor = ProtonTheme.colors.interactionBrandWeakNorm,
                    timeSelectorUnselectedContainerColor = ProtonTheme.colors.backgroundInvertedDeep,
                    periodSelectorSelectedContainerColor = ProtonTheme.colors.interactionBrandWeakNorm,
                    periodSelectorUnselectedContainerColor = ProtonTheme.colors.backgroundInvertedDeep,
                    clockDialColor = ProtonTheme.colors.interactionBrandWeakNorm
                )
            )
        }

    }
}

private const val MAX_EXPIRATION_DAYS = 28

private enum class DialogType {
    SelectDate,
    SelectTime,
    SetExpiration
}

@Preview
@Composable
@Suppress("MagicNumber")
fun PreviewCustomExpirationTimeDialog() {
    CustomExpirationDateTimePicker(
        onTimePicked = {},
        onDismiss = {},
        initialTime = Instant.fromEpochSeconds(1_709_557_304)
    )
}

@Preview
@Composable
fun PreviewSetCustomExpirationDialog() {
    SetCustomExpirationDialog(
        timePickerState = rememberTimePickerState(),
        datePickerState = rememberDatePickerState(),
        onSetExpiration = {},
        onPickDate = {},
        onPickTime = {},
        onDismiss = {}
    )
}

