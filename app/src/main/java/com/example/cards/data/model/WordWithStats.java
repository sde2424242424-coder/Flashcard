package com.example.cards.data.model;

import androidx.room.ColumnInfo;

/**
 * WordWithStats
 *
 * Projection model used for list views and search results.
 * Represents a card combined with its review statistics.
 *
 * Fields:
 * - cardId:       unique identifier of the card
 * - front:        card front text
 * - back:         card back text
 * - learned:      true if user marked the card as learned
 *
 * Review statistics (may be null in DB if the card was never reviewed):
 * - ease:         SM-2 ease factor
 * - lastGrade:    last review grade (0–5)
 * - totalReviews: how many times the card has been reviewed
 */
public class WordWithStats {

    @ColumnInfo(name = "cardId")
    public long cardId;

    @ColumnInfo(name = "front")
    public String front;

    @ColumnInfo(name = "back")
    public String back;

    @ColumnInfo(name = "learned")
    public boolean learned;

    @ColumnInfo(name = "ease")
    public Double ease; // nullable → card may have no recorded SM-2 state yet

    @ColumnInfo(name = "lastGrade")
    public Integer lastGrade; // nullable for same reason

    @ColumnInfo(name = "totalReviews")
    public int totalReviews;
}
