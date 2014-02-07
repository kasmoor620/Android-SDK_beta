# Android SDK 5.1

This repository hosts the Weemo Android SDK and accompanying
demonstration app called "SDK_Helper".  The subdirectory named
"WeemoSDK_Lib" contains the SDK.  The repository as a whole can be
imported into Eclipse and the project is named "SDK_Helper."

If you are interested in obtaining only the Android SDK, copy the
subdirectory "WeemoSDK_Lib" into your own project.

The rest of this note describes how to compile and build the SDK_Helper.

## Overview

This project was made with Eclipse.  We recommend using Eclipse for building the SDK Helper to ensure a smooth experience.

Follow these 5 Step to test Weemo Technologies into a sample application.


## Run the code


### Step 0 - Install the sources

```
git clone git@github.com:weemo/Android-SDK_beta.git
```

Later, when you update the sources, use:

```
git pull
```


### Step 1 - Check Android Developement Tools

Make sure you have Android plug-in installed within Eclipse.

Android Development Tools (ADT) is a plugin for the Eclipse IDE that is designed to give you a powerful, integrated environment in which to build Android applications.

ADT extends the capabilities of Eclipse to let you quickly set up new Android projects, create an application UI, add packages based on the Android Framework API, debug your applications using the Android SDK tools, and even export signed (or unsigned) .apk files in order to distribute your application.

If you need to install ADT, please check the [official ADT website](http://developer.android.com/sdk/installing/installing-adt.html)


### Step 2 - Install Findbugs Eclipse plug-in

FindBugs™ is a program to find bugs in Java programs. It looks for instances of "bug patterns" --- code instances that are likely to be errors.

While the SDK uses FindBugs, you don't have to install FindBugs to use th SDK. However, you have to install FindBugs to compile and use the Helper.

If you don't have Findbugs on your Eclipse, please download-it and intall-it. 

Follow instruction on projcet website:  [http://findbugs.sourceforge.net/](http://findbugs.cs.umd.edu/eclipse/)


### Step 3 - Import the project in Eclipse 

Import the Helper project in Eclipse as an "Existing Project"

<p align="center">
<img src="http://docs.weemo.com./img/android_build.png">

</p>


### Step 4 -  Configure your mobileAppId 

To configure you mobileWebId regarding the configuration of your Profile in Weemo Portal, please edit the configuration file: 
/res/values/weemo_conf.xml

Replace your "ENTER YOUR KEY HERE" by your mobileAppId provided by Weemo:

```
<?xml version="1.0" encoding="utf-8"?>
<resources xmlns:tools="http://schemas.android.com/tools" tools:ignore="TypographyEllipsis">

    <string name="weemo_mobileAppId">ENTER YOUR KEY HERE</string>
    
</resources>
```


### Step 5 - Run the project 

This project cannot run in an Emulator, please run it on your Android mobile ARM Device.

<p align="center">
<img src="http://docs.weemo.com/img/android_run.png">
</p>


## Changelog

This helper project is updated on an ad-hoc basis and does not necessary follows the relase agenda of the SDK.  
The best changelog is therefore the [commit log](https://github.com/weemo/Android-SDK_beta/commits/master).  
However, a simple changelog is maintained as part of the [SDK changelog](https://github.com/weemo/Android-SDK/blob/master/CHANGELOG.md).
