package com.example.cards.data.model;

import static androidx.room.ForeignKey.CASCADE;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "ReviewState",
        foreignKeys = {
                @ForeignKey(
                        entity = Card.class,
                        parentColumns = {"id"},
                        childColumns = {"cardId"},
                        onDelete = CASCADE
                )
        },
        indices = {
                @Index(value = {"cardId"}, unique = true), // по одной строке состояния на карточку
                @Index(value = {"dueAt"})
        }
)

public class ReviewState {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public long cardId;
    public long dueAt;
    public int step;
    public Integer lastGrade;
    public int intervalDays;
    public double ease;


}