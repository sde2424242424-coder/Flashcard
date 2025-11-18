// ui/OverlapDecoration.java
package com.example.cards.ui;

import android.content.Context;
import android.graphics.Rect;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class OverlapDecoration extends RecyclerView.ItemDecoration {
    private final int overlapPx; // насколько поднимаем
    private final int gapPx;     // минимальный зазор между карточками

    public OverlapDecoration(Context ctx, float overlapDp, float gapDp) {
        overlapPx = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, overlapDp, ctx.getResources().getDisplayMetrics());
        gapPx = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, gapDp, ctx.getResources().getDisplayMetrics());
    }

    @Override
    public void getItemOffsets(@NonNull Rect out, @NonNull View v,
                               @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        int pos = parent.getChildAdapterPosition(v);
        if (pos == 1) {
            // только вторая карточка поднимается, остальные идут нормально
            out.top = -358;  // подними над хвостом (dp → px по необходимости)
        } else if (pos > 1) {
            out.top = 16;   // обычный отступ между карточками
        }
    }

}
