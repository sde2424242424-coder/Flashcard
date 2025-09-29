package com.example.cards.domain;

import com.example.cards.data.model.Card;

import java.util.Collections;
import java.util.List;

public class ReviewUseCase {
    private final ReviewRepository repo;
    public ReviewUseCase(ReviewRepository repo) {
        this.repo = repo;
    }
    public List<Card> loadDueBatch(int limit) {
        long now = System.currentTimeMillis();
        List<Card> due = repo.getDueCards(now, limit);
        return due != null ? due : Collections.emptyList();
    }
    public void onAnswer(long cardId, int grade) {
        long now = System.currentTimeMillis();
        repo.reviewAndSchedule(cardId, grade, now);
    }
}