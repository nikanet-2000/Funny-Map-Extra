package funnymap.features.dungeon

import funnymap.core.map.Puzzle
import funnymap.core.map.Room
import funnymap.core.map.RoomState
import funnymap.events.ChatEvent
import funnymap.events.ScoreboardEvent
import funnymap.events.TabListEvent
import funnymap.features.dungeon.MimicDetector.setMimicKilled
import funnymap.features.dungeon.ScoreCalculation.getBonusScore
import funnymap.utils.Location
import funnymap.utils.Location.inDungeons
import funnymap.utils.Utils.equalsOneOf
import funnymap.utils.Utils.removeFormatting
import net.minecraft.client.network.NetworkPlayerInfo
import net.minecraft.entity.monster.EntityZombie
import net.minecraft.network.play.server.S38PacketPlayerListItem
import net.minecraftforge.event.entity.living.LivingDeathEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.math.ceil

/**
 * Many parts of this code are modified from [Skytils](https://github.com/Skytils/SkytilsMod/blob/1.x/src/main/kotlin/gg/skytils/skytilsmod/features/impl/dungeons/ScoreCalculation.kt).
 */
object RunInformation {

    var deathCount = 0
    val completedPuzzles: Int
        get() = Dungeon.Info.puzzles.count { it.value }
    var totalPuzzles = 0
    var cryptsCount = 0
    var secretsFound = 0
    var secretPercentage = 0f
    val secretTotal: Int
        get() = (secretsFound / (secretPercentage + 0.0001f) + 0.5).toInt()
    var minSecrets = 0
    var mimicKilled = false
    private var completedRooms = 0
    val completedRoomsPercentage
        get() = (completedRooms + (if (!Location.inBoss) 1 else 0) + (if (!bloodDone) 1 else 0)) / (if (totalRooms == 0) 36 else totalRooms).toFloat()
    var bloodDone = false
    private val totalRooms: Int
        get() = (completedRooms / (clearedPercentage + 0.0001f) + 0.4).toInt()
    private var clearedPercentage = 0f
    var timeElapsed = 0

    private val deathsRegex = Regex("§r§a§lTeam Deaths: §r§f(?<deaths>\\d+)§r")
    private val puzzleCountRegex = Regex("§r§b§lPuzzles: §r§f\\((?<count>\\d)\\)§r")
    private val failedPuzzleRegex = Regex("§r (?<puzzle>.+): §r§7\\[§r§c§l✖§r§7] §.+")
    private val solvedPuzzleRegex = Regex("§r (?<puzzle>.+): §r§7\\[§r§a§l✔§r§7] §.+")
    private val cryptsPattern = Regex("§r Crypts: §r§6(?<crypts>\\d+)§r")
    private val secretsFoundPattern = Regex("§r Secrets Found: §r§b(?<secrets>\\d+)§r")
    private val secretsFoundPercentagePattern = Regex("§r Secrets Found: §r§[ae](?<percentage>[\\d.]+)%§r")
    private val roomCompletedPattern = Regex("§r Completed Rooms: §r§d(?<count>\\d+)§r")
    private val dungeonClearedPattern = Regex("Cleared: (?<percentage>\\d+)% \\(\\d+\\)")
    private val timeElapsedPattern = Regex("Time Elapsed: (?:(?<hrs>\\d+)h )?(?:(?<min>\\d+)m )?(?:(?<sec>\\d+)s)?")

    fun reset() {
        deathCount = 0
        totalPuzzles = 0
        cryptsCount = 0
        secretsFound = 0
        secretPercentage = 0f
        mimicKilled = false
        completedRooms = 0
        bloodDone = false
        clearedPercentage = 0f
        timeElapsed = 0

        ScoreCalculation.message270 = false
        ScoreCalculation.message300 = false

        MimicDetector.mimicPos = null
        MimicDetector.mimicOpenTime = 0L
    }

    @SubscribeEvent
    fun onScoreboard(event: ScoreboardEvent) {
        if (!inDungeons || event.packet.action != 2) return

        val maxSecrets = ceil(secretTotal * ScoreCalculation.getSecretPercent())

        minSecrets =
            ceil(maxSecrets * (40 - getBonusScore() + ScoreCalculation.getDeathDeduction()) / 40).toInt()

        val line = event.packet.players.joinToString(
            " ", prefix = event.packet.prefix, postfix = event.packet.suffix
        ).removeFormatting()

        if (line.startsWith("Cleared: ")) {
            val match = dungeonClearedPattern.matchEntire(line)?.groups ?: return
            clearedPercentage = (match["percentage"]?.value?.toFloatOrNull()?.div(100f)) ?: clearedPercentage
        } else if (line.startsWith("Time Elapsed:")) {
            val match = timeElapsedPattern.matchEntire(line)?.groups ?: return
            val hours = match["hrs"]?.value?.toIntOrNull() ?: 0
            val minutes = match["min"]?.value?.toIntOrNull() ?: 0
            val seconds = match["sec"]?.value?.toIntOrNull() ?: 0
            timeElapsed = hours * 3600 + minutes * 60 + seconds
        }
    }

    @SubscribeEvent
    fun onChat(event: ChatEvent) {
        if (mimicKilled) return
        if (event.text.startsWith("Party > ") || (event.text.contains(":") && !event.text.contains(">"))) {
            listOf("\$SKYTILS-DUNGEON-SCORE-MIMIC\$", "mimic dead", "mimic killed").forEach {
                if (event.text.contains(it, true)) {
                    mimicKilled = true
                    return
                }
            }

            listOf("blaze done", "blaze puzzle finished").forEach {
                if (event.text.contains(it, true)) {
                    val puzzle =
                        Dungeon.Info.puzzles.keys.find { puzzle -> puzzle.tabName == "Higher Or Lower" } ?: return
                    Dungeon.Info.puzzles[puzzle] = true
                    val room = Dungeon.Info.dungeonList.firstOrNull { tile ->
                        tile is Room && tile.data.name.equalsOneOf("Lower Blaze", "Higher Blaze")
                    } ?: return
                    PlayerTracker.roomStateChange(room, room.state, RoomState.CLEARED)
                    room.state = RoomState.CLEARED
                }
            }
        }
    }

    @SubscribeEvent
    fun onEntityDeath(event: LivingDeathEvent) {
        if (!inDungeons || event.entity !is EntityZombie || mimicKilled) return
        if (MimicDetector.isMimic(event.entity)) {
            setMimicKilled()
        }
    }

    @SubscribeEvent
    fun onTabList(event: TabListEvent) {
        if (!inDungeons || !event.packet.action.equalsOneOf(
                S38PacketPlayerListItem.Action.UPDATE_DISPLAY_NAME, S38PacketPlayerListItem.Action.ADD_PLAYER
            )
        ) return
        event.packet.entries.forEach {
            val text = it?.displayName?.formattedText ?: it?.profile?.name ?: return@forEach
            updateFromTabList(text)
        }
    }

    private fun updateFromTabList(text: String) {
        when {
            text.contains("Team Deaths:") -> {
                deathCount = deathsRegex.firstResult(text)?.toIntOrNull() ?: deathCount
            }

            text.contains("✔") -> {
                val puzzleName = solvedPuzzleRegex.firstResult(text) ?: return
                if (puzzleName == "???") return
                val puzzle = Dungeon.Info.puzzles.keys.find { it.tabName == puzzleName }
                if (puzzle == null) {
                    if (Dungeon.Info.puzzles.size < totalPuzzles) {
                        Puzzle.fromName(puzzleName)?.let { Dungeon.Info.puzzles.putIfAbsent(it, true) }
                    }
                } else {
                    Dungeon.Info.puzzles[puzzle] = true
                }
            }

            text.contains("✖") -> {
                val puzzleName = failedPuzzleRegex.firstResult(text) ?: return
                if (puzzleName == "???") return
                val puzzle = Dungeon.Info.puzzles.keys.find { it.tabName == puzzleName }
                if (puzzle == null) {
                    if (Dungeon.Info.puzzles.size < totalPuzzles) {
                        Puzzle.fromName(puzzleName)?.let { Dungeon.Info.puzzles.putIfAbsent(it, false) }
                    }
                } else {
                    Dungeon.Info.puzzles[puzzle] = false
                }
            }

            text.contains("Crypts:") -> {
                cryptsCount = cryptsPattern.firstResult(text)?.toIntOrNull() ?: cryptsCount
            }

            text.contains("Secrets Found:") -> {
                if (text.contains("%")) {
                    secretPercentage =
                        secretsFoundPercentagePattern.firstResult(text)?.toFloatOrNull()?.div(100f) ?: secretPercentage
                } else {
                    secretsFound = secretsFoundPattern.firstResult(text)?.toIntOrNull() ?: secretsFound
                }
            }

            text.contains("Completed Rooms") -> {
                completedRooms = roomCompletedPattern.firstResult(text)?.toIntOrNull() ?: completedRooms
            }
        }
    }

    fun updatePuzzleCount(tabList: List<Pair<NetworkPlayerInfo, String>>) {
        if (totalPuzzles != 0) return
        val puzzleCount = tabList.find { it.second.contains("Puzzles:") }?.second ?: return
        totalPuzzles = puzzleCountRegex.firstResult(puzzleCount)?.toIntOrNull() ?: totalPuzzles
    }

    private fun Regex.firstResult(input: CharSequence): String? {
        return this.matchEntire(input)?.groups?.get(1)?.value
    }
}
