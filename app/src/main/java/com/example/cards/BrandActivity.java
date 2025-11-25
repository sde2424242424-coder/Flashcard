//package com.example.cards;
//
//import android.content.Intent;
//import android.os.Bundle;
//import android.os.Handler;
//import android.os.Looper;
//import android.view.View;
//import android.widget.ImageView;
//
//import androidx.annotation.Nullable;
//import androidx.appcompat.app.AppCompatActivity;
//
//import com.example.cards.util.ThemeHelper;
//
///**
// * BrandActivity
// *
// * Intro / splash screen that plays a small character animation sequence
// * before navigating to {@link MainMenuActivity}.
// *
// * Behavior:
// * - The character starts walking from the left off-screen edge to the right.
// * - When it reaches the center, it performs a predefined sequence:
// *   TURN → STAND → SIT → BOW → SIT2 → STANDUP → TURNBACK.
// * - After the middle sequence finishes, the character continues walking
// *   off-screen to the right, and then the activity opens MainMenuActivity.
// *
// * Technical details:
// * - Uses a simple frame-by-frame animation based on ImageView and Handler.
// * - Each state has its own frame array and durations.
// * - Position is updated only in WALK state; middle sequence states are static.
// */
//public class BrandActivity extends AppCompatActivity {
//
//    /**
//     * High-level animation states.
//     */
//    private enum State {
//        WALK,       // walking across the screen
//        TURN,       // turning to face front
//        STAND,      // standing facing front
//        SIT,        // sitting down
//        BOW,        // bowing
//        SIT2,       // sitting after bow
//        STANDUP,    // standing up
//        TURNBACK    // turning back to walking direction
//    }
//
//    // Handler on main thread for scheduling frame updates.
//    private final Handler handler = new Handler(Looper.getMainLooper());
//
//    private ImageView imageView;
//
//    // ==== FRAMES (replace with your own resources in res/drawable) ====
//
//    // WALK (looped)
//    private final int[] walkFrames      = {
//            R.drawable.frame_walk1,
//            R.drawable.frame_walk2,
//            R.drawable.frame_walk3,
//            R.drawable.frame_walk4
//    };
//    private final int[] walkDurationsMs = { 120, 120, 120, 120 };
//
//    // TURN (towards front)
//    private final int[] turnFrames      = {
//            R.drawable.frame_turn1,
//            R.drawable.frame_turn2
//    };
//    private final int[] turnDurationsMs = { 140, 140 };
//
//    // STAND facing front (can be a single frame)
//    private final int[] standFrames      = { R.drawable.frame_stand1 };
//    private final int[] standDurationsMs = { 140 }; // hold pose
//
//    // SIT down
//    private final int[] sitFrames      = {
//            R.drawable.frame_sit1,
//            R.drawable.frame_sit2
//    };
//    private final int[] sitDurationsMs = { 140, 160 };
//
//    // BOW pose (single frame, held for several delays)
//    private final int[] bowFrames      = { R.drawable.frame_bow };
//    private final int[] bowDurationsMs = { 400, 400, 400, 400, 400 };
//
//    // STANDUP (reverse of sit)
//    private final int[] standupFrames      = {
//            R.drawable.frame_sit1,
//            R.drawable.frame_sit2
//    };
//    private final int[] standupDurationsMs = { 160, 160 };
//
//    // TURNBACK (turn back to walking direction)
//    private final int[] turnBackFrames      = {
//            R.drawable.frame_turn1,
//            R.drawable.frame_turn2
//    };
//    private final int[] turnBackDurationsMs = { 140, 140 };
//
//    // ==== Movement parameters ====
//    private float posX;          // current X coordinate
//    private float offRight;      // X position after which character is fully off-screen on the right
//    private float posY;          // fixed Y coordinate (vertical center)
//    private float maxX;          // right visible boundary (unused now, but kept)
//    private float midX;          // center of the screen (trigger for middle sequence)
//    private float stepPx = 16f;  // X step per frame in WALK state
//
//    // ==== Animation state ====
//    private State state = State.WALK;
//    private int frameIndex = 0;              // index inside current state's frames
//    private boolean midSequenceDone = false; // true after middle sequence executed once
//
//    // Middle sequence order (executed exactly once in the center).
//    private final State[] middleSeq = new State[] {
//            State.TURN,
//            State.STAND,
//            State.SIT,
//            State.BOW,
//            State.SIT2,
//            State.STANDUP,
//            State.TURNBACK
//    };
//    private int middleStep = 0;  // pointer to current step in middle sequence
//
//    @Override
//    protected void onCreate(@Nullable Bundle savedInstanceState) {
//        ThemeHelper.applyThemeFromPrefs(this);
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_brand);
//
//        imageView = findViewById(R.id.imgLogo);
//
//        // Wait until layout is measured to know parent and sprite sizes.
//        imageView.post(() -> {
//            View parent = (View) imageView.getParent();
//            float parentW = parent.getWidth();
//            float parentH = parent.getHeight();
//
//            float w = imageView.getWidth();
//            float h = imageView.getHeight();
//
//            // Start fully off-screen to the left.
//            posX = -w;
//            posY = (parentH - h) / 2f;
//            maxX = parentW - w;       // visible right boundary, kept for potential use
//            offRight = parentW + w;   // we consider animation done after passing this X
//            midX = parentW / 2f;      // center of the screen
//
//            imageView.setX(posX);
//            imageView.setY(posY);
//
//            state = State.WALK;
//            frameIndex = 0;
//            playNext();
//        });
//    }
//
//    /**
//     * Main animation loop: updates frame and state, then schedules the next step.
//     */
//    private void playNext() {
//        // Final condition: after middle sequence is done and the character has
//        // walked completely off-screen to the right, go to MainMenuActivity.
//        if (state == State.WALK && posX >= offRight && midSequenceDone) {
//            startActivity(new Intent(this, MainMenuActivity.class));
//            finish();
//            return;
//        }
//
//        switch (state) {
//            case WALK:
//                // Show current walk frame.
//                imageView.setImageResource(walkFrames[frameIndex % walkFrames.length]);
//
//                // Move character only in WALK state.
//                posX = posX + stepPx;
//                imageView.setX(posX);
//
//                // Middle sequence trigger: when sprite center passes screen center.
//                if (!midSequenceDone) {
//                    float spriteCenterX = posX + imageView.getWidth() / 2f;
//                    if (spriteCenterX >= midX) {
//                        // Start the middle animation sequence once.
//                        midSequenceDone = true;
//                        middleStep = 0;
//                        frameIndex = 0;
//                        state = middleSeq[middleStep];
//                        scheduleNext(currentStateDelay());
//                        return;
//                    }
//                }
//
//                scheduleNext(currentStateDelay());
//                frameIndex++;
//                break;
//
//            case TURN:
//            case STAND:
//            case SIT:
//            case BOW:
//            case SIT2:
//            case STANDUP:
//            case TURNBACK:
//                // Show frame for current state.
//                imageView.setImageResource(getFrameForState(state, frameIndex));
//
//                // Go to next frame in this state.
//                frameIndex++;
//                if (frameIndex >= getFrameCount(state)) {
//                    // Finished current state in middle sequence.
//                    frameIndex = 0;
//                    middleStep++;
//                    if (middleStep < middleSeq.length) {
//                        // Move to next state in middle sequence.
//                        state = middleSeq[middleStep];
//                    } else {
//                        // Entire middle sequence finished → back to WALK.
//                        state = State.WALK;
//                    }
//                }
//                scheduleNext(currentStateDelay());
//                break;
//        }
//    }
//
//    /**
//     * Returns frame delay for the current state and frame index.
//     */
//    private int currentStateDelay() {
//        switch (state) {
//            case WALK:
//                return walkDurationsMs[frameIndex % walkDurationsMs.length];
//            case TURN:
//                return turnDurationsMs[Math.min(frameIndex, turnDurationsMs.length - 1)];
//            case STAND:
//                return standDurationsMs[Math.min(frameIndex, standDurationsMs.length - 1)];
//            case SIT:
//                return sitDurationsMs[Math.min(frameIndex, sitDurationsMs.length - 1)];
//            case BOW:
//                return bowDurationsMs[Math.min(frameIndex, bowDurationsMs.length - 1)];
//            case SIT2:
//                return sitDurationsMs[Math.min(frameIndex, sitDurationsMs.length - 1)];
//            case STANDUP:
//                return standupDurationsMs[Math.min(frameIndex, standupDurationsMs.length - 1)];
//            case TURNBACK:
//                return turnBackDurationsMs[Math.min(frameIndex, turnBackDurationsMs.length - 1)];
//            default:
//                return 120;
//        }
//    }
//
//    /**
//     * Selects the frame drawable for the given state and local index.
//     */
//    private int getFrameForState(State s, int idx) {
//        switch (s) {
//            case WALK:
//                return walkFrames[idx % walkFrames.length];
//            case TURN:
//                return turnFrames[Math.min(idx, turnFrames.length - 1)];
//            case STAND:
//                return standFrames[Math.min(idx, standFrames.length - 1)];
//            case SIT:
//                return sitFrames[Math.min(idx, sitFrames.length - 1)];
//            case BOW:
//                return bowFrames[Math.min(idx, bowFrames.length - 1)];
//            case SIT2:
//                return sitFrames[Math.min(idx, sitFrames.length - 1)];
//            case STANDUP:
//                return standupFrames[Math.min(idx, standupFrames.length - 1)];
//            case TURNBACK:
//                return turnBackFrames[Math.min(idx, turnBackFrames.length - 1)];
//            default:
//                return walkFrames[0];
//        }
//    }
//
//    /**
//     * Returns how many frames the given state should play through.
//     * For BOW, frame count is based on durations array length, so we can
//     * hold a single frame for several "ticks".
//     */
//    private int getFrameCount(State s) {
//        switch (s) {
//            case WALK:     return walkFrames.length;
//            case TURN:     return turnFrames.length;
//            case STAND:    return standFrames.length;
//            case SIT:      return sitFrames.length;
//            case BOW:      return bowDurationsMs.length;
//            case SIT2:     return sitFrames.length;
//            case STANDUP:  return standupFrames.length;
//            case TURNBACK: return turnBackFrames.length;
//            default:       return 1;
//        }
//    }
//
//    /**
//     * Schedules the next animation step with the given delay.
//     */
//    private void scheduleNext(int delayMs) {
//        handler.postDelayed(this::playNext, delayMs);
//    }
//
//    @Override
//    protected void onDestroy() {
//        super.onDestroy();
//        // Cancel all pending callbacks to avoid leaks.
//        handler.removeCallbacksAndMessages(null);
//    }
//}
