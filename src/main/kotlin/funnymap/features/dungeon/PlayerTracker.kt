package funnymap.features.dungeon

import funnymap.FunnyMap
import funnymap.FunnyMap.mc
import funnymap.FunnyMap.scope
import funnymap.core.DungeonPlayer
import funnymap.core.RoomData
import funnymap.core.map.Room
import funnymap.core.map.RoomState
import funnymap.core.map.RoomType
import funnymap.core.map.Tile
import funnymap.utils.APIUtils
import funnymap.utils.Utils.equalsOneOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import net.minecraft.event.HoverEvent
import net.minecraft.util.ChatComponentText
import net.minecraft.util.ChatStyle
import net.minecraft.util.IChatComponent
import kotlin.time.Duration.Companion.milliseconds

object PlayerTracker {

    val roomClears: MutableMap<RoomData, Set<String>> = mutableMapOf()

    fun roomStateChange(room: Tile, state: RoomState, newState: RoomState) {
        if (room !is Room) return
        if (newState.equalsOneOf(RoomState.CLEARED, RoomState.GREEN) && state != RoomState.CLEARED) {
            val currentRooms =
                Dungeon.dungeonTeammates.map { Pair(it.value.formattedName, it.value.getCurrentRoom()) }
            roomClears[room.data] =
                currentRooms.filter { it.first != "" && it.second == room.data.name }.map { it.first }.toSet()
        }
    }

    fun onDungeonEnd() {
        val time = System.currentTimeMillis() - Dungeon.Info.startTime
        Dungeon.dungeonTeammates.forEach {
            it.value.roomVisits.add(Pair(time - it.value.lastTime, it.value.lastRoom))
        }

        scope.launch {
            Dungeon.dungeonTeammates.map { (_, player) ->
                async(Dispatchers.IO) {
                    Triple(
                        player.formattedName,
                        player,
                        APIUtils.getSecrets(player.uuid)
                    )
                }
            }.map {
                val (name, player, secrets) = it.await()
                getStatMessage(name, player, secrets)
            }.forEach {
                mc.thePlayer.addChatMessage(it)
            }
        }
    }

    fun getStatMessage(name: String, player: DungeonPlayer, secrets: Int): IChatComponent {
        val secretsComponent = ChatComponentText("§b${secrets - player.startingSecrets} §3secrets")

        val allClearedRooms = roomClears.filter { it.value.contains(name) }
        val soloClearedRooms = allClearedRooms.filter { it.value.size == 1 }
        val max = allClearedRooms.size
        val min = soloClearedRooms.size

        val roomComponent = ChatComponentText(
            "§b${if (soloClearedRooms.size != allClearedRooms.size) "$min-$max" else max} §3rooms cleared"
        ).apply {
            chatStyle =
                ChatStyle().setChatHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT, ChatComponentText(
                    roomClears.entries.filter {
                        !it.key.type.equalsOneOf(RoomType.BLOOD, RoomType.ENTRANCE, RoomType.FAIRY) &&
                                it.value.contains(name)
                    }.joinToString(
                        separator = "\n",
                        prefix = "$name's §eRooms Cleared:\n"
                    ) { (room, players) ->
                        if (players.size == 1) {
                            "§6${room.name}"
                        } else {
                            "§6${room.name} §7with ${
                                players.filter { it != name }.joinToString(separator = "§r, ")
                            }"
                        }
                    }
                )))
        }

        var lastTime = 0L

        val splitsComponent = ChatComponentText("§3Splits").apply {
            chatStyle = ChatStyle().setChatHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT, ChatComponentText(
                player.roomVisits.joinToString(
                    separator = "\n",
                    prefix = "$name's §eRoom Splits:\n"
                ) { (elapsed, room) ->
                    val start = lastTime.milliseconds
                    lastTime += elapsed
                    val end = lastTime.milliseconds
                    "§b$start §7- §b$end §6$room"
                }
            )))
        }

        val roomTimeComponent = ChatComponentText("§3Times").apply {
            chatStyle = ChatStyle().setChatHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT, ChatComponentText(
                player.roomVisits.groupBy { it.second }.entries.joinToString(
                    separator = "\n",
                    prefix = "$name's §eRoom Times:\n"
                ) { (room, times) ->
                    "§6$room §a- §b${times.sumOf { it.first }.milliseconds}"
                }
            )))
        }

        return ChatComponentText("${FunnyMap.CHAT_PREFIX} §3$name §f> ")
            .appendSibling(secretsComponent).appendText(" §6| ")
            .appendSibling(roomComponent).appendText(" §6| ")
            .appendSibling(splitsComponent).appendText(" §6| ")
            .appendSibling(roomTimeComponent)
    }
}
