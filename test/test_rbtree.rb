require 'helper'

class TestRbtree < Test::Unit::TestCase
  should "work" do
    rbtree = RBTree.new
    assert_equal rbtree.get, "test"
  end
end
