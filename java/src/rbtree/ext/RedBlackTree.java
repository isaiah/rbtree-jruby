package rbtree.ext;

import static org.jruby.RubyEnumerator.enumeratorize;
import static org.jruby.CompatVersion.*;

import org.jruby.Ruby;
import org.jruby.RubyObject;
import org.jruby.RubyClass;
import org.jruby.RubyString;
import org.jruby.RubyFixnum;
import org.jruby.RubyHash;
import org.jruby.RubyArray;
import org.jruby.RubyProc;
import org.jruby.runtime.Arity;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectMarshal;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.marshal.MarshalStream;
import org.jruby.runtime.marshal.UnmarshalStream;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.util.TypeConverter;
import org.jruby.javasupport.util.RuntimeHelpers;

import org.jruby.exceptions.RaiseException;

import java.util.concurrent.atomic.AtomicInteger;

import java.io.IOException;

@JRubyClass(name = "RedBlackTree")
public class RedBlackTree extends RubyObject {
    private Node root = NilNode.getInstance();
    private int size = 0;
    private static final int PROCDEFAULT_HASH_F = 1 << 10;
    private static final int DEFAULT_INSPECT_STR_SIZE = 20;
    private IRubyObject ifNone;
    private RubyProc cmpProc;
    private boolean dupes;

    public static RubyClass createRedBlackTreeClass(Ruby runtime) {
        RubyClass rbtreeClass = runtime.defineClass("RedBlackTree", runtime.getObject(), RBTREE_ALLOCATOR);
        rbtreeClass.setReifiedClass(RedBlackTree.class);
        rbtreeClass.includeModule(runtime.getEnumerable());
        rbtreeClass.setMarshal(RBTREE_MARSHAL);
        rbtreeClass.defineAnnotatedMethods(RedBlackTree.class);
        return rbtreeClass;
    }

    private static final ObjectAllocator RBTREE_ALLOCATOR = new ObjectAllocator() {
        public IRubyObject allocate(Ruby runtime, RubyClass klazz) {
            return new RedBlackTree(runtime, klazz);
        }
    };

    public RedBlackTree(final Ruby ruby, RubyClass rubyClass) {
        super(ruby, rubyClass);
        this.dupes = getMetaClass().getRealClass().getName().equals("RedBlackTree");
    }
    
    @JRubyMethod(name = "initialize", optional = 1)
    public IRubyObject initialize(ThreadContext context, IRubyObject[] args, Block block) {
        if (block.isGiven()) {
            if (args.length > 0) raiseArgumeneError();
            this.ifNone = getRuntime().newProc(Block.Type.PROC, block);
            flags |= PROCDEFAULT_HASH_F;
        } else {
            Arity.checkArgumentCount(getRuntime(), args, 0, 1);
            if (args.length == 1) this.ifNone = args[0];
        }
        return this;
    }

    private void raiseArgumeneError() {
        throw getRuntime().newArgumentError("wrong number arguments");
    }

    @JRubyMethod(name = "clear")
    public IRubyObject init() {
        this.root = NilNode.getInstance();
        this.size = 0;
        return this;
    }

    @JRubyMethod(name = "[]", rest = true, meta = true)
    public static IRubyObject create(final ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        RubyClass klass = (RubyClass) recv;
        Ruby runtime = context.getRuntime();
        final RedBlackTree rbtree;
        if (args.length == 1) {
            if (klass.getName().equals("RBTree") && args[0].getMetaClass().getRealClass().getName().equals("RedBlackTree")) 
                throw runtime.newTypeError("cannot convert MultiRBTree to RBTree");
            if (args[0] instanceof RedBlackTree) {
                rbtree = (RedBlackTree) klass.allocate();
                rbtree.update(context, (RedBlackTree) args[0], Block.NULL_BLOCK);
                return rbtree;
            }
            IRubyObject tmp = TypeConverter.convertToTypeWithCheck(args[0], runtime.getHash(), "to_hash");
            if (!tmp.isNil()) {
                rbtree = (RedBlackTree) klass.allocate();
                RubyHash hash = (RubyHash) tmp;
                hash.visitAll(new RubyHash.Visitor() {
                    @Override
                    public void visit(IRubyObject key, IRubyObject val) {
                        rbtree.internalPut(context, key, val, false);
                    }
                });
                return rbtree;
            }
            tmp = TypeConverter.convertToTypeWithCheck(args[0], runtime.getArray(), "to_ary");
            if (!tmp.isNil()) {
                rbtree = (RedBlackTree) klass.allocate();
                RubyArray arr = (RubyArray) tmp;
                for (int i = 0, j = arr.getLength(); i < j; i++) {
                    IRubyObject v = TypeConverter.convertToTypeWithCheck(arr.entry(i), runtime.getArray(), "to_ary");
                    if (v.isNil()) continue;
                    IRubyObject key = runtime.getNil();
                    IRubyObject val = runtime.getNil();
                    switch(((RubyArray) v).getLength()) {
                    case 2:
                        val = ((RubyArray) v).entry(1);
                    case 1:
                        key = ((RubyArray) v).entry(0);
                        rbtree.internalPut(context, key, val, false);
                    }
                }
                return rbtree;
            }
        }
        if (args.length % 2 != 0) throw runtime.newArgumentError("odd number of arguments");
        rbtree = (RedBlackTree) klass.allocate();
        for (int i = 0; i < args.length; i += 2) {
            rbtree.internalPut(context, args[i], args[i+1], false);
        } 
        return rbtree;
    }

    @JRubyMethod(name = "[]", required = 1)
    public IRubyObject op_aref(ThreadContext context, IRubyObject key) {
        Node node = internalGet(context, (RubyObject) key);
        return node == null ? callMethod(context, "default", key) : node.getValue();
    }

    @JRubyMethod(name = "[]=", required = 2, compat = RUBY1_8)
    public IRubyObject op_aset(ThreadContext context, IRubyObject key, IRubyObject val) {
        fastASetCheckString(context, key, val);
        return val;
    }
    @JRubyMethod(name = "[]=", required = 2, compat = RUBY1_9)
    public IRubyObject op_aset19(ThreadContext context, IRubyObject key, IRubyObject val) {
        fastASetCheckString19(context, key, val);
        return val;
    }

    public final void fastASetCheckString(ThreadContext context, IRubyObject key, IRubyObject value) {
        if (key instanceof RubyString) {
            op_asetForString(context, (RubyString) key, value);
        } else {
            internalPut(context, key, value);
        }
    }

    public final void fastASetCheckString19(ThreadContext context, IRubyObject key, IRubyObject value) {
        if (key.getMetaClass().getRealClass() == context.runtime.getString()) {
            op_asetForString(context, (RubyString) key, value);
        } else {
            internalPut(context, key, value);
        }
    }

    protected void op_asetForString(ThreadContext context, RubyString key, IRubyObject value) {
        Node node;
        if (!dupes && (node = internalGet(context, (RubyObject) key)) != null) {
            node.setValue(value);
        } else {
            checkIterating();
            if (!key.isFrozen()) {
                key = key.strDup(context.runtime);
                key.setFrozen(true);
            }
            internalPut(context, key, value, false);
        }
    }

    @JRubyMethod(name = "fetch", required = 1, optional = 1)
    public IRubyObject rbtree_fetch(ThreadContext context, IRubyObject[] args, Block block) {
        if (block.isGiven() && args.length == 2) {
            getRuntime().getWarnings().warn("block supersedes default value argument");
        }
        Node node = internalGet(context, (RubyObject) args[0]);
        if (node != null)
            return node.getValue();
        if (block.isGiven())
            return block.yield(context, args[0]);
        if (args.length == 1)
            throw getRuntime().newIndexError("key not found");
        return args[1];
    }

    @JRubyMethod(name = "index")
    public IRubyObject rbtree_index(final ThreadContext context, final IRubyObject value) {
        try {
            iteratorVisitAll(new Visitor() {
                public void visit(IRubyObject key, IRubyObject val) {
                    if (value.eql(val)) {
                        throw new FoundKey(key);
                    }
                }
            });
            return null;
        } catch (FoundKey found) {
            return found.key;
        }
    }

    private void checkCompatible(Ruby runtime, IRubyObject other) {
        if (!(other instanceof RedBlackTree))
            throw runtime.newTypeError(String.format("wrong argument type %s (expected %s)", other.getMetaClass().getRealClass().getName(), "RedBlackTree"));
        if (getMetaClass().getRealClass().getName().equals("RBTree") && other.getMetaClass().getRealClass().getName().equals("RedBlackTree")) 
            throw runtime.newTypeError("cannot convert MultiRBTree to RBTree");
    }

    @JRubyMethod(name = {"update", "merge!"})
    public IRubyObject update(ThreadContext context, IRubyObject other, Block block) {
        Ruby runtime = getRuntime();
        checkCompatible(runtime, other);
        RedBlackTree otherTree = (RedBlackTree) other;
        for (Node node = otherTree.minimum(); !node.isNull(); node = otherTree.successor(node)) {
            if (block.isGiven()) {
                op_aset(context, node.getKey(), block.yieldSpecific(context, node.getKey(), op_aref(context, node.getKey()), node.getValue()));
            } else {
                op_aset(context, node.getKey(), node.getValue());
            }
        }
        return this;
    }

    @JRubyMethod
    public IRubyObject merge(final ThreadContext context, final IRubyObject other) {
        Ruby runtime = getRuntime();
        // TODO should allow RubyHash
        if (!(other instanceof RedBlackTree)) {
            runtime.newArgumentError(String.format("wrong argument type %s (expected %s)", other.getMetaClass().getRealClass().getName(), "RedBlackTree"));
        }

        RedBlackTree result = (RedBlackTree) dup();
        RedBlackTree otherTree = (RedBlackTree) other;
        for (Node node = otherTree.minimum(); !node.isNull(); node = otherTree.successor(node)) {
            result.op_aset(context, node.getKey(), node.getValue());
        }
        return result;
    }

    @JRubyMethod(name = {"has_key?", "key?", "include?", "member?"})
    public IRubyObject has_key_p(final ThreadContext context, IRubyObject key) {
        return internalGet(context, (RubyObject) key) == null ? getRuntime().getFalse() : getRuntime().getTrue();
    }

    private boolean hasValue(final ThreadContext context, final IRubyObject value) {
        try {
            visitAll(new Visitor() {
                public void visit(IRubyObject key, IRubyObject val) {
                    if (equalInternal(context, val, value)) throw FOUND;
                }
            });
            return false;
        } catch (Found found) {
            return true;
        }
    }

    @JRubyMethod(name = {"has_value?", "value?"})
    public IRubyObject has_value_p(final ThreadContext context, final IRubyObject value) {
        return getRuntime().newBoolean(hasValue(context, value));
    }

    @JRubyMethod(name = "keys")
    public IRubyObject keys() {
        final RubyArray keys = getRuntime().newArray(size);
        visitAll(new Visitor() {
            public void visit(IRubyObject key, IRubyObject val) {
                keys.append(key);
            }
        });
        return keys;
    }

    @JRubyMethod(name = "values")
    public IRubyObject values() {
        final RubyArray values = getRuntime().newArray(size);
        visitAll(new Visitor() {
            public void visit(IRubyObject key, IRubyObject val) {
                values.append(val);
            }
        });
        return values;
    }

    @JRubyMethod(name = "to_hash")
    public IRubyObject to_hash() {
        Ruby runtime = getRuntime();
        if (getMetaClass().getRealClass().getName().equals("RedBlackTree"))
            throw runtime.newTypeError("cannot convert MultiRBTree to Hash");
        final RubyHash hash = new RubyHash(runtime, runtime.getHash());
        hash.default_value_set(ifNone);
        hash.setFlag(flags, true);
        visitAll(new Visitor() {
            public void visit(IRubyObject key, IRubyObject value) {
                hash.fastASet(key, value);
            }
        });
        return hash;
    }

    @JRubyMethod
    public IRubyObject to_rbtree() {
        return this;
    }

    @JRubyMethod(name = "readjust", optional = 1)
    public IRubyObject readjust(ThreadContext context, IRubyObject[] args, Block block) {
        RubyProc oldProc = cmpProc;
        RubyProc cmpfunc = null;
        if (block.isGiven()) {
            if (args.length > 0) raiseArgumeneError();
            cmpfunc = getRuntime().newProc(Block.Type.PROC, block);
        } else if (args.length == 1) {
            if (args[0] instanceof RubyProc) {
                cmpfunc = (RubyProc) args[0];
            } else if (args[0].isNil()) {
                cmpfunc = null;
            } else {
                throw getRuntime().newTypeError(String.format("wrong argument type %s (expected %s)", args[0].getMetaClass().getRealClass().getName(), "Proc"));
            }
        }
        RedBlackTree self = (RedBlackTree) this.dup();
        try {
            replaceInternal(context, self, cmpfunc);
        } catch (RaiseException e) {
            replaceInternal(context, self, oldProc);
            throw e;
        }
        return this;
    }

    @JRubyMethod(name = "default=")
    public IRubyObject setDefaultVal(ThreadContext context, IRubyObject defaultValue) {
        ifNone = defaultValue;
        flags &= ~PROCDEFAULT_HASH_F;

        return ifNone;
    }

    @JRubyMethod(name = "default")
    public IRubyObject default_value_get(ThreadContext context) {
        if ((flags & PROCDEFAULT_HASH_F) != 0) {
            return getRuntime().getNil();
        }
        return ifNone;
    }
    @JRubyMethod(name = "default")
    public IRubyObject default_value_get(ThreadContext context, IRubyObject arg) {
        if ((flags & PROCDEFAULT_HASH_F) != 0) {
            return RuntimeHelpers.invoke(context, ifNone, "call", this, arg);
        }
        return ifNone;
    }

    @JRubyMethod(name = "default_proc")
    public IRubyObject getDefaultProc() {
        return (flags & PROCDEFAULT_HASH_F) != 0 ? ifNone : getRuntime().getNil();
    }
    
    @JRubyMethod(name = "cmp_proc")
    public IRubyObject getCmpProc() {
        return this.cmpProc;
    }

    public RedBlackTree internalPut(ThreadContext context, IRubyObject key, IRubyObject value) {
        return internalPut(context, key, value, true);
    }

    public RedBlackTree internalPut(ThreadContext context, IRubyObject key, IRubyObject value, boolean checkExisting) {
        if (!dupes && checkExisting) {
            Node node = internalGet(context, (RubyObject) key); 
            if (node != null) {
                node.setValue(value);
                return this;
            }
        }

        Node x = new Node((RubyObject) key, value);
        internalPutHelper(context, x);
        while (x != this.root && x.getParent().getColor() == Color.RED){
            if (x.getParent() == x.getParent().getParent().getLeft()) {
                Node y = x.getParent().getParent().getRight();
                if (!y.isNull() && y.color == Color.RED) {
                    x.getParent().setColor(Color.BLACK);
                    y.setColor(Color.BLACK);
                    x.getParent().getParent().setColor(Color.RED);
                    x = x.getParent().getParent();
                } else {
                    if (x == x.getParent().getRight()) {
                        x = x.getParent();
                        leftRotate(x);
                    }
                    x.getParent().setColor(Color.BLACK);
                    x.getParent().getParent().setColor(Color.RED);
                    rightRotate(x.getParent().getParent());
                }
            } else {
                Node y = x.getParent().getParent().getLeft();
                if (!y.isNull() && y.isRed()) {
                    x.getParent().setBlack();
                    y.setBlack();
                    x.getParent().getParent().setRed();
                    x = x.getParent().getParent();
                } else {
                    if (x == x.getParent().getLeft()) {
                        x = x.getParent();
                        rightRotate(x);
                    }
                    x.getParent().setBlack();
                    x.getParent().getParent().setRed();
                    leftRotate(x.getParent().getParent());
                }
            }
        }
        root.setBlack();
        return this;
    }

    public IRubyObject internalDelete(ThreadContext context, Node z) {
        Node y = (z.getLeft().isNull() || z.getRight().isNull()) ? z : successor(z);
        Node x = y.getLeft().isNull() ? y.getRight() : y.getLeft();
        x.setParent(y.getParent());
        if (y.getParent().isNull()) {
            this.root = x;
        } else {
            if (y == y.getParent().getLeft()) {
                y.getParent().setLeft(x);
            } else {
                y.getParent().setRight(x);
            }
        }
        if (y != z) {
            z.setKey(y.getKey());
            z.setValue(y.getValue());
        }
        if (y.isBlack()) deleteFixup(x);
        this.size -= 1;

        return newArray(y);
    }

    private Node minimum() {
        if (this.root.isNull()) {
            return this.root;
        }
        return minimum(this.root);
    }

    private Node minimum(Node x) {
        while (!x.getLeft().isNull()) {
            x = x.getLeft();
        }
        return x;
    }

    private Node maximum() {
        if (this.root.isNull()) {
            return this.root;
        }
        return maximum(this.root);
    }

    private Node maximum(Node x) {
        while (!x.getRight().isNull())
            x = x.getRight();
        return x;
    }

    private Node successor(Node x) {
        if (!x.getRight().isNull()) return minimum(x.getRight());
        Node y = x.getParent();
        while (!y.isNull() && x == y.getRight()) {
            x = y;
            y = y.getParent();
        }
        return y;
    }

    private Node predecessor(Node x) {
        if (!x.getLeft().isNull()) return maximum(x.getLeft());
        Node y = x.getParent();
        while (!y.isNull() && x == y.getLeft()) {
            x = y;
            y = y.getParent();
        }
        return y;
    }

    @JRubyMethod(name = {"each_pair", "each"})
    public IRubyObject rbtree_each(final ThreadContext context, final Block block) {
        return block.isGiven() ? eachCommon(context, block) 
            : enumeratorize(getRuntime(), this, "each");
    }

    @JRubyMethod
    public IRubyObject each_key(final ThreadContext context, final Block block) {
        return block.isGiven() ? each_keyCommon(context, block) : enumeratorize(context.runtime, this, "each_key");
    }

    public RedBlackTree each_keyCommon(final ThreadContext context, final Block block) {
        iteratorVisitAll(new Visitor() {
            public void visit(IRubyObject key, IRubyObject val) {
                block.yield(context, key);
            }
        });
        return this;
    }

    @JRubyMethod
    public IRubyObject each_value(final ThreadContext context, final Block block) {
        return block.isGiven() ? each_valueCommon(context, block) : enumeratorize(context.runtime, this, "each_value");
    }

    public RedBlackTree each_valueCommon(final ThreadContext context, final Block block) {
        iteratorVisitAll(new Visitor() {
            public void visit(IRubyObject key, IRubyObject val) {
                block.yield(context, val);
            }
        });
        return this;
    }

    public IRubyObject eachCommon(final ThreadContext context, final Block block) {
        iteratorVisitAll(new Visitor() {
            public void visit(IRubyObject key, IRubyObject value) {
                block.yieldSpecific(context, key, value);
            }
        });
        return this;
    }

    @JRubyMethod
    public IRubyObject shift(ThreadContext context) {
        return nodeOrDefault(context, minimum(), true);
    }

    @JRubyMethod
    public IRubyObject pop(ThreadContext context) {
        return nodeOrDefault(context, maximum(), true);
    }

    @JRubyMethod
    public IRubyObject delete(ThreadContext context, IRubyObject key, Block block) {
        Node node = lower_boundInternal(context, key);
        if (node.isNull()) {
            if (block.isGiven()) {
                return block.yield(context, key);
            }
            return getRuntime().getNil();
        }
        internalDelete(context, node);
        return node.getValue();
    }

    @JRubyMethod
    public IRubyObject delete_if(final ThreadContext context, final Block block) {
        return block.isGiven() ? delete_ifInternal(context, block) : enumeratorize(context.runtime, this, "delete_if");
    }

    private IRubyObject delete_ifInternal(final ThreadContext context, final Block block) {
        final RedBlackTree self = this;
        iteratorVisitAll(new Visitor() {
            public void visit(IRubyObject key, IRubyObject val) {
                if (block.yieldSpecific(context, key, val).isTrue()) {
                    self.delete(context, key, Block.NULL_BLOCK);
                }
            }
        });
        return this;
    }

    @JRubyMethod(name = "reject!")
    public IRubyObject reject_bang(final ThreadContext context, final Block block) {
        return block.isGiven() ? reject_bangInternal(context, block) : enumeratorize(context.runtime, this, "reject!");
    }

    private IRubyObject reject_bangInternal(ThreadContext context, Block block) {
        int n = size;
        delete_if(context, block);
        if (n == size) return getRuntime().getNil();
        return this;
    }

    @JRubyMethod
    public IRubyObject reject(final ThreadContext context, final Block block) {
        return block.isGiven() ? rejectInternal(context, block) : enumeratorize(context.runtime, this, "reject");
    }

    private IRubyObject rejectInternal(ThreadContext context, Block block) {
        return ((RedBlackTree) dup()).reject_bangInternal(context, block);
    }

    @JRubyMethod
    public IRubyObject select(final ThreadContext context, final Block block) {
        final Ruby runtime = getRuntime();
        if (!block.isGiven()) return enumeratorize(runtime, this, "select");

        final RedBlackTree rbtree = (RedBlackTree) getMetaClass().getRealClass().allocate();
        iteratorVisitAll(new Visitor() {
            public void visit(IRubyObject key, IRubyObject value) {
                if (block.yieldSpecific(context, key, value).isTrue())
                    rbtree.internalPut(context, key, value);
            }
        });
        return rbtree;
    }

    @JRubyMethod
    public RubyArray to_a() {
        final Ruby runtime = getRuntime();
        final RubyArray result = runtime.newArray(size);
        iteratorVisitAll(new Visitor() {
            public void visit(IRubyObject key, IRubyObject value) {
                result.append(runtime.newArray(key, value));
            }
        });
        return result;
    }

    @JRubyMethod
    public IRubyObject flatten(final ThreadContext context, final Block block) {
        RubyArray arg = to_a();
        arg.callMethod(context, "flatten!", RubyFixnum.one(context.runtime));
        return arg;
    }

    @JRubyMethod(name = "values_at", rest = true)
    public IRubyObject values_at(final ThreadContext context, IRubyObject[] args) {
        RubyArray result = RubyArray.newArray(context.runtime, args.length);
        for (int i = 0; i < args.length; i++) {
            result.append(op_aref(context, args[i]));
        }
        return result;
    }

    @JRubyMethod
    public IRubyObject invert(final ThreadContext context) {
        final RedBlackTree rbtree = (RedBlackTree) getMetaClass().getRealClass().allocate();
        iteratorVisitAll(new Visitor() {
            public void visit(IRubyObject key, IRubyObject value) {
                rbtree.internalPut(context, value, key);
            }
        });
        return rbtree;
    }
    
    private IRubyObject nodeOrDefault(ThreadContext context, Node node) {
        return nodeOrDefault(context, node, false);
    }
    
    private IRubyObject nodeOrDefault(ThreadContext context, Node node, boolean deleteNode) {
        if (node.isNull()) {
            if (this.ifNone == null)
                return getRuntime().getNil();
            if ((flags & PROCDEFAULT_HASH_F) != 0)
                return RuntimeHelpers.invoke(context, ifNone, "call", this, getRuntime().getNil());
            return ifNone;
        }
        if (deleteNode) {
            internalDelete(context, node);
        }
        return newArray(node);
    }

    @JRubyMethod(name = {"reverse_inorder_walk", "reverse_each"})
    public IRubyObject reverse_each(final ThreadContext context, final Block block) {
        return block.isGiven() ? reverse_eachInternal(context, block) : enumeratorize(context.runtime, this, "reverse_each");
    }
    
    private IRubyObject reverse_eachInternal(final ThreadContext context, final Block block) {
        iteratorReverseVisitAll(new Visitor() {
            public void visit(IRubyObject key, IRubyObject value) {
                block.yieldSpecific(context, key, value);
            }
        });
        return this;
    }

    public Node internalGet(ThreadContext context, RubyObject key) {
        Node x = this.root;
        while (!x.isNull()) {
            int ret = compare(context, key, x.getKey());
            if (ret > 0) {
                x = x.getRight();
            } else if (ret < 0) {
                x = x.getLeft();
            } else {
                return x;
            }
        }
        return null;
    }

    @JRubyMethod(name = "empty?")
    public IRubyObject empty_p(ThreadContext context) {
        return getRuntime().newBoolean(size == 0);
    }

    @JRubyMethod(name = "black_height")
    public IRubyObject blackHeight() {
        Node x = this.root;
        int height = 0;
        while (!x.isNull()) {
            x = x.getLeft();
            if (x.isNull() || x.isBlack()) height += 1;
        }
        return RubyFixnum.newFixnum(getRuntime(), height);
    }

    private void leftRotate(Node x) {
        Node y = x.getRight();
        x.setRight(y.getLeft());

        if (!y.getLeft().isNull()) {
            y.getLeft().setParent(x);
        }
        y.setParent(x.getParent());
        if (x.getParent().isNull()) {
            this.root = y;
        } else {
            if (x == x.getParent().getLeft()) {
                x.getParent().setLeft(y);
            } else {
                x.getParent().setRight(y);
            }
        }
        y.setLeft(x);
        x.setParent(y);
    }

    private void rightRotate(Node x) {
        Node y = x.getLeft();
        x.setLeft(y.getRight());
        if (! y.getRight().isNull()) {
            y.getRight().setParent(x);
        }
        y.setParent(x.getParent());
        if (x.getParent().isNull()) {
            this.root = y;
        } else {
            if (x == x.getParent().getLeft()) {
                x.getParent().setLeft(y);
            } else {
                x.getParent().setRight(y);
            }
        }
        y.setRight(x);
        x.setParent(y);
    }
    private int compare(ThreadContext context, Node a, Node b) {
        return compare(context, a.getKey(), b.getKey());
    }

    private int compare(ThreadContext context, RubyObject a, RubyObject b) {
        if (context == null || cmpProc == null)
            return a.compareTo(b);
        return (int) cmpProc.call(context, new IRubyObject[]{a, b}).convertToInteger().getLongValue();
    }

    private void internalPutHelper(ThreadContext context, Node z) {
        Node y = NilNode.getInstance();
        Node x = this.root;
        while(!x.isNull()) {
            y = x;
            x = compare(context, z, x) < 0 ? x.getLeft() : x.getRight();
        } 
        z.setParent(y);
        if (y.isNull()) {
            this.root = z;
        } else {
            if (compare(context, z, y) < 0) {
                y.setLeft(z);
            } else {
                y.setRight(z);
            }
        }
        this.size += 1;
    }

    private void deleteFixup(Node x) {
        while (x != this.root && x.isBlack()) {
            if (x.isLeft()) {
                Node w = x.getParent().getRight();
                if (w.isRed()) {
                    w.setBlack();
                    x.getParent().setRed();
                    leftRotate(x.getParent());
                    w = x.getParent().getRight();
                }
                if (w.getLeft().isBlack() && w.getRight().isBlack()) {
                    w.setRed();
                    x = x.getParent();
                } else {
                    if (w.getRight().isBlack()) {
                        w.getLeft().setBlack();
                        w.setRed();
                        rightRotate(w);
                        w = x.getParent().getRight();
                    }
                    w.setColor(x.getParent().getColor());
                    x.getParent().setBlack();
                    w.getRight().setBlack();
                    leftRotate(x.getParent());
                    x = this.root;
                }
            } else {
                Node w = x.getParent().getLeft();
                if (w.isRed()) {
                    w.setBlack();
                    x.getParent().setRed();
                    rightRotate(x.getParent());
                    w = x.getParent().getLeft();
                }
                if (w.getRight().isBlack() && w.getLeft().isBlack()) {
                    w.setRed();
                    x = x.getParent();
                } else {
                    if (w.getLeft().isBlack()) {
                        w.getRight().setBlack();
                        w.setRed();
                        rightRotate(w);
                        w = x.getParent().getLeft();
                    }
                    w.setColor(x.getParent().getColor());
                    x.getParent().setBlack();
                    w.getLeft().setBlack();
                    rightRotate(x.getParent());
                    x = this.root;
                }
            }
        }
        x.setBlack();
    }

    // TODO demo method
    @JRubyMethod
    public RubyString get(ThreadContext context) {
        return RubyString.newString(getRuntime(), "test");
    }
    
    public Node getRoot() {
        return this.root;
    }

    @JRubyMethod(name = "size")
    public IRubyObject getSize() {
        return getRuntime().newFixnum(this.size);
    }

    @JRubyMethod
    public IRubyObject last(ThreadContext context) {
        return nodeOrDefault(context, maximum());
    }

    @JRubyMethod
    public IRubyObject first(ThreadContext context) {
        return nodeOrDefault(context, minimum());
    }

    public Node lower_boundInternal(ThreadContext context, IRubyObject key) {
        Ruby runtime = getRuntime();
        Node node = this.root;
        Node tentative = NilNode.getInstance();
        while (!node.isNull()) {
            int result = compare(context, (RubyObject) key, node.getKey());
            if (result > 0) {
                node = node.getRight();
            } else if (result < 0) {
                tentative = node;
                node = node.getLeft();
            } else {
                if (!dupes) {
                    return node;
                } else {
                    tentative = node;
                    node = node.getLeft();
                }
            }
        }
        return tentative;
    }

    @JRubyMethod
    public IRubyObject lower_bound(ThreadContext context, IRubyObject key) {
        Node node = lower_boundInternal(context, key);
        return node.isNull() ? context.runtime.getNil() : newArray(node);
    }

    public Node upper_boundInternal(ThreadContext context, IRubyObject key) {
        Ruby runtime = getRuntime();
        Node node = this.root;
        Node tentative = NilNode.getInstance();
        while (!node.isNull()) {
            int result = compare(context, (RubyObject) key, node.getKey());
            if (result < 0) {
                node = node.getLeft();
            } else if (result > 0) {
                tentative = node;
                node = node.getRight();
            } else {
                if (!dupes) {
                    return node;
                } else { // if there are duplicates, go to the far right
                    tentative = node;
                    node = node.getRight();
                }
            }
        }
        return tentative;
    }
    
    @JRubyMethod
    public IRubyObject upper_bound(ThreadContext context, IRubyObject key) {
        Node node = upper_boundInternal(context, key);
        return node.isNull() ? context.runtime.getNil() : newArray(node);
    }

    @JRubyMethod(name = "bound", required = 1, optional = 1)
    public IRubyObject bound(final ThreadContext context, final IRubyObject[] bounds, final Block block) {
        final Ruby runtime = getRuntime();
        final RubyArray ret = runtime.newArray();
        iteratorVisitAll(new Visitor() {
            public void visit(IRubyObject key, IRubyObject value) {
                if (((RubyObject) key).compareTo((RubyObject) bounds[0]) == 0
                    || bounds.length == 2 && ((RubyObject) key).compareTo((RubyObject) bounds[0]) >= 0 
                    && ((RubyObject) key).compareTo((RubyObject) bounds[1]) <= 0) {
                    if (block.isGiven()) {
                        block.yieldSpecific(context, key, value);
                    } else {
                        ret.add(runtime.newArray(key, value));
                    }
                }
            }
        });
        return ret;
    }

    private RubyArray newArray(Node node) {
        return getRuntime().newArray(node.getKey(), node.getValue());
    }

    @JRubyMethod(name = {"replace", "initialize_copy"})
    public IRubyObject replace(final ThreadContext context, IRubyObject other) {
        checkCompatible(context.runtime, other);
        RedBlackTree otherTree = (RedBlackTree) other;
        return replaceInternal(context, otherTree, otherTree.cmpProc);
    }

    private IRubyObject replaceInternal(final ThreadContext context, RedBlackTree otherTree, RubyProc cmpfunc) {
        init();
        if (this == otherTree) return this;
        this.ifNone = otherTree.ifNone;
        this.flags = otherTree.flags;
        this.cmpProc = cmpfunc;
        otherTree.visitAll(new Visitor() {
            public void visit(IRubyObject key, IRubyObject value) {
                internalPut(context, key, value);
            }
        });
        return this;
    }

    @JRubyMethod(name = "==")
    public IRubyObject op_equal(IRubyObject other) {
        Ruby runtime = getRuntime();
        if (this == other)
            return runtime.getTrue();
        if (!(other instanceof RedBlackTree)) 
            return runtime.getFalse();
        return this.dict_eq((RedBlackTree) other) ? runtime.getTrue() : runtime.getFalse();
    }

    private boolean dict_eq(RedBlackTree other) {
        if (this.size != other.size || ! similar(other))
            return false;
        for (Node node1 = minimum(), node2 = other.minimum();
                !node1.isNull() && !node2.isNull(); 
                node1 = successor(node1), node2 = other.successor(node2)) 
        {
            if (!node1.key.eql(node2.key) || !node1.value.eql(node2.value))
                return false;
        }
        return true;
    }

    private boolean similar(RedBlackTree other) {
        return this.cmpProc == other.cmpProc;
    }

    private byte[] comma_breakable(ThreadContext context, IRubyObject pp) {
        //if (pp == null) 
        return new byte[]{','};
        //return ((RubyString) RuntimeHelpers.invoke(context, pp, "comma_breakable")).getBytes();
    }

    private IRubyObject inspectRedBlackTree(final ThreadContext context, final IRubyObject pp) {
        final RubyString str = RubyString.newStringLight(context.runtime, DEFAULT_INSPECT_STR_SIZE);
        str.cat(new byte[]{'#', '<'}).cat(getMetaClass().getRealClass().getName().getBytes()).cat(": {".getBytes());
        final boolean[] firstEntry = new boolean[1];

        firstEntry[0] = true;
        final boolean is19 = context.runtime.is1_9();
        visitAll(new Visitor() {
            public void visit(IRubyObject key, IRubyObject value) {
                //if (!firstEntry[0]) str.cat((byte)',').cat((byte)' ');
                if (!firstEntry[0]) str.cat(comma_breakable(context, pp)).cat((byte)' ');

                RubyString inspectedKey = inspect(context, key);
                RubyString inspectedValue = inspect(context, value);

                if (is19) {
                    str.cat19(inspectedKey);
                    str.cat((byte)'=').cat((byte)'>');
                    str.cat19(inspectedValue);
                } else {
                    str.cat(inspectedKey);
                    str.cat((byte)'=').cat((byte)'>');
                    str.cat(inspectedValue);
                }
                
                firstEntry[0] = false;
            }
        });
        str.cat((byte) '}').cat(comma_breakable(context, pp)).cat(" default=".getBytes());
        if (ifNone == null) {
            str.cat("nil".getBytes());
        } else {
            str.cat(inspect(context, ifNone));
        }
        str.cat(comma_breakable(context, pp)).cat(" cmp_proc=".getBytes());
        if (cmpProc == null) {
            str.cat("nil".getBytes());
        } else {
            str.cat(inspect(context, cmpProc));
        }
        str.cat((byte)'>');
        return str;
    }

    //@JRubyMethod(name = "pretty_print")
    //public IRubyObject pretty_print(ThreadContext context, IRubyObject pp) {
    //    return inspectRedBlackTree(context, pp);
    //}

    @JRubyMethod(name = "inspect")
    public IRubyObject inspect(ThreadContext context) {
        if (getRuntime().isInspecting(this)) return getRuntime().newString("#<RBTree: ...>");

        try {
            getRuntime().registerInspecting(this);
            return inspectRedBlackTree(context, null);
        } finally {
            getRuntime().unregisterInspecting(this);
        }
    }

    @JRubyMethod(name = "to_s")
    public IRubyObject to_s(ThreadContext context) {
        Ruby runtime = context.runtime;
        if (runtime.isInspecting(this)) return runtime.newString("{...}");
        try {
            runtime.registerInspecting(this);
            return to_a().to_s();
        } finally {
            runtime.unregisterInspecting(this);
        }
    }

    private AtomicInteger iteratorCount = new AtomicInteger(0);
    private void iteratorEntry() {
        iteratorCount.incrementAndGet();
    }
    private void iteratorExit() {
        iteratorCount.decrementAndGet();
    }

    private void checkIterating() {
        if (iteratorCount.get() > 0) {
            throw getRuntime().newRuntimeError("can't add a new key into RBTree during iteration");
        }
    }

    private void visitAll(Visitor visitor) {
        for (Node x = minimum(); !x.isNull(); x = successor(x)) {
            visitor.visit(x.getKey(), x.getValue());
        }
    }

    public void iteratorReverseVisitAll(Visitor visitor){
        try {
            iteratorEntry();
            for (Node x = maximum(); !x.isNull(); x = predecessor(x)) {
                visitor.visit(x.getKey(), x.getValue());
            }
        } finally {
            iteratorExit();
        }
    }

    public void iteratorVisitAll(Visitor visitor){
        try {
            iteratorEntry();
            for (Node x = minimum(); !x.isNull(); x = successor(x)) {
                visitor.visit(x.getKey(), x.getValue());
            }
        } finally {
            iteratorExit();
        }
    }

    private static class VisitorIOException extends RuntimeException {
        VisitorIOException(Throwable cause) {
            super(cause);
        }
    }

    private static final ObjectMarshal RBTREE_MARSHAL = new ObjectMarshal() {
        public void marshalTo(Ruby runtime, final Object obj, RubyClass recv, final MarshalStream output) throws IOException {
            RedBlackTree rbtree = (RedBlackTree) obj;
            if (rbtree.size == 0) throw runtime.newArgumentError("cannot dump empty tree");
            if (rbtree.cmpProc != null) throw runtime.newArgumentError("cannot dump rbtree with compare proc");
            output.registerLinkTarget(rbtree);
            output.writeInt(rbtree.size);
            try {
                rbtree.visitAll(new Visitor() {
                    public void visit(IRubyObject key, IRubyObject value) {
                        try {
                            output.dumpObject(key);
                            output.dumpObject(value);
                        } catch (IOException e) {
                            throw new VisitorIOException(e);
                        }
                    }
                });
            } catch (VisitorIOException e) {
                throw (IOException) e.getCause();
            }
            //if (!rbtree.ifNone != null) output.dumpObject(rbtree.ifNone);
        }

        public Object unmarshalFrom(Ruby runtime, RubyClass type, UnmarshalStream input) throws IOException {
            RedBlackTree result = (RedBlackTree) type.allocate();
            input.registerLinkTarget(result);
            int size = input.unmarshalInt();
            for (int i = 0; i < size; i++) {
                result.internalPut(runtime.getCurrentContext(), input.unmarshalObject(), input.unmarshalObject());
            }
            //if (defaultValue) result.default_value_set(input.unmarshalObject());
            return result;
        }
    };

    private static abstract class Visitor {
        public abstract void visit(IRubyObject key, IRubyObject value);
    }

    private static class Found extends RuntimeException {
        @Override
        public synchronized Throwable fillInStackTrace() {
            return null;
        }
    }

    private static final Found FOUND = new Found();

    private static class FoundKey extends Found {
        public final IRubyObject key;
        FoundKey(IRubyObject key) {
            super();
            this.key = key;
        }
    }
}
