rbtree-jruby
============

This is a native implementation of RBTree for jruby.

From original RBTree c implementation README: 

RBTree is a sorted associative collection that is implemented with Red-Black Tree. The elements of RBTree are ordered and its interface is the almost same as Hash, so simply you can consider RBTree sorted Hash.

Red-Black Tree is a kind of binary tree that automatically balances
by itself when a node is inserted or deleted. Thus the complexity
for insert, search and delete is O(log N) in expected and worst
case. On the other hand the complexity of Hash is O(1). Because
Hash is unordered the data structure is more effective than
Red-Black Tree as an associative collection.

The elements of RBTree are sorted with natural ordering (by <=>
method) of its keys or by a comparator(Proc) set by readjust
method. It means all keys in RBTree should be comparable with each
other. Or a comparator that takes two arguments of a key should return
negative, 0, or positive depending on the first argument is less than,
equal to, or greater than the second one.

The interface of RBTree is the almost same as Hash and there are a
few methods to take advantage of the ordering:

  * lower_bound, upper_bound, bound
  * first, last
  * shift, pop
  * reverse_each

Note: while iterating RBTree (e.g. in a block of each method), it is
not modifiable, or TypeError is thrown.

RBTree supoorts pretty printing using pp.

This library contains two classes. One is RBTree and the other is
MultiRBTree that is a parent class of RBTree. RBTree does not allow
duplications of keys but MultiRBTree does.

```ruby
  require "rbtree"
  
  rbtree = RBTree["c", 10, "a", 20]
  rbtree["b"] = 30
  p rbtree["b"]              # => 30
  rbtree.each do |k, v|
    p [k, v]
  end                        # => ["a", 20] ["b", 30] ["c", 10]
    
  mrbtree = MultiRBTree["c", 10, "a", 20, "e", 30, "a", 40]
  p mrbtree.lower_bound("b") # => ["c", 10]
  mrbtree.bound("a", "d") do |k, v|
    p [k, v]
  end                        # => ["a", 20] ["a", 40] ["c", 10]
```

Benchmarks
-----------

Benchmark takes from headius' [redblack](https://github.com/headius/redblack) project.
Compared to [rbtree-pure](https://github.com/pwnall/rbtree-pure) and
[rbtree c extension](https://github.com/skade/rbtree).
![benchmark result](https://raw.github.com/isaiah/rbtree-jruby/master/benchmark/result.png)

For details, checkout [benchmark directory](https://github.com/isaiah/rbtree-jruby/tree/master/benchmark).

Requirement
-----------

  * JRuby

Install
-------

  $ sudo gem install rbtree-jruby

Copyright
---------

Copyright (c) 2012 Isaiah Peng. See LICENSE.txt for
further details.
