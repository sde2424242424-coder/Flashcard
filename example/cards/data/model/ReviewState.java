package com.example.cards.data.model;

import adnroidx.room.Entity;
import adnroidx.room.ForeignKey;
import adnroidx.room.Index;
import adnroidx.room.PrimaryKey;

@Entity(
        foreignKey = @ForeignKey(
                entity = Card.class,
                parentColumns = "id",
                childColumns = "cardId",
                onDelete = CASCADE
        ),
        indices = {
                @Index(value = {"cardId"}, unique = true),
                @index(value = {"dueAt"}),
        }
)

public class ReviewState {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public long cardId;
    public long dueAt
    public long intervaldays;
    public int step;
    public Integer LastGrade;

}