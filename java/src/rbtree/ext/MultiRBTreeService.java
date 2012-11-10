package rbtree.ext;

import org.jruby.Ruby;
import org.jruby.runtime.load.BasicLibraryService;

import java.io.IOException;

public class MultiRBTreeService implements BasicLibraryService {
    public boolean basicLoad(final Ruby ruby) throws IOException {
        MultiRBTree.createMultiRBTreeClass(ruby);
        return true;
    }
}
