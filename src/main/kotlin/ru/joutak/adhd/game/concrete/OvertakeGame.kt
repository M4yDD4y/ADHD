package ru.joutak.adhd.game.concrete

import org.bukkit.Bukkit
import org.bukkit.GameRules
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import ru.joutak.adhd.game.Game
import ru.joutak.adhd.game.GameState
import ru.joutak.adhd.game.mode.meta.ModeMeta
import ru.joutak.adhd.game.mode.meta.concrete.OvertakeModeMeta
import ru.joutak.adhd.world.Arena
import ru.joutak.adhd.world.SpawnPoint
import java.util.UUID


class OvertakeGame : Game() {

    lateinit var worldName: String

    lateinit var arena: Arena

    lateinit var members: Set<UUID>

    lateinit var meta: OvertakeModeMeta

    var state = GameState.START

    val result = mutableMapOf<UUID, Double>()

    val points = mutableMapOf<UUID, Int>()

    val currentPlayers = mutableListOf<UUID>()

    var ticks = 0

    override fun start(
        worldName: String, 
        arena: Arena, 
        members: Set<UUID>, 
        modeMeta: ModeMeta?
    ) {
        this.worldName = worldName
        this.arena = arena
        this.members = members
        this.meta = modeMeta as OvertakeModeMeta

        val world = Bukkit.getWorld(worldName)!!
        world.setGameRule(GameRules.FALL_DAMAGE, false)

        for (uuid in members) {
            points[uuid] = 0

            val player = Bukkit.getPlayer(uuid) ?: continue

            player.addPotionEffect(
                PotionEffect(
                    PotionEffectType.REGENERATION,
                    99999,
                    5,
                    false,
                    false,
                    false
                )
            )
            player.addPotionEffect(
                PotionEffect(
                    PotionEffectType.SPEED,
                    99999,
                    0,
                    false,
                    false,
                    false
                )
            )
            teleportToSpawn(player)
            giveStick(player)
        }

        state = GameState.RUN
    }

    fun teleportToSpawn(player: Player) {
        val world = Bukkit.getWorld(worldName)!!

        val spawn = arena.spawnPoints.random()

        player.teleport(Location(Bukkit.getWorld(worldName)!!,
            spawn.x, spawn.y, spawn.z, spawn.yaw, spawn.pitch))
    }

    fun giveStick(player: Player) {
        val stick = ItemStack(Material.STICK)
        stick.itemMeta.addEnchant(Enchantment.KNOCKBACK, 2, true)
        stick.itemMeta.setDisplayName("Палка-нагибалка")
        player.inventory.clear()
        player.inventory.addItem(stick)
        player.inventory.heldItemSlot = 0
    }

    override fun update() {
        ticks++
        if (state == GameState.RUN && currentPlayers.size == 1 && ticks % 20 == 0) {
            val player = currentPlayers[0]
            points[player] = points[player]!! + meta.pointsPerSec
            if (points[player] == meta.winPoints) {
                result[player] = 1.0
                finish()
            }
        }
    }

    override fun getGameState(): GameState {
        return state
    }

    override fun finish() {
        for (uuid in members) {
            val player = Bukkit.getPlayer(uuid)!!
            player.activePotionEffects.clear()
            player.inventory.clear()
        }
        val world = Bukkit.getWorld(worldName)!!
        world.setGameRule(GameRules.FALL_DAMAGE, true)

        state = GameState.FINISH
    }

    override fun summarize(): Map<UUID, Double> {
        return result
    }
}