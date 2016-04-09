# androvdr
This is a copy of https://code.google.com/archive/p/androvdr/ .

The apk from Google Play Store is from 2012 and crashes on CM13. I've imported the source into Android Studio 1.5 and recompiled it.

Some minor changes were made

1) ACRA was removed - it doesn't make sense anymore without anyone maintaining the project and a deprecated function was used.

2) Default Port for svdrp was set to 6419 instead of 2001 - as used in actual VDR-versions.

The compiled apk is included in directory androVDR.
