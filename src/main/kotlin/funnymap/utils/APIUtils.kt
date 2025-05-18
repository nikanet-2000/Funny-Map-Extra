package funnymap.utils

import com.google.gson.JsonParser
import funnymap.config.Config
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils

object APIUtils {
    fun fetch(uri: String): String? {
        HttpClients.createMinimal().use {
            try {
                val httpGet = HttpGet(uri)
                return EntityUtils.toString(it.execute(httpGet).entity)
            } catch (e: Exception) {
                return null
            }
        }
    }

    fun getSecrets(uuid: String): Int {
        val response = fetch("https://api.hypixel.net/player?key=${Config.apiKey}&uuid=${uuid}") ?: return 0
        val jsonObject = JsonParser().parse(response).toJsonObject() ?: return 0
        if (jsonObject.getJsonPrimitive("success")?.asBoolean == true) {
            return jsonObject.getJsonObject("player")?.getJsonObject("achievements")
                ?.getJsonPrimitive("skyblock_treasure_hunter")?.asInt ?: return 0
        }
        return 0
    }

    fun hasBonusPaulScore(): Boolean {
        val response = fetch("https://api.hypixel.net/resources/skyblock/election") ?: return false
        val jsonObject = JsonParser().parse(response).toJsonObject() ?: return false
        if (jsonObject.getJsonPrimitive("success")?.asBoolean == true) {
            val mayor = jsonObject.getJsonObject("mayor") ?: return false
            val name = mayor.getJsonPrimitive("name")?.asString
            if (name == "Paul") {
                return mayor.getJsonArray("perks")?.any {
                    it.toJsonObject()?.getJsonPrimitive("name")?.asString == "EZPZ"
                } ?: false
            }
        }
        return false
    }
}
