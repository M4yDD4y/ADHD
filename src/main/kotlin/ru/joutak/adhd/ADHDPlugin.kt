package ru.joutak.adhd

import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import ru.joutak.adhd.config.ADHDConfig
import ru.joutak.adhd.listener.BoundListener
import ru.joutak.adhd.listener.ArenaSwitchListener
import ru.joutak.adhd.listener.FreezeListener
import ru.joutak.adhd.listener.KeepInventoryListener
import ru.joutak.adhd.listener.PlayerSessionListener
import ru.joutak.adhd.listener.SpectatorListener
import ru.joutak.adhd.tournament.TournamentManager
import ru.joutak.minigames.MiniGamesCore
import ru.joutak.minigames.managers.MatchmakingManager

class ADHDPlugin : JavaPlugin() {
    companion object {
        @JvmStatic
        lateinit var instance: ADHDPlugin
    }

    override fun onEnable() {
        instance = this

        MiniGamesCore.initialize(this)

        ADHDConfig.load()

        TournamentManager.load()

        Bukkit.getPluginManager().registerEvents(PlayerSessionListener(), instance)
        Bukkit.getPluginManager().registerEvents(FreezeListener(), instance)
        Bukkit.getPluginManager().registerEvents(BoundListener(), instance)
        Bukkit.getPluginManager().registerEvents(ArenaSwitchListener(), instance)
        Bukkit.getPluginManager().registerEvents(KeepInventoryListener(), instance)
        Bukkit.getPluginManager().registerEvents(SpectatorListener(), instance)

        Bukkit.getPluginManager().registerEvents(ru.joutak.adhd.listener.mode.pvp.RespawnListener(), instance)
        Bukkit.getPluginManager().registerEvents(ru.joutak.adhd.listener.mode.pillars.RespawnListener(), instance)
        Bukkit.getPluginManager().registerEvents(ru.joutak.adhd.listener.mode.knights.RespawnListener(), instance)
        Bukkit.getPluginManager().registerEvents(ru.joutak.adhd.listener.mode.snipers.FireListener(), instance)
        Bukkit.getPluginManager().registerEvents(ru.joutak.adhd.listener.mode.snipers.RespawnListener(), instance)
        Bukkit.getPluginManager().registerEvents(ru.joutak.adhd.listener.mode.knights.FireListener(), instance)
        Bukkit.getPluginManager().registerEvents(ru.joutak.adhd.listener.mode.rps.GuiListener(), instance)
        Bukkit.getPluginManager().registerEvents(ru.joutak.adhd.listener.mode.casino.ChatListener(), instance)
        Bukkit.getPluginManager().registerEvents(ru.joutak.adhd.listener.mode.memory.HitListener(), instance)
        Bukkit.getPluginManager().registerEvents(ru.joutak.adhd.listener.mode.overtake.ZoneListener(), instance)

        Bukkit.getScheduler().runTaskTimer(instance, Runnable {
            val gInstance = MatchmakingManager.pollReady()

            if (gInstance != null) {
                TournamentManager.createTournament(gInstance)
            }
        }, 20L, 20L)

        instance.logger.info("Плагин ${instance.pluginMeta.name} версии ${instance.pluginMeta.version} включён!")
    }

    override fun onDisable() {
        TournamentManager.shutdown()
        MiniGamesCore.shutdown()
    }
}