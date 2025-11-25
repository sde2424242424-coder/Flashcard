package com.example.cards.data.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * LearnedState
 *
 * Simple flag table used to mark whether a card has been manually
 * marked as “learned” by the user (independent of SM-2 scheduling).
 *
 * Purpose:
 * - Allow user-controlled marking of cards (e.g., marking known words).
 * - Does not affect review scheduling directly.
 * - Integrated into UI lists through projection models (e.g., WordWithStats).
 *
 * Fields:
 * - cardId:  primary key, matches Card.id.
 * - learned: true if the user marked the card as learned.
 */
@Entity(tableName = "learned_state")
public class LearnedState {

    /** Primary key equal to the card ID. */
    @PrimaryKey
    public long cardId;

    /** User-defined learned/unlearned flag. */
    public boolean learned;
}
