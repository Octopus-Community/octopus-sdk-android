package com.octopuscommunity.sdk.ui.posts.create

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.node.LayoutAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ObserverModifierNode
import androidx.compose.ui.node.observeReads
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.relocation.bringIntoView
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** A block the member adds to the post composer from its action bar. */
internal enum class AddedBlock { Image, Poll }

/**
 * Which block the member just added, so the composer can scroll it into view.
 *
 * Only the member's own action arms it ([reveal]) — never the Bridge Share prefill or a poll
 * pre-seeded at launch, so opening the composer never scrolls. It stays armed while the member does
 * nothing else, because a picked image is decoded asynchronously: its preview is first laid out
 * without the picture, then grows, and must be revealed again at that point. Touching or dragging
 * the content, typing, editing the poll, removing the image or publishing ends it ([stop]), so a
 * later resize never pulls the member back. The armed block leaving the composer on its own ends it
 * too (a poll dropped because the community turns out to disable polls): there is nothing left to
 * reveal.
 */
@Stable
internal class AddedBlockRevealer {
    var target: AddedBlock? by mutableStateOf(null)
        private set

    /**
     * How many times [reveal] was called, so a block that is already laid out is revealed again on
     * each call, even when its layout does not change: a picked image that replaces the current one
     * keeps its node, and possibly its size.
     */
    var revealCount: Int by mutableIntStateOf(0)
        private set

    fun reveal(block: AddedBlock) {
        target = block
        revealCount++
    }

    fun stop() {
        target = null
    }
}

/** An [AddedBlockRevealer] that stops as soon as the member drags [scrollState]'s content. */
@Composable
internal fun rememberAddedBlockRevealer(scrollState: ScrollState): AddedBlockRevealer {
    val revealer = remember { AddedBlockRevealer() }
    LaunchedEffect(scrollState, revealer) {
        scrollState.interactionSource.interactions.collect { interaction ->
            if (interaction is DragInteraction.Start) revealer.stop()
        }
    }
    return revealer
}

/**
 * Ends [revealer]'s reveal as soon as the member touches this content: a tap in the text, a poll
 * answer or the image is the member moving on. Tapping the text opens the keyboard, which shrinks
 * the viewport, and a reveal still armed would scroll back to the block, pushing the caret out of
 * view. The touch is only observed, never consumed, so the content handles it as usual. The
 * buttons that add a block sit outside this content, so the keyboard that adding a poll opens on
 * its first answer still gets the poll revealed.
 */
internal fun Modifier.endsRevealOnTouch(revealer: AddedBlockRevealer): Modifier =
    pointerInput(revealer) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            revealer.stop()
        }
    }

/**
 * Scrolls [scrollState] by the smallest distance that shows this block while [revealer] targets
 * [block]: nothing when it is already fully visible, just enough when it fits in the viewport, its
 * top on the viewport's top when it is taller.
 *
 * While targeted, the block asks again each time something may have hidden it:
 * [AddedBlockRevealer.reveal] is called for it, which also covers a block already laid out (a
 * replaced image); it is laid out with a new size (a picked image grows once decoded); it moves
 * inside its parent (the image above a poll grows); or the viewport shrinks (the keyboard opening
 * for the poll's first answer, which changes the layout only). Nothing else asks: a recomposition
 * leaves the scroll alone, so a scroll that is not a drag (accessibility, focus traversal, a mouse
 * wheel) stays where it is until one of those happens. The block leaving the composition while
 * targeted ends the reveal.
 *
 * The requested rectangle is the block cut to the viewport's height, both read when the scroll
 * runs rather than when it is asked for, since the viewport may change in the same layout pass.
 * With the whole block, the default `BringIntoViewSpec` aligns whichever edge is closer, so a tall
 * block whose top is already above the viewport would get its bottom aligned — or no scroll at all
 * when it spans the viewport.
 */
internal fun Modifier.revealWhenAdded(
    block: AddedBlock,
    revealer: AddedBlockRevealer,
    scrollState: ScrollState,
): Modifier = this then RevealWhenAddedElement(block, revealer, scrollState)

// Equal across recompositions as long as its inputs are, so a recomposition neither updates the
// node nor remeasures the block. A lambda-based `onSizeChanged` would be rebuilt on each call,
// compared by identity, and re-run with an unchanged size.
private data class RevealWhenAddedElement(
    val block: AddedBlock,
    val revealer: AddedBlockRevealer,
    val scrollState: ScrollState,
) : ModifierNodeElement<RevealWhenAddedNode>() {
    override fun create() = RevealWhenAddedNode(block, revealer, scrollState)

    override fun update(node: RevealWhenAddedNode) {
        node.update(block, revealer, scrollState)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "revealWhenAdded"
        properties["block"] = block
        properties["revealer"] = revealer
        properties["scrollState"] = scrollState
    }
}

private class RevealWhenAddedNode(
    private var block: AddedBlock,
    private var revealer: AddedBlockRevealer,
    private var scrollState: ScrollState,
) : Modifier.Node(),
    LayoutAwareModifierNode,
    ObserverModifierNode {
    // It only listens to layout and to the revealer, so a new element has nothing to invalidate.
    override val shouldAutoInvalidate = false

    // What the node last saw, to react to changes only.
    private var armed = false
    private var revealCount = 0
    private var viewportHeight = 0
    private var size: IntSize? = null
    private var top: Float? = null

    private var request: Job? = null

    fun update(
        block: AddedBlock,
        revealer: AddedBlockRevealer,
        scrollState: ScrollState,
    ) {
        this.block = block
        this.revealer = revealer
        this.scrollState = scrollState
        observeRevealer()
    }

    override fun onAttach() {
        observeRevealer()
    }

    override fun onDetach() {
        // The node's coroutine scope is cancelled on detach, and a later attach starts afresh.
        request = null
        size = null
        top = null
        if (revealer.target == block) revealer.stop()
    }

    override fun onObservedReadsChanged() {
        val lastRevealCount = revealCount
        val lastViewportHeight = viewportHeight
        observeRevealer()
        when {
            !armed -> cancelRequest()
            revealCount != lastRevealCount -> requestReveal()
            viewportHeight < lastViewportHeight -> requestReveal()
        }
    }

    override fun onRemeasured(size: IntSize) {
        if (size == this.size) return
        this.size = size
        if (armed) requestReveal()
    }

    override fun onPlaced(coordinates: LayoutCoordinates) {
        // Relative to the scrolled content, so scrolling does not move it.
        val top = coordinates.positionInParent().y
        if (top == this.top) return
        this.top = top
        if (armed) requestReveal()
    }

    private fun observeRevealer() {
        observeReads {
            armed = revealer.target == block
            revealCount = revealer.revealCount
            viewportHeight = scrollState.viewportSize
        }
    }

    private fun requestReveal() {
        // Not laid out yet: its first measure asks. A request in flight reads the bounds again on
        // each frame of its scroll, so it already follows the change.
        if (size == null || request?.isActive == true) return
        request = coroutineScope.launch { bringIntoView { visibleBounds() } }
    }

    private fun cancelRequest() {
        request?.cancel()
        request = null
    }

    private fun visibleBounds(): Rect? {
        val size = size ?: return null
        val viewport = scrollState.viewportSize.takeIf { it > 0 } ?: size.height
        return Rect(0f, 0f, size.width.toFloat(), minOf(size.height, viewport).toFloat())
    }
}
