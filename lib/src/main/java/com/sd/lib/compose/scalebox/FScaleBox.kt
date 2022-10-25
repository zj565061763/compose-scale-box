package com.sd.lib.compose.scalebox

import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Velocity
import com.sd.lib.compose.gesture.fClick
import com.sd.lib.compose.gesture.fConsume
import com.sd.lib.compose.gesture.fPointerChange
import com.sd.lib.compose.gesture.fScaleGesture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

@Composable
fun FScaleBox(
    modifier: Modifier = Modifier,
    debug: Boolean = false,
    trackParentConsume: Boolean? = true,
    onTap: (() -> Unit)? = null,
    content: @Composable (Modifier) -> Unit,
) {
    val debugUpdated by rememberUpdatedState(debug)
    val trackParentConsumeUpdated by rememberUpdatedState(trackParentConsume)

    val coroutineScope = rememberCoroutineScope()
    val state = remember { FScaleBoxState(coroutineScope) }

    var hasMove by remember { mutableStateOf(false) }

    var isConsumedByParent by remember { mutableStateOf(false) }
    val mapDragResult = remember { mutableMapOf<PointerId, DragResult>() }

    var boxLayout by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var boxOffsetTracker by remember { mutableStateOf<OffsetTracker?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .onGloballyPositioned { layout ->
                state.boxSize = layout.size
                boxLayout = layout

                if (isConsumedByParent) {
                    boxOffsetTracker?.let { tracker ->
                        val offset = layout.offset()
                        if (tracker.track(offset)) {
                            isConsumedByParent = false
                            if (debugUpdated) {
                                logMsg { "isConsumedByParent false, track init:${tracker.initOffset} direction:${tracker.directionOffset} offset:$offset" }
                            }
                        }
                    }
                }
            }
            .run {
                if (state.isReady) {
                    fPointerChange(
                        onStart = {
                            if (debugUpdated) {
                                logMsg { "onStart" }
                            }
                            enableVelocity = true
                            hasMove = false
                            mapDragResult.clear()
                            isConsumedByParent = false
                            boxOffsetTracker = null
                            state.cancelAnimator()
                        },
                        onMove = { input ->
                            if (!input.isConsumed && pointerCount == 1) {
                                if (!isConsumedByParent) {
                                    val change = input.positionChange()
                                    val dragResult = state.handleDrag(change)
                                    mapDragResult[input.id] = dragResult

                                    when (dragResult) {
                                        DragResult.Changed -> {
                                            input.consume()
                                            hasMove = true
                                        }
                                        DragResult.OverDragX -> {
                                            if (boxOffsetTracker == null && trackParentConsumeUpdated == true) {
                                                boxLayout?.let { layout ->
                                                    val offset = layout.offset()
                                                    boxOffsetTracker = OffsetTracker.x(offset)
                                                    if (debugUpdated) {
                                                        logMsg { "create offset tracker x $offset" }
                                                    }
                                                }
                                            }
                                        }
                                        DragResult.OverDragY -> {
                                            if (boxOffsetTracker == null && trackParentConsumeUpdated == false) {
                                                boxLayout?.let { layout ->
                                                    val offset = layout.offset()
                                                    boxOffsetTracker = OffsetTracker.y(offset)
                                                    if (debugUpdated) {
                                                        logMsg { "create offset tracker y $offset" }
                                                    }
                                                }
                                            }
                                        }
                                        else -> {}
                                    }
                                }
                            }
                        },
                        onUp = { input ->
                            if (!input.isConsumed && pointerCount == 1) {
                                if (maxPointerCount == 1 && hasMove) {
                                    val velocity = getPointerVelocity(input.id)
                                    state.handleDragFling(velocity)
                                }
                            }
                        },
                        onFinish = {
                            isConsumedByParent = false
                            boxOffsetTracker = null
                            if (debugUpdated) {
                                logMsg { "onFinish" }
                            }
                        }
                    )
                        .fPointerChange(
                            pass = PointerEventPass.Final,
                            onMove = {
                                if (it.isConsumed) {
                                    val dragResult = mapDragResult.remove(it.id)
                                    if (dragResult == DragResult.OverDragX || dragResult == DragResult.OverDragY) {
                                        if (!isConsumedByParent && boxOffsetTracker != null) {
                                            isConsumedByParent = true
                                            if (debugUpdated) {
                                                logMsg { "isConsumedByParent true" }
                                            }
                                        }
                                    }
                                }
                            }
                        )
                        .fScaleGesture(
                            onStart = {
                                state.onScaleStart()
                            },
                            onFinish = {
                                state.onScaleFinish()
                            }
                        ) { centroid, change ->
                            state.onScale(
                                event = currentEvent!!,
                                centroid = centroid,
                                change = change,
                            )
                        }
                        .fClick(
                            onTap = { if (!state.hasScaled) onTap?.invoke() },
                            onDoubleTap = { state.handleDoubleClick() },
                        )
                } else {
                    this
                }
            }

    ) {
        content(
            Modifier
                .align(Alignment.Center)
                .onGloballyPositioned { state.contentSize = it.size }
                .graphicsLayer(
                    scaleX = state.scale,
                    scaleY = state.scale,
                    translationX = state.offsetX,
                    translationY = state.offsetY,
                    transformOrigin = state.transformOrigin,
                )
        )
    }
}

private class FScaleBoxState(
    val coroutineScope: CoroutineScope,
) {
    private val defaultScale = 1f
    private val maxScale = 10f
    private val doubleClickScale = 3f

    private val minScaleDrag = defaultScale * 0.5f
    private val maxScaleDrag = maxScale * 2

    private val _animateScale = Animatable(defaultScale)
    private var _animateOffsetX = Animatable(0f)
    private var _animateOffsetY = Animatable(0f)

    var boxSize by mutableStateOf(IntSize.Zero)
    var contentSize by mutableStateOf(IntSize.Zero)
    val isReady by derivedStateOf {
        boxSize.width > 0 && boxSize.height > 0
                && contentSize.width > 0 && contentSize.height > 0
    }

    var scale by mutableStateOf(defaultScale)
        private set

    var offsetX by mutableStateOf(0f)
        private set

    var offsetY by mutableStateOf(0f)
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

    val hasScaled get() = scale != defaultScale

    private var _isFirstScale = false
    private var _startScale = 1f

    fun cancelAnimator() {
        coroutineScope.launch {
            _animateScale.stop()
            _animateOffsetX.stop()
            _animateOffsetY.stop()
        }
    }

    fun handleDoubleClick() {
        coroutineScope.launch {
            if (scale == defaultScale) {
                animateScaleTo(doubleClickScale)
            } else {
                animateScaleTo(defaultScale)
            }
        }
    }

    fun handleDrag(change: Offset): DragResult {
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

    fun handleDragFling(velocity: Velocity) {
        coroutineScope.launch {
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
        coroutineScope.launch {
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

    fun onScaleStart() {
        _isFirstScale = true
        _startScale = scale
    }

    fun onScale(event: PointerEvent, centroid: Offset, change: Float) {
        event.fConsume()

        val min = minScaleDrag
        val max = if (_startScale == maxScale) maxScaleDrag else maxScale
        val newScale = (scale * change).coerceIn(min, max)

        if (_isFirstScale && newScale > defaultScale) {
            _isFirstScale = false
            applyTransformOrigin(centroid)
        }

        scale = newScale
    }

    fun onScaleFinish() {
        coroutineScope.launch {
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
        with(boundsX) {
            if (isOverSize) {
                if (offsetX < minOffset) {
                    animateOffsetXTo(minOffset)
                } else if (offsetX > maxOffset) {
                    animateOffsetXTo(maxOffset)
                }
            } else {
                animateOffsetXTo(centerOffset)
                transformOrigin = transformOrigin.copy(pivotFractionX = 0.5f)
                offsetX = 0f
            }
        }
        with(boundsY) {
            if (isOverSize) {
                if (offsetY < minOffset) {
                    animateOffsetYTo(minOffset)
                } else if (offsetY > maxOffset) {
                    animateOffsetYTo(maxOffset)
                }
            } else {
                animateOffsetYTo(centerOffset)
                transformOrigin = transformOrigin.copy(pivotFractionY = 0.5f)
                offsetY = 0f
            }
        }
    }

    private suspend fun animateScaleTo(targetValue: Float) {
        if (targetValue <= defaultScale) {
            coroutineScope.launch { animateOffsetXTo(0f) }
            coroutineScope.launch { animateOffsetYTo(0f) }
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
    val boxSize: Int,
    val contentSize: Int,
    val scale: Float,
    val pivot: Float,
) {
    init {
        require(boxSize >= 0)
        require(contentSize >= 0)
        require(boxSize >= contentSize)
        require(scale > 0)
    }

    private val halfDeltaSize get() = (boxSize - contentSize) / 2f
    private val currentSize get() = contentSize * scale

    val changedSize get() = currentSize - contentSize
    val isOverSize get() = currentSize > boxSize

    val minOffset
        get() = if (isOverSize) {
            val pivotSize = changedSize * (1f - pivot)
            halfDeltaSize - pivotSize
        } else {
            0f
        }

    val maxOffset
        get() = if (isOverSize) {
            val pivotSize = changedSize * (pivot)
            pivotSize - halfDeltaSize
        } else {
            0f
        }

    val centerOffset
        get() = if (isOverSize) {
            val delta = (maxOffset - minOffset) / 2f
            minOffset + delta
        } else {
            changedSize * (pivot - 0.5f)
        }

    fun getViewportPivot(contentOffset: Float, centroid: Float): Float {
        val pivotSize = changedSize * (pivot)
        val startOffset = pivotSize - halfDeltaSize - contentOffset
        val centroidOffset = startOffset + centroid
        return centroidOffset / currentSize
    }
}

private enum class DragResult {
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

private fun LayoutCoordinates.offset(): Offset {
    return this.localToWindow(Offset.Zero)
}

internal inline fun logMsg(block: () -> String) {
    Log.i("compose-scale-box", block())
}