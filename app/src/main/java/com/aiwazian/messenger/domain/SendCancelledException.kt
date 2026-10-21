/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.domain

import java.io.IOException

class SendCancelledException(tempId: Long) : IOException("Send #$tempId was cancelled")
