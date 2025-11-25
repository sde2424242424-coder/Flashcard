package com.example.cards.domain;

import com.example.cards.data.model.Card;
import java.util.Collections;
import java.util.List;

/**
 * ReviewUseCase
 *
 * Domain-level use case for reviewing cards within a specific deck.
 * Encapsulates business logic for:
 * - Fetching due cards for the current deck.
 * - Applying review results (grading).
 *
 * Intended to be injected into UI controllers (activities/fragments/viewmodels)
 * to keep domain logic separate from storage and UI.
 */
public class ReviewUseCase {

    // Repository providing deck-specific review operations.
    private final ReviewRepository repo;

    // ID of the deck this use case operates on.
    private final long deckId;

    /**
     * Constructs a ReviewUseCase bound to a specific deck.
     *
     * @param repo   deck-aware ReviewRepository
     * @param deckId ID of the deck whose cards this use case handles
     */
    public ReviewUseCase(ReviewRepository repo, long deckId) {
        this.repo = repo;
        this.deckId = deckId;
    }

    /**
     * Loads a batch of due cards for the current deck.
     *
     * @param limit maximum number of cards to load
     * @return list of due {@link Card}, never null
     */
    public List<Card> loadDueBatch(int limit) {
        long now = System.currentTimeMillis();
        List<Card> due = repo.getDueCards(deckId, now, limit);
        return due != null ? due : Collections.emptyList();
    }

    /**
     * Applies user's answer and updates scheduling for the given card.
     *
     * @param cardId ID of the reviewed card
     * @param grade  rating given by the user (difficulty/quality)
     */
    public void onAnswer(long cardId, int grade) {
        long now = System.currentTimeMillis();
        repo.reviewAndSchedule(cardId, grade, now);
    }
}
