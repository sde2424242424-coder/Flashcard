package com.example.cards;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.cards.util.ThemeHelper;

public class BrandActivity extends AppCompatActivity {

    private enum State { WALK, TURN, STAND, SIT, BOW, SIT2, STANDUP, TURNBACK }

    private final Handler handler = new Handler(Looper.getMainLooper());
    private ImageView imageView;

    // ==== КАДРЫ (замени на свои ресурсы в res/drawable) ====
    // Ходьба (2 кадра, по кругу)
    private final int[] walkFrames      = { R.drawable.frame_walk1, R.drawable.frame_walk2,R.drawable.frame_walk3, R.drawable.frame_walk4 };
    private final int[] walkDurationsMs = { 120, 120, 120, 120 };

    // Поворот (к фронту)
    private final int[] turnFrames      = { R.drawable.frame_turn1, R.drawable.frame_turn2 };
    private final int[] turnDurationsMs = { 140, 140 };

    // Стоит лицом
    private final int[] standFrames      = { R.drawable.frame_stand1 };   // можно 1 кадр
    private final int[] standDurationsMs = { 140 };                        // «держим» стойку

    // Садится
    private final int[] sitFrames      = { R.drawable.frame_sit1, R.drawable.frame_sit2 };
    private final int[] sitDurationsMs = { 140, 160 };

    // Поклон
    private final int[] bowFrames      = { R.drawable.frame_bow };
    private final int[] bowDurationsMs = { 400 , 400, 400, 400, 400 };

    // Встаёт
    private final int[] standupFrames      = { R.drawable.frame_sit1, R.drawable.frame_sit2 };
    private final int[] standupDurationsMs = { 160, 160 };



    // Поворот обратно (к походке вправо)
    private final int[] turnBackFrames      = { R.drawable.frame_turn1, R.drawable.frame_turn2 };
    private final int[] turnBackDurationsMs = { 140, 140 };

    // ==== Параметры движения ====
    private float posX;          // текущая X
    private float offRight;  // координата, после которой персонаж полностью вне экрана справа
    private float posY;          // фиксированная Y (центр по вертикали)
    private float maxX;          // правая граница (родитель - ширина спрайта)
    private float midX;          // середина экрана
    private float stepPx = 16f;  // шаг по X за кадр WALK

    // ==== Состояние ====
    private State state = State.WALK;
    private int frameIndex = 0;           // индекс кадра внутри текущего состояния
    private boolean midSequenceDone = false; // середину проигрываем один раз

    // Последовательность в середине
    private final State[] middleSeq = new State[]{
            State.TURN, State.STAND, State.SIT, State.BOW, State.SIT2, State.STANDUP, State.TURNBACK
    };
    private int middleStep = 0;  // указатель на текущий шаг последовательности

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        ThemeHelper.applyThemeFromPrefs(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_brand);

        imageView = findViewById(R.id.imgLogo);

        // ждём, когда лэйаут будет измерен (чтобы знать размеры)
        imageView.post(() -> {
            View parent = (View) imageView.getParent();
            float parentW = parent.getWidth();
            float parentH = parent.getHeight();

            float w = imageView.getWidth();
            float h = imageView.getHeight();

            posX = -w;                // ← старт полностью за левым краем (невидим)
            posY = (parentH - h) / 2f;
            maxX = parentW - w;       // правая видимая граница (оставим, если пригодится)
            offRight = parentW + w;  // закроем после того, как весь View уйдёт ещё на ширину себя
            midX = parentW / 2f;                // ← ЭТО ГЛАВНОЕ: середина экрана

            imageView.setX(posX);
            imageView.setY(posY);

            state = State.WALK;
            frameIndex = 0;
            playNext();
        });
    }

    private void playNext() {
        // финал: ушёл за экран справа после средней последовательности
        if (state == State.WALK && posX >= offRight && midSequenceDone) {
            startActivity(new Intent(this, MainMenuActivity.class));
            finish();
            return;
        }


        switch (state) {
            case WALK:
                // показать кадр ходьбы
                imageView.setImageResource(walkFrames[frameIndex % walkFrames.length]);

                // движение только в WALK
                posX = posX + stepPx;   // пусть уходит за край, чтобы сработало offRight
                imageView.setX(posX);

                // триггер середины — когда центр спрайта пересёк середину экрана
                if (!midSequenceDone) {
                    float spriteCenterX = posX + imageView.getWidth() / 2f;
                    if (spriteCenterX >= midX) {
                        // запускаем последовательность из 7 шагов
                        midSequenceDone = true;
                        middleStep = 0;
                        frameIndex = 0;
                        state = middleSeq[middleStep];
                        scheduleNext(currentStateDelay());
                        return;
                    }
                }

                scheduleNext(currentStateDelay());
                frameIndex++;
                break;

            case TURN:
            case STAND:
            case SIT:
            case BOW:
            case SIT2:
            case STANDUP:
            case TURNBACK:
                // показать кадр текущего состояния
                imageView.setImageResource(getFrameForState(state, frameIndex));

                // если дошли до конца кадров состояния — переходим к следующему
                frameIndex++;
                if (frameIndex >= getFrameCount(state)) {
                    // завершили текущий шаг последовательности
                    frameIndex = 0;
                    middleStep++;
                    if (middleStep < middleSeq.length) {
                        state = middleSeq[middleStep];
                    } else {
                        // вся последовательность выполнена — назад к ходьбе
                        state = State.WALK;
                    }
                }
                scheduleNext(currentStateDelay());
                break;
        }
    }

    private int currentStateDelay() {
        switch (state) {
            case WALK:
                return walkDurationsMs[frameIndex % walkDurationsMs.length];
            case TURN:
                return turnDurationsMs[Math.min(frameIndex, turnDurationsMs.length - 1)];
            case STAND:
                return standDurationsMs[Math.min(frameIndex, standDurationsMs.length - 1)];
            case SIT:
                return sitDurationsMs[Math.min(frameIndex, sitDurationsMs.length - 1)];
            case BOW:
                return bowDurationsMs[Math.min(frameIndex, bowDurationsMs.length - 1)];
            case SIT2:
                return sitDurationsMs[Math.min(frameIndex, sitDurationsMs.length - 1)];
            case STANDUP:
                return standupDurationsMs[Math.min(frameIndex, standupDurationsMs.length - 1)];
            case TURNBACK:
                return turnBackDurationsMs[Math.min(frameIndex, turnBackDurationsMs.length - 1)];
            default:
                return 120;
        }
    }

    private int getFrameForState(State s, int idx) {
        switch (s) {
            case WALK:
                return walkFrames[idx % walkFrames.length];
            case TURN:
                return turnFrames[Math.min(idx, turnFrames.length - 1)];
            case STAND:
                return standFrames[Math.min(idx, standFrames.length - 1)];
            case SIT:
                return sitFrames[Math.min(idx, sitFrames.length - 1)];
            case BOW:
                return bowFrames[Math.min(idx, bowFrames.length - 1)];
            case SIT2:
                return sitFrames[Math.min(idx, sitFrames.length - 1)];
            case STANDUP:
                return standupFrames[Math.min(idx, standupFrames.length - 1)];
            case TURNBACK:
                return turnBackFrames[Math.min(idx, turnBackFrames.length - 1)];
            default:
                return walkFrames[0];
        }
    }

    private int getFrameCount(State s) {
        switch (s) {
            case WALK:     return walkFrames.length;
            case TURN:     return turnFrames.length;
            case STAND:    return standFrames.length;
            case SIT:      return sitFrames.length;
            case BOW:      return bowDurationsMs.length;
            case SIT2:     return sitFrames.length;
            case STANDUP:  return standupFrames.length;
            case TURNBACK: return turnBackFrames.length;
            default:       return 1;
        }
    }

    private void scheduleNext(int delayMs) {
        handler.postDelayed(this::playNext, delayMs);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }
}
