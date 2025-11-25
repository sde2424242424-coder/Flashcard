package com.example.cards.data.model;

/**
 * Deck
 *
 * Lightweight model representing a flashcard deck.
 * Used mainly for UI lists (main menu) and navigation between screens.
 *
 * Fields:
 * - id:    unique deck identifier (1..N), also defines the associated DB file name
 * - title: display title shown in the main menu and toolbar
 */
public class Deck {

    /** Unique deck identifier (matches DB file cards_deck_<id>.db). */
    public final long id;

    /** Human-readable deck title. */
    public final String title;

    public Deck(long id, String title) {
        this.id = id;
        this.title = title;
    }
}
