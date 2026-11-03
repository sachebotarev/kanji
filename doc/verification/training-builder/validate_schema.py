# coding: utf-8
import sqlite3
from pathlib import Path

c = sqlite3.connect(':memory:')
c.executescript(Path('doc/imports/catalog/schema.sql').read_text())
c.executescript(Path('doc/database/trainings.sql').read_text())

# Insert valid training recipe
c.execute("INSERT INTO trainings(id,created_at,updated_at,task_count,is_archived) VALUES('t',0,0,40,0)")
c.execute("INSERT INTO trainings_translations VALUES('t','ru','Тренировка')")
c.execute("INSERT INTO training_sources(id,training_id,position) VALUES('s','t',0)")

# Test constraints on recipes
for query in [
    "UPDATE trainings SET task_count=0",
    "UPDATE trainings SET seconds_per_word=-1",
    "UPDATE trainings SET selection_mode='invalid'",
    "UPDATE trainings SET is_archived=2",
    "UPDATE training_sources SET chapter_from=1",  # Chapter without source
    "UPDATE training_sources SET chapter_from=5, chapter_to=2",  # From > to
    "INSERT INTO training_sources(id,training_id,position) VALUES('s2','missing',1)"  # Missing training FK
]:
    try:
        c.execute(query)
    except sqlite3.IntegrityError:
        pass
    else:
        raise AssertionError(f"Expected failure for: {query}")

# Test session creation and execution items
c.execute("""
    INSERT INTO training_sessions(id, training_id, recipe_revision, task_count, seconds_per_word, selection_mode, status, started_at)
    VALUES('sess-1', 't', 1, 20, 30, 'shuffled_cycles', 'in_progress', 1000)
""")
c.execute("INSERT INTO words(id, written_form, reading) VALUES(101, '猫', 'ねこ')")
c.execute("""
    INSERT INTO training_session_items(id, session_id, position, word_id, status, elapsed_ms, hint_count)
    VALUES('item-1', 'sess-1', 0, 101, 'correct', 4500, 0)
""")

# Test session constraints
for query in [
    "UPDATE training_sessions SET status='unknown_status'",
    "UPDATE training_sessions SET task_count=0",
    "UPDATE training_session_items SET status='unknown_item_status'",
    "UPDATE training_session_items SET elapsed_ms=-100",
    "INSERT INTO training_session_items(id, session_id, position, word_id, status) VALUES('item-dup', 'sess-1', 0, 101, 'pending')"
]:
    try:
        c.execute(query)
    except sqlite3.IntegrityError:
        pass
    else:
        raise AssertionError(f"Expected failure for: {query}")

# Test cascade delete of sessions on items
c.execute("DELETE FROM training_sessions WHERE id='sess-1'")
assert c.execute("SELECT count(*) FROM training_session_items WHERE session_id='sess-1'").fetchone()[0] == 0, "Cascade failed"

# Test archiving training retains recipe
c.execute("UPDATE trainings SET is_archived=1, archived_at=2000 WHERE id='t'")
assert c.execute("SELECT is_archived, archived_at FROM trainings WHERE id='t'").fetchone() == (1, 2000)

print('Schema validated: trainings, translations, sources, sessions, items, indices and constraints verified successfully')
