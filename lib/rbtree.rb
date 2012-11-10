require 'rbtree/ext/multi_r_b_tree'

class RBTree < MultiRBTree
end

class MultiRBTree
  def pretty_print(pp)
    pp.text "#<#{self.class.to_s}: "
    pp.pp_hash self
    pp.comma_breakable
    pp.text "default="
    pp.pp default
    pp.comma_breakable
    pp.text "cmp_proc="
    pp.pp cmp_proc
    pp.text ">"
  end

  def pretty_print_cycle(pp)
    pp.text "\"#<#{self.class.to_s}: ...>\""
  end
end
