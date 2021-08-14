package com.frogobox.board.mvvm.game;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.frogobox.board.R;
import com.frogobox.board.core.BaseActivity;
import com.frogobox.board.util.SingleFunc;
import com.frogobox.board.widget.Element;
import com.frogobox.board.model.GameState;
import com.frogobox.board.model.GameStatistics;
import com.frogobox.board.util.SingleConst;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.Calendar;

public class GameActivity extends BaseActivity {

    private int n = 4;
    private int points = 0;
    private int last_points = 0;
    private int highestNumber = 0;
    private int numberFieldSize = 0;

    private long record = 0;
    private long startingTime = Calendar.getInstance().getTimeInMillis();

    private boolean moved = false;
    private boolean undo = false;
    private boolean won2048 = false;
    private boolean newGame = false;
    private boolean gameOver = false;
    private boolean firstTime = true;
    private boolean saveState = true;
    private boolean createNewGame = true;
    private boolean animationActivated = true;

    private String filename = "";

    private GameState gameState = null;
    private GameStatistics gameStatistics = new GameStatistics(n);

    private Element[][] elements = null;
    private Element[][] last_elements = null;
    private Element[][] backgroundElements = null;

    private RelativeLayout touch_field;
    private RelativeLayout number_field;
    private RelativeLayout number_field_background;

    private ImageView restartButton;
    private ImageView undoButton;

    private TextView textFieldPoints;
    private TextView textFieldRecord;

    private View.OnTouchListener swipeListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        animationActivated = sharedPref.getBoolean(SingleConst.Pref.PREF_ANIMATION_ACTIVATED, true);

        if (sharedPref.getBoolean(SingleConst.Pref.PREF_SETTINGS_DISPLAY, true))
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        saveState = true;

        if (savedInstanceState == null) {
            Intent intent = getIntent();
            if (firstTime && intent.getBooleanExtra(SingleConst.Extra.EXTRA_NEW, true)) {
                createNewGame = true;
                firstTime = false;
            }
        }

        initResources();
        setupShowAdsBanner(findViewById(R.id.ads_banner));
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        createNewGame = false;
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onBackPressed() {
        save();
        super.onBackPressed();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_toolbar_game, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        } else if (id == android.R.id.home) {
            save();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        start();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        save();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem item = menu.findItem(R.id.action_settings);
        item.setVisible(false);
        super.onPrepareOptionsMenu(menu);
        return true;
    }

    @Override
    public void onPause() {
        Log.i("lifecycle", "pause");
        save();
        super.onPause();
    }

    public Element[][] deepCopy(Element[][] e) {
        Element[][] r = new Element[e.length][];
        for (int i = 0; i < r.length; i++) {
            r[i] = new Element[e[i].length];
            for (int j = 0; j < r[i].length; j++) {
                r[i][j] = e[i][j].copy();
            }
        }
        return r;
    }

    public GameState readStateFromFile() {
        GameState nS = new GameState(n);
        try {
            File file = new File(getFilesDir(), filename);
            FileInputStream fileIn = new FileInputStream(file);
            ObjectInputStream in = new ObjectInputStream(fileIn);
            nS = (GameState) in.readObject();
            boolean emptyField = true;
            for (int i = 0; i < nS.numbers.length; i++) {
                if (nS.numbers[i] > 0) {
                    emptyField = false;
                    break;
                }
            }
            if (emptyField || nS.n != n) {
                nS = new GameState(n);
                newGame = true;
            }
            in.close();
            fileIn.close();
        } catch (Exception e) {
            newGame = true;
            e.printStackTrace();
        }
        return nS;
    }

    public GameStatistics readStatisticsFromFile() {
        GameStatistics gS = new GameStatistics(n);
        try {
            File file = new File(getFilesDir(), SingleConst.Const.FILE_STATISTIC + n + SingleConst.Ext.TXT);
            FileInputStream fileIn = new FileInputStream(file);
            ObjectInputStream in = new ObjectInputStream(fileIn);
            gS = (GameStatistics) in.readObject();
            in.close();
            fileIn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return gS;
    }

    public void start() {
        Log.i("activity", "start");
        saveState = true;
        ViewGroup.LayoutParams lp = number_field.getLayoutParams();

        //setting squared Number Field
        if (number_field.getHeight() > number_field.getWidth())
            lp.height = number_field.getWidth();
        else
            lp.width = number_field.getHeight();
        number_field.setLayoutParams(lp);
        number_field_background.setLayoutParams(lp);

        initialize();
        setListener();
        if (newGame) {
            moved = true;
            addNumber();
        }
        newGame = false;

    }

    public void initResources() {

        number_field = findViewById(R.id.number_field);
        number_field_background = findViewById(R.id.number_field_background);
        touch_field = findViewById(R.id.touch_field);

        textFieldPoints = findViewById(R.id.points);
        textFieldRecord = findViewById(R.id.record);

        restartButton = findViewById(R.id.restartButton);
        undoButton = findViewById(R.id.undoButton);

        restartButton.setOnClickListener(v -> {
            SingleFunc.INSTANCE.saveStatisticsToFile(this, gameStatistics);
            createNewGame();
            setupShowAdsInterstitial();
        });

        undoButton.setOnClickListener(v -> {
            undoButton.setVisibility(View.INVISIBLE);
            if (undo && last_elements != null) {
                gameStatistics.undo();
                elements = last_elements;
                points = last_points;
                number_field.removeAllViews();
                points = last_points;
                textFieldPoints.setText(String.valueOf(points));
                setDPositions(false);
                for (Element[] i : elements) {
                    for (Element j : i) {
                        j.setVisibility(View.INVISIBLE);
                        number_field.addView(j);
                        j.drawItem();
                    }
                }
                for (int i = 0; i < elements.length; i++) {
                    for (int j = 0; j < elements[i].length; j++) {
                        elements[i][j].setOnTouchListener(swipeListener);
                        backgroundElements[i][j].setOnTouchListener(swipeListener);
                    }
                }
                updateGameState();
                drawAllElements(elements);
                number_field.refreshDrawableState();
            }
            undo = false;
        });

    }

    public void save() {
        Log.i("saving", "save");
        if (!createNewGame)
            SingleFunc.INSTANCE.saveStateToFile(this, gameState, filename, saveState);
        gameStatistics.addTimePlayed(Calendar.getInstance().getTimeInMillis() - startingTime);
        startingTime = Calendar.getInstance().getTimeInMillis();
        SingleFunc.INSTANCE.saveStatisticsToFile(this, gameStatistics);
        firstTime = true;
    }

    public void createNewGame() {
        createNewGame = true;
        getIntent().putExtra(SingleConst.Extra.EXTRA_NEW, true);
        number_field.removeAllViews();
        number_field_background.removeAllViews();
        initialize();
    }

    public void initializeState() {
        points = 0;
        Intent intent = getIntent();
        n = intent.getIntExtra(SingleConst.Extra.EXTRA_N, 4);
        newGame = intent.getBooleanExtra(SingleConst.Extra.EXTRA_NEW, true);
        filename = intent.getStringExtra(SingleConst.Extra.EXTRA_FILENAME);
        undo = intent.getBooleanExtra(SingleConst.Extra.EXTRA_UNDO, false);
        if (!newGame) {
            gameState = readStateFromFile();
            points = gameState.points;
            last_points = gameState.last_points;
        } else {
            gameState = new GameState(n);
            newGame = true;
        }
        elements = new Element[n][n];
        last_elements = new Element[n][n];
        backgroundElements = new Element[n][n];
        saveState = true;
    }

    public void drawAllElements(Element[][] e) {
        for (Element[] i : e) {
            for (Element j : i) {
                j.drawItem();
            }
        }
    }

    public void updateGameState() {
        gameState = new GameState(elements, last_elements);
        gameState.n = n;
        gameState.points = points;
        gameState.last_points = last_points;
        gameState.undo = undo;
        updateHighestNumber();
        check2048();
    }

    public void initialize() {
        Log.i("activity", "initialize");
        if (getIntent().getIntExtra(SingleConst.Extra.EXTRA_N, 4) != n || createNewGame) {
            initializeState();

        }
        gameStatistics = readStatisticsFromFile();
        record = gameStatistics.getRecord();
        last_points = gameState.last_points;
        createNewGame = false;
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int abstand = (10 * metrics.densityDpi) / DisplayMetrics.DENSITY_DEFAULT;
        numberFieldSize = number_field.getWidth();
        if (numberFieldSize > number_field.getHeight())
            numberFieldSize = number_field.getHeight();
        int number_size = (numberFieldSize - abstand) / n - abstand;

        textFieldRecord.setText(String.valueOf(record));
        textFieldPoints.setText(String.valueOf(points));

        if (undo)
            undoButton.setVisibility(View.VISIBLE);
        else
            undoButton.setVisibility(View.INVISIBLE);

        number_field_background.removeAllViews();
        number_field.removeAllViews();
        for (int i = 0; i < elements.length; i++) {
            for (int j = 0; j < elements[i].length; j++) {
                //background elements
                backgroundElements[i][j] = new Element(this);
                //backgroundElements[i][j].setVisibility(View.INVISIBLE);

                elements[i][j] = new Element(this);
                elements[i][j].setNumber(gameState.getNumber(i, j));

                elements[i][j].drawItem();
                if (elements[i][j].getNumber() >= SingleConst.Games.WINTHRESHOLD)
                    won2048 = true;
                RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(number_size, number_size);
                lp.setMarginStart(abstand + j * (number_size + abstand));
                lp.topMargin = abstand + i * (number_size + abstand);
                elements[i][j].setDPosition(lp.getMarginStart(), lp.topMargin);
                elements[i][j].setLayoutParams(lp);
                backgroundElements[i][j].setLayoutParams(lp);
                elements[i][j].updateFontSize();
                backgroundElements[i][j].setLayoutParams(lp);
                backgroundElements[i][j].setOnTouchListener(swipeListener);
                elements[i][j].setOnTouchListener(swipeListener);
                number_field_background.addView(backgroundElements[i][j]);
                number_field.addView(elements[i][j]);
            }
        }
        last_elements = deepCopy(elements);
        if (undo) {
            for (int i = 0; i < elements.length; i++) {
                for (int j = 0; j < elements[i].length; j++) {
                    last_elements[i][j].setNumber(gameState.getLastNumber(i, j));
                }
            }
        }
        if (newGame) {
            moved = true;
            addNumber();
            moved = true;
            addNumber();
            newGame = false;
        }
    }

    public void switchElementPositions(Element e1, Element e2) {
        int i = e1.getdPosX();
        int j = e1.getdPosY();

        e1.animateMoving = true;
        e1.setDPosition(e2.getdPosX(), e2.getdPosY());
        e2.animateMoving = false;
        e2.setDPosition(i, j);

    }

    public void setListener() {
        swipeListener = new GameGesture(this) {
            public boolean onSwipeTop() {
                Element[][] temp = deepCopy(elements);
                int temp_points = points;
                moved = false;
                Element s = new Element(getApplicationContext());

                for (int i = 0; i < elements.length; i++) {
                    s.number = elements[0][i].number;
                    s.posX = 0;
                    s.posY = i;


                    for (int j = 1; j < elements[i].length; j++) {
                        if (elements[j][i].number != 0 && (s.number == 0 || s.number == elements[j][i].number)) {
                            moved = true;
                            elements[j][i].setNumber(s.number + elements[j][i].number);
                            elements[s.posX][s.posY].setNumber(0);
                            switchElementPositions(elements[j][i], elements[s.posX][s.posY]);
                            Element z = elements[j][i];
                            elements[j][i] = elements[s.posX][s.posY];
                            elements[s.posX][s.posY] = z;
                            if (s.number != 0)
                                points += elements[s.posX][s.posY].number;
                            if (s.number != 0)
                                s.posX++;
                            j = s.posX;
                            s.number = elements[j][i].number;

                        } else if (elements[j][i].number != 0) {
                            s.number = elements[j][i].number;
                            s.posX = j;
                            s.posY = i;
                        }
                    }

                }
                for (int i = 0; i < elements.length; i++) {
                    s.number = elements[0][i].number;
                    s.posX = 0;
                    s.posY = i;


                    for (int j = 1; j < elements[i].length; j++) {
                        if (elements[j][i].number != 0 && s.number == 0) {
                            moved = true;
                            elements[j][i].setNumber(s.number + elements[j][i].number);
                            elements[s.posX][s.posY].setNumber(0);
                            switchElementPositions(elements[j][i], elements[s.posX][s.posY]);
                            Element z = elements[j][i];
                            elements[j][i] = elements[s.posX][s.posY];
                            elements[s.posX][s.posY] = z;
                            if (s.number != 0)
                                s.posX++;
                            j = s.posX;
                            s.number = elements[j][i].number;

                        } else if (s.number != 0) {
                            s.number = elements[j][i].number;
                            s.posX = j;
                            s.posY = i;
                        }
                    }

                }
                if (moved) {
                    gameStatistics.addMoves(1);
                    last_points = temp_points;
                    last_elements = temp;
                    undoButton.setVisibility(View.VISIBLE);
                    undo = true;
                }
                if (moved)
                    gameStatistics.moveT();
                addNumber();
                setDPositions(animationActivated);
                updateGameState();
                return false;
            }

            public boolean onSwipeRight() {
                Element[][] temp = deepCopy(elements);
                int temp_points = points;
                moved = false;
                Element s = new Element(getApplicationContext());
                for (int i = 0; i < elements.length; i++) {
                    s.number = elements[i][elements[i].length - 1].number;
                    s.posX = i;
                    s.posY = elements[i].length - 1;


                    for (int j = elements[i].length - 2; j >= 0; j--) {
                        if (elements[i][j].number != 0 && (s.number == 0 || s.number == elements[i][j].number)) {
                            moved = true;

                            elements[i][j].setNumber(s.number + elements[i][j].number);
                            elements[s.posX][s.posY].setNumber(0);
                            switchElementPositions(elements[i][j], elements[s.posX][s.posY]);
                            Element z = elements[i][j];
                            elements[i][j] = elements[s.posX][s.posY];
                            elements[s.posX][s.posY] = z;

                            if (s.number != 0)
                                points += elements[s.posX][s.posY].number;
                            if (s.number != 0)
                                s.posY--;
                            j = s.posY;
                            s.number = elements[i][j].number;
                        } else if (elements[i][j].number != 0) {
                            s.number = elements[i][j].number;
                            s.posX = i;
                            s.posY = j;
                        }
                    }

                }
                for (int i = 0; i < elements.length; i++) {
                    s.number = elements[i][elements[i].length - 1].number;
                    s.posX = i;
                    s.posY = elements[i].length - 1;


                    for (int j = elements[i].length - 2; j >= 0; j--) {
                        if (elements[i][j].number != 0 && s.number == 0) {
                            moved = true;

                            elements[i][j].setNumber(s.number + elements[i][j].number);
                            elements[s.posX][s.posY].setNumber(0);
                            switchElementPositions(elements[i][j], elements[s.posX][s.posY]);
                            Element z = elements[i][j];
                            elements[i][j] = elements[s.posX][s.posY];
                            elements[s.posX][s.posY] = z;


                            if (s.number != 0)
                                s.posY--;
                            j = s.posY;
                            s.number = elements[i][j].number;
                        } else if (s.number != 0) {
                            s.number = elements[i][j].number;
                            s.posX = i;
                            s.posY = j;
                        }
                    }

                }
                if (moved) {
                    gameStatistics.addMoves(1);
                    last_points = temp_points;
                    last_elements = temp;
                    undoButton.setVisibility(View.VISIBLE);
                    undo = true;
                }
                if (moved)
                    gameStatistics.moveR();
                addNumber();
                setDPositions(animationActivated);
                updateGameState();

                //es wurde nach rechts gewischt, hier den Code einfügen
                return false;
            }

            public boolean onSwipeLeft() {
                Element[][] temp = deepCopy(elements);
                int temp_points = points;
                moved = false;
                Element s = new Element(getApplicationContext());
                for (int i = 0; i < elements.length; i++) {
                    s.number = elements[i][0].number;
                    s.posX = i;
                    s.posY = 0;


                    for (int j = 1; j < elements[i].length; j++) {
                        if (elements[i][j].number != 0 && (s.number == 0 || s.number == elements[i][j].number)) {
                            moved = true;


                            elements[i][j].setNumber(s.number + elements[i][j].number);
                            elements[s.posX][s.posY].setNumber(0);
                            switchElementPositions(elements[i][j], elements[s.posX][s.posY]);
                            Element z = elements[i][j];
                            elements[i][j] = elements[s.posX][s.posY];
                            elements[s.posX][s.posY] = z;

                            if (s.number != 0)
                                points += elements[s.posX][s.posY].number;
                            if (s.number != 0)
                                s.posY++;
                            j = s.posY;
                            s.number = elements[i][j].number;
                        } else if (elements[i][j].number != 0) {
                            s.number = elements[i][j].number;
                            s.posX = i;
                            s.posY = j;
                        }
                    }

                }
                for (int i = 0; i < elements.length; i++) {
                    s.number = elements[i][0].number;
                    s.posX = i;
                    s.posY = 0;

                    for (int j = 1; j < elements[i].length; j++) {
                        if (elements[i][j].number != 0 && s.number == 0) {
                            moved = true;

                            elements[i][j].setNumber(s.number + elements[i][j].number);
                            elements[s.posX][s.posY].setNumber(0);
                            switchElementPositions(elements[i][j], elements[s.posX][s.posY]);
                            Element z = elements[i][j];
                            elements[i][j] = elements[s.posX][s.posY];
                            elements[s.posX][s.posY] = z;

                            if (s.number != 0)
                                s.posY++;
                            j = s.posY;
                            s.number = elements[i][j].number;
                        } else if (s.number != 0) {
                            s.number = elements[i][j].number;
                            s.posX = i;
                            s.posY = j;
                        }
                    }

                }
                if (moved) {
                    gameStatistics.addMoves(1);
                    last_points = temp_points;
                    last_elements = temp;
                    undoButton.setVisibility(View.VISIBLE);
                    undo = true;
                }
                if (moved)
                    gameStatistics.moveL();
                addNumber();
                setDPositions(animationActivated);
                updateGameState();
                //es wurde nach links gewischt, hier den Code einfügen
                return false;
            }

            public boolean onSwipeBottom() {
                Element[][] temp = deepCopy(elements);
                int temp_points = points;
                moved = false;
                Element s = new Element(getApplicationContext());
                for (int i = 0; i < elements.length; i++) {
                    s.number = elements[elements[i].length - 1][i].number;
                    s.posX = elements[i].length - 1;
                    s.posY = i;


                    for (int j = elements[i].length - 2; j >= 0; j--) {
                        if (elements[j][i].number != 0 && (s.number == 0 || s.number == elements[j][i].number)) {
                            moved = true;

                            elements[j][i].setNumber(s.number + elements[j][i].number);
                            elements[s.posX][s.posY].setNumber(0);
                            switchElementPositions(elements[j][i], elements[s.posX][s.posY]);
                            Element z = elements[j][i];
                            elements[j][i] = elements[s.posX][s.posY];
                            elements[s.posX][s.posY] = z;

                            if (s.number != 0)
                                points += elements[s.posX][s.posY].number;
                            if (s.number != 0)
                                s.posX--;
                            j = s.posX;
                            s.number = elements[j][i].number;
                        } else if (elements[j][i].number != 0) {
                            s.number = elements[j][i].number;
                            s.posX = j;
                            s.posY = i;
                        }
                    }

                }
                for (int i = 0; i < elements.length; i++) {
                    s.number = elements[elements[i].length - 1][i].number;
                    s.posX = elements[i].length - 1;
                    s.posY = i;


                    for (int j = elements[i].length - 2; j >= 0; j--) {
                        if (elements[j][i].number != 0 && s.number == 0) {
                            moved = true;

                            elements[j][i].setNumber(s.number + elements[j][i].number);
                            elements[s.posX][s.posY].setNumber(0);
                            switchElementPositions(elements[j][i], elements[s.posX][s.posY]);
                            Element z = elements[j][i];
                            elements[j][i] = elements[s.posX][s.posY];
                            elements[s.posX][s.posY] = z;

                            if (s.number != 0)
                                s.posX--;
                            j = s.posX;
                            s.number = elements[j][i].number;
                        } else if (s.number != 0) {
                            s.number = elements[j][i].number;
                            s.posX = j;
                            s.posY = i;
                        }
                    }

                }
                if (moved) {
                    gameStatistics.addMoves(1);
                    last_points = temp_points;
                    last_elements = temp;
                    undoButton.setVisibility(View.VISIBLE);
                    undo = true;
                }
                if (moved)
                    gameStatistics.moveD();
                addNumber();
                setDPositions(animationActivated);
                updateGameState();
                //es wurde nach unten gewischt, hier den Code einfügen
                return false;
            }

            public boolean nichts() {
                //es wurde keine wischrichtung erkannt, hier den Code einfügen
                return false;
            }
        };
        touch_field.setOnTouchListener(swipeListener);
        number_field.setOnTouchListener(swipeListener);
        for (int i = 0; i < elements.length; i++) {
            for (int j = 0; j < elements[i].length; j++) {
                elements[i][j].setOnTouchListener(swipeListener);
                backgroundElements[i][j].setOnTouchListener(swipeListener);
            }
        }
    }

    public void updateHighestNumber() {
        for (Element[] element : elements) {
            for (Element value : element) {
                if (highestNumber < value.number) {
                    highestNumber = value.number;
                    gameStatistics.setHighestNumber(highestNumber);
                }
            }
        }
    }

    public void check2048() {
        if (!won2048)
            for (Element[] element : elements) {
                for (Element value : element) {
                    if (value.number == SingleConst.Games.WINTHRESHOLD) {
                        SingleFunc.INSTANCE.saveStatisticsToFile(GameActivity.this, gameStatistics);
                        new AlertDialog.Builder(this)
                                .setTitle((this.getResources().getString(R.string.Titel_V_Message)))
                                .setMessage((this.getResources().getString(R.string.Winning_Message)))
                                .setNegativeButton((this.getResources().getString(R.string.No_Message)), (dialog, which) -> {
                                    onBackPressed();
                                    setupShowAdsInterstitial();
                                })
                                .setPositiveButton((this.getResources().getString(R.string.Yes_Message)), (dialog, which) -> {
                                    setupShowAdsInterstitial();
                                })
                                .setCancelable(false)
                                .create().show();
                        won2048 = true;
                    }
                }
            }
    }

    public void setDPositions(boolean animation) {
        long SCALINGSPEED = SingleConst.Games.INIT_SCALINGSPEED;
        long ADDINGSPEED = SingleConst.Games.INIT_ADDINGSPEED;
        long MOVINGSPEED = SingleConst.Games.INIT_MOVINGSPEED;
        boolean scale = true;
        if (!animation) {
            SCALINGSPEED = 1;
            ADDINGSPEED = 1;
            MOVINGSPEED = 1;
            scale = false;
        }
        for (Element[] i : elements) {
            for (Element j : i) {
                if (j.dPosX != j.getX()) {
                    if (j.animateMoving && animation) {
                        if (j.number != j.dNumber)
                            j.animate().x(j.dPosX).setDuration(MOVINGSPEED).setStartDelay(0).setInterpolator(new LinearInterpolator()).setListener(new MovingListener(j, scale)).start();
                        else
                            j.animate().x(j.dPosX).setDuration(MOVINGSPEED).setStartDelay(0).setInterpolator(new LinearInterpolator()).setListener(new MovingListener(j, false)).start();

                    } else {
                        if (!animation) {
                            ViewGroup.MarginLayoutParams lp1 = (ViewGroup.MarginLayoutParams) j.getLayoutParams();
                            lp1.leftMargin = j.dPosX;
                            j.setLayoutParams(lp1);
                            j.drawItem();
                        } else
                            j.animate().x(j.dPosX).setDuration(0).setStartDelay(MOVINGSPEED).setInterpolator(new LinearInterpolator()).setListener(new MovingListener(j, false)).start();

                    }

                }
                if (j.dPosY != j.getY()) {
                    if (j.animateMoving && animation) {
                        if (j.number != j.dNumber)
                            j.animate().y(j.dPosY).setDuration(MOVINGSPEED).setStartDelay(0).setInterpolator(new LinearInterpolator()).setListener(new MovingListener(j, scale)).start();
                        else
                            j.animate().y(j.dPosY).setDuration(MOVINGSPEED).setStartDelay(0).setInterpolator(new LinearInterpolator()).setListener(new MovingListener(j, false)).start();

                    } else {
                        if (!animation) {
                            ViewGroup.MarginLayoutParams lp1 = (ViewGroup.MarginLayoutParams) j.getLayoutParams();
                            lp1.topMargin = j.dPosY;
                            j.setLayoutParams(lp1);
                            j.drawItem();
                        } else
                            j.animate().y(j.dPosY).setDuration(0).setStartDelay(MOVINGSPEED).setInterpolator(new LinearInterpolator()).setListener(new MovingListener(j, false)).start();

                    }


                }
            }
        }
    }

    public void addNumber() {

        if (points > record) {
            record = points;
            gameStatistics.setRecord(record);
            textFieldRecord.setText(String.valueOf(record));
        }
        if (moved) {
            gameOver = false;
            moved = false;
            textFieldPoints.setText(String.valueOf(points));
            Element[] empty_fields = new Element[n * n];
            int counter = 0;
            for (Element[] element : elements) {
                for (Element value : element) {
                    if (value.number == 0) {
                        empty_fields[counter++] = value;
                    }
                }
            }
            if (counter > 0) {
                int index = (int) (Math.random() * counter);
                int number = 2;
                if (Math.random() > SingleConst.Games.PROPABILITYFORTWO)
                    number = 4;

                empty_fields[index].setNumber(number);
                empty_fields[index].drawItem();
                if (animationActivated) {
                    empty_fields[index].setAlpha(0);
                    empty_fields[index].animate().alpha(1).setInterpolator(new LinearInterpolator()).setStartDelay(SingleConst.Games.INIT_MOVINGSPEED).setDuration(SingleConst.Games.INIT_ADDINGSPEED).start();
                }
                if (counter == 1) {
                    gameOver = true;
                    for (int i = 0; i < elements.length; i++) {
                        for (int j = 0; j < elements[i].length; j++) {
                            if ((i + 1 < elements.length && elements[i][j].number == elements[i + 1][j].number) || (j + 1 < elements[i].length && elements[i][j].number == elements[i][j + 1].number)) {
                                gameOver = false;
                                break;
                            }
                        }
                    }
                }
            }
            updateGameState();

            if (gameOver) {
                gameOver();
            }
        }
        Log.i("number of elements", "" + number_field.getChildCount() + ", " + number_field_background.getChildCount());
    }

    public void gameOver() {
        Log.i("record", "" + record + ", " + gameStatistics.getRecord());
        SingleFunc.INSTANCE.saveStatisticsToFile(this, gameStatistics);
        new AlertDialog.Builder(this)
                .setTitle((this.getResources().getString(R.string.Titel_L_Message, points)))
                .setMessage(this.getResources().getString(R.string.Lost_Message, points))
                .setNegativeButton((this.getResources().getString(R.string.No_Message)), (dialog, which) -> {
                    createNewGame = true;
                    getIntent().putExtra(SingleConst.Extra.EXTRA_NEW, true);
                    initialize();
                    SingleFunc.INSTANCE.deleteStateFile(GameActivity.this, filename);
                    saveState = false;
                    GameActivity.this.onBackPressed();
                    setupShowAdsInterstitial();
                })
                .setPositiveButton((this.getResources().getString(R.string.Yes_Message)), (dialog, which) -> {
                    createNewGame();
                    setupShowAdsInterstitial();
                })
                .setCancelable(false)
                .create().show();
        Log.i("record", "danach");
    }

}