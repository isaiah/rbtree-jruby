package rbtree.ext;

import org.jruby.Ruby;
import org.jruby.RubyObject;
import org.jruby.RubyClass;
import org.jruby.RubyString;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;

@JRubyClass(name = "RBTree")
public class RBTree extends RubyObject {
    public RBTree(final Ruby ruby, RubyClass rubyClass) {
        super(ruby, rubyClass);
    }

    @JRubyMethod(name = "initialize", rest = true)
    public IRubyObject initialize(ThreadContext context, IRubyObject[] args) {
        return this;
    }

    @JRubyMethod
    public RubyString get(ThreadContext context) {
        return RubyString.newString(context.getRuntime(), "test");
    }
}
