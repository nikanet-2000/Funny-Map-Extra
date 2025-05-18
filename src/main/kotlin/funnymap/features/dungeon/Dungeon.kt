package funnymap.features.dungeon

import funnymap.config.Config
import funnymap.core.DungeonPlayer
import funnymap.core.map.*
import funnymap.events.ChatEvent
import funnymap.features.dungeon.Dungeon.Info.ended
import funnymap.utils.Location
import funnymap.utils.Location.inDungeons
import funnymap.utils.MapUtils
import funnymap.utils.TabList
import funnymap.utils.Utils.equalsOneOf
import gg.essential.universal.UChat
import net.minecraft.event.ClickEvent
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object Dungeon {

    val dungeonTeammates = mutableMapOf<String, DungeonPlayer>()
    val espDoors = mutableListOf<Door>()

    private val keyGainRegex = listOf(
        Regex(".+ §r§ehas obtained §r§a§r§.+ Key§r§e!§r"),
        Regex("§r§eA §r§a§r§.+ Key§r§e was picked up!§r")
    )
    private val keyUseRegex = listOf(
        Regex("§r§cThe §r§c§lBLOOD DOOR§r§c has been opened!§r"),
        Regex("§r§a.+§r§a opened a §r§8§lWITHER §r§adoor!§r"),
    )

    fun onTick() {
        if (!inDungeons) return

        if (shouldSearchMimic()) {
            MimicDetector.findMimic()?.let {
                if (Config.scanChatInfo) UChat.chat("&7Mimic Room: &c$it")
                Info.mimicFound = true
            }
        }

        if (!MapUtils.calibrated) {
            MapUtils.calibrated = MapUtils.calibrateMap()
        }

        if (MapUtils.mapDataUpdated) {
            MapUpdate.updateRooms()
            MapUtils.mapDataUpdated = false
        }

        if (Location.dungeonFloor.equalsOneOf(6, 7)) {
            MimicDetector.checkMimicDead()
        }
        ScoreCalculation.updateScore()

        TabList.getDungeonTabList()?.let {
            MapUpdate.updatePlayers(it)
            RunInformation.updatePuzzleCount(it)
        }

        if (DungeonScan.shouldScan) {
//            scope.launch { DungeonScan.scan() }
            DungeonScan.scan()
        }
    }

    @SubscribeEvent
    fun onChatPacket(event: ChatEvent) {
        if (!inDungeons) return
        if (event.packet.chatComponent.siblings.any {
                it.chatStyle?.chatClickEvent?.run {
                    action == ClickEvent.Action.RUN_COMMAND && value == "/showextrastats"
                } == true
            }) {
            ended = true
            if (Config.teamInfo) {
                PlayerTracker.onDungeonEnd()
            }
        }

        if (keyGainRegex.any { it.matches(event.packet.chatComponent.formattedText) }) {
            Info.keys++
        }

        if (keyUseRegex.any { it.matches(event.packet.chatComponent.formattedText) }) {
            Info.keys--
        }

        when (event.text) {
            "Starting in 4 seconds." -> MapUpdate.preloadHeads()
            "[NPC] Mort: Here, I found this map when I first entered the dungeon." -> {
                MapUpdate.getPlayers()
                Info.startTime = System.currentTimeMillis()
            }
        }
    }

    @SubscribeEvent
    fun onWorldLoad(event: WorldEvent.Unload) {
        reset()
    }

    fun reset() {
        Info.reset()
        dungeonTeammates.clear()
        espDoors.clear()
        PlayerTracker.roomClears.clear()
        MapUtils.calibrated = false
        MapUtils.mapData = null
        DungeonScan.hasScanned = false
        RunInformation.reset()
    }

    private fun shouldSearchMimic() =
        !Info.mimicFound && Location.dungeonFloor.equalsOneOf(6, 7) && !Config.legitMode

    object Info {
        // 6 x 6 room grid, 11 x 11 with connections
        val dungeonList = Array<Tile>(121) { Unknown(0, 0) }
        val uniqueRooms = mutableSetOf<UniqueRoom>()
        var roomCount = 0
        val puzzles = mutableMapOf<Puzzle, Boolean>()

        var trapType = ""
        var witherDoors = 0
        var cryptCount = 0
        var secretCount = 0
        var mimicFound = false

        var startTime = 0L
        var ended = false
        var keys = 0
        fun reset() {
            dungeonList.fill(Unknown(0, 0))
            uniqueRooms.clear()
            roomCount = 0
            puzzles.clear()

            trapType = ""
            witherDoors = 0
            cryptCount = 0
            secretCount = 0
            mimicFound = false

            startTime = 0L
            ended = false
            keys = 0
        }
    }
}
