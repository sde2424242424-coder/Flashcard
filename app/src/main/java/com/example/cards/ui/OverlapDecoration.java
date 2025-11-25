// ui/OverlapDecoration.java
package com.example.cards.ui;

import android.content.Context;
import android.graphics.Rect;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/**
 * OverlapDecoration
 *
 * Custom ItemDecoration that visually overlaps the second card in the list
 * above the first one, while keeping regular spacing for all other items.
 *
 * Intended use:
 * - Creates a stylized “stacked cards” effect for the first two items.
 * - The second item is lifted upward by a fixed amount.
 * - Items from index 2 and further receive a normal gap.
 *
 * Notes:
 * - overlapDp controls how high the second item is lifted.
 * - gapDp controls regular spacing for other cards.
 */
public class OverlapDecoration extends RecyclerView.ItemDecoration {

    private final int overlapPx; // vertical shift applied to the 2nd item
    private final int gapPx;     // normal spacing for items after the 2nd

    /**
     * @param ctx        context for converting dp to px
     * @param overlapDp  how much to move item #1 upward (in dp)
     * @param gapDp      spacing for items starting from index 2 (in dp)
     */
    public OverlapDecoration(Context ctx, float overlapDp, float gapDp) {
        overlapPx = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                overlapDp,
                ctx.getResources().getDisplayMetrics()
        );
        gapPx = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                gapDp,
                ctx.getResources().getDisplayMetrics()
        );
    }

    @Override
    public void getItemOffsets(
            @NonNull Rect out,
            @NonNull View v,
            @NonNull RecyclerView parent,
            @NonNull RecyclerView.State state
    ) {
        int pos = parent.getChildAdapterPosition(v);

        if (pos == RecyclerView.NO_POSITION) return;

        if (pos == 1) {
            // Lift the second card upward for the overlap effect.
            out.top = -overlapPx;
        } else if (pos > 1) {
            // Apply normal spacing for all following items.
            out.top = gapPx;
        } else {
            // pos == 0 → first item: no changes.
            out.top = 0;
        }
    }
}
