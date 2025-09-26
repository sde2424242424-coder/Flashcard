package com.example.cards.ui.today;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.cards.data.model.Card;
import com.example.cards.repo.CardRepository;

import java.util.List;

public class TodayViewModel extends ViewModel {
    private final CardRepository repo;
    private final MutableLiveData<List<Card>> dueCards = new MutableLiveData<>();

    public TodayViewModel(CardRepository repo) {
        this.repo = repo;
    }

    public LiveData<List<Card>> getDueCards() { return dueCards; }

    public void loadDue(int limit) {
        long now = System.currentTimeMillis();
        List<Card> cards = repo.getDueCards(now, limit);
        dueCards.postValue(cards);
    }

    public void onAnswer(long cardId, int grade) {
        long now = System.currentTimeMillis();
        repo.reviewAndSchedule(cardId, grade, now);
        loadDue(20); // перегрузить список
    }
}
