# PureMusic proguard rules.
# Kept empty for P1. R8 rules added per-phase when dependencies need them.

# TagLib JNI 桥接类：native 方法名不可被混淆
-keepclassmembers class com.pure.music.taglib.TagLibMetadataReader {
    private native <methods>;
}
-keepclassmembers class com.pure.music.taglib.TagLibWriter {
    private native <methods>;
}
