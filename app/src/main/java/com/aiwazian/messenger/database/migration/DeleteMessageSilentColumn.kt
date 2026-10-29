/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.database.migration

import androidx.room3.DeleteColumn
import androidx.room3.migration.AutoMigrationSpec

@DeleteColumn(
    tableName = "message",
    columnName = "silent"
)
class DeleteMessageSilentColumn : AutoMigrationSpec
