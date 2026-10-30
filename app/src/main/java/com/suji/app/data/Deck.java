package com.suji.app.data;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/** 背诵本（一个主题 / 一本书 / 一批资料）。 */
@Entity(tableName = "decks")
public class Deck {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public String name = "";

    /** 描述，可为空。 */
    public String description = "";

    /** 主题色（ARGB），0 表示使用默认色。 */
    public int color = 0;

    public long createdAt = 0L;

    public boolean archived = false;

    public Deck() {
    }

    @Ignore
    public Deck(String name) {
        this.name = name;
        this.createdAt = System.currentTimeMillis();
    }
}
