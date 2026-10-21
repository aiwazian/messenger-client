package com.aiwazian.messenger.repository

import android.content.Context
import android.provider.ContactsContract
import com.aiwazian.messenger.domain.DeviceContact
import com.aiwazian.messenger.utils.PhoneLinks
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceContactsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun getContacts(): List<DeviceContact> =
        withContext(Dispatchers.IO) { queryContacts() }

    private fun queryContacts(): List<DeviceContact> {
        val contacts = mutableMapOf<Long, DeviceContactBuilder>()
        val region = PhoneLinks.deviceRegion(context)

        context.contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            PROJECTION,
            SELECTION,
            SELECTION_ARGS,
            SORT_ORDER
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(ContactsContract.Data.CONTACT_ID)
            val nameIndex = cursor.getColumnIndexOrThrow(ContactsContract.Data.DISPLAY_NAME)
            val mimetypeIndex = cursor.getColumnIndexOrThrow(ContactsContract.Data.MIMETYPE)
            val data1Index = cursor.getColumnIndexOrThrow(ContactsContract.Data.DATA1)
            val data2Index = cursor.getColumnIndexOrThrow(ContactsContract.Data.DATA2)
            val data4Index = cursor.getColumnIndexOrThrow(ContactsContract.Data.DATA4)

            while (cursor.moveToNext()) {
                val contactId = cursor.getLong(idIndex)
                val builder = contacts.getOrPut(contactId) { DeviceContactBuilder() }

                if (builder.displayName.isBlank()) {
                    builder.displayName = cursor.getString(nameIndex).orEmpty().trim()
                }

                when (cursor.getString(mimetypeIndex)) {
                    ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE -> {
                        cursor.getString(data1Index)
                            ?.trim()
                            ?.takeIf { it.isNotEmpty() }
                            ?.let { raw -> builder.phones.add(PhoneLinks.formatInternational(raw, region)) }
                    }

                    ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE -> {
                        cursor.getString(data1Index)
                            ?.trim()
                            ?.takeIf { it.isNotEmpty() }
                            ?.let(builder.emails::add)
                    }

                    ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE -> {
                        cursor.getString(data1Index)
                            ?.trim()
                            ?.takeIf { it.isNotEmpty() }
                            ?.let(builder.urls::add)
                    }

                    ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE -> {
                        cursor.getString(data1Index)
                            ?.trim()
                            ?.takeIf { it.isNotEmpty() }
                            ?.let(builder.addresses::add)
                    }

                    ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE -> {
                        if (builder.organization.isNullOrBlank()) {
                            builder.organization = cursor.getString(data1Index)
                                ?.trim()
                                ?.takeIf { it.isNotEmpty() }
                        }

                        if (builder.jobTitle.isNullOrBlank()) {
                            builder.jobTitle = cursor.getString(data4Index)
                                ?.trim()
                                ?.takeIf { it.isNotEmpty() }
                        }
                    }

                    ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE -> {
                        val eventType = cursor.getInt(data2Index)

                        if (eventType == ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY) {
                            builder.birthday = cursor.getString(data1Index)
                                ?.trim()
                                ?.takeIf { it.isNotEmpty() }
                                ?.let(::normalizeBirthday)
                        }
                    }
                }
            }
        }

        return contacts.values
            .map { it.build() }
            .filter { it.displayName.isNotEmpty() }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.displayName })
    }

    private fun normalizeBirthday(raw: String): String {
        return try {
            LocalDate.parse(raw).format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
        } catch (_: Exception) {
            raw
        }
    }

    private class DeviceContactBuilder {
        var displayName: String = ""

        val phones = linkedSetOf<String>()
        val emails = linkedSetOf<String>()
        val urls = linkedSetOf<String>()
        val addresses = linkedSetOf<String>()

        var organization: String? = null
        var jobTitle: String? = null
        var birthday: String? = null

        fun build() = DeviceContact(
            displayName = displayName,
            phones = phones.toList(),
            emails = emails.toList(),
            urls = urls.toList(),
            addresses = addresses.toList(),
            organization = organization,
            jobTitle = jobTitle,
            birthday = birthday
        )
    }

    private companion object {
        val PROJECTION = arrayOf(
            ContactsContract.Data.CONTACT_ID,
            ContactsContract.Data.DISPLAY_NAME,
            ContactsContract.Data.MIMETYPE,
            ContactsContract.Data.DATA1,
            ContactsContract.Data.DATA2,
            ContactsContract.Data.DATA4
        )

        val SELECTION = "${ContactsContract.Data.MIMETYPE} IN (?, ?, ?, ?, ?, ?)"

        val SELECTION_ARGS = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE,
            ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE,
            ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE,
            ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE,
            ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE,
            ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE
        )

        val SORT_ORDER = "${ContactsContract.Data.DISPLAY_NAME} COLLATE NOCASE ASC"
    }
}
