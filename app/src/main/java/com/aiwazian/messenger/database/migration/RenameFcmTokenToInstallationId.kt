/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.database.migration

import androidx.room3.RenameColumn
import androidx.room3.migration.AutoMigrationSpec

/**
 * В колонке теперь лежит Firebase Installation ID, а не registration token:
 * новый API FCM адресует уведомления по FID.
 */
@RenameColumn(
    tableName = "account",
    fromColumnName = "fcmToken",
    toColumnName = "installationId"
)
class RenameFcmTokenToInstallationId : AutoMigrationSpec
