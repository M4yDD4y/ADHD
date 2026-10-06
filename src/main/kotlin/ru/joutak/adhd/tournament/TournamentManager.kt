package ru.joutak.adhd.tournament

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.title.Title
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import ru.joutak.adhd.ADHDPlugin
import ru.joutak.adhd.config.ADHDConfig
import ru.joutak.adhd.game.Game
import ru.joutak.adhd.world.WorldManager
import ru.joutak.minigames.command.ready.ReadyCommand
import ru.joutak.minigames.domain.GameInstance
import ru.joutak.minigames.domain.GameInstanceConfig
import ru.joutak.minigames.domain.MatchmakingMode
import ru.joutak.minigames.managers.MatchmakingManager
import java.util.UUID
import kotlin.math.ceil

object TournamentManager {

    val playerTournaments = mutableMapOf<UUID, Tournament>()

    val activeTournaments = mutableSetOf<Tournament>()

    var shutdownFlag = false

    val prepareAnnounceTasks = mutableMapOf<Tournament, BukkitRunnable>()

    fun handleJoin(player: Player) {
        sendToLobby(player)
    }

    fun handleQuit(player: Player) {
        val tournament = playerTournaments.remove(player.uniqueId) ?: return

        tournament.remove(player)
    }

    fun load() {
        MatchmakingManager.loadInstances(
            listOf(
                GameInstanceConfig(
                    "default",
                    ADHDConfig.maxPlayers,
                    1,
                    meta = mapOf("matchmaking_mode" to "SOLO"),
                    matchmakingMode = MatchmakingMode.SOLO,
                )
            )
        )

        WorldManager.clearOnStartUp()
    }

    fun createTournament(instance: GameInstance) {
        val everyone = instance.teams.flatten()

        val teamIdByPlayer = buildMap {
            instance.teams.forEachIndexed { index, players ->
                players.forEach { put(it.uniqueId, index + 1) }
            }
        }

        val teamKeysByTeamId = buildMap {
            instance.tournamentTeamKeys.forEachIndexed { index, key ->
                if (!key.isNullOrBlank()) put(index + 1, key)
            }
        }

        if (!WorldManager.isAvailable()) {
            ADHDPlugin.instance.logger.severe("World template can't load. Game won't start...")

            for (player in everyone) {
                retry(player)
            }

            return
        } else if (ADHDConfig.modes.isEmpty()) {
            ADHDPlugin.instance.logger.severe("No modes were loaded. Game won't start...")

            for (player in everyone) {
                retry(player)
            }

            return
        }

        instance.teams.forEach { l -> l.clear() }

        val participants = instance.getActivePlayerIds().toMutableList()

        val pool = createPool()

        val tournament = Tournament(
            participants = participants,
            pool = pool,
            teamIdByPlayer = teamIdByPlayer.filterKeys { it in participants },
            teamKeysByTeamId = teamKeysByTeamId,
        )

        ADHDPlugin.instance.logger.info("Выбраны режимы для турнира $tournament - $pool")

        activeTournaments.add(tournament)

        participants.forEach { uUID -> playerTournaments[uUID] = tournament }

        prepareAnnounceTasks[tournament] = object : BukkitRunnable() {
            override fun run() {
                announce(tournament)
            }
        }

        prepareAnnounceTasks[tournament]!!.runTaskTimer(ADHDPlugin.instance, 0L, 10L)

        WorldManager.generate(tournament)
    }

    fun removePrepareAnnounce(tournament: Tournament) {
        val task = prepareAnnounceTasks.remove(tournament) ?: return

        task.cancel()

        for (uuid in tournament.participants) {
            val player = Bukkit.getPlayer(uuid) ?: continue

            player.clearTitle()
        }
    }

    fun retry(player: Player) {
        MatchmakingManager.removePlayer(player)

        Bukkit.getScheduler().runTask(ADHDPlugin.instance, Runnable {
            if (player.isOnline) {
                ReadyCommand.performReady(player)
            }
        })
    }

    private fun announce(tournament: Tournament) {
        val title = Title.title(Component.text("Подготовка к игре").color(NamedTextColor.GOLD),
            Component.text("Пожалуйста подождите").color(NamedTextColor.GRAY),
            0, 16, 0)

        for (uuid in tournament.participants) {
            val player = Bukkit.getPlayer(uuid) ?: continue

            player.showTitle(title)
        }
    }

    fun createPool(): List<String> {
        val pool = mutableListOf<String>()

        val names = ADHDConfig.modes.keys.toMutableSet() - ADHDConfig.singleModeNames

        var used = mutableSetOf<String>()

        //for (i in 0..<ceil(2 * ADHDConfig.pointsGoal - 1).toInt()) {
        //    val name = (names - used).randomOrNull() ?: (names - mutableSetOf(pool[pool.size - 1])).random()

        //    if (names.size == used.size) used = mutableSetOf(name)

        //    used.add(name)

        //    pool.add(name)
        //}
        for (name in names) {
            pool.add(name)
        }

        return pool
    }

    fun sendToLobby(player: Player) {
        val lobby = WorldManager.getLobbyWorld()

        player.gameMode = GameMode.ADVENTURE
        player.health = 20.0
        player.saturation = 20.0f
        player.foodLevel = 20
        player.isGlowing = false

        player.inventory.clear()

        player.activePotionEffects.forEach { player.removePotionEffect(it.type) }

        player.leaveVehicle()

        Bukkit.getOnlinePlayers().forEach { it.showEntity(ADHDPlugin.instance, player) }

        player.teleport(lobby.spawnLocation)
    }

    fun getGame(player: Player): Game? {
        val tournament = playerTournaments[player.uniqueId] ?: return null

        return tournament.getGame(player)
    }

    fun finish(tournament: Tournament) {
        activeTournaments.remove(tournament)

        removePrepareAnnounce(tournament)

        for (uuid in tournament.participants) {
            playerTournaments.remove(uuid)

            val player = Bukkit.getPlayer(uuid) ?: continue

            sendToLobby(player)

            if (!shutdownFlag) {
                MatchmakingManager.removePlayer(player)
            }
        }

        WorldManager.clear(tournament)
    }

    fun shutdown() {
        shutdownFlag = true

        activeTournaments.forEach { tournament -> tournament.finish() }
    }

    fun isInLobby(player: Player) = !MatchmakingManager.isPlayerInStartedGame(player.uniqueId)
}