/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.HomeWork
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.rounded.AlternateEmail
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.WorkOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.aiwazian.messenger.R
import com.aiwazian.messenger.domain.MessageContact
import com.aiwazian.messenger.ui.app.AppBottomSheet
import com.aiwazian.messenger.ui.app.AppBottomSheetFooterActions
import com.aiwazian.messenger.ui.components.section.SectionButton
import com.aiwazian.messenger.ui.components.section.SectionContainer
import com.aiwazian.messenger.utils.LinkNormalizer
import com.aiwazian.messenger.utils.PhoneLinks
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactViewBottomSheet(
    contact: MessageContact,
    onCopyValue: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val region = PhoneLinks.deviceRegion(context)
    
    AppBottomSheet(onDismissRequest = onDismiss, stickyFooter = {
        AppBottomSheetFooterActions.PrimaryButton(
            text = stringResource(R.string.add_contact),
            onClick = { context.startActivity(buildInsertContactIntent(contact)) }
        )
    }) { padding ->
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(padding)
        ) {
            Text(
                text = contact.name,
                fontSize = 18.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(10.dp))
            
            SectionContainer(contentPadding = PaddingValues.Zero) {
                contact.phones.forEach { phone ->
                    ContactValueRow(
                        headlineText = PhoneLinks.formatInternational(phone, region),
                        labelRes = R.string.contact_phone,
                        leadingIcon = Icons.Outlined.Phone,
                        onCopy = { onCopyValue(PhoneLinks.formatInternational(phone, region)) },
                        onClick = { openDialer(context, phone) })
                }
                
                contact.emails.forEach { email ->
                    ContactValueRow(
                        headlineText = email,
                        labelRes = R.string.contact_email,
                        leadingIcon = Icons.Rounded.AlternateEmail,
                        onCopy = { onCopyValue(email) },
                        onClick = { openEmailApp(context, email) })
                }
                
                contact.urls.forEach { url ->
                    ContactValueRow(
                        headlineText = url,
                        labelRes = R.string.contact_website,
                        leadingIcon = Icons.Rounded.Link,
                        onCopy = { onCopyValue(url) },
                        onClick = { openInAppBrowser(context, url) })
                }
                
                contact.addresses.forEach { address ->
                    ContactValueRow(
                        headlineText = address,
                        labelRes = R.string.contact_address,
                        leadingIcon = Icons.Outlined.LocationOn,
                        onCopy = { onCopyValue(address) },
                        onClick = { onCopyValue(address) })
                }
                
                contact.organization?.let { organization ->
                    ContactValueRow(
                        headlineText = organization,
                        labelRes = R.string.contact_organization,
                        leadingIcon = Icons.Outlined.HomeWork,
                        onCopy = { onCopyValue(organization) },
                        onClick = { onCopyValue(organization) })
                }
                
                contact.jobTitle?.let { jobTitle ->
                    ContactValueRow(
                        headlineText = jobTitle,
                        labelRes = R.string.contact_job_title,
                        leadingIcon = Icons.Rounded.WorkOutline,
                        onCopy = { onCopyValue(jobTitle) },
                        onClick = { onCopyValue(jobTitle) })
                }
                
                contact.birthday?.let { birthday ->
                    ContactValueRow(
                        headlineText = birthday,
                        labelRes = R.string.contact_birthday,
                        leadingIcon = Icons.Outlined.CalendarToday,
                        onCopy = { onCopyValue(birthday) },
                        onClick = { onCopyValue(birthday) })
                }
            }
        }
    }
}

@Composable
private fun ContactValueRow(
    headlineText: String,
    labelRes: Int,
    leadingIcon: ImageVector,
    onCopy: () -> Unit,
    onClick: () -> Unit
) {
    SectionButton(
        headlineText = headlineText,
        supportingText = stringResource(labelRes),
        leadingIcon = leadingIcon,
        onClick = onClick,
        onLongClick = onCopy
    )
}

private fun openDialer(context: Context, phone: String) {
    val intent = Intent(Intent.ACTION_DIAL, "tel:$phone".toUri())
    
    runCatching { context.startActivity(intent) }
}

private fun openEmailApp(context: Context, email: String) {
    val intent = Intent(Intent.ACTION_SENDTO, "mailto:${Uri.encode(email)}".toUri())
    
    runCatching { context.startActivity(intent) }
}

private fun openInAppBrowser(context: Context, url: String) {
    val tabsIntent = CustomTabsIntent.Builder()
        .setShowTitle(true)
        .setTranslateLocale(Locale.getDefault())
        .build()
    
    runCatching {
        tabsIntent.launchUrl(context, LinkNormalizer.normalize(url).toUri())
    }
}

private fun buildInsertContactIntent(contact: MessageContact): Intent {
    return Intent(ContactsContract.Intents.Insert.ACTION).apply {
        type = ContactsContract.RawContacts.CONTENT_TYPE
        
        putExtra(ContactsContract.Intents.Insert.NAME, contact.name)
        
        contact.phones.getOrNull(0)?.let {
            putExtra(ContactsContract.Intents.Insert.PHONE, it)
        }
        contact.phones.getOrNull(1)?.let {
            putExtra(ContactsContract.Intents.Insert.SECONDARY_PHONE, it)
        }
        contact.phones.getOrNull(2)?.let {
            putExtra(ContactsContract.Intents.Insert.TERTIARY_PHONE, it)
        }
        
        contact.emails.getOrNull(0)?.let {
            putExtra(ContactsContract.Intents.Insert.EMAIL, it)
        }
        contact.emails.getOrNull(1)?.let {
            putExtra(ContactsContract.Intents.Insert.SECONDARY_EMAIL, it)
        }
        contact.emails.getOrNull(2)?.let {
            putExtra(ContactsContract.Intents.Insert.TERTIARY_EMAIL, it)
        }
        
        contact.addresses.firstOrNull()?.let {
            putExtra(ContactsContract.Intents.Insert.POSTAL, it)
        }
        
        contact.organization?.let {
            putExtra(ContactsContract.Intents.Insert.COMPANY, it)
        }
        
        contact.jobTitle?.let {
            putExtra(ContactsContract.Intents.Insert.JOB_TITLE, it)
        }
    }
}
