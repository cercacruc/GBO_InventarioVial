# Release must not emit existing file paths or diagnostic payloads through Android Log.
# No broad keep rules: Room/WorkManager and other libraries supply their own consumer rules.
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
    public static int wtf(...);
    public static int println(...);
}
