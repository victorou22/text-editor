package editor;

import java.util.List;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Objects;

/** 
Text Editor Application
Written by Victor Ou
*/
    
public class Editor extends Application {
    private Cursor cursor;
    private static final int STARTING_WINDOW_WIDTH = 500;
    private static final int STARTING_WINDOW_HEIGHT = 500;
    private static final double STARTING_TEXT_POSITION_X = 5.0;
    private static final double STARTING_TEXT_POSITION_Y = 0.0;
    private static final int MARGIN = 5;
    private static int windowWidth;
    private static int windowHeight;
    public Group root;
    public Group textRoot;
    private TextStorage buffer;       //buffer used to store the characters input
    private static String fileName;

    //* Constructor */
    public Editor() {
        buffer = new TextStorage(STARTING_TEXT_POSITION_X, STARTING_TEXT_POSITION_Y);
        
        windowWidth = STARTING_WINDOW_WIDTH;
        windowHeight = STARTING_WINDOW_HEIGHT;
    }
    
    public static double getTextWidth(Text text) {
        return Math.ceil(text.getLayoutBounds().getWidth());
    }
    
    /** An event handler that displays the current position of the mouse whenever it is clicked. */
    private class MouseClickEventHandler implements EventHandler<MouseEvent> {

        MouseClickEventHandler(Group root) {
        }


        @Override
        public void handle(MouseEvent mouseEvent) {            
            double mousePressedX = mouseEvent.getX();
            double mousePressedY = mouseEvent.getY();

            if (buffer.moveToClosestNode(mousePressedX, mousePressedY)) {
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
        KeyEventHandler(int windowWidth, int windowHeight) {
        }

        @Override
        public void handle(KeyEvent keyEvent) {
            if (keyEvent.getEventType() == KeyEvent.KEY_TYPED && !keyEvent.isShortcutDown()) {
                // Use the KEY_TYPED event rather than KEY_PRESSED for letter keys, because with
                // the KEY_TYPED event, javafx handles the "Shift" key and associated
                // capitalization.
                String characterTyped = keyEvent.getCharacter();
                if (Objects.equals(characterTyped, "\r")) {
                    buffer.addCharToTextStorage(cursor.getX(), cursor.getY(), "\n", textRoot);
                    keyEvent.consume();
                    buffer.reformatText(maxMinusMargin(windowWidth), maxMinusMargin(windowHeight));
                    cursor.updateCursor("ENTER");
                } else if (characterTyped.length() > 0 && characterTyped.charAt(0) != 8) {
                    // Ignore control keys, which have non-zero length, as well as the backspace
                    // key, which is represented as a character of value = 8 on Windows.
                    buffer.addCharToTextStorage(cursor.getX(), cursor.getY(), characterTyped, textRoot);
                    keyEvent.consume();
                    buffer.reformatText(maxMinusMargin(windowWidth), maxMinusMargin(windowHeight));
                    cursor.updateCursor("AFTER");
                }
            }

            else if (keyEvent.getEventType() == KeyEvent.KEY_PRESSED) {
                // Arrow keys should be processed using the KEY_PRESSED event, because KEY_PRESSED
                // events have a code that we can check (KEY_TYPED events don't have an associated
                // KeyCode).
                KeyCode code = keyEvent.getCode();
                if (keyEvent.isShortcutDown()) {
                    if (code == KeyCode.S) {
                        writeFile(fileName);
                    } else if (code == KeyCode.EQUALS) {
                        buffer.changeFontSize(4, cursor);
                        buffer.reformatText(maxMinusMargin(windowWidth), maxMinusMargin(windowHeight));
                        cursor.updateCursor("AFTER");
                    } else if (code == KeyCode.MINUS) {
                        buffer.changeFontSize(-4, cursor);
                        buffer.reformatText(maxMinusMargin(windowWidth), maxMinusMargin(windowHeight));
                        cursor.updateCursor("AFTER");
                    }
                } else if (code == KeyCode.UP) {
                    cursor.moveCursorUp();
                } else if (code == KeyCode.DOWN) {
                    cursor.moveCursorDown();
                } else if (code == KeyCode.LEFT) {
                    cursor.moveCursorLeft();
                } else if (code == KeyCode.RIGHT) {
                    cursor.moveCursorRight();
                } else if (code == KeyCode.BACK_SPACE) {
                    buffer.deleteCharFromTextStorage(textRoot);
                    buffer.reformatText(maxMinusMargin(windowWidth), maxMinusMargin(windowHeight));
                    cursor.updateCursor("AFTER");
                }
            }
        }
    }
        
    private static int maxMinusMargin(int max) {
        return max - MARGIN;
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
            // It's good practice to read files using a buffered reader.  A buffered reader reads
            // big chunks of the file from the disk, and then buffers them in memory.  Otherwise,
            // if you read one character at a time from the file using FileReader, each character
            // read causes a separate read from disk.  You'll learn more about this if you take more
            // CS classes, but for now, take our word for it!
            BufferedReader bufferedReader = new BufferedReader(reader);

            int intRead = -1;
            // Keep reading from the file input until read() returns -1, which means the end of the file
            // was reached.
            
            while ((intRead = bufferedReader.read()) != -1) {
                // The integer read can be cast to a char, because we're assuming ASCII.
                char charRead = (char) intRead;
                buffer.addCharToTextStorage(0.0, 0.0, String.valueOf(charRead), textRoot); //the starting x and y do not matter because it will be reformatted                     
            }
            // Close the reader and writer.
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
        
        //Input text file
        List<String> inputs = getParameters().getRaw();
        if (inputs.isEmpty()) {
            System.out.println("Expected usage: editor.Editor <source filename>");
            System.exit(1);
        }
        fileName = inputs.get(0);
        
        //if the file is read in successfully, reformat the text and update the cursor
        if (readFile(fileName)) {
            buffer.reformatText(maxMinusMargin(windowWidth), maxMinusMargin(windowHeight));
            cursor.updateCursor("AFTER");
        }

        // To get information about what keys the user is pressing, create an EventHandler.
        // EventHandler subclasses must override the "handle" function, which will be called
        // by javafx.
        EventHandler<KeyEvent> keyEventHandler =
                new KeyEventHandler(windowWidth, windowHeight);
        // Register the event handler to be called for all KEY_PRESSED and KEY_TYPED events.
        scene.setOnKeyTyped(keyEventHandler);
        scene.setOnKeyPressed(keyEventHandler);
        
        scene.setOnMouseClicked(new MouseClickEventHandler(root));
        
        primaryStage.setTitle("Text Editor");
        
        // Register listeners that resize the horizontal edge when the window is re-sized.
        scene.widthProperty().addListener(new ChangeListener<Number>() {
            @Override public void changed(
                    ObservableValue<? extends Number> observableValue,
                    Number oldScreenWidth,
                    Number newScreenWidth) {
                // Re-compute window width.
                windowWidth = newScreenWidth.intValue();
                buffer.reformatText(maxMinusMargin(windowWidth), maxMinusMargin(windowHeight));
            }
        });

        // This is boilerplate, necessary to setup the window where things are displayed.
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
        //testReadFile
    }
}
