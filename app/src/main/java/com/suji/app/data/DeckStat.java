package com.suji.app.data;

import androidx.room.Embedded;

/** 背诵本 + 统计数字，供首页列表使用。 */
public class DeckStat {

    @Embedded
    public Deck deck;

    public int total;

    public int dueCount;

    public int newCount;
}
