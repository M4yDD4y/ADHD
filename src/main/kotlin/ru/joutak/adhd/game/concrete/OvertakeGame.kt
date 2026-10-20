package ru.joutak.adhd.game.concrete

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.title.Title
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.GameRules
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.boss.BarColor
import org.bukkit.boss.BarStyle
import org.bukkit.boss.BossBar
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import ru.joutak.adhd.ADHDPlugin
import ru.joutak.adhd.config.ADHDConfig
import ru.joutak.adhd.config.map.meta.concrete.OvertakeMapMeta
import ru.joutak.adhd.game.Game
import ru.joutak.adhd.game.GameState
import ru.joutak.adhd.game.mode.meta.ModeMeta
import ru.joutak.adhd.game.mode.meta.concrete.OvertakeModeMeta
import ru.joutak.adhd.world.Arena
import ru.joutak.adhd.world.SpawnPoint
import java.util.UUID
import kotlin.math.floor


class OvertakeGame : Game() {

    lateinit var worldName: String

    lateinit var arena: Arena

    lateinit var members: Set<UUID>

    lateinit var meta: OvertakeModeMeta

    var state = GameState.START

    var lSpawn: SpawnPoint? = null

    val result = mutableMapOf<UUID, Double>()

    val points = mutableMapOf<UUID, Int>()

    val currentPlayers = mutableListOf<UUID>()
    val bossBars = mutableMapOf<UUID, BossBar>()
    val armorStands = mutableListOf<ArmorStand>()

    var ticks = 0

    var pointer = 0.0

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

        val baseX = ADHDConfig.configMaps[1]!!.spawnPoints[0].x
        pointer = floor((arena.spawnPoints[0].x - baseX) / 512)

        for (corner in calculateCorners()) {
            val loc = Location(world, corner.first, corner.second, corner.third)
            loc.chunk.addPluginChunkTicket(ADHDPlugin.instance)

            val stand = world.spawn(loc, ArmorStand::class.java)
            stand.isVisible = false
            stand.setGravity(false)
            stand.isMarker = true
            stand.equipment.setHelmet(ItemStack(Material.GREEN_CONCRETE))
            armorStands.add(stand)
        }

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
            val bossbar = Bukkit.createBossBar(
                player.name,
                BarColor.BLUE,
                BarStyle.SEGMENTED_20
            )
            bossbar.progress = 0.0
            bossBars[uuid] = bossbar
            teleportToSpawn(player)
            player.gameMode = GameMode.ADVENTURE
            giveStick(player)
        }
        for (uuid in members) {
            members.forEach { id -> bossBars[uuid]!!.addPlayer(Bukkit.getPlayer(id)!!) }
        }

        state = GameState.RUN
    }

    fun teleportToSpawn(player: Player) {
        val world = Bukkit.getWorld(worldName)!!

        val spawns = arena.spawnPoints.toMutableSet()

        if (lSpawn != null) {
            spawns -= mutableSetOf(lSpawn!!)
        }

        val chosen: SpawnPoint = if (spawns.isEmpty()) {
            lSpawn!!
        } else {
            spawns.random()
        }

        lSpawn = chosen

        player.teleport(Location(world, chosen.x, chosen.y, chosen.z, chosen.yaw, chosen.pitch))
    }

    fun giveStick(player: Player) {
        val stick = ItemStack(Material.STICK)
        val stickMeta = stick.itemMeta
        stickMeta.addEnchant(Enchantment.KNOCKBACK, meta.knockMulti, true)
        stick.itemMeta = stickMeta
        player.inventory.clear()
        player.inventory.addItem(stick)
        player.inventory.heldItemSlot = 0
    }

    fun calculateCorners(): List<Triple<Double, Double, Double>> {
        val mapMeta = arena.metas["overtake"] as OvertakeMapMeta
        val corner1 = Triple(mapMeta.center.x + 512 * pointer + mapMeta.radius,
            mapMeta.center.y, mapMeta.center.z + mapMeta.radius)
        val corner2 = Triple(mapMeta.center.x + 512 * pointer - mapMeta.radius,
            mapMeta.center.y, mapMeta.center.z - mapMeta.radius)
        val corner3 = Triple(mapMeta.center.x + 512 * pointer + mapMeta.radius,
            mapMeta.center.y, mapMeta.center.z - mapMeta.radius)
        val corner4 = Triple(mapMeta.center.x + 512 * pointer - mapMeta.radius,
            mapMeta.center.y, mapMeta.center.z + mapMeta.radius)
        val corners = mutableListOf<Triple<Double, Double, Double>>()
        corners.add(corner1)
        corners.add(corner2)
        corners.add(corner3)
        corners.add(corner4)
        return corners
    }

    override fun update() {
        ticks++
        if (state == GameState.RUN && currentPlayers.size == 1 && ticks % 20 == 0) {
            val player = currentPlayers[0]
            points[player] = points[player]!! + meta.pointsPerSec
            bossBars[player]!!.progress = points[player]!! / meta.winPoints.toDouble()
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
            bossBars[uuid]!!.removeAll()
            bossBars.remove(uuid)

            if (result.containsKey(uuid)) {
                player.showTitle(Title.title(Component.text("Победа!").color(NamedTextColor.GREEN),
                    Component.text("Вы удержали точку!").color(NamedTextColor.GREEN)))
            } else {
                player.showTitle(Title.title(Component.text("Поражение...").color(NamedTextColor.RED),
                    Component.text("Вы не смогли удержать точку.").color(NamedTextColor.RED)))
            }
        }
        armorStands.forEach { stand ->
            stand.remove()
        }
        armorStands.clear()
        val world = Bukkit.getWorld(worldName)!!
        world.setGameRule(GameRules.FALL_DAMAGE, true)

        state = GameState.FINISH
    }

    override fun summarize(): Map<UUID, Double> {
        return result
    }
}