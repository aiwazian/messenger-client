package com.aiwazian.messenger.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity("account")
data class AccountEntity(@PrimaryKey val id: Long, val isCurrent: Boolean)