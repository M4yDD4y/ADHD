package ru.joutak.adhd.config

import org.bukkit.configuration.file.YamlConfiguration
import ru.joutak.adhd.ADHDPlugin
import ru.joutak.adhd.config.map.loader.MapMetaLoader
import ru.joutak.adhd.config.map.loader.concrete.MemoryMapMetaLoader
import ru.joutak.adhd.config.map.loader.concrete.PVPMapMetaLoader
import ru.joutak.adhd.config.map.loader.concrete.PillarsMapMetaLoader
import ru.joutak.adhd.config.map.loader.concrete.VentilatorMapMetaLoader
import ru.joutak.adhd.config.map.meta.MapMeta
import ru.joutak.adhd.game.mode.Mode
import ru.joutak.adhd.game.mode.loader.concrete.CasinoModeMetaLoader
import ru.joutak.adhd.game.mode.loader.concrete.KnightsModeMetaLoader
import ru.joutak.adhd.game.mode.loader.concrete.PVPModeMetaLoader
import ru.joutak.adhd.game.mode.loader.concrete.PillarsModeMetaLoader
import ru.joutak.adhd.game.mode.loader.concrete.SnipersModeMetaLoader
import ru.joutak.adhd.game.mode.meta.ModeMeta
import ru.joutak.adhd.world.ConfigMap
import ru.joutak.adhd.world.SpawnPoint
import java.io.File
import kotlin.math.floor

object ADHDConfig {

    val registeredModes = mapOf(
        Pair("PVP", PVPModeMetaLoader()),
        Pair("Knights", KnightsModeMetaLoader()),
        Pair("Snipers", SnipersModeMetaLoader()),
        Pair("Pillars", PillarsModeMetaLoader()),
        Pair("RPS", null),
        Pair("Memory", null),
        Pair("Casino", CasinoModeMetaLoader()),
        Pair("Overtake", null)
    )

    val singleModeNames = mutableSetOf<String>()

    var maxPlayers = 4
        private set

    var pointsGoal = 10.0
        private set

    var templateWorldName = "template"
        private set

    var lobbyWorld = "lobby"
        private set

    var ceremonyEnabled = false
        private set

    var ceremonyDuration = 16
        private set

    var ceremonySpawnPoint: SpawnPoint = SpawnPoint(0.0, 0.0, 0.0, 0.0f, 0.0f)

    val configMaps = mutableMapOf<Int, ConfigMap>()

    val modes = mutableMapOf<String, Mode>()

    private val mapMetaLoaders = mutableMapOf<String, MapMetaLoader>()

    fun load() {
        val file = File(ADHDPlugin.instance.dataFolder, "config.yml")

        if (!file.exists()) {
            ADHDPlugin.instance.saveResource("config.yml", true)
        }

        val config = YamlConfiguration.loadConfiguration(file)

        maxPlayers = config.getInt("default.maxPlayers", 4)

        pointsGoal = config.getDouble("default.pointsGoal", 10.0)

        templateWorldName = config.getString("default.templateWorldName") ?: "template"

        lobbyWorld = config.getString("default.lobbyWorld") ?: "lobby"

        ceremonyEnabled = config.getBoolean("ceremony.enabled")

        ceremonyDuration = config.getInt("ceremony.duration", 16)

        val ceremonySpawnSection = config.getConfigurationSection("ceremony.center")

        if (ceremonySpawnSection != null) {
            val x = ceremonySpawnSection.getDouble("x")
            val y = ceremonySpawnSection.getDouble("y")
            val z = ceremonySpawnSection.getDouble("z")
            val yaw = ceremonySpawnSection.getDouble("yaw").toFloat()
            val pitch = ceremonySpawnSection.getDouble("pitch").toFloat()

            ceremonySpawnPoint = SpawnPoint(x, y, z, yaw, pitch)
        }

        loadConfigMaps(config)

        ADHDPlugin.instance.logger.info("Карты: $configMaps")

        loadModes()

        ADHDPlugin.instance.logger.info("Режимы: $modes")

        ADHDPlugin.instance.logger.info("Одиночные режимы: $singleModeNames")
    }

    fun loadConfigMaps(config: YamlConfiguration) {
        registerMapMetaLoaders()

        configMaps.clear()

        val mapsSection = config.getConfigurationSection("maps") ?: return

        for (mapId in mapsSection.getKeys(false)) {
            val mapSection = mapsSection.getConfigurationSection(mapId) ?: continue

            val spawnsSection = mapSection.getConfigurationSection("spawns")

            val spawns = mutableListOf<SpawnPoint>()

            spawnsSection?.let {
                for (spawnId in it.getKeys(false)) {
                    val spawnSection = it.getConfigurationSection(spawnId) ?: continue

                    val x = spawnSection.getDouble("x")
                    val y = spawnSection.getDouble("y")
                    var z = spawnSection.getDouble("z")
                    z -= floor(z / 512) * 512
                    val yaw = spawnSection.getDouble("yaw").toFloat()
                    val pitch = spawnSection.getDouble("pitch").toFloat()

                    spawns.add(SpawnPoint(x, y, z, yaw, pitch))
                }
            }

            if (spawns.isEmpty()) continue

            val metaSection = mapSection.getConfigurationSection("meta")

            val metas = mutableMapOf<String, MapMeta>()

            metaSection?.let {
                for (metaType in it.getKeys(false)) {
                    val metaSection = it.getConfigurationSection(metaType) ?: continue

                    val loader = mapMetaLoaders[metaType] ?: continue

                    metas[metaType] = loader.load(metaSection)
                }
            }

            configMaps[mapId.toInt()] = ConfigMap(spawns, metas)
        }
    }

    fun registerMapMetaLoaders() {
        mapMetaLoaders["pvp"] = PVPMapMetaLoader()
        mapMetaLoaders["pillars"] = PillarsMapMetaLoader()
        mapMetaLoaders["ventilator"] = VentilatorMapMetaLoader()
        mapMetaLoaders["memory"] = MemoryMapMetaLoader()
    }

    fun loadModes() {
        modes.clear()

        singleModeNames.clear()

        for (modeName in registeredModes.keys) {
            val file = File(ADHDPlugin.instance.dataFolder, "config_$modeName.yml")

            if (!file.exists()) {
                ADHDPlugin.instance.saveResource("config_$modeName.yml", true)
            }

            val config = YamlConfiguration.loadConfiguration(file)

            val enabled = config.getBoolean("default.enabled")

            if (!enabled) continue

            val isSingle = config.getBoolean("default.single")

            if (isSingle) singleModeNames.add(modeName)

            val duration = config.getInt("default.duration", 30)

            val maps = config.getIntegerList("default.maps").toSet().intersect(configMaps.keys)

            if (maps.isEmpty()) continue

            val metaSection = config.getConfigurationSection("meta")

            var meta: ModeMeta? = null

            if (metaSection != null) {
                meta = registeredModes[modeName]?.load(metaSection)
            }

            val displayName = config.getString("default.displayName") ?: "Режим"

            val description = config.getString("default.description") ?: "Описание режима"

            modes[modeName] = Mode(duration, maps.toList(), meta, displayName, description)
        }
    }
}