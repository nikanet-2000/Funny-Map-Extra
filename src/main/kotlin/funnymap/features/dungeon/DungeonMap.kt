package funnymap.features.dungeon

import funnymap.core.RoomData
import funnymap.core.map.*
import funnymap.utils.MapUtils

class DungeonMap(mapColors: ByteArray) {
    private var centerColors: ByteArray = ByteArray(121)
    private var sideColors: ByteArray = ByteArray(121)
    private val cacheTiles: Array<Tile?> = Array(121) { null }

    init {
        val halfRoom = MapUtils.roomSize / 2
        val halfTile = halfRoom + 2
        val startX = MapUtils.startCorner.first + halfRoom
        val startY = MapUtils.startCorner.second + halfRoom

        for (y in 0..10) {
            for (x in 0..10) {
                val mapX = startX + x * halfTile
                val mapY = startY + y * halfTile

                if (mapX >= 128 || mapY >= 128) continue

                centerColors[y * 11 + x] = mapColors[mapY * 128 + mapX]

                val sideIndex = if (x % 2 == 0 && y % 2 == 0) {
                    val topX = mapX - halfRoom
                    val topY = mapY - halfRoom
                    topY * 128 + topX
                } else {
                    val horizontal = y % 2 == 1
                    if (horizontal) {
                        mapY * 128 + mapX - 4
                    } else {
                        (mapY - 4) * 128 + mapX
                    }
                }

                sideColors[y * 11 + x] = mapColors[sideIndex]
            }
        }
    }

    fun getTile(arrayX: Int, arrayY: Int): Tile {
        val index = arrayY * 11 + arrayX
        if (index !in cacheTiles.indices) return Unknown(0, 0)
        if (cacheTiles[index] == null) {
            val xPos = DungeonScan.startX + arrayX * (DungeonScan.roomSize shr 1)
            val zPos = DungeonScan.startZ + arrayY * (DungeonScan.roomSize shr 1)
            cacheTiles[index] = scanTile(arrayX, arrayY, xPos, zPos)
        }
        return cacheTiles[index] ?: Unknown(0, 0)
    }

    private fun scanTile(arrayX: Int, arrayY: Int, worldX: Int, worldZ: Int): Tile {
        val centerColor = centerColors[arrayY * 11 + arrayX].toInt()
        val sideColor = sideColors[arrayY * 11 + arrayX].toInt()

        if (centerColor == 0) return Unknown(worldX, worldZ)

        return if (arrayX % 2 == 0 && arrayY % 2 == 0) {
            val type = RoomType.fromMapColor(sideColor) ?: return Unknown(worldX, worldZ)
            Room(worldX, worldZ, RoomData.createUnknown(type)).apply {
                state = when (centerColor) {
                    18 -> when (type) {
                        RoomType.BLOOD -> RoomState.DISCOVERED
                        RoomType.PUZZLE -> RoomState.FAILED
                        else -> state
                    }

                    30 -> when (type) {
                        RoomType.ENTRANCE -> RoomState.DISCOVERED
                        else -> RoomState.GREEN
                    }

                    34 -> RoomState.CLEARED
                    85, 119 -> RoomState.UNOPENED
                    else -> RoomState.DISCOVERED
                }
            }
        } else {
            if (sideColor == 0) {
                val type = DoorType.fromMapColor(centerColor) ?: return Unknown(worldX, worldZ)
                Door(worldX, worldZ, type).apply {
                    state = if (centerColor == 85) RoomState.UNOPENED else RoomState.DISCOVERED
                }
            } else {
                val type = RoomType.fromMapColor(sideColor) ?: return Unknown(worldX, worldZ)
                Room(worldX, worldZ, RoomData.createUnknown(type)).apply {
                    state = RoomState.DISCOVERED
                    isSeparator = true
                }
            }
        }
    }
}
