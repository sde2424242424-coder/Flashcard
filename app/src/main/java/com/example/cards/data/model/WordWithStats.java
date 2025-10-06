package com.example.cards.data.model;

public class WordWithStats {
    public long id;
    public String front;
    public String back;
    public Double ease;        // может быть null, если слова ещё не повторялись
    public Integer lastGrade;  // 0..5 или null
    public Integer intervalDays;
    public Long dueAt;
    public int totalReviews;   // COUNT из лога
}