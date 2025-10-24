package com.example.cards.data.model;

import androidx.room.ColumnInfo;

public class WordWithStats {
    @ColumnInfo(name = "cardId")       public long    cardId;
    @ColumnInfo(name = "front")        public String  front;
    @ColumnInfo(name = "back")         public String  back;
    @ColumnInfo(name = "learned")      public boolean learned;

    @ColumnInfo(name = "ease")         public Double  ease;         // может быть null
    @ColumnInfo(name = "lastGrade")    public Integer lastGrade;    // может быть null
    @ColumnInfo(name = "totalReviews") public int     totalReviews;
}
