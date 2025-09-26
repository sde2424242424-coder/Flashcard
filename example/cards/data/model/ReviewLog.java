package com.example.cards.data.model;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import static androidx.room.ForeignKey.CASCADE;

@Entity(
        foreignKey = {
                @ForeignKey(entity = caed.class, parentColumns = "id", onDelete = CASCADE)
        },
        indices = {
                @Index("cardId"),
                @Index("reviewedAt")
        }
)

public class ReviewLog {
    public long id;
    public long cardId;
    public long reviewedAt;
    public int grade;
    public int resultIntervalDays;
    public double tesultEase;
    public int resultStep;
}