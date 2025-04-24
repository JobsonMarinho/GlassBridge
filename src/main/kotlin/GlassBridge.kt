package net.hypedmc.glassbridge

import co.aikar.commands.BaseCommand
import co.aikar.commands.PaperCommandManager
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.CommandPermission
import co.aikar.commands.annotation.Subcommand
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.command.CommandSender
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.util.Vector
import kotlin.math.floor
import kotlin.random.Random

class GlassBridge : JavaPlugin(), Listener {

    private val fakePlatforms = mutableListOf<List<Location>>()
    private val safePlatforms = mutableListOf<List<Location>>()
    private var isRunning = false

    private lateinit var startPoint: Location
    private lateinit var endPoint: Location

    private val margin = 3

    private val platformCount = 22
    private val gapBetweenSegments = 4

    override fun onEnable() {
        server.pluginManager.registerEvents(this, this)

        startPoint = Location(Bukkit.getWorld("world"), 970.0, 79.0, 2538.0)
        endPoint = Location(Bukkit.getWorld("world"), 878.0, 79.0, 2538.0)

        val commandManager = PaperCommandManager(this)

        commandManager.registerCommand(
            @CommandAlias("eponte")
            @CommandPermission("glassbridge.command")
            object : BaseCommand() {

                @Subcommand("iniciar")
                fun start(sender: CommandSender) {
                    if (isRunning) {
                        sender.sendMessage("§cO evento já está ativo!")
                        return
                    }
                    startEvent()
                    sender.sendMessage("§aEvento iniciado!")
                }

                @Subcommand("parar")
                fun stop(sender: CommandSender) {
                    if (!isRunning) {
                        sender.sendMessage("§cNão há evento ativo no momento!")
                        return
                    }
                    stopEvent()
                    sender.sendMessage("§aEvento terminado!")
                }

            })

    }

    override fun onDisable() {
        if (isRunning) {
            clearBridge()
        }
    }

    private fun startEvent() {
        isRunning = true
        fakePlatforms.clear()
        safePlatforms.clear()

        val effectiveStartX = startPoint.blockX - margin
        val fixedY = startPoint.blockY
        val fixedZ = startPoint.blockZ

        val world: World = startPoint.world ?: return

        for (i in 0 until platformCount) {
            val segmentX = effectiveStartX - (i * gapBetweenSegments)

            val rightPlatformLoc =
                Location(world, segmentX.toDouble(), fixedY.toDouble(), (fixedZ - 3).toDouble())
            val leftPlatformLoc =
                Location(world, segmentX.toDouble(), fixedY.toDouble(), (fixedZ + 2).toDouble())

            createPlatform(rightPlatformLoc)
            createPlatform(leftPlatformLoc)

            if (Random.nextBoolean()) {
                storePlatformBlocks(rightPlatformLoc, fake = false)
                storePlatformBlocks(leftPlatformLoc, fake = true)
            } else {
                storePlatformBlocks(rightPlatformLoc, fake = true)
                storePlatformBlocks(leftPlatformLoc, fake = false)
            }
        }
    }

    private fun stopEvent() {
        safePlatforms.forEach { platform ->
            platform.forEach { loc ->
                loc.world.getBlockAt(loc).type = Material.AIR
            }
        }
        fakePlatforms.forEach { platform ->
            platform.forEach { loc ->
                loc.world.getBlockAt(loc).type = Material.AIR
            }
        }
        safePlatforms.clear()
        fakePlatforms.clear()
        isRunning = false
    }

    private fun createPlatform(start: Location) {
        val world = start.world
        for (dx in 0 until 2) {
            for (dz in 0 until 2) {
                val block = world.getBlockAt(start.blockX + dx, start.blockY, start.blockZ + dz)
                block.type = Material.GLASS
            }
        }
    }

    private fun storePlatformBlocks(start: Location, fake: Boolean) {
        val world = start.world
        val platformBlocks = mutableListOf<Location>()
        for (dx in 0 until 2) {
            for (dz in 0 until 2) {
                val blockLoc =
                    Location(world, start.blockX + dx.toDouble(), start.blockY.toDouble(), start.blockZ + dz.toDouble())
                platformBlocks.add(blockLoc)
            }
        }
        if (fake) {
            fakePlatforms.add(platformBlocks)
        } else {
            safePlatforms.add(platformBlocks)
        }
    }

    private fun clearBridge() {
        stopEvent()
    }

    @EventHandler
    fun onPlayerMove(event: PlayerMoveEvent) {
        if (!isRunning) return

        val player = event.player
        val world = player.world
        if (world.name != "world") return
        val baseY = player.location.blockY - 1

        val offsets = listOf(
            Pair(0.0, 0.0),
            Pair(0.3, 0.3),
            Pair(0.3, -0.3),
            Pair(-0.3, 0.3),
            Pair(-0.3, -0.3)
        )

        fakePlatforms.toList().forEach { platform ->
            val hit = offsets.any { (dx, dz) ->
                val x = floor(player.location.x + dx).toInt()
                val z = floor(player.location.z + dz).toInt()
                platform.any { loc ->
                    loc.blockX == x && loc.blockY == baseY && loc.blockZ == z
                }
            }
            if (hit) {
                player.velocity.zero()
                platform.forEach { loc ->
                    loc.world.getBlockAt(loc).type = Material.AIR
                }
                fakePlatforms.remove(platform)
                player.velocity = Vector(0.0, -4.0, 0.0)
                return
            }
        }
    }

}