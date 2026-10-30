package com.suji.app.data;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** 一张背诵卡片：正面（提示 / 问题）+ 背面（答案 / 解释）。 */
@Entity(tableName = "cards", indices = {@Index("deckId"), @Index("dueAt")})
public class Card {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public long deckId;

    /** 正面。 */
    public String front = "";

    /** 背面。 */
    public String back = "";

    /** 备注 / 例句 / 口诀，可为空。 */
    public String note = "";

    /** 标签，逗号分隔，可为空。 */
    public String tags = "";

    /** 当前处于艾宾浩斯曲线的第几阶段，0 = 刚学。等于阶段总数时视为已掌握。 */
    public int stage = 0;

    /** 下次复习时间（毫秒时间戳）。 */
    public long dueAt = 0L;

    public long createdAt = 0L;

    public long lastReviewAt = 0L;

    /** 第一次复习时间，用于统计"今天新学了几张"。 */
    public long firstReviewAt = 0L;

    /** 累计复习次数。 */
    public int reviews = 0;

    /** 遗忘次数。 */
    public int lapses = 0;

    public boolean archived = false;

    public Card() {
    }

    @Ignore
    public Card(long deckId, String front, String back) {
        this.deckId = deckId;
        this.front = front == null ? "" : front;
        this.back = back == null ? "" : back;
        this.createdAt = System.currentTimeMillis();
        this.dueAt = this.createdAt;
    }
}
