# Learner2g
The historic source code of a now obsolete Java-based e-learning platform.

To compile it, go in the directory `198-learner2g-stable` and run `./import.rb`. Despite being developed a few years back, I was able to compile with recent stable Java 8 without too much trouble. If you can't compile, you're probably missing a development dependency, like `Ruby` or `fastjar`. Usually this can be figured out by examining the log output carefully for clues.

As far as running it goes, you're on your own. These days browsers don't support Java, though IIRC the student application was able to launch stand-alone as well. The teacher side always launches stand-alone. In the middle IIRC you need an invocation of the Deliver2g server. Sorry for the sparse documentation this project was always intended for internal use only.
