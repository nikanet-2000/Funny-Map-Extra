package funnymap.utils

import com.google.gson.JsonParser
import funnymap.FunnyMap
import net.minecraftforge.fml.common.versioning.DefaultArtifactVersion

object UpdateChecker {
    fun hasUpdate(): Int {
        val response = APIUtils.fetch("https://api.github.com/repos/Harry282/FunnyMap/releases")
        val version = JsonParser().parse(response).toJsonArray()
            ?.get(0)?.toJsonObject()
            ?.getJsonPrimitive("tag_name")?.asString ?: return 0
        val current = DefaultArtifactVersion(FunnyMap.MOD_VERSION.replace("pre", "beta"))
        val latest = DefaultArtifactVersion(version.replace("pre", "beta"))
        return latest.compareTo(current)
    }
}
