package editor;

import javafx.scene.text.Text;
import java.util.Objects;

//FastLinkedList is a LinkedList data structure meant for use for Nodes of Text Objects
public class FastLinkedList {
    private class Node {
        Text t;
        Node prev;
        Node next;
    }
    
    private Node sentinel;
    private Node currentNode;   //points to the current Node where the cursor is
    private static double LINE_HEIGHT;
    
    public FastLinkedList() {     //constructor
        sentinel = new Node();
        currentNode = sentinel;
        sentinel.prev = sentinel;
        sentinel.next = sentinel;
    }
    
    public FastLinkedList(double height) {  //constructor
        sentinel = new Node();
        currentNode = sentinel;
        sentinel.prev = sentinel;
        sentinel.next = sentinel;
        LINE_HEIGHT = height;
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
    
    public Text moveToPreviousNode() {  //returns the old Node Text
        currentNode = currentNode.prev;
        return currentNode.next.t;
    }
    
    public Text moveToNextNode() {      //returns the old Node Text
        Text ret = currentNode.t;
        currentNode = currentNode.next;
        return ret;
    }
    
    public Text deleteChar() {                              //deletes the current Node
        Text ret = currentNode.t;
        currentNode.prev.next = currentNode.next;
        currentNode.next.prev = currentNode.prev;
        currentNode = currentNode.prev;
        return ret;
    }
    
    public void reformatText(double x, double y, double xMax, double yMax) {            //recalculates all of the text positions and textwraps. x and y are starting positions, xMax and yMax are the window limits
        Node prevSpace = null;
        Node runner = sentinel.next;
        while(runner != sentinel) {
            Text text = runner.t;
            if (Objects.equals(text.getText(), "\r")) { //if the node is a newline
                x = 5.0;
                y += LINE_HEIGHT;
            } else {
                /* keep track of the words between spaces **/
                if (Objects.equals(text.getText(), " ")) {
                    prevSpace = runner;
                }
                if (x + 5.0 >= xMax) {
                    x = 5.0;
                    y += LINE_HEIGHT;
                    runner = prevSpace.next;
                    text = runner.t;
                }
                text.setX(x);
                text.setY(y);
                x += Math.ceil(text.getLayoutBounds().getWidth());
            }
            runner = runner.next;
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
