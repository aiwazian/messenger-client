/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.HomeWork
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.rounded.AlternateEmail
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.WorkOutline
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aiwazian.messenger.R
import com.aiwazian.messenger.domain.DeviceContact
import com.aiwazian.messenger.domain.MessageContact
import com.aiwazian.messenger.extensions.findActivity
import com.aiwazian.messenger.ui.app.AppBottomSheet
import com.aiwazian.messenger.ui.app.AppBottomSheetFooterActions
import com.aiwazian.messenger.ui.components.MediaPickerNotice
import com.aiwazian.messenger.ui.components.openAppSettings
import com.aiwazian.messenger.ui.components.section.SectionContainer
import com.aiwazian.messenger.ui.components.section.SectionToggle
import com.aiwazian.messenger.ui.screens.chat.DeviceContactsViewModel

@Composable
fun ContactPickerContent(
    onContactSelected: (DeviceContact) -> Unit,
    contentPadding: PaddingValues = PaddingValues.Zero
) {
    val viewModel: DeviceContactsViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    var hasPermission by remember { mutableStateOf(context.hasContactsPermission()) }
    var wasAsked by remember { mutableStateOf(false) }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasPermission = isGranted
            wasAsked = true
            
            if (isGranted) {
                viewModel.loadContacts()
            }
        })
    
    LaunchedEffect(Unit) {
        if (hasPermission) {
            viewModel.loadContacts()
        } else {
            permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        }
    }
    
    Box(contentAlignment = Alignment.TopCenter) {
        when {
            !hasPermission -> {
                MediaPickerNotice(
                    text = stringResource(R.string.contact_picker_permission),
                    modifier = Modifier.padding(
                        top = contentPadding.calculateTopPadding(),
                        bottom = contentPadding.calculateBottomPadding()
                    ),
                    actionText = stringResource(R.string.contact_picker_permission_action),
                    onActionClick = {
                        if (context.canRequestContactsPermission(wasAsked)) {
                            permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                        } else {
                            context.openAppSettings()
                        }
                    })
            }
            
            uiState.isLoading && uiState.contacts.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(contentPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularWavyProgressIndicator()
                }
            }
            
            uiState.contacts.isEmpty() -> {
                MediaPickerNotice(
                    text = stringResource(R.string.contact_picker_empty),
                    modifier = Modifier.padding(contentPadding)
                )
            }
            
            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(
                        start = contentPadding.calculateStartPadding(LocalLayoutDirection.current),
                        end = contentPadding.calculateEndPadding(LocalLayoutDirection.current),
                        bottom = contentPadding.calculateBottomPadding()
                    )
                ) {
                    item {
                        Spacer(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                        )
                    }
                    
                    items(items = uiState.contacts) { contact ->
                        DeviceContactCard(
                            contact = contact,
                            onClick = { onContactSelected(contact) })
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceContactCard(
    contact: DeviceContact,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Text(
                text = contact.displayName,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp,
                lineHeight = 20.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            contact.phones.firstOrNull()?.let { phone ->
                Text(
                    text = phone,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    lineHeight = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactSendBottomSheet(
    contact: DeviceContact,
    onDismiss: () -> Unit,
    onSend: (MessageContact) -> Unit
) {
    val contactsViewModel: DeviceContactsViewModel = hiltViewModel()
    
    var selectedPhones by remember(contact) { mutableStateOf(contact.phones.toSet()) }
    var selectedEmails by remember(contact) { mutableStateOf(contact.emails.toSet()) }
    var selectedUrls by remember(contact) { mutableStateOf(contact.urls.toSet()) }
    var selectedAddresses by remember(contact) { mutableStateOf(contact.addresses.toSet()) }
    var includeOrganization by remember(contact) {
        mutableStateOf(!contact.organization.isNullOrBlank())
    }
    var includeJobTitle by remember(contact) {
        mutableStateOf(!contact.jobTitle.isNullOrBlank())
    }
    var includeBirthday by remember(contact) {
        mutableStateOf(!contact.birthday.isNullOrBlank())
    }
    
    AppBottomSheet(onDismissRequest = onDismiss, stickyFooter = {
        AppBottomSheetFooterActions.PrimaryButton(
            text = stringResource(R.string.send_contact),
            onClick = {
                onSend(
                    MessageContact(
                        name = contact.displayName,
                        phones = contact.phones.filter { it in selectedPhones },
                        emails = contact.emails.filter { it in selectedEmails },
                        urls = contact.urls.filter { it in selectedUrls },
                        addresses = contact.addresses.filter { it in selectedAddresses },
                        organization = contact.organization.takeIf { includeOrganization },
                        jobTitle = contact.jobTitle.takeIf { includeJobTitle },
                        birthday = contact.birthday.takeIf { includeBirthday }
                    )
                )
            },
            enabled = selectedPhones.isNotEmpty()
        )
    }) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            Text(
                text = contact.displayName,
                fontSize = 18.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            
            if (!contact.hasMultipleFields) {
                contact.phones.firstOrNull()?.let { phone ->
                    Text(
                        text = phone,
                        fontSize = 13.sp,
                        lineHeight = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(10.dp))
            
            if (contact.hasMultipleFields) {
                SectionContainer(contentPadding = PaddingValues.Zero) {
                    contact.phones.forEach { phone ->
                        ContactValueToggle(
                            headlineText = phone,
                            labelRes = R.string.contact_phone,
                            leadingIcon = Icons.Outlined.Phone,
                            isChecked = phone in selectedPhones,
                            onToggle = { selectedPhones = toggleValue(selectedPhones, phone) },
                            onCopy = { contactsViewModel.copyValue(phone) })
                    }
                    
                    contact.emails.forEach { email ->
                        ContactValueToggle(
                            headlineText = email,
                            labelRes = R.string.contact_email,
                            leadingIcon = Icons.Rounded.AlternateEmail,
                            isChecked = email in selectedEmails,
                            onToggle = { selectedEmails = toggleValue(selectedEmails, email) },
                            onCopy = { contactsViewModel.copyValue(email) })
                    }
                    
                    contact.urls.forEach { url ->
                        ContactValueToggle(
                            headlineText = url,
                            labelRes = R.string.contact_website,
                            leadingIcon = Icons.Rounded.Link,
                            isChecked = url in selectedUrls,
                            onToggle = { selectedUrls = toggleValue(selectedUrls, url) },
                            onCopy = { contactsViewModel.copyValue(url) })
                    }
                    
                    contact.addresses.forEach { address ->
                        ContactValueToggle(
                            headlineText = address,
                            labelRes = R.string.contact_address,
                            leadingIcon = Icons.Outlined.LocationOn,
                            isChecked = address in selectedAddresses,
                            onToggle = {
                                selectedAddresses =
                                    toggleValue(selectedAddresses, address)
                            },
                            onCopy = { contactsViewModel.copyValue(address) })
                    }
                    
                    contact.organization?.let { organization ->
                        ContactValueToggle(
                            headlineText = organization,
                            labelRes = R.string.contact_organization,
                            leadingIcon = Icons.Outlined.HomeWork,
                            isChecked = includeOrganization,
                            onToggle = { includeOrganization = !includeOrganization },
                            onCopy = { contactsViewModel.copyValue(organization) })
                    }
                    
                    contact.jobTitle?.let { jobTitle ->
                        ContactValueToggle(
                            headlineText = jobTitle,
                            labelRes = R.string.contact_job_title,
                            leadingIcon = Icons.Rounded.WorkOutline,
                            isChecked = includeJobTitle,
                            onToggle = { includeJobTitle = !includeJobTitle },
                            onCopy = { contactsViewModel.copyValue(jobTitle) })
                    }
                    
                    contact.birthday?.let { birthday ->
                        ContactValueToggle(
                            headlineText = birthday,
                            labelRes = R.string.contact_birthday,
                            leadingIcon = Icons.Outlined.CalendarToday,
                            isChecked = includeBirthday,
                            onToggle = { includeBirthday = !includeBirthday },
                            onCopy = { contactsViewModel.copyValue(birthday) })
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactValueToggle(
    headlineText: String,
    labelRes: Int,
    leadingIcon: ImageVector,
    isChecked: Boolean,
    onToggle: () -> Unit,
    onCopy: () -> Unit
) {
    SectionToggle(
        headlineText = headlineText,
        supportingText = stringResource(labelRes),
        leadingIcon = leadingIcon,
        isChecked = isChecked,
        onCheckedChange = onToggle,
        onLongClick = onCopy
    )
}

private fun toggleValue(values: Set<String>, value: String): Set<String> {
    return if (value in values) values - value else values + value
}

private fun Context.hasContactsPermission(): Boolean {
    return ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.READ_CONTACTS
    ) == PackageManager.PERMISSION_GRANTED
}

private fun Context.canRequestContactsPermission(wasAsked: Boolean): Boolean {
    if (!wasAsked) return true
    
    val activity = findActivity() ?: return false
    
    return ActivityCompat.shouldShowRequestPermissionRationale(
        activity,
        Manifest.permission.READ_CONTACTS
    )
}
