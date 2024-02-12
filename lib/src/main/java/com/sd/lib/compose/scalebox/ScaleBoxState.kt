package com.sd.lib.compose.scalebox

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Velocity
import com.sd.lib.compose.gesture.fConsume
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

@Composable
fun rememberScaleBoxState(): ScaleBoxState {
    val coroutineScope = rememberCoroutineScope()
    return remember(coroutineScope) { ScaleBoxState(coroutineScope) }
}

class ScaleBoxState internal constructor(
    private val scope: CoroutineScope
) {
    private val defaultScale = 1f
    private val maxScale = 10f
    private val doubleClickScale = 3f

    private val minScaleDrag = defaultScale * 0.5f
    private val maxScaleDrag = maxScale * 2

    private val _animateScale = Animatable(defaultScale)
    private var _animateOffsetX = Animatable(0f)
    private var _animateOffsetY = Animatable(0f)

    internal val isReady by derivedStateOf {
        boxSize.width > 0 && boxSize.height > 0
                && contentSize.width > 0 && contentSize.height > 0
    }

    internal var boxSize by mutableStateOf(IntSize.Zero)
    internal var contentSize by mutableStateOf(IntSize.Zero)

    var scale by mutableFloatStateOf(defaultScale)
        private set

    var offsetX by mutableFloatStateOf(0f)
        private set

    var offsetY by mutableFloatStateOf(0f)
        private set

    var transformOrigin by mutableStateOf(TransformOrigin.Center)
        private set

    private val boundsX: BoundsHandler
        get() = BoundsHandler(
            boxSize = boxSize.width,
            contentSize = contentSize.width,
            scale = scale,
            pivot = transformOrigin.pivotFractionX,
        )

    private val boundsY: BoundsHandler
        get() = BoundsHandler(
            boxSize = boxSize.height,
            contentSize = contentSize.height,
            scale = scale,
            pivot = transformOrigin.pivotFractionY,
        )

    private var _isFirstScale = false
    private var _startScale = 1f

    internal fun cancelAnimator() {
        scope.launch {
            _animateScale.stop()
            _animateOffsetX.stop()
            _animateOffsetY.stop()
        }
    }

    internal fun handleDoubleClick() {
        scope.launch {
            if (scale == defaultScale) {
                animateScaleTo(doubleClickScale)
            } else {
                animateScaleTo(defaultScale)
            }
        }
    }

    internal fun handleDrag(change: Offset): DragResult {
        val oldX = offsetX
        val oldY = offsetY

        val bx = boundsX
        with(bx) {
            if (isOverSize) {
                val newX = offsetX + change.x
                offsetX = newX.coerceIn(minOffset, maxOffset)
            }
        }

        val by = boundsY
        with(by) {
            if (isOverSize) {
                val newY = offsetY + change.y
                offsetY = newY.coerceIn(minOffset, maxOffset)
            }
        }

        if (offsetX == oldX && bx.isOverSize) {
            if (offsetX == bx.minOffset && change.x < 0 && change.preferX()) return DragResult.OverDragX
            if (offsetX == bx.maxOffset && change.x > 0 && change.preferX()) return DragResult.OverDragX
        }

        if (offsetY == oldY && by.isOverSize) {
            if (offsetY == by.minOffset && change.y < 0 && change.preferY()) return DragResult.OverDragY
            if (offsetY == by.maxOffset && change.y > 0 && change.preferY()) return DragResult.OverDragY
        }

        return if (offsetX == oldX && offsetY == oldY) {
            DragResult.Unchanged
        } else {
            DragResult.Changed
        }
    }

    internal fun handleDragFling(velocity: Velocity) {
        scope.launch {
            with(boundsX) {
                _animateOffsetX.stop()
                _animateOffsetX = Animatable(offsetX).apply { updateBounds(minOffset, maxOffset) }
                _animateOffsetX.animateDecay(
                    initialVelocity = velocity.x,
                    animationSpec = exponentialDecay(2f),
                ) {
                    offsetX = this.value
                }
            }
        }
        scope.launch {
            with(boundsY) {
                _animateOffsetY.stop()
                _animateOffsetY = Animatable(offsetY).apply { updateBounds(minOffset, maxOffset) }
                _animateOffsetY.animateDecay(
                    initialVelocity = velocity.y,
                    animationSpec = exponentialDecay(2f),
                ) {
                    offsetY = this.value
                }
            }
        }
    }

    internal fun onScaleStart() {
        _isFirstScale = true
        _startScale = scale
    }

    internal fun onScale(event: PointerEvent, centroid: Offset, change: Float) {
        event.fConsume { it.positionChanged() }

        val min = minScaleDrag
        val max = if (_startScale == maxScale) maxScaleDrag else maxScale
        val newScale = (scale * change).coerceIn(min, max)

        if (_isFirstScale && newScale > defaultScale) {
            _isFirstScale = false
            applyTransformOrigin(centroid)
        }

        scale = newScale
    }

    internal fun onScaleFinish() {
        scope.launch {
            if (scale <= defaultScale) {
                animateScaleTo(defaultScale)
                return@launch
            }

            checkOffset()

            if (scale > maxScale) {
                animateScaleTo(maxScale)
            }
        }
    }

    private fun applyTransformOrigin(centroid: Offset) {
        var pivotFractionX: Float
        var pivotFractionY: Float

        with(boundsX) {
            val newPivot = getViewportPivot(offsetX, centroid.x)
            val deltaOffset = (newPivot - pivot) * changedSize

            offsetX += deltaOffset
            pivotFractionX = newPivot
        }
        with(boundsY) {
            val newPivot = getViewportPivot(offsetY, centroid.y)
            val deltaOffset = (newPivot - pivot) * changedSize

            offsetY += deltaOffset
            pivotFractionY = newPivot
        }

        transformOrigin = TransformOrigin(pivotFractionX, pivotFractionY)
    }

    private suspend fun checkOffset() {
        var targetXBlock: (() -> Unit)? = null
        val targetX = with(boundsX) {
            if (isOverSize) {
                if (offsetX < minOffset) {
                    minOffset
                } else if (offsetX > maxOffset) {
                    maxOffset
                } else {
                    null
                }
            } else {
                targetXBlock = {
                    transformOrigin = transformOrigin.copy(pivotFractionX = 0.5f)
                    offsetX = 0f
                }
                centerOffset
            }
        }

        var targetYBlock: (() -> Unit)? = null
        val targetY = with(boundsY) {
            if (isOverSize) {
                if (offsetY < minOffset) {
                    minOffset
                } else if (offsetY > maxOffset) {
                    maxOffset
                } else {
                    null
                }
            } else {
                targetYBlock = {
                    transformOrigin = transformOrigin.copy(pivotFractionY = 0.5f)
                    offsetY = 0f
                }
                centerOffset
            }
        }

        targetX?.let { target ->
            scope.launch {
                animateOffsetXTo(target)
                targetXBlock?.invoke()
            }
        }

        targetY?.let { target ->
            scope.launch {
                animateOffsetYTo(target)
                targetYBlock?.invoke()
            }
        }
    }

    private suspend fun animateScaleTo(targetValue: Float) {
        if (targetValue <= defaultScale) {
            scope.launch { animateOffsetXTo(0f) }
            scope.launch { animateOffsetYTo(0f) }
        }

        _animateScale.snapTo(scale)
        _animateScale.animateTo(targetValue, tween()) {
            scale = this.value
        }

        if (scale <= defaultScale) {
            transformOrigin = TransformOrigin.Center
        }
    }

    private suspend fun animateOffsetXTo(targetValue: Float) {
        _animateOffsetX.stop()
        _animateOffsetX = Animatable(offsetX)
        _animateOffsetX.animateTo(targetValue, tween()) {
            offsetX = this.value
        }
    }

    private suspend fun animateOffsetYTo(targetValue: Float) {
        _animateOffsetY.stop()
        _animateOffsetY = Animatable(offsetY)
        _animateOffsetY.animateTo(targetValue, tween()) {
            offsetY = this.value
        }
    }
}

private class BoundsHandler(
    private val boxSize: Int,
    private val contentSize: Int,
    private val scale: Float,
    val pivot: Float,
) {
    init {
        require(boxSize >= 0)
        require(contentSize >= 0)
        require(boxSize >= contentSize)
        require(scale > 0)
    }

    private val halfDeltaSize: Float get() = (boxSize - contentSize) / 2f
    private val currentSize: Float get() = contentSize * scale

    val changedSize: Float get() = currentSize - contentSize
    val isOverSize: Boolean get() = currentSize > boxSize

    val minOffset: Float
        get() = if (isOverSize) {
            val pivotSize = changedSize * (1f - pivot)
            halfDeltaSize - pivotSize
        } else {
            0f
        }

    val maxOffset: Float
        get() = if (isOverSize) {
            val pivotSize = changedSize * (pivot)
            pivotSize - halfDeltaSize
        } else {
            0f
        }

    val centerOffset: Float
        get() = if (isOverSize) {
            val delta = (maxOffset - minOffset) / 2f
            minOffset + delta
        } else {
            changedSize * (pivot - 0.5f)
        }

    fun getViewportPivot(contentOffset: Float, centroid: Float): Float {
        val originalSize = centroid - halfDeltaSize
        val scaleSize = changedSize * pivot - contentOffset
        return (originalSize + scaleSize) / currentSize
    }
}

internal enum class DragResult {
    Changed,
    Unchanged,
    OverDragX,
    OverDragY,
}

private fun Offset.preferX(): Boolean {
    return x.absoluteValue > y.absoluteValue
}

private fun Offset.preferY(): Boolean {
    return y.absoluteValue > x.absoluteValue
}