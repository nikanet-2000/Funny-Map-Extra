package funnymap.ui

import funnymap.config.Config
import funnymap.features.dungeon.MapRender
import funnymap.features.dungeon.MapRenderList
import funnymap.utils.Location

class MapElement : MovableGuiElement() {
    override var x: Int by Config::mapX
    override var y: Int by Config::mapY
    override val h: Int
        get() = if (Config.mapShowRunInformation) 142 else 128
    override val w: Int
        get() = 128
    override var scale: Float by Config::mapScale
    override var x2: Int = (x + w * scale).toInt()
    override var y2: Int = (y + h * scale).toInt()

    override fun render() {
        if (Config.renderBeta) {
            MapRenderList.renderMap()
        } else {
            MapRender.renderMap()
        }
    }

    override fun shouldRender(): Boolean {
        if (!Config.mapEnabled) return false
        if (Config.mapHideInBoss && Location.inBoss) return false
        return super.shouldRender()
    }
}
