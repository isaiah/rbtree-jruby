package rbtree.ext;

import org.jruby.RubyInteger;

public class NilNode extends Node {
    private static NilNode nil = null;
    private NilNode() {
        this.color = Color.BLACK;
        this.left = this.right = this.parent = null;
    }

    public static NilNode getInstance() {
        if (nil == null) {
            nil = new NilNode();
        }
        return nil;
    }

    @Override
    public boolean isNull() {
        return true;
    }
}

