#$:.unshift(File.join(File.dirname(__FILE__), "..", "lib"))
require 'rbtree'
require "benchmark"

def rbt_bm
  n = 1_000
  a1 = []; n.times { a1 << rand(999_999) }
  a2 = []; n.times { a2 << rand(999_999) }

  start = Time.now
  tree = RBTree.new
  a1.each {|e| tree[e] = e }
  a2.each {|e| tree[e] }
  n.times do
    tree.delete_if{|k, v| v == rand(999_999)}
  end

  return Time.now - start
end

N = (ARGV[0] || 5).to_i

N.times do
  puts rbt_bm.to_f
  puts "GC.count = #{GC.count}" if GC.respond_to?(:count)
end
