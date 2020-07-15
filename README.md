# Requirements #

This plugin requires XL Release version 4.5+. For older versions of XL Release please use version [4.0.11](http://updates.jenkins-ci.org/download/plugins/xlrelease-plugin/4.0.11/xlrelease-plugin.hpi) of this plugin.

## Build ##
The Jenkins plugin build is powered by the <a href="https://github.com/jenkinsci/gradle-jpi-plugin">gradle-jpi-plugin</a> (see its <a href="https://wiki.jenkins-ci.org/display/JENKINS/Gradle+JPI+Plugin">documentation</a>).

There are following targets defined:

Builds **.hpi** file

    ./gradlew jpi

Run development server:

    ./gradlew server

## Debugging ##

Debugging is configured with GRADLE_OPTIONS env variable.

    GRADLE_OPTS="${GRADLE_OPTS} -Xdebug -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=8000" ./gradlew clean server

## Testing ##

Additionally to unit tests there are integration tests which you can run by following command:

    ./gradlew clean itest

Integration tests require that you have a running XL Release server at http://localhost:5516 with standard credentials. You can override the location and credentials using Gradle properties (`gradle.properties` file or command-line):

    xlReleaseIntegration.host=https://my.xl-release/xl-release-context
    xlReleaseIntegration.username=user
    xlReleaseIntegration.password=password

NOTE: if you change one or more of those parameters it is mandatory to clean before running the tests again.

### Development and Gradle ###

There are several caveats which might be useful when updating Jenkins core version or `jpi` plugin version.

#### jenkins-module vs jm ####

There are some dependencies coming inserted by `jpi` plugin which fail to be resolved. Seems like some artifacts used to have `jenkins-module` packaging and it was replaced by `jm`. You will see an error like this:

    Could not resolve all dependencies for configuration ':testCompile'.
    > Could not find instance-identity.jenkins-module (org.jenkins-ci.modules:instance-identity:1.3).
      Searched in the following locations:
          http://maven.jenkins-ci.org/content/repositories/releases/org/jenkins-ci/modules/instance-identity/1.3/instance-identity-1.3.jenkins-module

To workaround this you need to add proper dependency and exclude the missing one:

    dependencies {
        testCompile "org.jenkins-ci.modules:instance-identity:1.3@jm"
    }
    configurations {
        all*.exclude group: 'org.jenkins-ci.modules', module: 'instance-identity'
    }

You might have to do this for several dependencies.

#### Class not found when running itest ####

For some reason not all dependencies get inherited by Gradle configuration `itestCompile` from `testCompile`. You will see an error like this in your itest results:

    java.lang.NoClassDefFoundError: hudson/tasks/Mailer

To fix this you need to add all missing dependencies manually. You can find them using following commands for example:

    ./gradlew dependencies --configuration testCompile > t.txt
    ./gradlew dependencies --configuration itestCompile > i.txt
    diff t.txt i.txt

Then add them to dependencies:

    dependencies {
        itestCompile "org.jenkins-ci.plugins:ant:1.1@jar"
        ...
    }

## Releasing ##

See the [article on XebiaLabs wiki](https://xebialabs.atlassian.net/wiki/display/Labs/Developing+and+releasing+the+Jenkins+plugin).

## Notes ##

As doCheck and doAutoComplete not work seamlessly, We gave a validate button for version 6.0.0 of plugin. For more information see Jenkins Bug [JENKINS-37204](https://issues.jenkins-ci.org/browse/JENKINS-37204)
