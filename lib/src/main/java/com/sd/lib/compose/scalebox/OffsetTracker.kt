package com.sd.lib.compose.scalebox

import androidx.compose.ui.geometry.Offset

internal abstract class OffsetTracker
protected constructor(val initOffset: Offset) {

    var directionOffset: Offset? = null
        private set

    fun track(offset: Offset): Boolean {
        if (directionOffset == null && compareOffset(offset, initOffset) != null) {
            directionOffset = offset
            return false
        }

        val direction = directionOffset ?: return false
        if (compareOffset(offset, initOffset) == null) return true

        val compare = compareOffset(direction, initOffset)
        if (compare == true) {
            if (compareOffset(offset, initOffset) == false) return true
        } else if (compare == false) {
            if (compareOffset(offset, initOffset) == true) return true
        } else {
            error("directionOffset == initOffset")
        }
        return false
    }

    abstract fun compareOffset(a: Offset, b: Offset): Boolean?

    companion object {
        fun x(initOffset: Offset): OffsetTracker {
            return OffsetXTracker(initOffset)
        }

        fun y(initOffset: Offset): OffsetTracker {
            return OffsetYTracker(initOffset)
        }
    }
}

private class OffsetXTracker(initOffset: Offset) : OffsetTracker(initOffset) {
    override fun compareOffset(a: Offset, b: Offset): Boolean? {
        return if (a.x > b.x) {
            true
        } else if (a.x < b.x) {
            false
        } else {
            null
        }
    }
}

private class OffsetYTracker(initOffset: Offset) : OffsetTracker(initOffset) {
    override fun compareOffset(a: Offset, b: Offset): Boolean? {
        return if (a.y > b.y) {
            true
        } else if (a.y < b.y) {
            false
        } else {
            null
        }
    }
}