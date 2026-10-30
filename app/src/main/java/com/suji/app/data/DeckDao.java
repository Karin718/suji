package com.suji.app.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface DeckDao {

    @Insert
    long insert(Deck deck);

    @Update
    void update(Deck deck);

    @Delete
    void delete(Deck deck);

    @Query("SELECT * FROM decks WHERE archived = 0 ORDER BY createdAt ASC, id ASC")
    LiveData<List<Deck>> observeAll();

    @Query("SELECT * FROM decks ORDER BY createdAt ASC, id ASC")
    List<Deck> all();

    @Query("SELECT * FROM decks WHERE id = :id")
    Deck byId(long id);

    @Query("SELECT * FROM decks WHERE id = :id")
    LiveData<Deck> observe(long id);

    @Query("SELECT COUNT(*) FROM decks")
    int count();

    @Query("DELETE FROM decks")
    void clear();

    @Query("SELECT d.*, "
            + "(SELECT COUNT(*) FROM cards c WHERE c.deckId = d.id AND c.archived = 0) AS total, "
            + "(SELECT COUNT(*) FROM cards c WHERE c.deckId = d.id AND c.archived = 0 AND c.reviews > 0 "
            + " AND c.dueAt <= CAST(strftime('%s','now') AS INTEGER) * 1000) AS dueCount, "
            + "(SELECT COUNT(*) FROM cards c WHERE c.deckId = d.id AND c.archived = 0 AND c.reviews = 0) AS newCount "
            + "FROM decks d WHERE d.archived = 0 ORDER BY d.createdAt ASC, d.id ASC")
    LiveData<List<DeckStat>> observeStats();

    @Query("SELECT d.*, "
            + "(SELECT COUNT(*) FROM cards c WHERE c.deckId = d.id AND c.archived = 0) AS total, "
            + "(SELECT COUNT(*) FROM cards c WHERE c.deckId = d.id AND c.archived = 0 AND c.reviews > 0 "
            + " AND c.dueAt <= CAST(strftime('%s','now') AS INTEGER) * 1000) AS dueCount, "
            + "(SELECT COUNT(*) FROM cards c WHERE c.deckId = d.id AND c.archived = 0 AND c.reviews = 0) AS newCount "
            + "FROM decks d WHERE d.id = :deckId")
    DeckStat statOf(long deckId);
}
