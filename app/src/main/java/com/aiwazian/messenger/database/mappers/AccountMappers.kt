package com.aiwazian.messenger.database.mappers

import com.aiwazian.messenger.data.LocalAccount
import com.aiwazian.messenger.database.entity.AccountEntity

fun LocalAccount.toEntity(): AccountEntity {
    return AccountEntity(
        id = this.id,
        isCurrent = this.isCurrent
    )
}

fun AccountEntity.toLocal(): LocalAccount {
    return LocalAccount(
        id = this.id,
        isCurrent = this.isCurrent
    )
}