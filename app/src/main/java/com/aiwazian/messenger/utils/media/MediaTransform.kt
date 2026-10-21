/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.utils.media

import android.graphics.Bitmap
import android.graphics.Matrix

data class MediaTransform(
    val rotationDegrees: Int = 0,
    val isMirrored: Boolean = false
) {
    val isIdentity: Boolean
        get() = rotationDegrees == 0 && !isMirrored
    
    val swapsSides: Boolean
        get() = rotationDegrees % HALF_TURN != 0
    
    val mirrorScaleX: Float
        get() = if (isMirrored) -1f else 1f
    
    fun rotated(): MediaTransform = copy(
        rotationDegrees = (rotationDegrees - QUARTER_TURN).mod(FULL_TURN)
    )

    fun mirrored(): MediaTransform = MediaTransform(
        rotationDegrees = (-rotationDegrees).mod(FULL_TURN),
        isMirrored = !isMirrored
    )

    fun toMatrix(): Matrix = Matrix().apply {
        if (isMirrored) {
            postScale(-1f, 1f)
        }
        
        if (rotationDegrees != 0) {
            postRotate(rotationDegrees.toFloat())
        }
    }
    
    companion object {
        val None = MediaTransform()
        
        const val QUARTER_TURN = 90
        
        private const val HALF_TURN = 180
        private const val FULL_TURN = 360
    }
}

fun Bitmap.transformed(transform: MediaTransform): Bitmap {
    if (transform.isIdentity) {
        return this
    }
    
    return Bitmap.createBitmap(this, 0, 0, width, height, transform.toMatrix(), true)
}

fun Bitmap.rotatedQuarter(): Bitmap = transformed(MediaTransform.None.rotated())

fun Bitmap.mirrored(): Bitmap = transformed(MediaTransform(isMirrored = true))
