package editor;

import javafx.scene.shape.Rectangle;
import javafx.scene.Group;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;

/* Cursor class that handles all of the cursor aspects **/
public class Cursor {
    private static double STARTING_CURSOR_X = 5.0;
    private static double STARTING_CURSOR_Y = 0.0;
    private static double LINE_HEIGHT;
    private final Rectangle cursor;
    private TextStorage buffer;
    
    
    public Cursor(TextStorage buffer, Group root) {
        Text tempHeight = TextStorage.charToText(0, 0, "a");
        LINE_HEIGHT = tempHeight.getLayoutBounds().getHeight();
        this.buffer = buffer;
        cursor = new Rectangle(STARTING_CURSOR_X, STARTING_CURSOR_Y, 0.0, LINE_HEIGHT);
        this.makeCursor();
        root.getChildren().add(cursor);
    }
    
    /** An EventHandler to handle changing blinking of the cursor. */
    private class CursorBlinkEventHandler implements EventHandler<ActionEvent> {
        private boolean isVisible = false;

        CursorBlinkEventHandler() {
            // Flip the width of the cursor to simulate blinking
            changeWidth();
        }

        private void changeWidth() {
            if (isVisible) {
                cursor.setWidth(0.0);
            } else {
                cursor.setWidth(1.0);
            }
            isVisible = !isVisible;
        }

        @Override
        public void handle(ActionEvent event) {
            changeWidth();
        }
    }
    
    /* Makes the blinking cursor */
    private void makeCursor() {
        // Create a Timeline that will call the "handle" function of CursorBlinkEventHandler
        // every 0.5 second.
        final Timeline timeline = new Timeline();
        // The rectangle should continue blinking forever.
        timeline.setCycleCount(Timeline.INDEFINITE);
        CursorBlinkEventHandler cursorChange = new CursorBlinkEventHandler();
        KeyFrame keyFrame = new KeyFrame(Duration.millis(500), cursorChange);
        timeline.getKeyFrames().add(keyFrame);
        timeline.play();
    }
    
    public void updateCursor(String status) {    //updates the cursor after adding a Text with one Text width
        Text currText = buffer.getCurrText();
        
        switch(status)
        {
            case "ENTER":
                cursor.setX(STARTING_CURSOR_X);
                cursor.setY(currText.getY() + LINE_HEIGHT);
                break;
            case "AFTER":   //put the cursor after the text
                cursor.setX(currText.getX() + Editor.getTextWidth(currText));
                cursor.setY(currText.getY());
                break;
            case "BEFORE":
                cursor.setX(currText.getX());
                cursor.setY(currText.getY());
        }
    }
    
    public void changeCursorHeight(double height) {
        LINE_HEIGHT = height;
        cursor.setHeight(LINE_HEIGHT);
    }
    
    public double getX() {
        return cursor.getX();
    }
    
    public double getY() {
        return cursor.getY();
    }
    
    public void moveCursorUp() {
        if(buffer.moveToClosestNode(cursor.getX(), cursor.getY() - LINE_HEIGHT)) {                        
            this.updateCursor("AFTER");
        }
    }
    
    public void moveCursorDown() {
        if(buffer.moveToClosestNode(cursor.getX(), cursor.getY() + LINE_HEIGHT)) {
            this.updateCursor("AFTER");
        }
    }
    
    public void moveCursorLeft() {
        if(!buffer.isBeginning()) {
            if (buffer.isFirstCharOfLine() && cursor.getX() != STARTING_CURSOR_X) {
                cursor.setX(STARTING_CURSOR_X);
            } else {
                buffer.moveToPreviousNode();
                this.updateCursor("AFTER");
            }
        }
    }
    
    public void moveCursorRight() {
        if(!buffer.isEnd()) {
            if (cursor.getX() == STARTING_CURSOR_X && !buffer.isNewline()) {
                cursor.setX(STARTING_CURSOR_X + Editor.getTextWidth(buffer.getCurrText()));
            } else {
                buffer.moveToNextNode();
                if (buffer.isFirstCharOfLine()) {
                    this.updateCursor("BEFORE");
                } else {
                    this.updateCursor("AFTER");
                }
            }
        }
    }
}