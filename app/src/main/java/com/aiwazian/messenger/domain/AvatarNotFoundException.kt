/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.domain

class AvatarNotFoundException(val fileId: String) :
    Exception("Avatar file $fileId no longer exists on the server")
