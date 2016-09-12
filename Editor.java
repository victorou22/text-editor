package editor;

import java.util.List;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Orientation;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.scene.input.MouseEvent;
import javafx.scene.control.ScrollBar;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Objects;

/**
* Text Editor Application
* @author Victor Ou
*
*Created according to the approximate specifications of 
* http://datastructur.es/sp16/materials/proj/proj2/proj2.html
*
* (Compile using a *.txt file name as the first command line argument. 
* If the file exists, it will be opened. If not, a blank document will be created.)
*
* A basic text editor that supports the following features:
* -Current position is denoted with a flashing cursor
* -Word wrap
* -Newlines
* -Backspace (Not delete though)
* -Open a file
*    The file name of the file to be opened should be given as the first command line argument. (mandatory)
* -Save a file (Ctrl/Cmd+S)
*     The file is written to the name of first command line argument.
* -Arrow keys for navigation
* -Mouse input (clicking moves the cursor as you would expect)
* -Window re-sizing
* -Vertical scrolling using scroll bar
* -Undo and redo (Ctrl/Cmd+Z and Ctrl/Cmd+Y)
* -Changing the font size by 4 points (Ctrl/Cmd+PLUS and Ctrl/Cmd+MINUS)
*/
    
public class Editor extends Application {
    private Cursor cursor;
    private ScrollBar scrollBar;
    private static final int STARTING_WINDOW_WIDTH = 500;
    private static final int STARTING_WINDOW_HEIGHT = 500;
    private static final double STARTING_TEXT_POSITION_X = 5.0;
    private static final double STARTING_TEXT_POSITION_Y = 0.0;
    private static int MARGIN = 5;
    private static int windowWidth;
    private static int windowHeight;
    private static int scrollOffset = 0;    //offset for the scroll bar
    public Group root;
    public Group textRoot;
    private TextStorage buffer;             //datastructure used to store the characters input
    private static String fileName;

    //* Constructor */
    public Editor() {
        buffer = new TextStorage(STARTING_TEXT_POSITION_X, STARTING_TEXT_POSITION_Y);
        
        windowWidth = STARTING_WINDOW_WIDTH;
        windowHeight = STARTING_WINDOW_HEIGHT;
    }
    
    public static double getTextWidth(Text text) {
        /** Calculates the width of Test objects */
        return Math.ceil(text.getLayoutBounds().getWidth());
    }
    
    private static int maxMinusMargin(int max) {
        /** Calculates the usable window width after subtracting the margin */
        return max - MARGIN;
    }
    
    /** An event handler that moves to cursor to wherever the mouse is clicked. */
    private class MouseClickEventHandler implements EventHandler<MouseEvent> {
        
        MouseClickEventHandler (Group root) {}

        @Override
        public void handle(MouseEvent mouseEvent) {            
            double mousePressedX = mouseEvent.getX();
            double mousePressedY = mouseEvent.getY();

            if (buffer.moveToClosestNode(mousePressedX, mousePressedY, scrollOffset)) {
                if (buffer.leftOfCurrText(mousePressedX)) {
                    cursor.updateCursor("BEFORE");
                } else {
                    cursor.updateCursor("AFTER");
                }
            } else {
                buffer.moveToLastNode();
                cursor.updateCursor("AFTER");
            } 
        }
    }
    
    /** An EventHandler to handle keys that get pressed. */
    private class KeyEventHandler implements EventHandler<KeyEvent> {
        
        KeyEventHandler (int windowWidth, int windowHeight) {}

        @Override
        public void handle(KeyEvent keyEvent) {
            if (keyEvent.getEventType() == KeyEvent.KEY_TYPED && !keyEvent.isShortcutDown()) {
                // Use the KEY_TYPED event rather than KEY_PRESSED for letter keys, because with
                // the KEY_TYPED event, javafx handles the "Shift" key and associated
                // capitalization.
                String characterTyped = keyEvent.getCharacter();
                if (Objects.equals(characterTyped, "\r")) {     //special handling for newlines
                    buffer.addCharToTextStorage(cursor.getX(), cursor.getY(), "\n", textRoot);
                    keyEvent.consume();
                    buffer.reformatText(maxMinusMargin(windowWidth), windowHeight);
                    cursor.updateCursor("ENTER");
                    checkSnapback();
                    scrollBar.setMax(buffer.totalHeightOfLines() - windowHeight);
                    buffer.clearRedo();
                } else if (characterTyped.length() > 0 && characterTyped.charAt(0) != 8) {
                    // Processing regular keypresses (letters, symbols, etc)
                    // Ignore control keys, which have non-zero length, as well as the backspace
                    // key, which is represented as a character of value = 8 on Windows.
                    buffer.addCharToTextStorage(cursor.getX(), cursor.getY(), characterTyped, textRoot);
                    keyEvent.consume();
                    buffer.reformatText(maxMinusMargin(windowWidth), windowHeight);
                    cursor.updateCursor("AFTER");
                    checkSnapback();
                    scrollBar.setMax(buffer.totalHeightOfLines() - windowHeight);
                    buffer.clearRedo();
                }
            }

            else if (keyEvent.getEventType() == KeyEvent.KEY_PRESSED) {
                // Arrow keys should be processed using the KEY_PRESSED event, because KEY_PRESSED
                // events have a code that we can check (KEY_TYPED events don't have an associated
                // KeyCode).
                KeyCode code = keyEvent.getCode();
                if (keyEvent.isShortcutDown()) {        //processing ctrl/cmd keypresses
                    if (code == KeyCode.S) {
                        writeFile(fileName);
                    } else if (code == KeyCode.EQUALS) {
                        buffer.changeFontSize(4, cursor);
                        buffer.reformatText(maxMinusMargin(windowWidth), windowHeight);
                        cursor.updateCursor("AFTER");
                    } else if (code == KeyCode.MINUS) {
                        buffer.changeFontSize(-4, cursor);
                        buffer.reformatText(maxMinusMargin(windowWidth), windowHeight);
                        cursor.updateCursor("AFTER");
                    } else if (code == KeyCode.Z) {
                        buffer.undoAction(textRoot);
                        buffer.reformatText(maxMinusMargin(windowWidth), windowHeight);
                        cursor.updateCursor("AFTER");
                    } else if (code == KeyCode.Y) {
                        buffer.redoAction(textRoot);
                        buffer.reformatText(maxMinusMargin(windowWidth), windowHeight);
                        cursor.updateCursor("AFTER");
                    }
                } else if (code == KeyCode.UP) {
                    cursor.moveCursorUp(scrollOffset);
                    checkSnapback();
                } else if (code == KeyCode.DOWN) {
                    cursor.moveCursorDown(scrollOffset);
                    checkSnapback();
                } else if (code == KeyCode.LEFT) {
                    cursor.moveCursorLeft();
                    checkSnapback();
                } else if (code == KeyCode.RIGHT) {
                    cursor.moveCursorRight();
                    checkSnapback();
                } else if (code == KeyCode.BACK_SPACE) {
                    buffer.deleteCharFromTextStorage(textRoot);
                    buffer.reformatText(maxMinusMargin(windowWidth), windowHeight);
                    cursor.updateCursor("AFTER");
                    buffer.clearRedo();
                }
            }
        }
    }
    
    private void checkSnapback() {
        /* Check after a key press if the cursor is out of screen.
         * If so, jump to the cursor */
        if (cursor.isCursorOutOfScreenAbove(scrollOffset)) {
            scrollBar.setValue(cursor.getY());
        } else if (cursor.isCursorOutOfScreenBelow(scrollOffset, windowHeight)) {
            int offset = (int) (Math.round(cursor.getY()) - windowHeight) + (int) Math.round(cursor.lineHeight());
            scrollBar.setValue(offset);
        }
    }
    
    private void writeFile(String outputFileName) {       
        buffer.writeFile(outputFileName);
    }
    
    private boolean readFile(String inputFileName) {
        try {
            // Check to make sure that the input file exists!
            if (!inputFileName.endsWith(".txt")) {
                System.out.println("Unable to open file name " + inputFileName);
                System.exit(1);
            }
            File inputFile = new File(inputFileName);
            if (!inputFile.exists()) {
                return false;
            }
            FileReader reader = new FileReader(inputFile);

            BufferedReader bufferedReader = new BufferedReader(reader);

            int intRead = -1;
            // Keep reading from the file input until read() returns -1, which means the end of the file was reached.
            
            while ((intRead = bufferedReader.read()) != -1) {
                // The integer read can be cast to a char, because we're assuming ASCII.
                char charRead = (char) intRead;
                buffer.addCharToTextStorage(0.0, 0.0, String.valueOf(charRead), textRoot); //the starting x and y do not matter because it will be reformatted                     
            }

            bufferedReader.close();
        } catch (FileNotFoundException fileNotFoundException) {
            System.out.println("File not found! Exception was: " + fileNotFoundException);
        } catch (IOException ioException) {
            System.out.println("Error when copying; exception was: " + ioException);
        }
        return true;
    }
    
    @Override
    public void start(Stage primaryStage) {
        // Create a Node that will be the parent of all things displayed on the screen.
        root = new Group();
        textRoot = new Group();
        root.getChildren().add(textRoot);
        // The Scene represents the window: its height and width will be the height and width
        // of the window displayed.
        Scene scene = new Scene(root, windowWidth, windowHeight, Color.WHITE);
        
        //Create cursor
        cursor = new Cursor(buffer, textRoot);
        
        //Create scroll bar
        scrollBar = new ScrollBar();
        scrollBar.setOrientation(Orientation.VERTICAL);
        scrollBar.setPrefHeight(windowHeight);        
        root.getChildren().add(scrollBar); 
        
        //ChangeListener for scroll bar
        scrollBar.valueProperty().addListener(new ChangeListener<Number>() {
            public void changed(
                ObservableValue<? extends Number> observableValue,
                Number oldValue,
                Number newValue) {
                scrollOffset = newValue.intValue();
                textRoot.setLayoutY(-1*scrollOffset);
            }
        });
        
        int usableScreenWidth = windowWidth - (int) scrollBar.getLayoutBounds().getWidth();
        MARGIN = windowWidth - usableScreenWidth;
        scrollBar.setLayoutX(usableScreenWidth);
        
        //Input text file
        List<String> inputs = getParameters().getRaw();
        if (inputs.isEmpty()) {
            System.out.println("Expected usage: java editor.Editor <source filename> where <source filename> is in the form of *.txt");
            System.exit(1);
        }
        fileName = inputs.get(0);
        
        //if the file is read in successfully, reformat the text and update the cursor
        if (readFile(fileName)) {
            buffer.reformatText(maxMinusMargin(windowWidth), windowHeight);
            cursor.updateCursor("AFTER");
        }
        
        scrollBar.setMin(0);
        scrollBar.setMax(buffer.totalHeightOfLines() - windowHeight);

        EventHandler<KeyEvent> keyEventHandler =
                new KeyEventHandler(windowWidth, windowHeight);
        // Register the event handler to be called for all KEY_PRESSED and KEY_TYPED events.
        scene.setOnKeyTyped(keyEventHandler);
        scene.setOnKeyPressed(keyEventHandler);
        
        scene.setOnMouseClicked(new MouseClickEventHandler(root));        
        
        primaryStage.setTitle("Text Editor");
        
        // ChangeListeners for window re-sizing
        scene.widthProperty().addListener(new ChangeListener<Number>() {
            @Override public void changed(
                    ObservableValue<? extends Number> observableValue,
                    Number oldScreenWidth,
                    Number newScreenWidth) {
                // Re-compute window width.
                windowWidth = newScreenWidth.intValue();
                buffer.reformatText(maxMinusMargin(windowWidth), windowHeight);
                int usableScreenWidth = windowWidth - (int) scrollBar.getLayoutBounds().getWidth();
                scrollBar.setLayoutX(usableScreenWidth);
            }
        });
        scene.heightProperty().addListener(new ChangeListener<Number>() {
            @Override public void changed(
                    ObservableValue<? extends Number> observableValue,
                    Number oldScreenHeight,
                    Number newScreenHeight) {
                windowHeight = newScreenHeight.intValue();
                scrollBar.setMax(buffer.totalHeightOfLines() - windowHeight);
                scrollBar.setPrefHeight(windowHeight);
            }
        });

        // This is boilerplate, necessary to setup the window where things are displayed.
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
