package editor;

import java.util.List;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.VPos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Objects;


public class Editor extends Application {
    private final Rectangle cursor;
    private static final int STARTING_FONT_SIZE = 12;
    private static int fontSize = STARTING_FONT_SIZE;
    private static final int MARGIN = 5;
    private static String fontName = "Verdana";
    private static double currXPos = MARGIN;       //xPos of the cursor
    private static double currYPos = 0.0;       //yPos of the cursor
    private static int windowWidth;
    private static int windowHeight;
    public Group root;
    public Group textRoot;
    private FastLinkedList buffer;       //buffer used to store the characters input

    //* Constructor */
    public Editor() {
        Text tempHeight = charToText(currXPos, currYPos, "a");
        double charHeight = tempHeight.getLayoutBounds().getHeight();
        cursor = new Rectangle(currXPos, currYPos, 0.0, charHeight);
        buffer = new FastLinkedList(charHeight);
        
        windowWidth = 500;
        windowHeight = 500;
    }
    
    /** An EventHandler to handle keys that get pressed. */
    private class KeyEventHandler implements EventHandler<KeyEvent> {
        private static final double STARTING_TEXT_POSITION_X = 5.0;
        private static final double STARTING_TEXT_POSITION_Y = 0.0;

        /** The Text to display on the screen. */
        private Text displayText = new Text(STARTING_TEXT_POSITION_X, STARTING_TEXT_POSITION_Y, "");

        KeyEventHandler(int windowWidth, int windowHeight) {
            // Always set the text origin to be VPos.TOP! Setting the origin to be VPos.TOP means
            // that when the text is assigned a y-position, that position corresponds to the
            // highest position across all letters (for example, the top of a letter like "I", as
            // opposed to the top of a letter like "e"), which makes calculating positions much
            // simpler!
            //displayText.setTextOrigin(VPos.TOP);
            //displayText.setFont(Font.font(fontName, fontSize));

            // All new Nodes need to be added to the root in order to be displayed.
            //textRoot.getChildren().add(displayText);
        }

        @Override
        public void handle(KeyEvent keyEvent) {
            if (keyEvent.getEventType() == KeyEvent.KEY_TYPED) {
                // Use the KEY_TYPED event rather than KEY_PRESSED for letter keys, because with
                // the KEY_TYPED event, javafx handles the "Shift" key and associated
                // capitalization.
                String characterTyped = keyEvent.getCharacter();
                if (Objects.equals(characterTyped, "\r")) {
                    Text toBeAdded = charToText(currXPos, currYPos, characterTyped);
                    currXPos = 5.0;
                    currYPos += toBeAdded.getLayoutBounds().getHeight()/2;      //newlines are double height
                    buffer.addChar(toBeAdded);
                    keyEvent.consume();
                    buffer.reformatText(5.0, 0.0, maxMinusMargin(windowWidth), maxMinusMargin(windowHeight));
                    updateCursor();
                } else if (characterTyped.length() > 0 && characterTyped.charAt(0) != 8) {
                    // Ignore control keys, which have non-zero length, as well as the backspace
                    // key, which is represented as a character of value = 8 on Windows.
                    Text toBeAdded = charToText(currXPos, currYPos, characterTyped);
                    currXPos += getTextWidth(toBeAdded);        //increase the x position by the width
                    buffer.addChar(toBeAdded);
                    textRoot.getChildren().add(toBeAdded);
                    keyEvent.consume();
                    buffer.reformatText(5.0, 0.0, maxMinusMargin(windowWidth), maxMinusMargin(windowHeight));
                    updateCursor();
                }

            } else if (keyEvent.getEventType() == KeyEvent.KEY_PRESSED) {
                // Arrow keys should be processed using the KEY_PRESSED event, because KEY_PRESSED
                // events have a code that we can check (KEY_TYPED events don't have an associated
                // KeyCode).
                KeyCode code = keyEvent.getCode();
                if (code == KeyCode.UP) {
                    fontSize += 5;
                    displayText.setFont(Font.font(fontName, fontSize));
                    centerText();
                } else if (code == KeyCode.DOWN) {
                    fontSize = Math.max(0, fontSize - 5);
                    displayText.setFont(Font.font(fontName, fontSize));
                    centerText();
                } else if (code == KeyCode.LEFT) {
                    if(currXPos <= STARTING_TEXT_POSITION_X) {
                        return;
                    }
                    Text leftText = buffer.moveToPreviousNode();
                    currXPos -= Math.ceil(leftText.getLayoutBounds().getWidth());
                    updateCursor();
                } else if (code == KeyCode.RIGHT) { //need to implement end of text
                    Text rightText = buffer.moveToNextNode();
                    currXPos += Math.ceil(rightText.getLayoutBounds().getWidth());
                    updateCursor();
                } else if (code == KeyCode.BACK_SPACE) {
                    Text deleted = buffer.deleteChar();
                    currXPos -= getTextWidth(deleted);
                    textRoot.getChildren().remove(deleted);
                    buffer.reformatText(5.0, 0.0, maxMinusMargin(windowWidth), maxMinusMargin(windowHeight));
                    updateCursor();
                    //reflow
                }
            }
        }

        private void centerText() {
            // Figure out the size of the current text.
            double textHeight = displayText.getLayoutBounds().getHeight();
            double textWidth = displayText.getLayoutBounds().getWidth();

            // Calculate the position so that the text will be centered on the screen.
            double textTop = 0;
            double textLeft = 5;

            // Re-position the text.
            displayText.setX(textLeft);
            displayText.setY(textTop);

            // Make sure the text appears in front of any other objects you might add.
            displayText.toFront();
        }
    }
    
    private static Text charToText(double xPos, double yPos, String c) {                 //converts the char to Text and applies all necessary modifications
        Text toBeAdded = new Text(xPos, yPos, c);
        toBeAdded.setTextOrigin(VPos.TOP);
        toBeAdded.setFont(Font.font(fontName, fontSize));
        toBeAdded.toFront();
        return toBeAdded;
    }
    
    private static int maxMinusMargin(int max) {
        return max - MARGIN;
    }
    
    private void readFile(String inputFileName) {
        try {
            File inputFile = new File(inputFileName);
            // Check to make sure that the input file exists!
            if (!inputFile.exists()) {
                System.out.println("Unable to copy because file with name " + inputFileName
                    + " does not exist");
                return;
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
                Text toBeAdded = charToText(currXPos, currYPos, String.valueOf(charRead));                      //set text                      
                currXPos += getTextWidth(toBeAdded);                  //increase the x position by the width 
                buffer.addChar(toBeAdded);
                textRoot.getChildren().add(toBeAdded);
            }

            // Close the reader and writer.
            bufferedReader.close();
        } catch (FileNotFoundException fileNotFoundException) {
            System.out.println("File not found! Exception was: " + fileNotFoundException);
        } catch (IOException ioException) {
            System.out.println("Error when copying; exception was: " + ioException);
        }
    }
    
    private double getTextWidth(Text text) {
        return Math.ceil(text.getLayoutBounds().getWidth());
    }
    
    /* Updates the position of the cursor **/
    private void updateCursor() {
        currXPos = buffer.getCurrTextX() + getTextWidth(buffer.getCurrText());
        currYPos = buffer.getCurrTextY();        
        cursor.setX(currXPos);
        cursor.setY(currYPos);
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
    public void makeCursor() {
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
    
    @Override
    public void start(Stage primaryStage) {
        // Create a Node that will be the parent of all things displayed on the screen.
        root = new Group();
        textRoot = new Group();
        root.getChildren().add(textRoot);
        // The Scene represents the window: its height and width will be the height and width
        // of the window displayed.
        Scene scene = new Scene(root, windowWidth, windowHeight, Color.WHITE);
        
        //Input text file
        List<String> inputs = getParameters().getRaw();
        
        //Testing readFile
        /*
        try{
            readFile(inputs.get(0));
            buffer.print();
        } catch (Exception e) {
            System.out.println(e);
        }
        **/

        // To get information about what keys the user is pressing, create an EventHandler.
        // EventHandler subclasses must override the "handle" function, which will be called
        // by javafx.
        EventHandler<KeyEvent> keyEventHandler =
                new KeyEventHandler(windowWidth, windowHeight);
        // Register the event handler to be called for all KEY_PRESSED and KEY_TYPED events.
        scene.setOnKeyTyped(keyEventHandler);
        scene.setOnKeyPressed(keyEventHandler);
        
        textRoot.getChildren().add(cursor);
        makeCursor();
        primaryStage.setTitle("Text Editor");
        
        // Register listeners that resize Allen when the window is re-sized.
        // We're using some new syntax here to create a ChangeListener with an overridden
        // changed() method; this is called instantiating an "anonymous class."  If you're curious
        // to learn more about this syntax, try Googling "Java anonymous class".  Beware that
        // IntelliJ sometimes collapses code blocks like this! If this happens, you can click on
        // the "+" icon that's to the left of the code (and to the right of the line numbers) to
        // expand the code again.
        scene.widthProperty().addListener(new ChangeListener<Number>() {
            @Override public void changed(
                    ObservableValue<? extends Number> observableValue,
                    Number oldScreenWidth,
                    Number newScreenWidth) {
                // Re-compute window width.
                windowWidth = newScreenWidth.intValue();
                buffer.reformatText(5.0, 0.0, windowWidth, windowHeight);
            }
        });
        scene.heightProperty().addListener(new ChangeListener<Number>() {
            @Override public void changed(
                    ObservableValue<? extends Number> observableValue,
                    Number oldScreenHeight,
                    Number newScreenHeight) {
                windowHeight = newScreenHeight.intValue();
                buffer.reformatText(5.0, 0.0, windowWidth, windowHeight);
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
