package ru.joutak.adhd.config.map.loader.concrete

import org.bukkit.configuration.ConfigurationSection
import ru.joutak.adhd.config.map.loader.MapMetaLoader
import ru.joutak.adhd.config.map.meta.MapMeta
import ru.joutak.adhd.config.map.meta.concrete.OvertakeMapMeta
import ru.joutak.adhd.world.SpawnPoint

class OvertakeMapMetaLoader : MapMetaLoader {
    override fun load(section: ConfigurationSection): MapMeta {
        val centerX = section.getDouble("center.x")
        val centerY = section.getDouble("center.y")
        val centerZ = section.getDouble("center.z")
        val radius = section.getInt("center.radius")

        val center = SpawnPoint(centerX, centerY, centerZ, 0F, 0F)

        return OvertakeMapMeta(center, radius)
    }
}