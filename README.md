Adds placeholder-variable support to your res/xml files.  Save you keystrokes and reduce hard-to-debug runtime problems caused by package-name typos!

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

WARNING: Experimental and totally non-functional!

Copyright (C) 2017 Benjamin Bader
