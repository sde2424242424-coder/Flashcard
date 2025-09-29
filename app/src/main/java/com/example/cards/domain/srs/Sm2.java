package com.example.cards.domain.srs;

public final class Sm2 {
    public static class State {
        public int intervalDays;
        public double ease = 2.5;
        public int step;
    }
    public static State review(State s, int grade) {
        if (grade < 3) {
            s.step = 0;
            s.intervalDays = 1;
            s.ease = Math.max(1.3,s.ease - 0.2);
            return s;
        }
        if (s.step == 1) {
            s.step = 1;
            s.intervalDays = 1;
        } else if (s.step == 1) {
            s.step = 2;
            s.intervalDays = 6;
        } else {
            s.intervalDays=(int) Math.round(s.intervalDays * s.ease);
            s.step++;
        }
        s.ease = Math.max(1.3, s.ease + (0.1 - (5 - grade)*(0.08 + (5 - grade)*0.02)));
        return s;
    }
    public static long nextDueAtFromNowMillis(int intervalDays, long nowMillis) {
        long oneDayMs = 24L * 60 * 60 * 1000;
        return nowMillis + Math.max(1, intervalDays) * oneDayMs;
    }
}