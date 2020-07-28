# Links

* Plugin Home: https://github.com/sbt/sbt/tree/0.13/scripted/plugin
* Code
** Plugin: https://github.com/sbt/sbt/blob/0.13/scripted/plugin/src/main/scala/sbt/ScriptedPlugin.scala
** Scripted: https://github.com/sbt/sbt/blob/0.13/scripted/sbt/src/main/scala/sbt/test/ScriptedTests.scala
** Scripted's dependencies: https://github.com/sbt/sbt-zero-thirteen/tree/0.13/scripted/base/src/main/scala/xsbt/test
* Blog post from the author of sbt-assembly: http://eed3si9n.com/testing-sbt-plugins
* Examples
** From xsbt-web-plugin: https://github.com/earldouglas/xsbt-web-plugin/tree/master/src/sbt-test
** From sbt-assembly: https://github.com/sbt/sbt-assembly/tree/master/src/sbt-test/sbt-assembly

# Usage

## All tests

```bash
4 sbt
sbt$ scripted
```

## Specific tests

```bash
sbt$ scripted [group]/[test]
```

For example to run all the tests in "versioning":

```bash
sbt$ scripted versioning/*
```

For example to run the "fromGit" tests in "versioning":

```bash
sbt$ scripted versioning/fromGit
```

# Notes

* When running tests you will see "[error]" statements. These do not (always) mean errors in the test; they are errors
 in the *code under test*, which is often what is being tested. The final "[success]" or "[error]" is really the only
 the only one that matters
* You cannot have anything but directories within a [group] -- any files will be treated as directories and it will
 bail out with an error.
* Code in the scripted tests won't be compiled until *after* the plugin is published. So try to avoid being lazy and
 letting the compiler catch your dumb errors because it takes a while for the compiler to get the point where it can
 check your code
* IntelliJ does not really understand what's going on with scripted tests. It can see Scala files and gives you syntax
 highlighting but it can't tell there's a "project" so referencing a class from another file will show errors. Just
 don't trust IntelliJ too much.
* If you add a new check task you *must* parse the arguments even if you don't use them, i.e. you must add
```val cmd: Seq[String] = spaceDelimited("...").parsed``` to your check task.
* I think you can add a [file named "disabled"](https://github.com/sbt/sbt/blob/1.0.x/scripted/sbt/src/main/scala/sbt/test/ScriptedTests.scala#L40)
 to the [test] directory to disable the test.
* You must put all your tests in a file named "test". It is [not configurable](https://github.com/sbt/sbt/blob/0.13/scripted/sbt/src/main/scala/sbt/test/ScriptedTests.scala#L19)
* You can make a separate test file called "[pending](https://github.com/sbt/sbt/blob/0.13/scripted/sbt/src/main/scala/sbt/test/ScriptedTests.scala#L20)".
 I think "pending" tests are like a scratch area so you can run a few tests in isolation before moving them to the main
 "test" file.
 ** Their value is somewhat dubious -- Their value is dubious. I've found it useful when working with large
 "test" scripts. You can copy out a subsection of "test" into "pending", get "pending" passing, and copy it back into
 test".
* A "pending" test will always fail -- even if all the tests pass. The final failure is cryptic message "Mark as
 passing to remove this failure." which I think means to copy all your passing tests to "test".
* Pending tests will always create a failure at the end. I guess so you can never mistakenly merge them? (There's no
 comments; I'm forced to speculate.)* You cannot have a comment line with no comment, e.g. ```# ``` is an invalid comment line.
* Many of the built-in "]" commands require quotes around the entire command if there are any spaces
* Be careful with escaping and spaces:
** Sometimes you have to double escape things, e.g. if you want to '\' you need to '\\'
** An extra or unexpected spaces will break the parser, .e.g ```] '; task1 ; task2'``` is fine but
```] ' ; task1 ; task2'``` (space between the first ' and ;) will fail.
* If you have a syntax error in your script then the script will fail as soon as you try running it. If this happens
  with a "pending" script you won't get an error telling you what the problem is, it just fails mysteriously. ```] last
  scripted``` won't tell you anything more either.
* You can combine commands into single line using ";" as long as you start with a ";"
* It is best to use semver/log's "grepLog" command with triple quotes.
* Scripted tests ALWAYS run at 'Info' logging levels. [You can't change it.](https://github.com/sbt/sbt-zero-thirteen/blob/0.13/util/log/src/main/scala/sbt/BasicLogger.scala#L9)
