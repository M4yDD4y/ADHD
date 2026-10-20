package ru.joutak.adhd.listener.mode.overtake

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.inventory.ItemStack
import ru.joutak.adhd.ADHDPlugin
import ru.joutak.adhd.config.ADHDConfig
import ru.joutak.adhd.config.map.meta.concrete.OvertakeMapMeta
import ru.joutak.adhd.game.GameState
import ru.joutak.adhd.game.concrete.OvertakeGame
import ru.joutak.adhd.tournament.TournamentManager

class ZoneListener : Listener {

    val overtakeMapMeta = ADHDConfig.configMaps[1]!!.metas["overtake"] as OvertakeMapMeta
    val center = overtakeMapMeta.center
    val radius = overtakeMapMeta.radius
    val lastSoundTime = mutableMapOf<Player, Long>()

    @EventHandler
    fun onMove(event: PlayerMoveEvent) {
        val player = event.player
        val game = TournamentManager.getGame(player)

        if (game is OvertakeGame && game.getGameState() == GameState.RUN) {
            if (isInZone(event.to, game) && !isInZone(event.from, game)) {
                player.sendMessage("Вы вошли в зону захвата!")
                playSound(player, Sound.BLOCK_NOTE_BLOCK_CHIME, 1.5f)
                game.currentPlayers.add(player.uniqueId)
                changeColor(game)
            } else if (!isInZone(event.to, game) && isInZone(event.from, game)) {
                player.sendMessage("Вы вышли из зоны захвата!")
                playSound(player, Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f)
                game.currentPlayers.remove(player.uniqueId)
                changeColor(game)
            }
        }
    }

    fun isInZone(location: Location, game: OvertakeGame): Boolean {
        if (location.x in (center.x + 512 * game.pointer - radius)..(center.x + 512 * game.pointer + radius) &&
            location.z in (center.z - radius)..(center.z + radius)) {
            return true
        } else {
            return false
        }
    }

    fun changeColor(game: OvertakeGame) {
        when (game.currentPlayers.size) {
            0 -> game.armorStands.forEach { stand ->
                stand.equipment.clear()
                stand.equipment.setHelmet(ItemStack(Material.GREEN_CONCRETE))
            }
            1 -> game.armorStands.forEach { stand ->
                stand.equipment.clear()
                stand.equipment.setHelmet(ItemStack(Material.RED_CONCRETE))
            }
            2 -> game.armorStands.forEach { stand ->
                stand.equipment.clear()
                stand.equipment.setHelmet(ItemStack(Material.BLUE_CONCRETE))
            }
        }
    }

    fun playSound(player: Player, sound: Sound, volume: Float) {
        val now = System.currentTimeMillis()
        val lastTime = lastSoundTime.getOrDefault(player, 0L)

        if (now - lastTime > 1500) {
            player.playSound(player.location, sound, volume, 1.0f)
            lastSoundTime[player] = now
        }
    }
}