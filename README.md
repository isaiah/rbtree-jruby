rbtree-jruby
============

This is a native implementation of RBTree for jruby.

RBTree is a sorted associative collection that is implemented with Red-Black Tree. The elements of RBTree are ordered and its interface is the almost same as Hash, so simply you can consider RBTree sorted Hash.

Usage
-------------
```ruby
irb(main):001:0> require 'rbtree'
=> true
irb(main):002:0> a = RBTree[*%w[a A b B d D c C]]
=> #<RBTree: {"a"=>"A", "b"=>"B", "c"=>"C", "d"=>"D"}, default=nil, cmp_proc=nil>
irb(main):003:0> a.keys
=> ["a", "b", "c", "d"]
irb(main):004:0> a.values
=> ["A", "B", "C", "D"]
```

Contributing to rbtree-jruby
----------------------------
 
* Check out the latest master to make sure the feature hasn't been implemented or the bug hasn't been fixed yet.
* Check out the issue tracker to make sure someone already hasn't requested it and/or contributed it.
* Fork the project.
* Start a feature/bugfix branch.
* Commit and push until you are happy with your contribution.
* Make sure to add tests for it. This is important so I don't break it in a future version unintentionally.
* Please try not to mess with the Rakefile, version, or history. If you want to have your own version, or is otherwise necessary, that is fine, but please isolate to its own commit so I can cherry-pick around it.

Copyright
---------

Copyright (c) 2012 Isaiah Peng. See LICENSE.txt for
further details.
