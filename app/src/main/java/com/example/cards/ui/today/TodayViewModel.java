package com.example.cards.ui.today;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.cards.data.model.Card;
import com.example.cards.repo.CardRepository;

import java.util.List;

/**
 * TodayViewModel
 *
 * ViewModel responsible for providing "due today" cards using the repository
 * and exposing them via LiveData for UI components.
 *
 * Responsibilities:
 * - Request the list of due cards from the repository.
 * - Expose immutable LiveData to the UI.
 * - Handle review results and reschedule cards.
 * - Refresh the list when an answer is submitted.
 *
 * Notes:
 * - loadDue(limit) retrieves cards that are scheduled for review at the current time.
 * - onAnswer(cardId, grade) commits review result and then refreshes the list.
 */
public class TodayViewModel extends ViewModel {

    private final CardRepository repo;

    // Holds list of due cards observed by UI.
    private final MutableLiveData<List<Card>> dueCards = new MutableLiveData<>();

    /**
     * @param repo repository used for fetching and updating review data
     */
    public TodayViewModel(CardRepository repo) {
        this.repo = repo;
    }

    /**
     * Returns LiveData of due cards.
     */
    public LiveData<List<Card>> getDueCards() {
        return dueCards;
    }

    /**
     * Loads due cards up to the specified limit.
     *
     * @param limit maximum number of cards to load
     */
    public void loadDue(int limit) {
        long now = System.currentTimeMillis();
        List<Card> cards = repo.getDueCards(now, limit);
        dueCards.postValue(cards);
    }

    /**
     * Called when the user answers a card with a given grade.
     * Updates its next review time and reloads the due list.
     *
     * @param cardId ID of the reviewed card
     * @param grade  difficulty grade selected by the user
     */
    public void onAnswer(long cardId, int grade) {
        long now = System.currentTimeMillis();
        repo.reviewAndSchedule(cardId, grade, now);
        loadDue(20); // refresh list after reviewing
    }
}
