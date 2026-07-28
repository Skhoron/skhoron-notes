# Skhoron Notes (:app) — правила R8/ProGuard для release-сборки.
#
# Правила, специфичные для Room/Tink/SQLCipher/org.json теперь приезжают автоматически
# из :core/consumer-rules.pro (модуль :core помечен consumerProguardFiles) — дублировать
# их здесь не нужно.

# ---- Coil (загрузка изображений вложений/логотипа) ----
-dontwarn coil.**

# ---- Kotlin metadata ----
-keepattributes *Annotation*, InnerClasses, Signature, Exceptions
-keep class kotlin.Metadata { *; }

# ---- Наш собственный Application-класс и точки входа ----
-keep class dev.skhoron.notes.SkhoronNotesApp { *; }
-keep class dev.skhoron.notes.MainActivity { *; }

# ВАЖНО: эти правила не проверены реальной release-сборкой (в среде разработки нет
# интернета для прогонки Gradle). Перед публикацией собери `./gradlew assembleRelease`
# локально и убедись, что приложение запускается после минификации.