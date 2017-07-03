Adds placeholder-variable support to your res/xml files.  Saves you keystrokes and reduces hard-to-debug runtime problems caused by package-name typos!

For example, in `res/xml/syncadapter.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<sync-adapter
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:contentAuthority="${applicationId}.providerAuthority"
    android:accountType="${applicationId}.ACCOUNT"
    android:allowParallelSyncs="false"
    android:isAlwaysSyncable="true"
    android:supportsUploading="true"
    android:userVisible="false"
    />
```

To use:

```gradle
// in root build.gradle

buildscript {
  dependencies {
    classpath 'com.bendb.placeholders:placeholders:0.1.0'
  }
}

// in your app's build.gradle

apply plugin: 'com.android.application' // MUST COME FIRST
apply plugin: 'com.bendb.placeholders'

// Any AndroidManifest.xml placeholder will now be applied to your XML resources...
android {
  defaultConfig {
    // ...even those that you customize
    manifestPlaceholders = [
      someValue: "MyGreatValue"
    ]
  }
}
```

This plugin Works On My Machine, but only for aapt version 1.  Version 2 is the default aapt for build tools 3, incidentally.

If you want to use this plugin with the new build tools, you can disable aapt2 in `gradle.properties` by adding the following line:
```
android.enableAapt2=false
```

Copyright (C) 2017 Benjamin Bader
