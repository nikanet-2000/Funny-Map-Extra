package funnymap.events

import funnymap.utils.Utils.removeFormatting
import net.minecraft.network.play.server.S02PacketChat
import net.minecraft.network.play.server.S38PacketPlayerListItem
import net.minecraft.network.play.server.S3EPacketTeams
import net.minecraftforge.fml.common.eventhandler.Event

class ChatEvent(val packet: S02PacketChat) : Event() {
    val text: String by lazy { packet.chatComponent.unformattedText.removeFormatting() }
}

class TabListEvent(val packet: S38PacketPlayerListItem) : Event()

class ScoreboardEvent(val packet: S3EPacketTeams) : Event()
