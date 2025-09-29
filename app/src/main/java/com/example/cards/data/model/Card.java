package com.example.cards.data.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(indices = {})
public class Card {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    public String front = "";

    @NonNull
    public String back = "";

    public long createdAt;
}
