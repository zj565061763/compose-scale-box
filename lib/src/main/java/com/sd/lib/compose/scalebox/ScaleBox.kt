package com.sd.lib.compose.scalebox

import android.util.Log
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.onSizeChanged
import com.sd.lib.compose.gesture.fPointer

@Composable
fun ScaleBox(
    modifier: Modifier = Modifier,
    state: ScaleBoxState = rememberScaleBoxState(),
    debug: Boolean = false,
    onTap: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    var hasDrag by remember { mutableStateOf(false) }
    var hasScale by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .onSizeChanged {
                state.boxSize = it
            }
            .let { m ->
                if (state.isReady) {
                    m
                        .fPointer(
                            pass = PointerEventPass.Initial,
                            onStart = {
                                state.cancelAnimator()
                            },
                        )
                        .handleDrag(
                            state = state,
                            debug = debug,
                        )
                        .handleScale(
                            state = state,
                            debug = debug,
                        )
                        .handleTap(
                            state = state,
                            debug = debug,
                            onTap = onTap,
                        )
                } else {
                    m
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        ContentBox(
            state = state,
            content = content,
        )
    }
}

private fun Modifier.handleDrag(
    state: ScaleBoxState,
    debug: Boolean,
): Modifier = composed {
    var hasDrag by remember { mutableStateOf(false) }
    this.fPointer(
        onStart = {
            this.calculatePan = true
            hasDrag = false
        },
        onCalculate = {
            if (maxPointerCount == 1) {
                currentEvent.changes
                    .firstOrNull { it.positionChanged() }
                    ?.let { input ->
                        val dragResult = state.handleDrag(this.pan)
                        logMsg(debug) { "pan drag $dragResult" }
                        when (dragResult) {
                            DragResult.Changed -> {
                                input.consume()
                                hasDrag = true
                            }

                            else -> {}
                        }
                    }
            } else if (maxPointerCount > 1) {
                cancelPointer()
                hasDrag = false
            }
        },
        onMove = { input ->
            if (hasDrag) {
                velocityAdd(input)
            }
        },
        onUp = { input ->
            if (hasDrag && !input.isConsumed && maxPointerCount == 1) {
                velocityGet(input.id)?.let { velocity ->
                    logMsg(debug) { "pan onUp" }
                    state.handleDragFling(velocity)
                }
            }
        },
    )
}

private fun Modifier.handleScale(
    state: ScaleBoxState,
    debug: Boolean,
): Modifier = composed {
    var hasScale by remember { mutableStateOf(false) }
    this.fPointer(
        onStart = {
            this.calculateZoom = true
            hasScale = false
        },
        onCalculate = {
            if (pointerCount == 2 && currentEvent.changes.any { it.positionChanged() }) {
                if (!hasScale) {
                    hasScale = true
                    logMsg(debug) { "zoom onScaleStart" }
                    state.onScaleStart()
                }

                logMsg(debug) { "zoom onScale" }
                state.onScale(
                    event = this.currentEvent,
                    centroid = this.centroid,
                    change = this.zoom,
                )
            }
        },
        onUp = {
            if (pointerCount == 2) {
                if (hasScale) {
                    logMsg(debug) { "zoom onScaleFinish" }
                    hasScale = false
                    state.onScaleFinish()
                }
            }
        },
    )
}

private fun Modifier.handleTap(
    state: ScaleBoxState,
    debug: Boolean,
    onTap: (() -> Unit)? = null,
): Modifier = composed {
    val onTapUpdated by rememberUpdatedState(onTap)
    this.pointerInput(Unit) {
        detectTapGestures(
            onDoubleTap = {
                logMsg(debug) { "onDoubleTap" }
                state.handleDoubleClick()
            },
            onTap = {
                logMsg(debug) { "onTap" }
                onTapUpdated?.invoke()
            },
        )
    }
}

@Composable
private fun ContentBox(
    state: ScaleBoxState,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .onSizeChanged {
                state.contentSize = it
            }
            .graphicsLayer(
                scaleX = state.scale,
                scaleY = state.scale,
                translationX = state.offsetX,
                translationY = state.offsetY,
                transformOrigin = state.transformOrigin,
            ),
    ) {
        content()
    }
}

internal inline fun logMsg(
    debug: Boolean,
    block: () -> String,
) {
    if (debug) {
        Log.i("ScaleBox", block())
    }
}