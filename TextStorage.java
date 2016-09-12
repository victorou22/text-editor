package editor;

import javafx.scene.text.Text;
import javafx.scene.text.Font;
import javafx.scene.Group;
import java.util.Objects;
import javafx.geometry.VPos;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.ArrayDeque;

/** TextStorage is a LinkedList data structure meant for storage of 
* Text Objects. A HashMap is used for quick access to the lines of text.
* A stack is used for the implementation of undo and redo. */
public class TextStorage {
    private class Node {
        Text t = new Text(5.0, 0.0, "");
        Node prev;
        Node next;
    }
    
    private class TextEvent {   //Text input/deletion information stored for undo/redo operations
        Text text;
        Node nodeOfLastEvent;
        String action;
        
        public TextEvent(Text t, Node n, String s) {
            text = t;
            nodeOfLastEvent = n;
            action = s;
        }
    }
    
    private Node sentinel;
    private Node currentNode;   //points to the current Node where the cursor is
    private HashMap<Integer, Node> lineNumbers; //HashMap that stores the first Node of each line for fast cursor access
    private static double LINE_HEIGHT;
    private static double STARTING_TEXT_POSITION_X;
    private static double STARTING_TEXT_POSITION_Y;
    private static final int STARTING_FONT_SIZE = 12;
    private static int fontSize = STARTING_FONT_SIZE;
    private static String fontName = "Verdana";
    private static ArrayDeque<TextEvent> undo;
    private static ArrayDeque<TextEvent> redo;
    private static boolean undoing = false;
    
    /** Constructor */
    public TextStorage(double startingX, double startingY) {
        sentinel = new Node();
        currentNode = sentinel;
        sentinel.prev = sentinel;
        sentinel.next = sentinel;
        Text tempHeight = charToText(0, 0, "a");
        LINE_HEIGHT = tempHeight.getLayoutBounds().getHeight();
        STARTING_TEXT_POSITION_X = startingX;
        STARTING_TEXT_POSITION_Y = startingY;
        lineNumbers = new HashMap<Integer, Node>();
        undo = new ArrayDeque<TextEvent>(100);
        redo = new ArrayDeque<TextEvent>(100);
    }
    
    public boolean isBeginning() {
        return currentNode == sentinel;
    }
    
    public boolean isEnd() {
        return currentNode.next == sentinel;
    }
    
    public Text getCurrText() {
        return currentNode.t;
    }
    
    public double totalHeightOfLines() {
        return Math.ceil(LINE_HEIGHT*(lineNumbers.size()));
    }
    
    private void addChar(Text text) {   
        /* Adds Text to the end of the list. Undo information is stored. */
        Node n = new Node();
        n.t = text;
        n.prev = currentNode;
        n.next = currentNode.next;
        currentNode.next.prev = n;
        currentNode.next = n;
        currentNode = n;
        
        if (!undoing) {
            if (undo.size() == 100) {
                undo.removeLast();
            }
            undo.push(new TextEvent(text, currentNode, "ADD"));
        }
    }
    
    public boolean isFirstCharOfLine() {
        return lineNumbers.containsValue(currentNode);
    }
    
    private boolean isFirstCharOfLine(Node node) {
        return lineNumbers.containsValue(node);
    }
    
    public void moveToPreviousNode() {  
        /* If there is a newline before on the previous line, skip it */
        if (currentNode.prev.t.getText().equals("\n") && !this.isFirstCharOfLine(currentNode.prev)) {
            currentNode = currentNode.prev.prev;
        } else {    
            currentNode = currentNode.prev;
        }
    }
    
    public void moveToNextNode() {  
        /* If there is a newline next on the same line, skip it */
        if (currentNode.next.t.getText().equals("\n") && !this.isFirstCharOfLine(currentNode.next)) {
            currentNode = currentNode.next.next;
        } else {
            currentNode = currentNode.next;
        }
    }
    
    public void moveToLastNode() {
        currentNode = sentinel.prev;
    }
    
    public boolean isNewline() {
        return currentNode.t.getText().equals("\n");
    }
    
    public boolean moveToClosestNode(double xPos, double yPos, int scrollOffset) {
        /* Returns Node closest to input coordinates or null if invalid coordinates or position is after the last line possible */
        int lineNum = calcLineNumber(yPos + scrollOffset);
        Node nodeInLine = lineNumbers.get(lineNum);
        if (nodeInLine == null) {
            return false;
        } else {
            while (nodeInLine.t.getX() + Editor.getTextWidth(nodeInLine.t) < xPos) {
                if ((nodeInLine.t.getText().equals("\n")) || (nodeInLine.next == sentinel) || (nodeInLine.next.t.getText().equals("\n"))) {
                    break;
                }
                nodeInLine = nodeInLine.next;
            }
            currentNode = nodeInLine;
        }
        return true;
    }
    
    private Text deleteChar() {
        Text ret = currentNode.t;
        if (!undoing) {
            if (undo.size() == 100) {
                undo.removeLast();
            }
            undo.push(new TextEvent(currentNode.t, currentNode.prev, "DELETE"));
        }
        
        currentNode.prev.next = currentNode.next;
        currentNode.next.prev = currentNode.prev;
        currentNode = currentNode.prev;
        return ret;
    }
    
    public void addCharToTextStorage(double x, double y, String c, Group root) {
        Text toBeAdded = charToText(x, y, c);
        this.addChar(toBeAdded);
        if (!c.equals("\n")) {
            root.getChildren().add(toBeAdded);
        }
    }
    
    public void deleteCharFromTextStorage(Group root) {
        Text deleted = this.deleteChar();
        root.getChildren().remove(deleted);
    }
    
    public boolean leftOfCurrText(double xPos) {
        return (currentNode.t.getX() + 0.5*Editor.getTextWidth(currentNode.t) > xPos);
    }
    
    public static Text charToText(double xPos, double yPos, String c) {
        /* Converts the char to Text and applies all necessary modifications */
            Text toBeAdded = new Text(xPos, yPos, c);
            toBeAdded.setTextOrigin(VPos.TOP);
            toBeAdded.setFont(Font.font(fontName, fontSize));
            toBeAdded.toFront();
            return toBeAdded;
        }
    
    public void changeFontSize(int increment, Cursor cursor) {
        /* Could have optimized by implementing in reformatText, but using two loops for simplicity */
        if (increment >= 0) {
            fontSize += increment;
        } else {    //If negative increment, cannot go below zero
            fontSize = Math.max(0, fontSize + increment);
        }
        Text tempHeight = charToText(0, 0, "a");
        LINE_HEIGHT = tempHeight.getLayoutBounds().getHeight();
        cursor.changeCursorHeight(LINE_HEIGHT);
        Node runner = sentinel.next;
        while (runner != sentinel) {
            runner.t.setFont(Font.font(fontName, fontSize));
            runner = runner.next;
        }
    }
    
    private int calcLineNumber(double y) {
        return (int) (y/LINE_HEIGHT);
    }
    
    public void undoAction(Group root) {
        if (!undo.isEmpty()) {
            undoing = true;
            TextEvent event = undo.pop();
            redo.push(event);
            currentNode = event.nodeOfLastEvent;
            if (event.action.equals("ADD")) {
                this.deleteCharFromTextStorage(root);
            } else if (event.action.equals("DELETE")) {
                Text toBeAdded = event.text;
                this.addChar(toBeAdded);
                if (!toBeAdded.getText().equals("\n")) {
                    root.getChildren().add(toBeAdded);
                }
            }
        }
        undoing = false;
    }
    
    public void clearRedo() {
        redo.clear();
    }
    
    public void redoAction(Group root) {
        if (!redo.isEmpty()) {
            undoing = true;
            TextEvent event = redo.pop();
            undo.push(event);
            if (event.action.equals("ADD")) {
                Text toBeAdded = event.text;
                this.addChar(toBeAdded);
                if (!toBeAdded.getText().equals("\n")) {
                    root.getChildren().add(toBeAdded);
                }
            } else if (event.action.equals("DELETE")) {
                currentNode = event.nodeOfLastEvent.next;
                this.deleteCharFromTextStorage(root);
            }
        }
        undoing = false;
    }
    
    public void reformatText(double xMax, double yMax) {
        /* Recalculates all of the text positions and textwraps. xMax and yMax are the window limits */
        Node prevSpace = null;  //Has there been a space on this line yet?
        lineNumbers.clear();
        boolean isStartNextLine = true;
        Node runner = sentinel.next;
        double currX = STARTING_TEXT_POSITION_X;
        double currY = STARTING_TEXT_POSITION_Y;
        while (runner != sentinel) {
            Text text = runner.t;
            // Keep track of the words between spaces
            if (Objects.equals(text.getText(), " ")) {
                prevSpace = runner;
            }
            // If reach end of line, newline
                if (currX + Editor.getTextWidth(text) >= xMax) { 
                    /* If there had a been a space earlier and the right edge is reached, wrap the text starting from that earlier space */
                    if (runner.t.getText().equals(" ")) {
                        text.setX(currX);
                        text.setY(currY);
                        runner = runner.next;
                        continue;
                    }
                    if (prevSpace != null) {
                        runner = prevSpace.next;
                        text = runner.t;
                    }
                    currX = STARTING_TEXT_POSITION_X;
                    currY += LINE_HEIGHT;
                    isStartNextLine = true;
                }
                
                if (isStartNextLine && currX == STARTING_TEXT_POSITION_X) {
                    lineNumbers.put(calcLineNumber(currY), runner);
                    isStartNextLine = false;
                    prevSpace = null;   // There aren't any spaces in this new line yet
                }
                text.setX(currX);
                text.setY(currY);
                currX += Editor.getTextWidth(text);
                if (Objects.equals(text.getText(), "\n")) {
                    currX = STARTING_TEXT_POSITION_X;
                    currY += LINE_HEIGHT;
                    isStartNextLine = true;
                }
                runner = runner.next;
            }
        }
    
    public void writeFile(String outputFileName) {
        if (sentinel.next == sentinel) {
            System.out.println("There is nothing to write.");
            return;
        }
        try {
            FileWriter writer = new FileWriter(outputFileName);
        
            Node runner = sentinel.next;
            while (runner != sentinel) {
                String toWrite = runner.t.getText();
                writer.write(toWrite);
                runner = runner.next;
            }
        
            System.out.println("Successfully saved file to " + outputFileName);
            writer.close();
        } catch (IOException ioException) {
            System.out.println("Error when saving; exception was: " + ioException);
        }
    }
}
