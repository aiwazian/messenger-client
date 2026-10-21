/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.domain

import java.io.IOException

sealed class AttachmentUploadException(message: String, cause: Throwable? = null) :
    IOException(message, cause) {
    
    class SourceMissing(val uri: String, cause: Throwable? = null) :
        AttachmentUploadException("Attachment source $uri is gone", cause)
    
    class Empty(val uri: String) :
        AttachmentUploadException("Attachment source $uri is empty")
    
    class TooLarge(val size: Long, val limit: Long) :
        AttachmentUploadException("File size $size exceeds limit of $limit bytes")
}
