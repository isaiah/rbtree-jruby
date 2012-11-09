package rbtree.ext;

import org.jruby.RubyObject;
import org.jruby.runtime.builtin.IRubyObject;

public class Node {
    protected Color color;
    protected RubyObject key;
    protected IRubyObject value;
    protected Node left;
    protected Node right;
    protected Node parent;

    protected Node() {}

    protected Node(IRubyObject key, IRubyObject value) {
        this(key, value, Color.RED);
    }

    protected Node(IRubyObject key, IRubyObject value, Color color) {
        this.key = (RubyObject) key;
        this.value = value;
        this.color = color;
        this.left = this.right = this.parent = NilNode.getInstance();
    }

    public boolean isRed() {
        return this.color == Color.RED;
    }

    public boolean isBlack() {
        return !isRed();
    }

    public RubyObject getKey() {
        return this.key;
    }

    public IRubyObject getValue() {
        return this.value;
    }

    public Node getLeft() {
        return this.left;
    }

    public Node getRight() {
        return this.right;
    }
    public Node getParent() {
        return this.parent;
    }
    public Node getGrandParent() {
        return this.parent.getParent();
    }
    public Color getColor() {
        return this.color;
    }
    public void setLeft(Node node) {
        this.left = node;
    }
    public void setRight(Node node) {
        this.right = node;
    }
    public void setParent(Node node) {
        this.parent = node;
    }
    public void setColor(Color color) {
        this.color = color;
    }
    public void setKey(IRubyObject key) {
        this.key = (RubyObject) key;
    }
    public void setValue(IRubyObject val) {
        this.value = val;
    }
    public void setBlack() {
        this.color = Color.BLACK;
    }
    public void setRed() {
        this.color = Color.RED;
    }
    public boolean isNull() {
        return false;
    }

    public boolean isLeft() {
        return this == this.parent.left;
    }

    public boolean isRight() {
        return this == this.parent.right;
    }

    public Node getSibling() {
        return isLeft() ? this.parent.right : this.parent.left;
    }
}
