package funnymap.features.dungeon

import funnymap.FunnyMap
import funnymap.FunnyMap.mc
import funnymap.config.Config
import funnymap.core.map.*
import funnymap.features.dungeon.DungeonScan.scan
import funnymap.utils.Location.dungeonFloor
import funnymap.utils.Utils
import funnymap.utils.Utils.equalsOneOf
import net.minecraft.init.Blocks
import net.minecraft.util.BlockPos
import kotlin.math.ceil

/**
 * Handles everything related to scanning the dungeon. Running [scan] will update the instance of [Dungeon].
 */
object DungeonScan {

    /**
     * The size of each dungeon room in blocks.
     */
    const val roomSize = 32

    /**
     * The starting coordinates to start scanning (the north-west corner).
     */
    const val startX = -185
    const val startZ = -185

    private var lastScanTime = 0L
    var isScanning = false
    var hasScanned = false

    val shouldScan: Boolean
        get() = Config.autoScan && !isScanning && !hasScanned && System.currentTimeMillis() - lastScanTime >= 250 && dungeonFloor != -1

    fun scan() {
        isScanning = true
        var allChunksLoaded = true

        // Scans the dungeon in a 11x11 grid.
        for (x in 0..10) {
            for (z in 0..10) {
                // Translates the grid index into world position.
                val xPos = startX + x * (roomSize shr 1)
                val zPos = startZ + z * (roomSize shr 1)

                if (!mc.theWorld.getChunkFromChunkCoords(xPos shr 4, zPos shr 4).isLoaded) {
                    // The room being scanned has not been loaded in.
                    allChunksLoaded = false
                    continue
                }

                // This room has already been added in a previous scan.
                if (Dungeon.Info.dungeonList[x + z * 11].run {
                        this !is Unknown && (this as? Room)?.data?.name != "Unknown"
                    }) continue

                scanRoom(xPos, zPos, z, x)?.let {
                    val prev = Dungeon.Info.dungeonList[z * 11 + x]
                    if (it is Room) {
                        if ((prev as? Room)?.uniqueRoom != null) {
                            prev.uniqueRoom?.addTile(x, z, it)
                        } else if (Dungeon.Info.uniqueRooms.none { unique -> unique.name == it.data.name }) {
                            UniqueRoom(x, z, it)
                        }
                        MapUpdate.roomAdded = true
                    }
                    Dungeon.Info.dungeonList[z * 11 + x] = it
                    MapRenderList.renderUpdated = true
                }
            }
        }

        if (MapUpdate.roomAdded) {
            MapUpdate.updateUniques()
        }

        if (allChunksLoaded) {
            if (Config.scanChatInfo) {
                val maxSecrets = ceil(Dungeon.Info.secretCount * ScoreCalculation.getSecretPercent())
                var maxBonus = 5
                if (dungeonFloor.equalsOneOf(6, 7)) maxBonus += 2
                if (ScoreCalculation.paul) maxBonus += 10
                val minSecrets = ceil(maxSecrets * (40 - maxBonus) / 40).toInt()

                val lines = mutableListOf(
                    "&aScan Finished!",
                    "&aPuzzles (&c${Dungeon.Info.puzzles.size}&a):",
                    Dungeon.Info.puzzles.entries.joinToString(
                        separator = "\n&b- &d",
                        prefix = "&b- &d"
                    ) { it.key.roomDataName },
                    "&6Trap: &a${Dungeon.Info.trapType}",
                    "&8Wither Doors: &7${Dungeon.Info.witherDoors - 1}",
                    "&7Total Crypts: &6${Dungeon.Info.cryptCount}",
                    "&7Total Secrets: &b${Dungeon.Info.secretCount}",
                    "&7Minimum Secrets: &e${minSecrets}"
                )
                Utils.modMessage(lines.joinToString(separator = "\n"))
            }
            Dungeon.Info.roomCount = Dungeon.Info.dungeonList.filter { it is Room && !it.isSeparator }.size
            hasScanned = true
        }

        lastScanTime = System.currentTimeMillis()
        isScanning = false
    }

    private fun scanRoom(x: Int, z: Int, row: Int, column: Int): Tile? {
        val height = mc.theWorld.getChunkFromChunkCoords(x shr 4, z shr 4).getHeightValue(x and 15, z and 15)
        if (height == 0) return null

        val rowEven = row and 1 == 0
        val columnEven = column and 1 == 0

        return when {
            // Scanning a room
            rowEven && columnEven -> {
                val roomCore = ScanUtils.getCore(x, z)
                Room(x, z, ScanUtils.getRoomData(roomCore) ?: return null).apply {
                    core = roomCore
                }
            }

            // Can only be the center "block" of a 2x2 room.
            !rowEven && !columnEven -> {
                Dungeon.Info.dungeonList[column - 1 + (row - 1) * 11].let {
                    if (it is Room) {
                        Room(x, z, it.data).apply {
                            isSeparator = true
                        }
                    } else null
                }
            }

            // Doorway between rooms
            // Old trap has a single block at 82
            height.equalsOneOf(74, 82) -> {
                Door(
                    x, z,
                    // Finds door type from door block
                    type = when (mc.theWorld.getBlockState(BlockPos(x, 69, z)).block) {
                        Blocks.coal_block -> {
                            Dungeon.Info.witherDoors++
                            DoorType.WITHER
                        }

                        Blocks.monster_egg -> DoorType.ENTRANCE
                        Blocks.stained_hardened_clay -> DoorType.BLOOD
                        else -> DoorType.NORMAL
                    }
                )
            }

            // Connection between large rooms
            else -> {
                Dungeon.Info.dungeonList[if (rowEven) row * 11 + column - 1 else (row - 1) * 11 + column].let {
                    if (it !is Room) {
                        null
                    } else if (it.data.type == RoomType.ENTRANCE) {
                        Door(x, z, DoorType.ENTRANCE)
                    } else {
                        Room(x, z, it.data).apply { isSeparator = true }
                    }
                }
            }
        }
    }
}
