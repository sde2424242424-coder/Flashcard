package com.example.cards.data.model;

/**
 * WordWithSchedule
 *
 * Projection model combining card data and review scheduling metadata.
 * Typically used for screens showing upcoming reviews or detailed statistics.
 *
 * Fields:
 * - id:           card ID
 * - front:        card front text
 * - back:         card back text
 *
 * Review properties (nullable if the card has never been reviewed):
 * - ease:         SM-2 ease factor
 * - lastGrade:    last review grade (0–5)
 * - intervalDays: review interval in days (0 if in learning phase)
 * - dueAt:        timestamp when the card is next due (ms since epoch)
 *
 * Aggregated statistics:
 * - totalReviews: number of recorded review events for this card
 */
public class WordWithSchedule {

    /** Unique card identifier (AS id). */
    public long id;

    /** Front side of the card (AS front). */
    public String front;

    /** Back side of the card (AS back). */
    public String back;

    /** SM-2 ease factor; nullable if no review state exists. */
    public Double ease;

    /** Last given grade (0–5); nullable if never reviewed. */
    public Integer lastGrade;

    /** Interval in days between reviews; nullable for new cards. */
    public Integer intervalDays;

    /** Timestamp when the card becomes due (nullable). */
    public Long dueAt;

    /** Total number of times the card has been reviewed. */
    public int totalReviews;
}
