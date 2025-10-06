package com.example.cards.data.model;

import static androidx.room.ForeignKey.CASCADE;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "review_state",
        indices = {@Index(value = "cardId", unique = true)})
public class ReviewState {
    @PrimaryKey(autoGenerate = true) public long id;

    @ColumnInfo public long cardId;       // FK на Card.id
    @ColumnInfo public int  intervalDays; // 0, 1, 3, 7, ...
    @ColumnInfo public double ease;       // 2.5 по умолчанию (double!)
    @ColumnInfo public int  step;         // шаг внутри “learning”
    @ColumnInfo public long dueAt;        // millis when due
    @ColumnInfo
    public Integer lastGrade; // 0..5, можно null
}
