package com.suji.app.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface CardDao {

    @Insert
    long insert(Card card);

    @Insert
    List<Long> insertAll(List<Card> cards);

    @Update
    void update(Card card);

    @Delete
    void delete(Card card);

    @Query("DELETE FROM cards WHERE deckId = :deckId")
    void deleteByDeck(long deckId);

    @Query("SELECT * FROM cards WHERE deckId = :deckId AND archived = 0 ORDER BY createdAt ASC, id ASC")
    LiveData<List<Card>> observeByDeck(long deckId);

    @Query("SELECT * FROM cards WHERE deckId = :deckId AND archived = 0 ORDER BY createdAt ASC, id ASC")
    List<Card> byDeck(long deckId);

    @Query("SELECT * FROM cards WHERE id = :id")
    Card byId(long id);

    /** 到期的旧卡片（已经学过、且到期时间已过）。deckId < 0 表示全部背诵本。 */
    @Query("SELECT * FROM cards WHERE archived = 0 AND reviews > 0 AND dueAt <= :now "
            + "AND (:deckId < 0 OR deckId = :deckId) ORDER BY dueAt ASC LIMIT :limit")
    List<Card> dueCards(long now, long deckId, int limit);

    /** 还没学过的新卡片。 */
    @Query("SELECT * FROM cards WHERE archived = 0 AND reviews = 0 "
            + "AND (:deckId < 0 OR deckId = :deckId) ORDER BY createdAt ASC, id ASC LIMIT :limit")
    List<Card> newCards(long deckId, int limit);

    @Query("SELECT COUNT(*) FROM cards WHERE archived = 0")
    int countAll();

    @Query("SELECT COUNT(*) FROM cards WHERE archived = 0 AND reviews = 0")
    int countNew();

    @Query("SELECT COUNT(*) FROM cards WHERE archived = 0 AND reviews = 0 AND (:deckId < 0 OR deckId = :deckId)")
    int countNewIn(long deckId);

    @Query("SELECT COUNT(*) FROM cards WHERE archived = 0 AND reviews > 0 "
            + "AND dueAt <= CAST(strftime('%s','now') AS INTEGER) * 1000 AND (:deckId < 0 OR deckId = :deckId)")
    int countDue(long deckId);

    @Query("SELECT COUNT(*) FROM cards WHERE archived = 0 AND stage >= :maxStage")
    int countMastered(int maxStage);

    @Query("SELECT COUNT(*) FROM cards WHERE archived = 0 AND reviews > 0 AND stage < :maxStage")
    int countLearning(int maxStage);

    /** 今天新接触的卡片数量（用于"每日新学上限"）。 */
    @Query("SELECT COUNT(*) FROM cards WHERE firstReviewAt >= :from AND firstReviewAt < :to")
    int countIntroducedBetween(long from, long to);

    @Query("UPDATE cards SET stage = 0, dueAt = :now, reviews = 0, lapses = 0, "
            + "firstReviewAt = 0, lastReviewAt = 0 WHERE id = :id")
    void resetProgress(long id, long now);

    @Query("UPDATE cards SET deckId = :toDeck WHERE deckId = :fromDeck")
    void moveDeck(long fromDeck, long toDeck);

    @Query("SELECT * FROM cards")
    List<Card> all();

    @Query("DELETE FROM cards")
    void clear();
}
