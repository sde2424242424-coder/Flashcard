package com.example.cards.data.model;


import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import static androidx.room.ForeignKey.CASCADE;

@Entity(
        tableName = "ReviewLog",
        foreignKeys = {
                @ForeignKey(
                        entity = Card.class,
                        parentColumns = {"id"},
                        childColumns = {"cardId"},
                        onDelete = CASCADE
                )
        },
        indices = {
                @Index("cardId"),
                @Index("reviewedAt")
        }
)
public class ReviewLog {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public long cardId;
    public long reviewedAt;   // время ревью
    public int grade;

    public int resultIntervalDays;
    public int resultStep;
    public double resultEase;
    public long ts;
}
