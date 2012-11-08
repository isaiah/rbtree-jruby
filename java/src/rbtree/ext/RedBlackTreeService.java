package rbtree.ext;

import org.jruby.Ruby;
import org.jruby.runtime.load.BasicLibraryService;

import java.io.IOException;

public class RedBlackTreeService implements BasicLibraryService {
    public boolean basicLoad(final Ruby ruby) throws IOException {
        RedBlackTree.createRedBlackTreeClass(ruby);
        return true;
    }
}
