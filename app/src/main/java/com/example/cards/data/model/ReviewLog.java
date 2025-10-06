package com.example.cards.data.model;


import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import static androidx.room.ForeignKey.CASCADE;

/*@Entity(
        tableName = "review_log",
        foreignKeys = @ForeignKey(
                entity = Card.class,
                parentColumns = {"id"},
                childColumns = {"cardId"},
                onDelete = CASCADE
        ),
        indices = {
                @Index("cardId"),
                @Index("reviewedAt")
        }
)*/
@Entity(tableName = "review_log",
        indices = {@Index("cardId")})
public class ReviewLog {
    @PrimaryKey(autoGenerate = true) public long id;

    @ColumnInfo public long cardId;
    @ColumnInfo public long reviewedAt;          // millis
    @ColumnInfo public int  grade;               // 0..5
    @ColumnInfo public int  resultIntervalDays;
    @ColumnInfo public double resultEase;
    @ColumnInfo
    public int  resultStep;
    @NonNull
    public long ts;
}

