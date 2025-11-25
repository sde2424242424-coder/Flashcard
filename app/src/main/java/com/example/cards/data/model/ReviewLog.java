package com.example.cards.data.model;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * ReviewLog
 *
 * Entity representing a single review event recorded for a card.
 * Each time the user grades a card, one ReviewLog entry is created.
 *
 * Purpose:
 * - Keep a detailed history of all review attempts.
 * - Provide data for analytics (graphs, statistics, progress tracking).
 * - Store resulting SM-2 parameters at the moment of review.
 *
 * Fields:
 * - id:                  auto-generated primary key.
 * - cardId:              ID of the card being reviewed.
 * - reviewedAt:          timestamp of review (milliseconds).
 * - grade:               grade given by user (0..5).
 * - resultIntervalDays:  interval in days after applying SM-2.
 * - resultEase:          ease factor after applying SM-2.
 * - resultStep:          learning step (0..2 learning, ≥3 mature).
 *
 * Note:
 * - "ts" appears redundant because reviewedAt already stores the timestamp.
 *   It is included for compatibility with older code and must be non-null.
 */
@Entity(
        tableName = "review_log",
        indices = { @Index("cardId") }
)
public class ReviewLog {

    @PrimaryKey(autoGenerate = true)
    public long id;

    /** ID of the reviewed card. */
    @ColumnInfo
    public long cardId;

    /** Timestamp when the card was reviewed (ms since epoch). */
    @ColumnInfo
    public long reviewedAt;

    /** User's grade 0..5. */
    @ColumnInfo
    public int grade;

    /** Resulting SM-2 interval in days after this review. */
    @ColumnInfo
    public int resultIntervalDays;

    /** Resulting ease factor after this review. */
    @ColumnInfo
    public double resultEase;

    /** Resulting learning/review step after this review. */
    @ColumnInfo
    public int resultStep;

    /**
     * Additional timestamp field.
     * Historically used; must be non-null.
     */
    @NonNull
    public long ts;
}
