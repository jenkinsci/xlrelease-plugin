## Preface ##

This document describes the functionality provided by **XL Release Jenkins Plugin**.

## Overview ##
The **XL Release Jenkins Plugin** enables 2 post-build actions: "Create a release from a template" and "Start a release" which can be combined sequentially.

### Features ###

* Create a release from a template
	* Select the template to create the release from
	* Enter variables as defined within the template
* Start a release

### Requirements ###

* **Jenkins**: Jenkins **LTS** version {{supportedLtsVersion}} or higher.

### Create a release from a template ###

Creates a release from a template that was defined in XL Release. You can also specify the variables that are contained in the template.

### Start a release ###

If checked, the created release will directly be started.


## Configuration ##

There are 2 places to configure the **XL Release Jenkins Plugin**: global Jenkins configuration and job configuration.

### Plugin configuration ###

At *Manage Jenkins* -> *Configure System* you can specify XL Release server URL and one or more sets of credentials. Different sets can be used for different jobs.

### Job configuration ###

In the Job Configuration page, choose *Post-build Actions* -> *Add post-build action* -> *Release with XL Release*. Now you can configure which actions to perform and other settings. To get an explanation of each setting, click on the question mark sign located right next to each setting.

##How do I ...?##

**Generate version of my application automatically?**

If you practice continuous delivery, it may be useful to increase the version automatically after each build. For this you can use one of Jenkins environment variables in the Version field. For example ${BUILD_NUMBER}. To see the complete list of available variables you can go to http://\<jenkins-host\>:\<jenkins-port\>/env-vars.html

## Release notes ##