package com.example.cards.domain.srs;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.ThreadLocalRandom;

public final class Sm2 {

    /** Параметры, которые можно вынести в настройки. */
    public static final class Config {
        public double easeMin = 1.30;
        public double easeMax = 3.00;
        public double easeInit = 2.50;

        // Коэффициенты изменения ease в SM-2:
        // ΔE = 0.1 - (5 - grade) * (0.08 + (5 - grade) * 0.02)
        public double deltaBase = 0.10;
        public double deltaA = 0.08;
        public double deltaB = 0.02;

        // Обучающие интервалы (step 0,1,2) — миллисекунды
        public long learnStep0 = TimeUnit.MINUTES.toMillis(1);   // 1 мин
        public long learnStep1 = TimeUnit.MINUTES.toMillis(10);  // 10 мин
        public long learnStep2 = TimeUnit.DAYS.toMillis(1);      // 1 день

        // Включить фазирование dueAt на локальные 03:00
        public boolean alignDueAtTo3am = true;

        // Случайная «подсечка» интервала, чтобы разносить карточки
        public double fuzzPercentMin = 0.05;  // 5%
        public double fuzzPercentMax = 0.15;  // 15%
        public boolean enableFuzz = true;
    }

    public static final class State {
        public int intervalDays; // интервальные дни (для ревью-режима)
        public double ease;      // 1.3..3.0
        public int step;         // 0,1,2 — обучающие шаги; >=3 считаем «выпущен»
    }

    private static int clampGrade(int g) {
        if (g < 0) return 0;
        if (g > 5) return 5;
        return g;
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    /** Главная точка — применяет отзыв (grade) к состоянию. */
    public static State review(State s, int grade, long nowMillis, Config cfg) {
        int g = clampGrade(grade);
        if (s.ease <= 0) s.ease = cfg.easeInit;

        // Ошибка → возвращаемся в обучение
        if (g < 3) {
            s.step = 0;
            s.intervalDays = 0;
            s.ease = clamp(s.ease - 0.20, cfg.easeMin, cfg.easeMax); // можно смягчить
            return s;
        }

        // Корректируем ease согласно SM-2
        int diff = 5 - g; // 0..2
        double delta = cfg.deltaBase - diff * (cfg.deltaA + diff * cfg.deltaB);
        s.ease = clamp(s.ease + delta, cfg.easeMin, cfg.easeMax);

        // Если ещё в обучении — двигаем по шагам
        if (s.step < 3) {
            s.step++;
            if (s.step >= 3) {
                // выпуск в интервальные: 1д, затем 3д, затем *ease
                s.intervalDays = (s.intervalDays <= 0) ? 1 : s.intervalDays;
            }
            return s;
        }

        // Интервальный режим
        if (s.intervalDays <= 0) {
            s.intervalDays = 1;
        } else if (s.intervalDays == 1) {
            s.intervalDays = 3;
        } else {
            s.intervalDays = (int) Math.max(1, Math.round(s.intervalDays * s.ease));
        }
        return s;
    }

    /** Следующая дата показа с учётом интервала в днях и настроек. */
    public static long nextDueAtFromNowMillis(int intervalDays, long nowMillis, Config cfg) {
        long base = nowMillis + TimeUnit.DAYS.toMillis(Math.max(0, intervalDays));

        // Фазирование на 03:00 локального времени (упрощённо, без tzdb)
        if (cfg.alignDueAtTo3am) {
            // сдвигаем к 03:00 ближайшего дня base
            long dayMs = TimeUnit.DAYS.toMillis(1);
            long localDayStart = base - (base % dayMs);
            base = localDayStart + TimeUnit.HOURS.toMillis(3);
        }

        // Небольшая «подсечка» интервала
        if (cfg.enableFuzz && intervalDays > 0) {
            double p = ThreadLocalRandom.current()
                    .nextDouble(cfg.fuzzPercentMin, cfg.fuzzPercentMax);
            long jitter = (long) (TimeUnit.DAYS.toMillis(intervalDays) * p);
            base += ThreadLocalRandom.current().nextBoolean() ? jitter : -jitter;
        }

        return base;
    }

    /** Удобный дефолт. */
    public static State review(State s, int grade) {
        return review(s, grade, System.currentTimeMillis(), new Config());
    }

    public static long nextDueAtFromNowMillis(int intervalDays, long nowMillis) {
        return nextDueAtFromNowMillis(intervalDays, nowMillis, new Config());
    }
}
