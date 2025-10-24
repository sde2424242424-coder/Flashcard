package com.example.cards.data.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "learned_state")
public class LearnedState {
    @PrimaryKey
    public long cardId;      // = cards.id
    public boolean learned;  // 0/1
}
