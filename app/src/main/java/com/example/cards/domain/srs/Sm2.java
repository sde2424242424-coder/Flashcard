package com.example.cards.domain.srs;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Sm2
 *
 * Implementation of a configurable SM-2–style spaced repetition algorithm.
 * Works with a simple {@link State} object and a {@link Config} object
 * that defines all tunable parameters (ease bounds, learning steps, fuzzing, etc.).
 *
 * Main responsibilities:
 * - Transform a review {@code grade} (0–5) into an updated SM-2 state
 *   (interval, ease, learning step).
 * - Compute the next {@code dueAt} timestamp using interval in days and config.
 *
 * Usage:
 * - Construct a {@link State} and (optionally) {@link Config}.
 * - Call {@link #review(State, int)} or {@link #review(State, int, long, Config)}.
 * - Store resulting state and call {@link #nextDueAtFromNowMillis(int, long)} to get due time.
 */
public final class Sm2 {

    /**
     * Configuration parameters for SM-2 behavior and scheduling.
     * All fields are public so they can be adjusted before use.
     */
    public static final class Config {
        // Minimum/maximum ease factor and initial ease value.
        public double easeMin = 1.30;
        public double easeMax = 3.00;
        public double easeInit = 2.50;

        // Ease adjustment formula based on the original SM-2:
        // ΔE = 0.1 - (5 - grade) * (0.08 + (5 - grade) * 0.02)
        public double deltaBase = 0.10;
        public double deltaA = 0.08;
        public double deltaB = 0.02;

        // Learning steps (step 0,1,2) in milliseconds.
        public long learnStep0 = TimeUnit.MINUTES.toMillis(1);   // 1 min
        public long learnStep1 = TimeUnit.MINUTES.toMillis(10);  // 10 min
        public long learnStep2 = TimeUnit.DAYS.toMillis(1);      // 1 day

        // Align dueAt to local 03:00 (simplified, no timezone DB).
        public boolean alignDueAtTo3am = true;

        // Random “fuzz” to avoid showing all cards at the exact same time.
        public double fuzzPercentMin = 0.05;  // 5%
        public double fuzzPercentMax = 0.15;  // 15%
        public boolean enableFuzz = true;
    }

    /**
     * SM-2 state used by the scheduler.
     *
     * intervalDays – current review interval in days.
     * ease         – ease factor (1.3 .. 3.0 typical range).
     * step         – learning step: 0,1,2 means “learning phase”;
     *                >=3 is treated as regular review phase.
     */
    public static final class State {
        public int intervalDays; // interval in days for review mode
        public double ease;      // 1.3..3.0
        public int step;         // 0,1,2 – learning steps; >=3 means “mature”
    }

    private static int clampGrade(int g) {
        if (g < 0) return 0;
        if (g > 5) return 5;
        return g;
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    /**
     * Core review method that applies a grade to the given state using
     * the supplied configuration.
     *
     * @param s          current SM-2 state (will be mutated and returned)
     * @param grade      user grade 0–5 (0..2 = fail, 3..5 = pass)
     * @param nowMillis  current time in ms (can be used by callers if needed)
     * @param cfg        configuration with ease bounds, fuzzing, etc.
     * @return updated {@link State} (same instance as input)
     */
    public static State review(State s, int grade, long nowMillis, Config cfg) {
        int g = clampGrade(grade);
        if (s.ease <= 0) s.ease = cfg.easeInit;

        // Failure: reset to learning.
        if (g < 3) {
            s.step = 0;
            s.intervalDays = 0;
            // Penalize ease a bit, but keep it within bounds.
            s.ease = clamp(s.ease - 0.20, cfg.easeMin, cfg.easeMax);
            return s;
        }

        // Adjust ease according to SM-2 formula.
        int diff = 5 - g; // 0..2
        double delta = cfg.deltaBase - diff * (cfg.deltaA + diff * cfg.deltaB);
        s.ease = clamp(s.ease + delta, cfg.easeMin, cfg.easeMax);

        // Learning steps: move through steps 0,1,2 before going to interval mode.
        if (s.step < 3) {
            s.step++;
            if (s.step >= 3) {
                // Transition to interval mode:
                // start with 1 day (or keep existing interval if already > 0).
                s.intervalDays = (s.intervalDays <= 0) ? 1 : s.intervalDays;
            }
            return s;
        }

        // Interval mode.
        if (s.intervalDays <= 0) {
            s.intervalDays = 1;
        } else if (s.intervalDays == 1) {
            s.intervalDays = 3;
        } else {
            s.intervalDays = (int) Math.max(1, Math.round(s.intervalDays * s.ease));
        }
        return s;
    }

    /**
     * Computes the next due timestamp from nowMillis and a given interval
     * in days, taking into account alignment to 03:00 and optional fuzzing.
     *
     * @param intervalDays interval in days
     * @param nowMillis    current time in milliseconds
     * @param cfg          configuration
     * @return calculated dueAt timestamp in milliseconds
     */
    public static long nextDueAtFromNowMillis(int intervalDays, long nowMillis, Config cfg) {
        long base = nowMillis + TimeUnit.DAYS.toMillis(Math.max(0, intervalDays));

        // Align due to 03:00 local time (very simplified).
        if (cfg.alignDueAtTo3am) {
            long dayMs = TimeUnit.DAYS.toMillis(1);
            long localDayStart = base - (base % dayMs);
            base = localDayStart + TimeUnit.HOURS.toMillis(3);
        }

        // Apply slight random fuzz so cards are not all due at exactly the same moment.
        if (cfg.enableFuzz && intervalDays > 0) {
            double p = ThreadLocalRandom.current()
                    .nextDouble(cfg.fuzzPercentMin, cfg.fuzzPercentMax);
            long jitter = (long) (TimeUnit.DAYS.toMillis(intervalDays) * p);
            base += ThreadLocalRandom.current().nextBoolean() ? jitter : -jitter;
        }

        return base;
    }

    /**
     * Convenience overload using default configuration and current time.
     *
     * @param s     current state
     * @param grade grade 0–5
     * @return updated state
     */
    public static State review(State s, int grade) {
        return review(s, grade, System.currentTimeMillis(), new Config());
    }

    /**
     * Convenience overload using default configuration.
     *
     * @param intervalDays interval in days
     * @param nowMillis    current time
     * @return next due timestamp in ms
     */
    public static long nextDueAtFromNowMillis(int intervalDays, long nowMillis) {
        return nextDueAtFromNowMillis(intervalDays, nowMillis, new Config());
    }
}
