package ru.joutak.adhd.game.concrete

import ru.joutak.adhd.game.Game
import ru.joutak.adhd.game.GameState
import ru.joutak.adhd.game.mode.meta.ModeMeta
import ru.joutak.adhd.world.Arena
import ru.joutak.adhd.world.SpawnPoint
import java.util.UUID


class OvertakeGame : Game() {

    lateinit var worldName: String

    lateinit var arena: Arena

    lateinit var members: Set<UUID>

    var state = GameState.START

    val result = mutableMapOf<UUID, Double>()

    lateinit var points: List<SpawnPoint>

    override fun start(
        worldName: String, 
        arena: Arena, 
        members: Set<UUID>, 
        modeMeta: ModeMeta?
    ) {
        TODO("Not yet implemented")
    }

    override fun update() {
        TODO("Not yet implemented")
    }

    override fun getGameState(): GameState {
        return state
    }

    override fun finish() {
        TODO("Not yet implemented")
    }

    override fun summarize(): Map<UUID, Double> {
        return result
    }
}