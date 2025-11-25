package com.example.cards.data.model;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Card
 *
 * Core entity representing a single flashcard in a specific deck.
 * Stored inside deck-specific databases (cards_deck_<id>.db).
 *
 * Fields:
 * - id:         auto-generated primary key
 * - deckId:     foreign grouping identifier (logical deck number 1..N)
 * - front:      word / question side
 * - back:       translation / answer side
 *
 * Additional flags:
 * - createdAt:  timestamp (optional, defaults to 0)
 * - learned:    user-defined learned marker
 * - excluded:   user-defined skip flag (excluded from reviews)
 *
 * Unique index:
 * - (deckId, front, back) to prevent duplicate cards inside the same deck.
 */
@Entity(
        tableName = "cards",
        indices = {@Index(value = {"deckId", "front", "back"}, unique = true)}
)
public class Card {

    /** Auto-generated primary key. */
    @PrimaryKey(autoGenerate = true)
    public long id;

    /** Logical deck identifier. */
    public long deckId;

    /** Card front side (word). */
    @NonNull public String front = "";

    /** Card back side (translation). */
    @NonNull public String back = "";

    /** Optional timestamp (used for sorting or diagnostics). */
    @ColumnInfo(defaultValue = "0")
    public long createdAt;

    /** User-controlled “learned” flag (default 0). */
    @ColumnInfo(defaultValue = "0")
    public boolean learned;

    /** Exclusion flag (skips card in study mode). */
    @ColumnInfo(defaultValue = "0")
    public boolean excluded;

    // -------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------
    public long getId() { return id; }
    public long getDeckId() { return deckId; }
    @NonNull public String getFront() { return front; }
    @NonNull public String getBack()  { return back; }

    // -------------------------------------------------------------
    // Optional setters (if you prefer encapsulation instead of public fields)
    // -------------------------------------------------------------
    public boolean isExcluded() { return excluded; }
    public void setExcluded(boolean v) { this.excluded = v; }
}
