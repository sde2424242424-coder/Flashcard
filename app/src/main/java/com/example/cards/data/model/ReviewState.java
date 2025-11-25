package com.example.cards.data.model;

import static androidx.room.ForeignKey.CASCADE;

import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * ReviewState
 *
 * Room entity representing the current spaced-repetition state
 * of a single card. Each card has at most one ReviewState row.
 *
 * This model stores:
 * - intervalDays:   current review interval in days (0 for learning)
 * - ease:           SM-2 ease factor (usually 1.3–3.0)
 * - step:           learning step (0–2 for learning, ≥3 for mature cards)
 * - dueAt:          timestamp when this card becomes due
 * - lastGrade:      last review grade (nullable if not reviewed yet)
 *
 * The table enforces a foreign key relation with Card(id).
 */
@Entity(
        tableName = "review_state",
        foreignKeys = @ForeignKey(
                entity = Card.class,
                parentColumns = "id",
                childColumns = "cardId",
                onDelete = CASCADE
        ),
        indices = {
                @Index(value = "cardId") // non-unique index for fast lookup by cardId
        }
)
public class ReviewState {

    @PrimaryKey(autoGenerate = true)
    public long id;

    /** Foreign key referencing Card(id). */
    @ColumnInfo(index = true)
    public long cardId;

    /** Current interval length in days (0 = learning). */
    public int intervalDays;

    /** SM-2 ease factor. */
    public float ease;

    /** Learning step (0–2) or ≥3 for interval mode. */
    public int step;

    /** Timestamp when the card becomes due for review. */
    public long dueAt;

    /** Last user grade for this card (may be null if never reviewed). */
    @Nullable
    public Integer lastGrade;
}
