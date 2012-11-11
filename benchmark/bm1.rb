#$:.unshift(File.join(File.dirname(__FILE__), "..", "lib"))
require 'rbtree'
require "benchmark"

def rbt_bm
  n = 100_000
  a1 = []; n.times { a1 << rand(999_999) }
  a2 = []; n.times { a2 << rand(999_999) }

  start = Time.now

  tree = RBTree.new

  n.times {|i| tree[i] = i }
  n.times {|i| tree.delete(i) }

  tree = RBTree.new
  a1.each {|e| tree[e] = e }
  a2.each {|e| tree[e] }
  tree.each {|key, value| value + 1 }
  tree.reverse_each {|key, value| value + 1 }
  return Time.now - start
end

N = (ARGV[0] || 5).to_i

N.times do
  puts rbt_bm.to_f
  #puts "GC.count = #{GC.count}" if GC.respond_to?(:count)
end
