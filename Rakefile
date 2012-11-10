# encoding: utf-8

require 'rubygems'
require 'bundler'

include\
  begin
    RbConfig
  rescue NameError
    Config
  end

begin
  Bundler.setup(:default, :development)
rescue Bundler::BundlerError => e
  $stderr.puts e.message
  $stderr.puts "Run `bundle install` to install missing gems"
  exit e.status_code
end
require 'rake'

require 'jeweler'
Jeweler::Tasks.new do |gem|
  # gem is a Gem::Specification... see http://docs.rubygems.org/read/chapter/20 for more options
  gem.name = "rbtree-jruby"
  gem.homepage = "http://github.com/isaiah/rbtree-jruby"
  gem.license = "MIT"
  gem.summary = %Q{RBTree for jruby}
  gem.description = %Q{RBTree is a sorted associative collection that is implemented with Red-Black Tree. The elements of RBTree are ordered and its interface is the almost same as Hash, so simply you can consider RBTree sorted Hash.}
  gem.email = "issaria@gmail.com"
  gem.authors = ["Isaiah Peng"]
  # dependencies defined in Gemfile
end
Jeweler::RubygemsDotOrgTasks.new

require 'rake/testtask'
Rake::TestTask.new(:test) do |test|
  test.libs << 'lib' << 'test'
  test.pattern = 'test/**/test_*.rb'
  test.verbose = true
end

require 'rcov/rcovtask'
Rcov::RcovTask.new do |test|
  test.libs << 'test'
  test.pattern = 'test/**/test_*.rb'
  test.verbose = true
  test.rcov_opts << '--exclude "gems/*"'
end

task :default => :test

require 'rdoc/task'
Rake::RDocTask.new do |rdoc|
  version = File.exist?('VERSION') ? File.read('VERSION') : ""

  rdoc.rdoc_dir = 'rdoc'
  rdoc.title = "rbtree-jruby #{version}"
  rdoc.rdoc_files.include('README*')
  rdoc.rdoc_files.include('lib/**/*.rb')
end

require 'ant'

directory "pkg/classes"

desc "Clean up build artifacts"
task :clean do
  rm_rf "pkg/classes"
  rm_rf "lib/rbtree/ext/*.jar"
end

desc "Compile the extension"
task :compile => "pkg/classes" do |t|
  ant.javac :srcdir => "java", :destdir => t.prerequisites.first,
    :source => "1.5", :target => "1.5", :debug => true,
    :classpath => "${java.class.path}:${sun.boot.class.path}"
end

desc "Build the jar"
task :jar => [:clean, :compile] do
  ant.jar :basedir => "pkg/classes", :destfile => "lib/rbtree/ext/multi_r_b_tree.jar", :includes => "**/*.class"
end
 
task :package => :jar

desc "Run the specs"
task :spec => :jar do
  ruby "-S", "spec", "spec"
end
