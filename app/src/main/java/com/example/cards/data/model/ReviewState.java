package com.example.cards.data.model;

import static androidx.room.ForeignKey.CASCADE;

import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "review_state",
        foreignKeys = @ForeignKey(
                entity = Card.class,
                parentColumns = "id",
                childColumns = "cardId",
                onDelete = ForeignKey.CASCADE
        )
)
public class ReviewState {
    @PrimaryKey(autoGenerate = true) public long id;
    @ColumnInfo(index = true) public long cardId; // создаст неуникальный индекс
    public int intervalDays;
    public float ease;
    public int step;
    public long dueAt;
    @Nullable public Integer lastGrade;
}

