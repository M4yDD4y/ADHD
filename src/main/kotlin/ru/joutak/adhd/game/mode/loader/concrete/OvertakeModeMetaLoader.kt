package ru.joutak.adhd.game.mode.loader.concrete

import org.bukkit.configuration.ConfigurationSection
import ru.joutak.adhd.game.mode.loader.ModeMetaLoader
import ru.joutak.adhd.game.mode.meta.ModeMeta
import ru.joutak.adhd.game.mode.meta.concrete.OvertakeModeMeta

class OvertakeModeMetaLoader : ModeMetaLoader {
    override fun load(section: ConfigurationSection): ModeMeta {
        val winPoints = section.getInt("points_to_win")
        val pointsPerSec = section.getInt("points_per_second")
        val knockMulti = section.getDouble("knockback_multiplier")

        return OvertakeModeMeta(winPoints, pointsPerSec, knockMulti)
    }
}