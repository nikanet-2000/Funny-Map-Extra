package funnymap.utils

import funnymap.FunnyMap.mc
import funnymap.features.dungeon.DungeonScan
import funnymap.utils.Location.inDungeons
import funnymap.utils.Utils.equalsOneOf
import net.minecraft.item.ItemMap
import net.minecraft.item.ItemStack
import net.minecraft.network.play.server.S34PacketMaps
import net.minecraft.util.Vec4b
import net.minecraft.world.storage.MapData

object MapUtils {
    val Vec4b.mapX
        get() = (this.func_176112_b() + 128) shr 1

    val Vec4b.mapZ
        get() = (this.func_176113_c() + 128) shr 1

    val Vec4b.yaw
        get() = this.func_176111_d() * 22.5f

    var mapData: MapData? = null
    var startCorner = Pair(5, 5)
    var coordMultiplier = 0.625
    var roomSize = 16
    var halfRoomSize = roomSize / 2
    val connectorSize = 4
    var calibrated = false
    var mapDataUpdated = false

    private fun getMapItem(): ItemStack? {
        val map = mc.thePlayer?.inventory?.getStackInSlot(8) ?: return null
        if (map.item !is ItemMap || !map.displayName.contains("Magical Map")) return null
        return map
    }

    fun updateMapData(packet: S34PacketMaps) {
        if (!inDungeons) return
        Utils.runMinecraftThread {
            val map = getMapItem()
            if (map != null) {
                mapData = (map.item as ItemMap).getMapData(map, mc.theWorld)
            }
            if (mapData == null) {
                mapData = MapData("map_${packet.mapId}")
            }
            packet.setMapdataTo(mapData)
            mapDataUpdated = true
        }
    }

    /**
     * Calibrates map metrics based on the size and location of the entrance room.
     */
    fun calibrateMap(): Boolean {
        val (start, size) = findEntranceCorner()
        if (size.equalsOneOf(16, 18)) {
            roomSize = size
            halfRoomSize = roomSize / 2
            startCorner = when (Location.dungeonFloor) {
                0 -> Pair(22, 22)
                1 -> Pair(22, 11)
                2, 3 -> Pair(11, 11)
                else -> {
                    val startX = start and 127
                    val startZ = start shr 7
                    Pair(startX % (roomSize + 4), startZ % (roomSize + 4))
                }
            }
            coordMultiplier = (roomSize + connectorSize).toDouble() / DungeonScan.roomSize
            return true
        }
        return false
    }

    /**
     * Finds the starting index of the entrance room as well as the size of the room.
     */
    private fun findEntranceCorner(): Pair<Int, Int> {
        var start = 0
        var currLength = 0
        mapData?.colors?.forEachIndexed { index, byte ->
            if (byte.toInt() == 30) {
                if (currLength == 0) start = index
                currLength++
            } else {
                if (currLength >= 16) {
                    return Pair(start, currLength)
                }
                currLength = 0
            }
        }
        return Pair(start, currLength)
    }
}
