package com.example.cards.data.model;

/**
 * Модель для списка колод в главном меню.
 * Содержит id, title и вычисляемый процент выученных слов (0..100).
 */
public class Deck {
    public final long id;
    public final String title;
    private int percent = 0;

    public Deck(long id, String title) {
        this.id = id;
        this.title = title;
    }

    public int getPercent() {
        return percent;
    }

    public void setPercent(int percent) {
        if (percent < 0) percent = 0;
        if (percent > 100) percent = 100;
        this.percent = percent;
    }
}