package editor;

import javafx.scene.text.Text;
import javafx.scene.text.Font;
import javafx.scene.Group;
import java.util.Objects;
import javafx.geometry.VPos;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

/**TextStorage is a LinkedList data structure meant for use for Nodes of Text Objects. A HashMap is used for quick access to the lines of text
*/
public class TextStorage {
    private class Node {
        Text t = new Text(5.0, 0.0, "");
        Node prev;
        Node next;
    }
    
    private Node sentinel;
    private Node currentNode;   //points to the current Node where the cursor is
    private HashMap<Integer, Node> lineNumbers;
    private static double LINE_HEIGHT;
    private static double STARTING_TEXT_POSITION_X;
    private static double STARTING_TEXT_POSITION_Y;
    private static final int STARTING_FONT_SIZE = 12;
    private static int fontSize = STARTING_FONT_SIZE;
    private static String fontName = "Verdana";
    
    public TextStorage(double startingX, double startingY) {  //constructor
        sentinel = new Node();
        currentNode = sentinel;
        sentinel.prev = sentinel;
        sentinel.next = sentinel;
        Text tempHeight = charToText(0, 0, "a");
        LINE_HEIGHT = tempHeight.getLayoutBounds().getHeight();
        STARTING_TEXT_POSITION_X = startingX;
        STARTING_TEXT_POSITION_Y = startingY;
        lineNumbers = new HashMap<Integer, Node>();
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
    
    private void addChar(Text text) {   //adds c to the end of the list with coordinates x and y
        Node n = new Node();
        n.t = text;
        n.prev = currentNode;
        n.next = currentNode.next;
        currentNode.next.prev = n;
        currentNode.next = n;
        currentNode = n;
    }
    
    public boolean isFirstCharOfLine() {
        return lineNumbers.containsValue(currentNode);
    }
    
    private boolean isFirstCharOfLine(Node node) {
        return lineNumbers.containsValue(node);
    }
    
    public void moveToPreviousNode() {  //if there is a newline before on the previous line, skip it
        if (currentNode.prev.t.getText().equals("\n") && !this.isFirstCharOfLine(currentNode.prev)) {
            currentNode = currentNode.prev.prev;
        } else {    
            currentNode = currentNode.prev;
        }
    }
    
    public void moveToNextNode() {  //if there is a newline next on the same line, skip it
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
        //returns Node closest to input coordinates or null if invalid coordinates or position is after the last line possible
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
    
    private Text deleteChar() {                              //deletes the current Node
        Text ret = currentNode.t;
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
    
    public static Text charToText(double xPos, double yPos, String c) {                 //converts the char to Text and applies all necessary modifications
            Text toBeAdded = new Text(xPos, yPos, c);
            toBeAdded.setTextOrigin(VPos.TOP);
            toBeAdded.setFont(Font.font(fontName, fontSize));
            toBeAdded.toFront();
            return toBeAdded;
        }
    
    public void changeFontSize(int increment, Cursor cursor) {  //could have optimized by implementing in reformatText, but using two loops for simplicity
        if (increment >= 0) {
            fontSize += increment;
        } else {    //if negative increment, cannot go below zero
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
    
    public void reformatText(double xMax, double yMax) {            //recalculates all of the text positions and textwraps. xMax and yMax are the window limits
        Node prevSpace = null;  //has there been a space on this line
        lineNumbers.clear();
        boolean isStartNextLine = true;
        Node runner = sentinel.next;
        double currX = STARTING_TEXT_POSITION_X;
        double currY = STARTING_TEXT_POSITION_Y;
        while (runner != sentinel) {
            Text text = runner.t;
            /* keep track of the words between spaces **/
            if (Objects.equals(text.getText(), " ")) {
                prevSpace = runner;
            }
            /* if reach end of line, newline **/
                if (currX + Editor.getTextWidth(text) >= xMax) { //if the next char would exceed the margin
                    /* if there had a been a space earlier and the right edge is reached, wrap the text starting from that earlier space **/
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
                    prevSpace = null;   //there aren't any spaces in this new line yet
                }
                text.setX(currX);
                text.setY(currY);
                currX += Editor.getTextWidth(text);
                if (Objects.equals(text.getText(), "\n")) { //if the node is a newline
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
