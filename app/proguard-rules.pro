# Maximum R8 Optimization & Obfuscation Rules

# 3 passes of optimization
-optimizationpasses 3

# Allow R8 to expand access modifiers (e.g. public/protected/private) for deeper optimizations and inlining
-allowaccessmodification

# Repackage obfuscated classes into root package for maximum obfuscation and reduced DEX size
-repackageclasses ''

# Overload method names aggressively during obfuscation
-overloadaggressively

# Keep dynamic launcher aliases referenced by string concatenation in LauncherIconController
-keep class com.pulse.bluetoothdisable.LauncherAlias*

# Keep Device Admin Receiver and Tile Service components
-keep class com.pulse.bluetoothdisable.admin.AppDeviceAdminReceiver
-keep class com.pulse.bluetoothdisable.quicksettings.BluetoothDisableTileService
