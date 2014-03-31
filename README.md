#Build#
The Jenkins plugin build is powered by the <a href="https://github.com/jenkinsci/gradle-jpi-plugin">gradle-jpi-plugin</a> (see its <a href="https://wiki.jenkins-ci.org/display/JENKINS/Gradle+JPI+Plugin">documentation</a>).

There are following targets defined:

Builds **.hpi** file

    gradle jpi

Run development server:

    gradle server

###Debugging###

Debugging is configured with GRADLE_OPTIONS env variable.

    GRADLE_OPTS="${GRADLE_OPTS} -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5006" ./gradlew clean server

###Testing###

Running integration testing can be done using
    
    gradle test

This uses the `mvn test` because of the following issues with the gradle jpi plugin
https://issues.jenkins-ci.org/browse/JENKINS-17129
https://issues.jenkins-ci.org/browse/JENKINS-19942