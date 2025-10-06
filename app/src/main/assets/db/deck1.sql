PRAGMA user_version = 11;

DROP TABLE IF EXISTS cards;
DROP TABLE IF EXISTS review_state;
DROP TABLE IF EXISTS review_log;

-- -------------------------------
-- Таблица карточек (cards)
-- -------------------------------
CREATE TABLE cards(
  id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
  deckId INTEGER NOT NULL,
  front TEXT NOT NULL,
  back TEXT NOT NULL,
  createdAt INTEGER NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX IF NOT EXISTS index_cards_deckId_front_back
  ON cards(deckId, front, back);

-- -------------------------------
-- Таблица состояния повторения (review_state)
-- -------------------------------
CREATE TABLE IF NOT EXISTS review_state(
  id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
  cardId INTEGER NOT NULL,
  intervalDays INTEGER NOT NULL,
  ease REAL NOT NULL,
  step INTEGER NOT NULL,
  dueAt INTEGER NOT NULL,
  lastGrade INTEGER
);

CREATE UNIQUE INDEX IF NOT EXISTS index_review_state_cardId
  ON review_state(cardId);

-- -------------------------------
-- Таблица логов повторений (review_log)
-- -------------------------------
CREATE TABLE IF NOT EXISTS review_log(
  id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
  cardId INTEGER NOT NULL,
  reviewedAt INTEGER NOT NULL,
  grade INTEGER NOT NULL,
  resultIntervalDays INTEGER NOT NULL,
  resultEase REAL NOT NULL,
  resultStep INTEGER NOT NULL,
  ts INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS index_review_log_cardId
  ON review_log(cardId);

-- -------------------------------
-- Начальные карточки
-- -------------------------------
INSERT INTO cards (deckId, front, back, createdAt) VALUES
(1, '일', 'работа; день', strftime('%s','now')*1000),
(1, '학교', 'школа', strftime('%s','now')*1000),
(1, '시간', 'время; час', strftime('%s','now')*1000),
(1, '사람', 'человек', strftime('%s','now')*1000),
(1, '사랑', 'любовь', strftime('%s','now')*1000),
(1, '책', 'книга', strftime('%s','now')*1000),
(1, '음식', 'еда', strftime('%s','now')*1000),
(1, '친구', 'друг', strftime('%s','now')*1000),
(1, '집', 'дом', strftime('%s','now')*1000),
(1, '자동차', 'автомобиль', strftime('%s','now')*1000),
(1, '길', 'дорога', strftime('%s','now')*1000),
(1, '마음', 'душа; сердце', strftime('%s','now')*1000),
(1, '눈', 'глаз; снег', strftime('%s','now')*1000),
(1, '손', 'рука (кисть)', strftime('%s','now')*1000),
(1, '전화', 'телефон', strftime('%s','now')*1000),
(1, '영화', 'фильм', strftime('%s','now')*1000),
(1, '공부', 'учёба', strftime('%s','now')*1000),
(1, '음악', 'музыка', strftime('%s','now')*1000),
(1, '여행', 'путешествие', strftime('%s','now')*1000),
(1, '책상', 'письменный стол', strftime('%s','now')*1000);
