package com.frixs.zcu_kiv_mkz_seminar.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.frixs.zcu_kiv_mkz_seminar.R;
import com.frixs.zcu_kiv_mkz_seminar.classes.SharedData;
import com.frixs.zcu_kiv_mkz_seminar.engine.GameEngine;
import com.frixs.zcu_kiv_mkz_seminar.enums.Direction;
import com.frixs.zcu_kiv_mkz_seminar.enums.GameState;
import com.frixs.zcu_kiv_mkz_seminar.views.GameView;

public class GameActivity extends AppCompatActivity implements View.OnTouchListener {

    private GameEngine gameEngine;
    private GameView gameView;

    private final Handler handler = new Handler();

    /** Game tick delay */
    private long gameTick;

    /** Countdown tick. */
    private final long countdownTick = 1000;
    /** Delay after loading the activity. */
    private final long activityLoadingTick = 1000;
    /** Countdown iteration. */
    private final int countdownIter = 3;
    /** Countdown iteration recorded. */
    private int countdownIterCounter = countdownIter;
    /** Says if the game is paused. If yes the game will stop updating. */
    private boolean isGamePaused = false;

    /** Previous positions. */
    private float prevPosX, prevPosY;

    private MediaPlayer backgroundMusic;
    public static MediaPlayer deathSFX;
    public static MediaPlayer fruitSFX;
    public static MediaPlayer specialSFX;

    private TextView currentScoreTV;
    private TextView bestScoreTV;
    private Button restartBTN;
    private LinearLayout activeActionBarLL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        // Initialize the game activity.
        initialize();
        gameView.setOnTouchListener(this);

        // Start the game.
        start();

        backgroundMusic = MediaPlayer.create(this, R.raw.game_music);
        backgroundMusic.setLooping(true);
        backgroundMusic.start();

        deathSFX = MediaPlayer.create(this, R.raw.death);
        fruitSFX = MediaPlayer.create(this, R.raw.apple);
        specialSFX = MediaPlayer.create(this, R.raw.special);
    }

    /**
     * Initialize the game activity.
     */
    private void initialize() {
        // Engine initialization.
        gameEngine = new GameEngine();
        gameEngine.initGame();

        gameView = (GameView) findViewById(R.id.gameView);
        // The game view should has set the alpha already in xml file. Set it programmatically too.
        gameView.setAlpha(0.0f);

        // Update map and redraw. Show the map before starting the main handler update method.
        gameView.setViewMap(gameEngine.getTileMap());
        gameView.invalidate();

        // FadeIn the game map after loading.
        gameView.animate().alpha(1.0f);

        // Get layout items.
        currentScoreTV = (TextView) findViewById(R.id.currentScoreFieldTV);
        bestScoreTV = (TextView) findViewById(R.id.bestScoreFieldTV);
        restartBTN = (Button) findViewById(R.id.restartBTN);
        activeActionBarLL = (LinearLayout) findViewById(R.id.activeActionBar);

        // Initialize score to default values.
        currentScoreTV.setText("0");
        SharedPreferences settigns = getSharedPreferences(SharedData._ROOT, Context.MODE_PRIVATE);
        int highScore = settigns.getInt(SharedData.BEST_SCORE, 0);
        bestScoreTV.setText("" + highScore);
    }

    /**
     * Start the game activity.
     */
    private void start() {
        gameTick = gameEngine.getGameTick();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                startGameCountdown();
            }
        }, activityLoadingTick);
    }

    /**
     * Start the game with countdown.
     */
    private void startGameCountdown() {
        // Countdown to start of the game.
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (countdownIterCounter + 1 <= 0) {
                    startUpdateGameHandler();
                    countdownIterCounter = countdownIter;

                } else {
                    if (countdownIterCounter > 0) {
                        onGameCountdown("" + countdownIterCounter);
                    } else {
                        onGameCountdown("START");
                    }

                    --countdownIterCounter;

                    if (!isGamePaused) {
                        startGameCountdown();
                    }
                }
            }
        }, countdownTick);
    }

    /**
     * Main handler update stream.
     */
    private void startUpdateGameHandler() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // Update game (1 step/tick in the game).
                gameEngine.update();

                // Update action bar GUI.
                updateActiveActions();

                // Keep updating if the game is running.
                if (gameEngine.getCurrentGameState() == GameState.Running) {
                    gameTick = gameEngine.getGameTick();
                    if (!isGamePaused) {
                        handler.postDelayed(this, gameTick);
                    }
                }

                // GameOver things to do...
                if (gameEngine.getCurrentGameState() == GameState.GameOver) {
                    onGameOver();
                }

                // Update map and redraw.
                gameView.setViewMap(gameEngine.getTileMap());
                gameView.invalidate();

                // Update score.
                currentScoreTV.setText("" + gameEngine.getScore());
                if (Integer.parseInt((String) bestScoreTV.getText()) < gameEngine.getScore()) {
                    bestScoreTV.setText("" + gameEngine.getScore());
                }
            }
        }, gameTick);
    }

    /**
     * GameOver event handler.
     */
    private void onGameOver() {
        Toast.makeText(this, "GAME OVER", Toast.LENGTH_SHORT).show();

        // Save the score.
        SharedPreferences settigns = getSharedPreferences(SharedData._ROOT, Context.MODE_PRIVATE);
        int highScore = settigns.getInt(SharedData.BEST_SCORE, 0);
        if (gameEngine.getScore() > highScore) {
            SharedPreferences.Editor editor = settigns.edit();
            editor.putInt(SharedData.BEST_SCORE, gameEngine.getScore());
            editor.apply();
        }

        deathSFX.start();

        // Show option to restart the game.
        restartBTN.setVisibility(View.VISIBLE);
    }

    /**
     * Update active actions in GUI.
     */
    private void updateActiveActions() {
        activeActionBarLL.removeAllViews();

        if (gameEngine.getActiveActions().size() > 0) {
            activeActionBarLL.setPadding(30, 15, 30, 15);
        } else {
            activeActionBarLL.setPadding(0, 0, 0, 0);
        }

        for (int i = 0; i < gameEngine.getActiveActions().size(); i++) {
            TextView actionIcon = new TextView(this);
            actionIcon.setText("█ " + (gameEngine.getActiveActions().get(i).getActionDurationCounter() + 1));
            actionIcon.setTextSize(16);
            actionIcon.setTextColor(getResources().getColor(gameEngine.getActiveActions().get(i).getTileType().getColor()));
            actionIcon.setPadding((i == 0 ? 0 : 15), 0, 0, 0);
            actionIcon.setShadowLayer(10f, 4, 4, Color.BLACK);
            activeActionBarLL.addView(actionIcon);
        }
    }

    /**
     * Game countdown event handler.
     */
    private void onGameCountdown(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }

    /**
     * On click event BTN.
     * @param view
     */
    public void onClickControlUpBTN(View view) {
        gameEngine.setDesiredDirection(Direction.North);
    }

    /**
     * On click event BTN.
     * @param view
     */
    public void onClickControlRightBTN(View view) {
        gameEngine.setDesiredDirection(Direction.East);
    }

    /**
     * On click event BTN.
     * @param view
     */
    public void onClickControlDownBTN(View view) {
        gameEngine.setDesiredDirection(Direction.South);
    }

    /**
     * On click event BTN.
     * @param view
     */
    public void onClickControlLeftBTN(View view) {
        gameEngine.setDesiredDirection(Direction.West);
    }

    /**
     * On click event BTN.
     * @param view
     */
    public void onClickRestartBTN(View view) {
        finish();
        startActivity(getIntent());
    }

    /**
     * On click event BTN.
     * @param view
     */
    public void onClickLeaveBTN(View view) {
        isGamePaused = true;

        backgroundMusic.stop();
        backgroundMusic.release();
        backgroundMusic = null;

        Intent i = new Intent(this, MainActivity.class);
        startActivity(i);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_UP:
                float newPosX = event.getX();
                float newPosY = event.getY();

                // Calculate where we swiped.
                if (Math.abs(newPosX - prevPosX) > Math.abs(newPosY - prevPosY)) {
                    // LEFT - RIGHT directions.
                    if (newPosX > prevPosX) {
                        // RIGHT.
                        gameEngine.setDesiredDirection(Direction.East);
                    } else {
                        // LEFT.
                        gameEngine.setDesiredDirection(Direction.West);
                    }
                } else {
                    // UP - DOWN directions.
                    if (newPosY > prevPosY) {
                        // DOWN.
                        gameEngine.setDesiredDirection(Direction.South);
                    } else {
                        // UP.
                        gameEngine.setDesiredDirection(Direction.North);
                    }
                }

                break;
            case MotionEvent.ACTION_DOWN:
                prevPosX = event.getX();
                prevPosY = event.getY();
                break;
        }

        return true;
    }
}
