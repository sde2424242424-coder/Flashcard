package com.example.cards.data.model;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "cards",
        indices = {@Index(value = {"deckId", "front", "back"}, unique = true)}
)
public class Card {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public long deckId;

    @NonNull public String front = "";
    @NonNull public String back = "";

    // --- getters ---
    public long getId() { return id; }
    public long getDeckId() { return deckId; }
    @NonNull public String getFront() { return front; }
    @NonNull public String getBack()  { return back; }


    // getters/setters…
    public boolean isExcluded() { return excluded; }
    public void setExcluded(boolean v) { this.excluded = v; }
    @ColumnInfo(defaultValue = "0")
    public long createdAt;     // ← чтобы Expected совпал с DEFAULT 0

    @ColumnInfo(defaultValue = "0")
    public boolean learned;    // ← тоже DEFAULT 0 (0/1)

    @ColumnInfo(defaultValue = "0")
    public boolean excluded;   // уже было





    // (опционально) setters, если нужны Room-геттеры/сеттеры вместо public полей
    // public void setId(long id) { this.id = id; }
    // public void setDeckId(long deckId) { this.deckId = deckId; }
    // public void setFront(@NonNull String front) { this.front = front; }
    // public void setBack(@NonNull String back) { this.back = back; }
}
