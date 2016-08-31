package editor;

import javafx.scene.text.Text;
import java.util.Objects;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

//FastLinkedList is a LinkedList data structure meant for use for Nodes of Text Objects
public class FastLinkedList {
    private class Node {
        Text t = new Text(5.0, 0.0, "");
        Node prev;
        Node next;
    }
    
    private Node sentinel;
    private Node currentNode;   //points to the current Node where the cursor is
    private HashMap<Integer, Node> lineNumbers;
    private static double LINE_HEIGHT;
    
    public FastLinkedList(double height) {  //constructor
        sentinel = new Node();
        currentNode = sentinel;
        sentinel.prev = sentinel;
        sentinel.next = sentinel;
        LINE_HEIGHT = height;
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
    
    public double getCurrTextX() {
        return currentNode.t.getX();
    }
    
    public double getCurrTextY() {
        return currentNode.t.getY();
    }
    
    public void addChar(Text text) {   //adds c to the end of the list with coordinates x and y
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
    
    public void moveToPreviousNode() {
        Node runner = currentNode.prev;
        while (runner.t.getText().equals("\n")) {
            runner = runner.prev;
        }
        currentNode = runner;
    }
    
    public void moveToNextNode() {
        Node runner = currentNode.next;
        while (runner.t.getText().equals("\n")) {
            runner = runner.next;
        }
        currentNode = runner;
    }
    
    public Text deleteChar() {                              //deletes the current Node
        Text ret = currentNode.t;
        currentNode.prev.next = currentNode.next;
        currentNode.next.prev = currentNode.prev;
        currentNode = currentNode.prev;
        return ret;
    }
    
    private Integer calcLineNumber(double y) {
        return new Integer((int) (y/LINE_HEIGHT));
    }
    
    public void reformatText(double x, double y, double xMax, double yMax) {            //recalculates all of the text positions and textwraps. x and y are starting positions, xMax and yMax are the window limits
        Node prevSpace = null;
        boolean isStartNextLine = false;
        Node runner = sentinel.next;
        while (runner != sentinel) {
            Text text = runner.t;
            if (Objects.equals(text.getText(), "\n")) { //if the node is a newline
                x = 5.0;
                y += LINE_HEIGHT;
                isStartNextLine = true;
            } else {
                /* keep track of the words between spaces **/
                if (Objects.equals(text.getText(), " ")) {
                    prevSpace = runner;
                }
                /* if reach end of line, newline **/
                if (x + 5.0 >= xMax) { //adding 5 more pixels in margin looks better
                    x = 5.0;
                    y += LINE_HEIGHT;
                    runner = prevSpace.next;
                    text = runner.t;
                    isStartNextLine = true;
                }
                if (isStartNextLine) {
                    lineNumbers.put(calcLineNumber(y), runner);
                    isStartNextLine = false;
                }
                text.setX(x);
                text.setY(y);
                x += Math.ceil(text.getLayoutBounds().getWidth());
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
    
    public void print() {                           //prints out the list for testing purposes
        Node runner = sentinel.next;
        while(runner != sentinel) {
            System.out.println(runner.t.getText());
            runner = runner.next;
        }
    }
}
