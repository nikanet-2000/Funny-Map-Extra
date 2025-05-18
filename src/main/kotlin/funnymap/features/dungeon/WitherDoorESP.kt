package funnymap.features.dungeon

import funnymap.FunnyMap.mc
import funnymap.config.Config
import funnymap.core.map.RoomState
import funnymap.utils.Location.inBoss
import funnymap.utils.Location.inDungeons
import funnymap.utils.RenderUtils
import funnymap.utils.RenderUtils.getInterpolatedPosition
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.util.AxisAlignedBB
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object WitherDoorESP {
    @SubscribeEvent
    fun onRender(event: RenderWorldLastEvent) {
        if (!inDungeons || inBoss || Config.witherDoorESP == 0) return

        mc.mcProfiler.startSection("funnymap-3d")

        val (x, y, z) = mc.renderViewEntity.getInterpolatedPosition(event.partialTicks)
        GlStateManager.translate(-x, -y, -z)
        Dungeon.espDoors.forEach { door ->
            if (Config.witherDoorESP == 1 && door.state == RoomState.UNDISCOVERED) return@forEach
            val aabb = AxisAlignedBB(door.x - 1.0, 69.0, door.z - 1.0, door.x + 2.0, 73.0, door.z + 2.0)
            RenderUtils.drawBox(
                aabb,
                if (Dungeon.Info.keys > 0) Config.witherDoorKeyColor else Config.witherDoorNoKeyColor,
                Config.witherDoorOutlineWidth,
                Config.witherDoorOutline,
                Config.witherDoorFill,
                true
            )
        }
        GlStateManager.translate(x, y, z)

        mc.mcProfiler.endSection()
    }
}
