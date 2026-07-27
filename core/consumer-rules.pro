# Правила, специфичные для зависимостей :core (Room, SQLCipher, Tink/security-crypto).
# Помечены consumerProguardFiles — значит применяются автоматически к любому модулю,
# который зависит от :core (то есть к :app), без ручного дублирования в его proguard-rules.pro.

# ---- Room ----
-keep class dev.skhoron.notes.data.local.entity.** { *; }
-keep class dev.skhoron.notes.data.local.Converters { *; }
-dontwarn androidx.room.paging.**

# ---- EncryptedSharedPreferences / Tink (security-crypto) ----
-keep class com.google.crypto.tink.** { *; }
-keep class com.google.crypto.tink.proto.** { *; }
-keepclassmembers class com.google.crypto.tink.** { *; }
-dontwarn com.google.crypto.tink.**
-dontwarn com.google.errorprone.annotations.**

# ---- SQLCipher ----
-keep class net.sqlcipher.** { *; }
-keep class net.sqlcipher.database.** { *; }
-dontwarn net.sqlcipher.**

# ---- org.json (сериализация чек-листа в Converters) ----
-dontwarn org.json.**

# ---- Kotlin coroutines / Flow ----
-keepclassmembernames class kotlinx.coroutines.** { volatile <fields>; }
-dontwarn kotlinx.coroutines.**