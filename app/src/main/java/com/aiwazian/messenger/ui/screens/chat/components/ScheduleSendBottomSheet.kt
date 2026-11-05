/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat.components

import android.text.format.DateFormat
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import com.aiwazian.messenger.R
import com.aiwazian.messenger.ui.app.AppBottomSheet
import com.aiwazian.messenger.ui.app.AppBottomSheetFooterActions
import com.aiwazian.messenger.ui.components.section.SectionButton
import com.aiwazian.messenger.ui.components.section.SectionContainer
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleSendBottomSheet(
    onDismissRequest: () -> Unit,
    onConfirm: (Long) -> Unit,
    initialTime: Long? = null
) {
    val context = LocalContext.current
    val zone = remember { ZoneId.systemDefault() }
    
    val initial = remember(initialTime) {
        initialTime
            ?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDateTime() }
            ?: LocalDateTime.now(zone)
                .withSecond(0)
                .withNano(0)
                .plusMinutes(1)
    }
    
    var selectedDate by remember { mutableStateOf(initial.toLocalDate()) }
    var selectedTime by remember { mutableStateOf(initial.toLocalTime()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        while (true) {
            delay(1.seconds)
            
            val now = LocalDateTime.now(zone)
            val current = LocalDateTime.of(selectedDate, selectedTime)
            
            if (!current.isAfter(now) && !selectedDate.isAfter(LocalDate.now(zone))) {
                val next = now.plusMinutes(1).truncatedTo(ChronoUnit.MINUTES)
                
                selectedDate = next.toLocalDate()
                selectedTime = next.toLocalTime()
            }
        }
    }
    
    val locale = LocalLocale.current.platformLocale
    val dateFormatter = remember(locale) {
        DateTimeFormatter.ofPattern(
            DateFormat.getBestDateTimePattern(locale, "d MMM"),
            locale
        )
    }
    val timeFormatter = remember(locale) {
        DateTimeFormatter.ofPattern("HH:mm", locale)
    }
    
    val scheduledDateTime = LocalDateTime.of(selectedDate, selectedTime)
    val scheduledMillis = scheduledDateTime.atZone(zone).toInstant().toEpochMilli()
    
    val isToday = selectedDate.isEqual(LocalDate.now(zone))
    val timeIsValid = !isToday || scheduledDateTime.isAfter(LocalDateTime.now(zone))
    
    AppBottomSheet(
        onDismissRequest = onDismissRequest,
        stickyFooter = {
            AppBottomSheetFooterActions.PrimaryButton(
                text = stringResource(
                    R.string.send_scheduled_at,
                    selectedDate.format(dateFormatter),
                    selectedTime.format(timeFormatter)
                ),
                enabled = timeIsValid,
                onClick = { onConfirm(scheduledMillis) }
            )
        }
    ) { padding ->
        SectionContainer(contentPadding = padding) {
            SectionButton(
                headlineText = selectedDate.format(dateFormatter),
                leadingIcon = Icons.Outlined.CalendarToday,
                onClick = { showDatePicker = true }
            )
            SectionButton(
                headlineText = selectedTime.format(timeFormatter),
                leadingIcon = Icons.Outlined.Schedule,
                onClick = { showTimePicker = true }
            )
        }
    }
    
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.atStartOfDay(zone).toInstant().toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    val date = Instant.ofEpochMilli(utcTimeMillis).atZone(zone).toLocalDate()
                    return !date.isBefore(LocalDate.now(zone))
                }
            }
        )
        
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            selectedDate = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
                        }
                        showDatePicker = false
                    }
                ) {
                    Text(text = stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(text = stringResource(R.string.cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
    
    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = selectedTime.hour,
            initialMinute = selectedTime.minute,
            is24Hour = DateFormat.is24HourFormat(context)
        )
        
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedTime = LocalTime.of(timePickerState.hour, timePickerState.minute)
                        showTimePicker = false
                    }
                ) {
                    Text(text = stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text(text = stringResource(R.string.cancel))
                }
            },
            text = {
                TimePicker(state = timePickerState)
            }
        )
    }
}
