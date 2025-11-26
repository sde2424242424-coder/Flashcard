package com.example.cards.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cards.R;

/**
 * FoxDecoration
 *
 * ItemDecoration that draws a fox attached to the first deck card:
 * - The body is drawn under the cards (onDraw).
 * - The head is drawn above the cards (onDrawOver).
 *
 * Behavior:
 * - The fox is visually “anchored” to the first card (adapter position 0).
 * - While the first item is visible, its actual position is used as an anchor.
 * - After the first item is recycled and leaves the screen, the fox continues
 *   to move smoothly upward based on scroll offset, and disappears only when
 *   fully outside of the screen.
 */
public class FoxDecoration extends RecyclerView.ItemDecoration {

    private final Drawable headDrawable;
    private final Drawable bodyDrawable;

    // Scale factors for head and body sprites.
    private final float headScale;
    private final float bodyScale;

    // Offsets for positioning head/body relative to the anchor rect.
    private final int headOffsetX;
    private final int headOffsetY;
    private final int bodyOffsetX;
    private final int bodyOffsetY;

    /**
     * Anchor rectangle – last known position of the first card
     * (adapter position 0) in RecyclerView coordinates.
     * When the view is recycled, we move this rect manually on scroll.
     */
    private Rect anchorRect = null;

    // Keeps last scroll offset to compute delta scroll between frames.
    private int lastScrollOffset = 0;

    /**
     * Creates a new fox decoration that draws fox head & body around the first item.
     *
     * @param context used to resolve drawables and density
     */
    public FoxDecoration(Context context) {
        headDrawable = ContextCompat.getDrawable(context, R.drawable.fox_peek);
        bodyDrawable = ContextCompat.getDrawable(context, R.drawable.fox_body);

        float density = context.getResources().getDisplayMetrics().density;

        // Simple scaling; adjust if you need different visual size.
        headScale = 0.44f;
        bodyScale = 0.44f;

        // Position tweaks for head and body relative to the card.
        headOffsetX = (int) (60 * density);
        headOffsetY = (int) (30 * density);

        bodyOffsetX = (int) (60 * density);
        bodyOffsetY = (int) (-60 * density);
    }

    /**
     * Draws the fox body beneath card views.
     */
    @Override
    public void onDraw(@NonNull Canvas c,
                       @NonNull RecyclerView parent,
                       @NonNull RecyclerView.State state) {

        // Update anchor position for the first card before drawing.
        updateAnchor(parent);

        if (bodyDrawable == null || anchorRect == null) return;

        int bodyWidth  = (int) (bodyDrawable.getIntrinsicWidth()  * bodyScale);
        int bodyHeight = (int) (bodyDrawable.getIntrinsicHeight() * bodyScale);

        int centerX = anchorRect.centerX();
        int left   = centerX - bodyWidth / 2 + bodyOffsetX;
        int top    = anchorRect.bottom + bodyOffsetY;
        int right  = left + bodyWidth;
        int bottom = top + bodyHeight;

        // If the entire body is already above the screen, do not draw.
        if (bottom <= 0) return;

        bodyDrawable.setBounds(left, top, right, bottom);
        bodyDrawable.draw(c);
    }

    /**
     * Draws the fox head above card views.
     */
    @Override
    public void onDrawOver(@NonNull Canvas c,
                           @NonNull RecyclerView parent,
                           @NonNull RecyclerView.State state) {

        if (headDrawable == null || anchorRect == null) return;

        int headWidth  = (int) (headDrawable.getIntrinsicWidth()  * headScale);
        int headHeight = (int) (headDrawable.getIntrinsicHeight() * headScale);

        int centerX = anchorRect.centerX();
        int left    = centerX - headWidth / 2 + headOffsetX;

        // Attach head to the top edge of the anchor rect and offset upward.
        int bottom  = anchorRect.top + headOffsetY;
        int top     = bottom - headHeight;
        int right   = left + headWidth;

        // If the whole head is already above the screen, skip drawing.
        if (bottom <= 0) return;

        headDrawable.setBounds(left, top, right, bottom);
        headDrawable.draw(c);
    }

    /**
     * Updates anchorRect:
     * - If the first card view is currently attached and visible,
     *   anchorRect is set to its current bounds.
     * - If the first card is not attached anymore, we move anchorRect
     *   by the scroll delta so the fox continues to “fly away” upwards.
     */
    private void updateAnchor(RecyclerView parent) {
        int scrollOffset = parent.computeVerticalScrollOffset();

        View firstView = findFirstDeckView(parent);
        if (firstView != null) {
            // First item is visible – use its actual bounds as the anchor.
            anchorRect = getItemRect(firstView);
        } else if (anchorRect != null) {
            // First item is no longer visible – move anchor by scroll distance.
            int dy = scrollOffset - lastScrollOffset;
            anchorRect.offset(0, -dy);
        }

        lastScrollOffset = scrollOffset;
    }

    /**
     * Finds the view for the first deck item (adapterPosition == 0)
     * among currently attached child views, if any.
     */
    private View findFirstDeckView(RecyclerView parent) {
        int childCount = parent.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = parent.getChildAt(i);
            int pos = parent.getChildAdapterPosition(child);
            if (pos == 0) {
                return child;
            }
        }
        return null;
    }

    /**
     * Returns the bounding rectangle of a child view in RecyclerView coordinates.
     */
    private Rect getItemRect(View child) {
        return new Rect(
                child.getLeft(),
                child.getTop(),
                child.getRight(),
                child.getBottom()
        );
    }
}
