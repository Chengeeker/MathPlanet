package com.game.maths

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DefenseEngineTest {
    private fun engine(field: DefenseBattlefield = DefenseBattlefield.LAWN_POOL) =
        DefenseEngine(Random(7), field).also { it.start() }

    private fun DefenseEngine.advance(ms: Long, difficulty: DefenseDifficulty = DefenseDifficulty.RELAXED) {
        repeat((ms / 100).toInt()) { step(100, difficulty) }
    }

    @Test fun rosterContainsAllClassicPlantsAndZombiesPlusOneOriginal() {
        assertEquals(49, DefensePlant.entries.size)
        assertEquals(27, ZombieKind.entries.size)
        assertEquals(1, ZombieKind.entries.count { it == ZombieKind.DASHER })
    }

    @Test fun campaignCatalogMatchesAllFiftyLevelMilestones() {
        val firstEnemyLevels = mapOf(
            2 to setOf(ZombieKind.SIGNAL), 3 to setOf(ZombieKind.SHELL),
            6 to setOf(ZombieKind.VAULT), 8 to setOf(ZombieKind.HEAVY),
            11 to setOf(ZombieKind.NEWSPAPER), 13 to setOf(ZombieKind.SCREEN),
            16 to setOf(ZombieKind.FOOTBALL), 18 to setOf(ZombieKind.DANCER, ZombieKind.BACKUP),
            21 to setOf(ZombieKind.DUCK), 23 to setOf(ZombieKind.SNORKEL),
            26 to setOf(ZombieKind.ZAMBONI, ZombieKind.BOBSLED), 28 to setOf(ZombieKind.DOLPHIN),
            31 to setOf(ZombieKind.JACK), 33 to setOf(ZombieKind.BALLOON),
            36 to setOf(ZombieKind.DIGGER), 38 to setOf(ZombieKind.POGO),
            41 to setOf(ZombieKind.BUNGEE), 43 to setOf(ZombieKind.LADDER),
            46 to setOf(ZombieKind.CATAPULT), 48 to setOf(ZombieKind.GIANT),
            50 to setOf(ZombieKind.BOSS)
        )
        val plantUnlocks = mapOf(
            1 to DefensePlant.PEA, 2 to DefensePlant.SUN, 3 to DefensePlant.CHERRY,
            4 to DefensePlant.WALL, 6 to DefensePlant.MINE, 7 to DefensePlant.SNOW,
            8 to DefensePlant.CHOMPER, 9 to DefensePlant.DOUBLE, 11 to DefensePlant.PUFF,
            12 to DefensePlant.SUNSHROOM, 13 to DefensePlant.FUME, 14 to DefensePlant.GRAVE,
            16 to DefensePlant.HYPNO, 17 to DefensePlant.SCAREDY, 18 to DefensePlant.ICE,
            19 to DefensePlant.DOOM, 21 to DefensePlant.LILYPAD, 22 to DefensePlant.SQUASH,
            23 to DefensePlant.THREEPEATER, 24 to DefensePlant.TANGLE, 26 to DefensePlant.JALAPENO,
            27 to DefensePlant.SPIKEWEED, 28 to DefensePlant.TORCHWOOD, 29 to DefensePlant.TALLNUT,
            31 to DefensePlant.SEASHROOM, 32 to DefensePlant.PLANTERN, 33 to DefensePlant.CACTUS,
            34 to DefensePlant.BLOVER, 36 to DefensePlant.SPLITPEA, 37 to DefensePlant.STARFRUIT,
            38 to DefensePlant.PUMPKIN, 39 to DefensePlant.MAGNET, 41 to DefensePlant.CABBAGE,
            42 to DefensePlant.FLOWERPOT, 43 to DefensePlant.KERNEL, 44 to DefensePlant.COFFEE,
            46 to DefensePlant.GARLIC, 47 to DefensePlant.UMBRELLA, 48 to DefensePlant.MARIGOLD,
            49 to DefensePlant.MELON
        )
        val stars = listOf(
            listOf(1, 1, 2, 2, 2, 2, 2, 3, 3, 2),
            listOf(2, 2, 3, 3, 2, 3, 3, 3, 4, 3),
            listOf(2, 2, 3, 3, 2, 4, 3, 4, 4, 3),
            listOf(3, 3, 4, 4, 3, 4, 4, 4, 5, 4),
            listOf(3, 3, 4, 4, 3, 4, 4, 5, 5, 5)
        )

        assertEquals(50, DefenseCampaign.levels.size)
        DefenseCampaign.levels.forEach { level ->
            val expectedBattlefield = when (level.id) {
                in 1..10 -> DefenseBattlefield.DAY_LAWN
                in 11..20 -> DefenseBattlefield.NIGHT_LAWN
                in 21..30 -> DefenseBattlefield.DAY_POOL
                35 -> DefenseBattlefield.NIGHT_GARDEN
                in 31..40 -> DefenseBattlefield.FOG_POOL
                in 41..49 -> DefenseBattlefield.ROOFTOP
                else -> DefenseBattlefield.NIGHT_ROOF
            }
            val expectedRows = when (level.id) {
                1 -> setOf(2)
                2, 3 -> setOf(1, 2, 3)
                else -> (0 until expectedBattlefield.rowCount).toSet()
            }
            val expectedPlants = plantUnlocks.filterKeys { it <= level.id }.values.toSet() +
                if (level.id == 41) setOf(DefensePlant.FLOWERPOT) else emptySet()
            val expectedZombies = setOf(ZombieKind.DRIFTER) +
                firstEnemyLevels.filterKeys { it <= level.id }.values.flatten()
            val expectedEvents = firstEnemyLevels[level.id].orEmpty()
                .filterNot { it in setOf(ZombieKind.SIGNAL, ZombieKind.BACKUP, ZombieKind.BOSS) }.toSet()
            val local = (level.id - 1) % 10 + 1
            val expectedBudgetOffset = when (local) {
                1, 5 -> -1
                in 7..10 -> 1
                else -> 0
            }

            assertEquals("${level.label} battlefield", expectedBattlefield, level.battlefield)
            assertEquals("${level.label} open rows", expectedRows, level.activeRows)
            assertEquals("${level.label} plant unlocks", expectedPlants, level.allowedPlants)
            assertEquals("${level.label} enemy pool", expectedZombies, level.allowedZombies)
            assertEquals("${level.label} first encounter", expectedEvents, level.spawnEvents.map { it.zombie }.toSet())
            assertEquals("${level.label} budget offset", expectedBudgetOffset, level.budgetOffset)
            assertEquals("${level.label} pressure estimate", stars[(level.id - 1) / 10][local - 1], level.difficulty)
            assertTrue("${level.label} invalid lane", level.activeRows.all { it in 0 until level.battlefield.rowCount })
            assertTrue("${level.label} invalid event window", level.spawnEvents.all {
                it.count > 0 && it.notBeforeSec <= it.deadlineSec && it.telegraphSec >= 0
            })
        }
        assertEquals(150, DefenseCampaign.level(1)?.startingSun)
        assertEquals(setOf(DefensePlant.PEA), DefenseCampaign.level(1)?.allowedPlants)
        assertTrue(DefensePlant.FLOWERPOT in DefenseCampaign.defaultDeck(DefenseCampaign.level(41)!!))
    }

    @Test fun everyCampaignSpawnStaysOnAPlantableLane() {
        DefenseDifficulty.entries.forEach { difficulty ->
            DefenseCampaign.levels.forEach { level ->
                val game = DefenseEngine(Random(level.id), level.battlefield, level).also { it.start() }
                val firstSpawnAt = mutableMapOf<ZombieKind, Long>()
                repeat(1_500) {
                    game.step(100, difficulty)
                    game.zombies.forEach { firstSpawnAt.putIfAbsent(it.type, game.elapsedMs) }
                    assertTrue("${level.label} $difficulty spawned an enemy on a locked lane",
                        game.zombies.all { it.row in level.activeRows })
                    assertTrue("${level.label} $difficulty dropped sun on a locked lane",
                        game.stars.all { it.row in level.activeRows })
                }
                level.spawnEvents.forEach { event ->
                    val spawnedAt = firstSpawnAt[event.zombie]
                    assertNotNull("${level.label} $difficulty missed ${event.zombie}", spawnedAt)
                    assertTrue("${level.label} $difficulty introduced ${event.zombie} too early",
                        spawnedAt!! >= event.notBeforeSec * 1_000L)
                    assertTrue("${level.label} $difficulty introduced ${event.zombie} after its deadline",
                        spawnedAt <= event.deadlineSec * 1_000L)
                }
            }
        }
    }

    @Test fun finalCampaignBossSpawnsOnAnOpenLane() {
        val level = DefenseCampaign.level(50)!!
        val game = DefenseEngine(Random(50), level.battlefield, level).also { it.start() }
        level.activeRows.forEach { row ->
            repeat(9) { col -> game.plants += DefensePlantUnit(DefensePlant.PEA, row, col) }
        }
        var bossRow: Int? = null
        repeat(1_400) {
            game.step(100, DefenseDifficulty.HARD)
            if (bossRow == null) bossRow = game.zombies.firstOrNull { it.type == ZombieKind.BOSS }?.row
        }
        assertNotNull("5-10 did not reach its boss phase", bossRow)
        assertTrue("5-10 boss spawned on a locked lane", bossRow?.let { it in level.activeRows } == true)
    }

    @Test fun roofRequiresAndStacksPlantsOnFlowerpots() {
        val game = engine(DefenseBattlefield.ROOFTOP)
        game.select(DefensePlant.PEA)
        assertTrue(game.tapCell(0, 0).contains("花盆"))
        game.select(DefensePlant.FLOWERPOT)
        game.tapCell(0, 0)
        game.select(DefensePlant.PEA)
        game.tapCell(0, 0)
        assertEquals(listOf(DefensePlant.FLOWERPOT, DefensePlant.PEA), game.plants.map { it.type })
    }

    @Test fun nightMushroomsWakeAndCoffeeOnlyTargetsMushrooms() {
        val day = engine()
        day.select(DefensePlant.PUFF)
        day.tapCell(0, 0)
        assertFalse(day.plants.single().awake)
        day.select(DefensePlant.COFFEE)
        assertTrue(day.tapCell(0, 0).startsWith("已唤醒"))
        assertTrue(day.plants.single().awake)

        val night = engine(DefenseBattlefield.NIGHT_GARDEN)
        night.select(DefensePlant.PUFF)
        night.tapCell(0, 0)
        assertTrue(night.plants.single().awake)
        night.select(DefensePlant.SUN)
        night.tapCell(0, 1)
        night.select(DefensePlant.COFFEE)
        assertTrue(night.tapCell(0, 1).contains("沉睡的蘑菇"))
        assertEquals(DefensePlant.SUN, night.plants.last().type)
    }

    @Test fun imitaterCopiesSelectedPlantWithVisibleTransformState() {
        val game = engine()
        game.select(DefensePlant.IMITATER)
        game.chooseImitaterTarget(DefensePlant.PEA)
        game.tapCell(0, 0)
        val mimic = game.plants.single()
        assertEquals(DefensePlant.PEA, mimic.disguiseType)
        assertEquals(800L, mimic.transformingMs)
        game.advance(800)
        assertEquals(DefensePlant.PEA, mimic.type)
        assertEquals(0L, mimic.transformingMs)
    }

    @Test fun starfruitFiresFrontAndFourDiagonalOrVerticalShots() {
        val game = engine()
        val plant = DefensePlantUnit(DefensePlant.STARFRUIT, 2, 2)
        game.plants += plant
        game.zombies += DefenseZombieUnit(1, ZombieKind.DRIFTER, 2, 6f)
        game.advance(1_500)
        val shots = game.shots.filter { it.sourceType == DefensePlant.STARFRUIT }
        assertEquals(5, shots.size)
        assertEquals(setOf(1f to 0f, 1f to -1f, 1f to 1f, 0f to -1f, 0f to 1f),
            shots.map { it.direction to it.laneDirection }.toSet())
    }

    @Test fun screenDoorTakesDamageFromPeas() {
        val game = engine()
        game.plants += DefensePlantUnit(DefensePlant.PEA, 0, 0)
        val zombie = DefenseZombieUnit(1, ZombieKind.SCREEN, 0, 3f)
        game.zombies += zombie
        game.advance(4_000)
        assertTrue(zombie.armor < ZombieKind.SCREEN.armor)
        assertFalse(zombie.equipmentRemoved)
    }

    @Test fun magnetRemovesBucketAndRecordsFallingGearAnimation() {
        val game = engine(DefenseBattlefield.NIGHT_GARDEN)
        game.plants += DefensePlantUnit(DefensePlant.MAGNET, 0, 0, elapsedMs = 3_000).apply { awake = true }
        val zombie = DefenseZombieUnit(1, ZombieKind.HEAVY, 0, 2f)
        game.zombies += zombie
        game.step(100, DefenseDifficulty.RELAXED)
        assertTrue(zombie.equipmentRemoved)
        assertEquals(0, zombie.armor)
        assertTrue(zombie.gearLostMs > 0L)
        assertTrue(game.explosions.any { it.kind == DefenseEffectKind.MAGNET })
    }

    @Test fun damagedRoadConeFallsOffWithAVisibleStateWindow() {
        val game = engine()
        val zombie = DefenseZombieUnit(1, ZombieKind.SHELL, 0, 1.25f)
        game.zombies += zombie
        game.shots += DefenseShot(0, .8f, 400, false, sourceType = DefensePlant.PEA,
            targetKind = PlantKind.SHOT)
        game.step(100, DefenseDifficulty.RELAXED)
        assertTrue(zombie.equipmentRemoved)
        assertTrue(zombie.gearLostMs > 0L)
    }

    @Test fun magnetMakesDiggerSurfaceAndPogoStopJumping() {
        val game = engine(DefenseBattlefield.NIGHT_GARDEN)
        game.plants += DefensePlantUnit(DefensePlant.MAGNET, 0, 0, elapsedMs = 3_000).apply { awake = true }
        val digger = DefenseZombieUnit(1, ZombieKind.DIGGER, 0, 2f, underground = true)
        game.zombies += digger
        game.step(100, DefenseDifficulty.RELAXED)
        assertFalse(digger.underground)
        assertTrue(digger.returning)

        val second = engine(DefenseBattlefield.NIGHT_GARDEN)
        second.plants += DefensePlantUnit(DefensePlant.MAGNET, 0, 0, elapsedMs = 3_000).apply { awake = true }
        val pogo = DefenseZombieUnit(1, ZombieKind.POGO, 0, 2f, jumpingMs = 500)
        second.zombies += pogo
        second.step(100, DefenseDifficulty.RELAXED)
        assertTrue(pogo.equipmentRemoved)
        assertTrue(pogo.jumped)
    }

    @Test fun bungeeCapturesAcrossRowsAndCarriesPlantOnRetreat() {
        val game = engine()
        game.plants += DefensePlantUnit(DefensePlant.PEA, 3, 2)
        val zombie = DefenseZombieUnit(1, ZombieKind.BUNGEE, 0, 3f)
        game.zombies += zombie
        game.advance(2_000)
        assertFalse(game.plants.any { it.type == DefensePlant.PEA })
        assertEquals(DefensePlant.PEA, zombie.capturedPlantType)
        assertTrue(zombie.attackTriggered)
        game.advance(1_100)
        assertTrue(zombie.retreating)
        assertTrue(zombie.x > 2.65f)
    }

    @Test fun umbrellaBlocksBungeeAndShowsShieldEffect() {
        val game = engine()
        game.plants += DefensePlantUnit(DefensePlant.UMBRELLA, 1, 2)
        val target = DefensePlantUnit(DefensePlant.PEA, 2, 3)
        game.plants += target
        val zombie = DefenseZombieUnit(1, ZombieKind.BUNGEE, 0, 3.2f, targetRow = 2, targetCol = 3)
        game.zombies += zombie
        game.step(100, DefenseDifficulty.RELAXED)
        assertTrue(game.plants.contains(target))
        assertTrue(zombie.retreating)
        assertTrue(zombie.health > 0)
        assertTrue(game.explosions.any { it.kind == DefenseEffectKind.SHIELD })
    }

    @Test fun bungeeWithNoTargetLeavesWithoutCostingALife() {
        val game = engine()
        val zombie = DefenseZombieUnit(1, ZombieKind.BUNGEE, 0, 3f)
        game.zombies += zombie
        game.advance(3_000)
        assertTrue(zombie.retreating)
        assertTrue(zombie.health > 0)
        assertEquals(5, game.lives)
    }

    @Test fun ladderIsPlacedAndLetsNextZombiePass() {
        val game = engine()
        val plant = DefensePlantUnit(DefensePlant.PEA, 0, 2)
        game.plants += plant
        val ladder = DefenseZombieUnit(1, ZombieKind.LADDER, 0, 3f)
        game.zombies += ladder
        game.step(100, DefenseDifficulty.RELAXED)
        assertTrue(plant.laddered)
        assertTrue(ladder.equipmentRemoved)
        val following = DefenseZombieUnit(2, ZombieKind.DRIFTER, 0, 3f)
        game.zombies += following
        game.step(100, DefenseDifficulty.RELAXED)
        assertEquals(1.4f, following.x, .001f)
        assertEquals(DefensePlant.PEA.health, plant.health)
    }

    @Test fun yetiRetreatsAndGiantThrowsImp() {
        val game = engine()
        val yeti = DefenseZombieUnit(1, ZombieKind.YETI, 0, 5f, specialMs = 9_900)
        val giant = DefenseZombieUnit(2, ZombieKind.GIANT, 1, 5f, health = 1_400)
        game.zombies += yeti
        game.zombies += giant
        game.step(100, DefenseDifficulty.RELAXED)
        assertTrue(yeti.retreating)
        assertTrue(yeti.x > 5f)
        assertTrue(game.zombies.any { it.type == ZombieKind.IMP })
    }

    @Test fun graveBusterClearsGraveAndBloverClearsAirborneWithWindAnimation() {
        val night = engine(DefenseBattlefield.NIGHT_GARDEN)
        night.select(DefensePlant.GRAVE)
        night.tapCell(0, 2)
        night.advance(1_300)
        assertFalse((0 to 2) in night.gravestones)
        assertFalse(night.plants.any { it.type == DefensePlant.GRAVE })

        val game = engine()
        game.plants += DefensePlantUnit(DefensePlant.BLOVER, 0, 0)
        game.zombies += DefenseZombieUnit(1, ZombieKind.BALLOON, 0, 5f)
        game.step(100, DefenseDifficulty.RELAXED)
        assertFalse(game.zombies.any { it.type == ZombieKind.BALLOON })
        assertTrue(game.explosions.any { it.kind == DefenseEffectKind.WIND })
    }

    @Test fun planternRevealsNearbyTilesAfterNightFogArrives() {
        val game = engine(DefenseBattlefield.NIGHT_GARDEN)
        game.plants += DefensePlantUnit(DefensePlant.PLANTERN, 2, 4)
        game.advance(30_000)
        assertFalse(game.isFogged(0, 2))
        assertTrue(game.isFogged(0, 7))
    }

    @Test fun marigoldCoinIsAutoCollectedByAwakeGoldMagnet() {
        val game = engine()
        game.select(DefensePlant.GOLDMAGNET)
        game.tapCell(0, 0)
        game.select(DefensePlant.COFFEE)
        game.tapCell(0, 0)
        game.select(DefensePlant.MARIGOLD)
        game.tapCell(0, 1)
        game.advance(30_000)
        assertEquals(0, game.stars.count { it.coin })
        assertTrue(game.starFlights.isNotEmpty())
        assertEquals(1, game.collectedStars)
    }

    @Test fun roofBossLaunchesVisibleProjectileAtPlants() {
        val game = engine(DefenseBattlefield.ROOFTOP)
        game.plants += DefensePlantUnit(DefensePlant.PEA, 2, 3)
        val boss = DefenseZombieUnit(1, ZombieKind.BOSS, 2, 7f, specialMs = 2_500)
        game.zombies += boss
        game.step(100, DefenseDifficulty.RELAXED)
        assertNotNull(game.zombieShots.singleOrNull())
        game.advance(700)
        assertTrue(game.plants.single().health < DefensePlant.PEA.health)
    }
}
