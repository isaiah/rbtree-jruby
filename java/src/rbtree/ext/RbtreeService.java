package rbtree.ext;

import org.jruby.Ruby;
import org.jruby.RubyObject;
import org.jruby.RubyClass;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.load.BasicLibraryService;
import org.jruby.runtime.builtin.IRubyObject;

import java.io.IOException;

public class RbtreeService implements BasicLibraryService {
    public boolean basicLoad(final Ruby ruby) throws IOException {
        RubyClass rbtreeClass = ruby.defineClass("RBTree", ruby.getObject(), new ObjectAllocator() {
            public IRubyObject allocate(Ruby runtime, RubyClass klazz) {
                return new RBTree(runtime, klazz);
            }
        });
        rbtreeClass.defineAnnotatedMethods(RBTree.class);
        return true;
    }
}
