# Keep kotlinx.serialization generated serializers for our DTOs.
-keepclassmembers @kotlinx.serialization.Serializable class com.ssui.mobile.data.remote.dto.** {
    *** Companion;
    *** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}
