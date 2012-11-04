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
  gem.summary = %Q{Red Black tree java extension}
  gem.description = %Q{TODO: longer description of your gem}
  gem.email = "isaiah.peng@vcint.com"
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

JAVA_DIR = "java/src/rbtree/ext"
JAVA_SOURCES = FileList["#{JAVA_DIR}/*.java"]
JAVA_CLASSES = []
JAVA_RBTREE_JAR = File.expand_path("lib/rbtree/ext/rbtree.jar")

JRUBY_JAR = File.join(CONFIG['libdir'], 'jruby.jar')
if File.exists?(JRUBY_JAR)
  JAVA_SOURCES.each do |src|
    classpath = (Dir["java/lib/*.jar"] << "java/src" << JRUBY_JAR) * ":"
    obj = src.sub(/\.java/, '.class')
    file obj => src do
      sh "javac", "-classpath", classpath, "-source", "1.6", "-target", "1.6", src
    end
    JAVA_CLASSES << obj
  end
end

desc "Compile jruby extendsion"
task :compile => JAVA_CLASSES

file JAVA_RBTREE_JAR => :compile do
  cd "java/src" do
    rbtree_classes = FileList["rbtree/ext/*.class"]
    sh "jar", "cf", File.basename(JAVA_RBTREE_JAR), *rbtree_classes
    mv File.basename(JAVA_RBTREE_JAR), File.dirname(JAVA_RBTREE_JAR)
  end
end

desc "Create ext jar"
task :jar => JAVA_RBTREE_JAR
