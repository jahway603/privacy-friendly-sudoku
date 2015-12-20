package tu_darmstadt.sudoku.ui.view;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;

import java.util.LinkedList;

import tu_darmstadt.sudoku.controller.GameController;
import tu_darmstadt.sudoku.controller.Symbol;
import tu_darmstadt.sudoku.game.GameCell;
import tu_darmstadt.sudoku.game.ICellAction;
import tu_darmstadt.sudoku.game.listener.IHighlightChangedListener;

/**
 * Created by Timm Lippert on 11.11.2015.
 */
public class SudokuFieldLayout extends RelativeLayout implements IHighlightChangedListener {

    private GameController gameController;
    private int sectionHeight;
    private int sectionWidth;
    private int gameCellWidth;
    private int gameCellHeight;
    private SharedPreferences settings;

    private Paint p = new Paint();

    public SudokuCellView [][] gamecells;
    AttributeSet attrs;

    public SudokuFieldLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.attrs=attrs;
        setBackgroundColor(Color.argb(255, 200, 200, 200));
    }

    public void setSettingsAndGame(SharedPreferences sharedPref, GameController gc) {

        if (sharedPref == null) throw new IllegalArgumentException("SharedPreferences may not be null.");
        if (gc == null) throw new IllegalArgumentException("GameController may not be null.");

        settings = sharedPref;
        gameController = gc;
        gameController.registerHighlightChangedListener(this);

        gamecells = new SudokuCellView[gc.getSize()][gc.getSize()];

        OnTouchListener listener = new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(v instanceof SudokuCellView) {

                    SudokuCellView scv = ((SudokuCellView) v);

                    int row = scv.getRow();
                    int col = scv.getCol();

                    gameController.selectCell(row, col);
                }
                return false;
            }
        };

        sectionHeight = gameController.getSectionHeight();
        sectionWidth = gameController.getSectionWidth();

        for (int i = 0; i < gameController.getSize(); i++) {
            for (int j = 0; j < gameController.getSize(); j++) {
                gamecells[i][j] = new SudokuCellView(getContext(), attrs);
                gamecells[i][j].setOnTouchListener(listener);
                addView(gamecells[i][j]);
            }
        }
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if(gameController == null) return;

        p.setColor(Color.BLACK);
        p.setStrokeWidth(2);

        int horizontalSections = gameController.getSize() / sectionWidth;
        for(int i = 0; i <= horizontalSections; i++) {
            for(int j = -2; j < 2; j++) {
                canvas.drawLine((i * getWidth() / horizontalSections) + j, 0, (i * getWidth() / horizontalSections) + j, getHeight(), p);
            }
        }
        int verticalSections = (gameController.getSize()/sectionHeight);
        for(int i = 0; i <= verticalSections; i++) {
            for(int j = -2; j < 2; j++) {
                canvas.drawLine(0, (i * getHeight() / verticalSections) + j, getHeight(), (i * getHeight() / verticalSections) + j, p);
            }
        }
    }

    public void setSymbols(Symbol s) {
        for(int i = 0; i < gamecells.length ; i++) {
            for(int j = 0; j < gamecells[i].length; j++) {
                gamecells[i][j].setSymbols(s);
            }
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed,l,t,r,b);

        if(changed && gameController != null) {
            gameCellWidth = (Math.min(r-l, b-t)) / gameController.getSize();
            gameCellHeight = (Math.min(r-l, b-t)) / gameController.getSize();

            for (int i = 0; i < gameController.getSize(); i++) {
                for (int j = 0; j < gameController.getSize(); j++) {
                    gamecells[i][j].setValues(gameCellWidth, gameCellHeight, sectionHeight, sectionWidth, gameController.getGameCell(i, j),gameController.getSize());
                }
            }
        }
    }

    @Override
    public void onHighlightChanged() {

        final int row = gameController.getSelectedRow();
        final int col = gameController.getSelectedCol();

        // Reset everything
        for(int i = 0; i < gameController.getSize(); i++) {
            for(int j = 0; j < gameController.getSize(); j++) {
                gamecells[i][j].setHighlightType(CellHighlightTypes.Default);
            }
        }

        // Set connected Fields
        if(gameController.isValidCellSelected()) {
            //String syncConnPref = sharedPref.getString(SettingsActivity., "");
            final boolean highlightConnectedRow = settings.getBoolean("pref_highlight_rows", true);
            final boolean highlightConnectedColumn = settings.getBoolean("pref_highlight_cols", true);
            final boolean highlightConnectedSection = settings.getBoolean("pref_highlight_secs", true);

            for (GameCell c : gameController.getConnectedCells(row, col, highlightConnectedRow, highlightConnectedColumn, highlightConnectedSection)) {
                gamecells[c.getRow()][c.getCol()].setHighlightType(CellHighlightTypes.Connected);
            }
        }

        // highlight values
        final boolean highlightValues = settings.getBoolean("pref_highlight_vals", true);
        final boolean highlightNotes = settings.getBoolean("pref_highlight_notes", true);

        if(gameController.isValueHighlighted()) {
            for(GameCell c : gameController.actionOnCells(new ICellAction<LinkedList<GameCell>>() {
                @Override
                public LinkedList<GameCell> action(GameCell gc, LinkedList<GameCell> existing) {
                    if ((gameController.getHighlightedValue() == gc.getValue() && highlightValues)
                            || (gc.getNotes()[gameController.getHighlightedValue() - 1] && highlightNotes)) {
                        existing.add(gc);
                    }
                    return existing;
                }
            }, new LinkedList<GameCell>())) {
                gamecells[c.getRow()][c.getCol()].setHighlightType(CellHighlightTypes.Value_Highlighted);
            }
        }

        // Highlight selected/ current cell either green or red
        if(row != -1 && col != -1) {
            GameCell gc = gameController.getGameCell(row, col);
            if (gc.isFixed()) {
                gamecells[gc.getRow()][gc.getCol()].setHighlightType(CellHighlightTypes.Error);
            } else {
                gamecells[gc.getRow()][gc.getCol()].setHighlightType(CellHighlightTypes.Selected);
            }
        }
    }
}
