[![Build Status](https://travis-ci.org/EqualExperts/opslogger.svg?branch=master)](https://travis-ci.org/EqualExperts/opslogger)
[ ![Download](https://api.bintray.com/packages/equalexperts/open-source/opslogger/images/download.svg) ](https://bintray.com/equalexperts/open-source/opslogger/_latestVersion)

##How to get it
This library is available as a gradle dependency via Jcenter:

    "com.equalexperts:opslogger:0.1.1"

A support library is also available, which has extra features for testing your code that uses logging, and a tool
to generate documentation. Production code should not depend on the support library.

    "com.equalexperts:opslogger-support:0.1.1"


###Using Maven?
If you use maven, here are the dependency declarations you should use:

```xml
<dependency>
	<groupId>com.equalexperts</groupId>
	<artifactId>opslogger</artifactId>
	<version>0.1.1</version>
</dependency>

<dependency>
	<groupId>com.equalexperts</groupId>
	<artifactId>opslogger-support</artifactId>
	<version>0.1.1</version>
</dependency>
```

###Not using Jcenter already?
For maven, see this guide: https://bintray.com/bintray/jcenter#

For gradle, just add this snippet to your build script:

```groovy
repositories {  
    jcenter()  
}
```

##Documentation
For now, the best documentation is the sample-usage project in this repository. More (and better!) documentation is coming soon.

##Getting started with the source code
This project is built with [Gradle](http://www.gradle.org/). We use a feature called the "gradle wrapper" that will automatically install
Gradle if you don't have it already. You can generate an IDE template by typing `./gradlew cleanIdea idea` from the command line. This
will generate project files for [Intellij IDEA](http://www.jetbrains.com/idea/). Build the project and run all unit tests by typing
`./gradlew ci`.You can get information on other tasks by typing `./gradlew tasks`.

**Note that this software requires Java 8.**
