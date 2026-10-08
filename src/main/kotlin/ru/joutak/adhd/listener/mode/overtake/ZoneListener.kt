package ru.joutak.adhd.listener.mode.overtake

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent
import ru.joutak.adhd.ADHDPlugin
import ru.joutak.adhd.config.ADHDConfig
import ru.joutak.adhd.config.map.meta.concrete.OvertakeMapMeta
import ru.joutak.adhd.game.GameState
import ru.joutak.adhd.game.concrete.OvertakeGame
import ru.joutak.adhd.tournament.TournamentManager
import kotlin.math.ceil

class ZoneListener : Listener {

    @EventHandler
    fun onMove(event: PlayerMoveEvent) {
        val player = event.player
        val name = player.name
        val game = TournamentManager.getGame(player)

        //if (game is OvertakeGame && game.getGameState() == GameState.RUN) {
            val overtakeMapMeta = ADHDConfig.configMaps[1]?.metas["overtake"] as? OvertakeMapMeta
            val center = overtakeMapMeta!!.center
            val radius = overtakeMapMeta.radius
            if (event.to.x in (center.x - radius)..(center.x + radius) &&
                event.to.z in (center.z - radius)..(center.z + radius)) {
                ADHDPlugin.instance.logger.info("Игрок $name вошел в зону")
            } else {
                ADHDPlugin.instance.logger.info("Игрок $name вышел из зоны")
            }
        //}

    }
}