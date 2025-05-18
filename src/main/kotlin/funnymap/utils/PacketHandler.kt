package funnymap.utils

import funnymap.events.ChatEvent
import funnymap.events.ScoreboardEvent
import funnymap.events.TabListEvent
import net.minecraft.network.Packet
import net.minecraft.network.play.server.*
import net.minecraftforge.common.MinecraftForge

object PacketHandler {
    fun processPacket(packet: Packet<*>) {
        when (packet) {
            is S02PacketChat -> {
                if (packet.type.toInt() != 2) {
                    MinecraftForge.EVENT_BUS.post(ChatEvent(packet))
                }
            }

            is S3EPacketTeams -> {
                MinecraftForge.EVENT_BUS.post(ScoreboardEvent(packet))
            }

            is S38PacketPlayerListItem -> {
                MinecraftForge.EVENT_BUS.post(TabListEvent(packet))
            }

            is S34PacketMaps -> {
                MapUtils.updateMapData(packet)
            }
        }
    }
}
