package com.example.cards.data.model;

public class WordWithSchedule {
    public long    id;          // AS id
    public String  front;       // AS front
    public String  back;        // AS back
    public Double  ease;        // AS ease (nullable)
    public Integer lastGrade;   // AS lastGrade (nullable)
    public Integer intervalDays;// AS intervalDays (nullable)
    public Long    dueAt;       // AS dueAt (nullable)
    public int     totalReviews;// AS totalReviews (COUNT(...))
}
