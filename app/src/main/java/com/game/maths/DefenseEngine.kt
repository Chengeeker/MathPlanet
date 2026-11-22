package com.game.maths

import android.content.Context
import kotlin.random.Random

internal const val MINE_ARMING_MS = 6_000L
internal const val STAR_REWARD = 50
internal const val CHOMPER_BITE_MS = 720L
internal const val CHERRY_FUSE_MS = 650L
internal const val STAR_FLIGHT_MS = 620L

internal enum class DefenseDifficulty(
    val title: String, val keepsRunningWhileAnswering: Boolean,
    val firstSpawnMs: Long, val baseSpawnMs: Long, val minSpawnMs: Long, val waveThreatBudget: Int
) {
    RELAXED("简单", false, 32_000, 30_000, 27_000, 1),
    HARD("困难", true, 26_000, 25_000, 22_000, 1),
    NIGHTMARE("噩梦", true, 20_000, 21_000, 18_000, 2)
}

internal object DefensePreferences {
    private const val FILE = "star_guard_settings"
    fun load(context: Context): DefenseDifficulty {
        val value = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getString("difficulty", DefenseDifficulty.RELAXED.name)
        return DefenseDifficulty.entries.firstOrNull { it.name == value }
            ?: DefenseDifficulty.RELAXED
    }
    fun save(context: Context, value: DefenseDifficulty) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
            .putString("difficulty", value.name).apply()
    }
}

internal enum class DefensePlant(
    val title: String, val cost: Int, val rechargeMs: Long,
    val health: Int, val damage: Int, val intervalMs: Long,
    val kind: PlantKind
) {
    SUN("星芽采集器", 50, 7_500, 240, 0, 24_000, PlantKind.GENERATOR),
    PEA("脉冲豆射器", 100, 7_500, 300, 20, 1_425, PlantKind.SHOT),
    CHERRY("双子爆裂果", 150, 50_000, 300, 1_800, 0, PlantKind.BLAST),
    WALL("陨石护墙", 50, 30_000, 4_000, 0, 0, PlantKind.WALL),
    MINE("地脉陷阱", 25, 30_000, 300, 1_800, 0, PlantKind.MINE),
    SNOW("霜晶射手", 175, 7_500, 300, 20, 1_425, PlantKind.SLOW_SHOT),
    CHOMPER("吞星兽", 150, 7_500, 300, 1_800, 42_000, PlantKind.EAT),
    DOUBLE("双发脉冲器", 200, 7_500, 300, 40, 1_425, PlantKind.DOUBLE_SHOT),
    PUFF("短程孢子灯", 0, 7_500, 300, 20, 1_425, PlantKind.SHOT),
    SUNSHROOM("幼星采集器", 25, 7_500, 240, 0, 24_000, PlantKind.GENERATOR),
    FUME("星雾穿透器", 75, 7_500, 300, 20, 1_000, PlantKind.PIERCE),
    GRAVE("遗迹清理器", 75, 30_000, 300, 0, 0, PlantKind.SUPPORT),
    HYPNO("回声诱导器", 75, 30_000, 300, 0, 0, PlantKind.SUPPORT),
    SCAREDY("胆小脉冲器", 25, 7_500, 300, 20, 1_425, PlantKind.SHOT),
    ICE("极寒星核", 75, 50_000, 300, 0, 0, PlantKind.FREEZE),
    DOOM("暗物质奇点", 125, 50_000, 300, 1_800, 0, PlantKind.BLAST),
    LILYPAD("漂浮轨道板", 25, 7_500, 300, 0, 0, PlantKind.AQUATIC),
    SQUASH("坠星冲击器", 50, 30_000, 300, 1_800, 0, PlantKind.EAT),
    THREEPEATER("三向脉冲器", 325, 7_500, 300, 20, 1_425, PlantKind.TRIPLE_SHOT),
    TANGLE("缠绕水草", 25, 30_000, 300, 1_800, 0, PlantKind.KELP),
    JALAPENO("赤焰轨道炮", 125, 50_000, 300, 1_800, 0, PlantKind.ROW_BLAST),
    SPIKEWEED("地刺阵列", 100, 7_500, 300, 20, 1_000, PlantKind.SPIKE),
    TORCHWOOD("余烬增幅器", 175, 30_000, 1_200, 0, 0, PlantKind.TORCH),
    TALLNUT("高墙卫星", 125, 30_000, 8_000, 0, 0, PlantKind.WALL),
    SEASHROOM("潮汐孢子灯", 0, 7_500, 300, 20, 1_425, PlantKind.SHOT),
    PLANTERN("星雾灯塔", 25, 30_000, 300, 0, 0, PlantKind.SUPPORT),
    CACTUS("尖晶拦截器", 125, 7_500, 300, 20, 1_425, PlantKind.CACTUS),
    BLOVER("风暴驱散器", 100, 30_000, 300, 0, 0, PlantKind.SUPPORT),
    SPLITPEA("双向脉冲器", 125, 7_500, 300, 20, 1_425, PlantKind.SPLIT_SHOT),
    STARFRUIT("五向星芒炮", 125, 7_500, 300, 20, 1_425, PlantKind.STAR_SHOT),
    PUMPKIN("护盾外壳", 125, 30_000, 4_000, 0, 0, PlantKind.PUMPKIN),
    MAGNET("磁力剥离器", 100, 30_000, 300, 0, 0, PlantKind.MAGNET),
    CABBAGE("抛物线投手", 100, 7_500, 300, 40, 2_000, PlantKind.LOB),
    FLOWERPOT("轨道花盆", 25, 7_500, 300, 0, 0, PlantKind.SUPPORT),
    KERNEL("黏滞抛投器", 100, 7_500, 300, 20, 2_000, PlantKind.LOB_SLOW),
    COFFEE("唤醒脉冲器", 75, 7_500, 300, 0, 0, PlantKind.COFFEE),
    GARLIC("路线偏转器", 50, 30_000, 400, 0, 0, PlantKind.GARLIC),
    UMBRELLA("轨道防护伞", 100, 30_000, 300, 0, 0, PlantKind.UMBRELLA),
    MARIGOLD("星币花", 50, 7_500, 240, 0, 30_000, PlantKind.GENERATOR),
    MELON("重力瓜投手", 300, 7_500, 300, 80, 3_000, PlantKind.LOB),
    GATLING("四连脉冲炮", 250, 50_000, 300, 80, 1_425, PlantKind.GATLING),
    TWINSUN("双核采集器", 150, 50_000, 240, 0, 24_000, PlantKind.TWIN_GENERATOR),
    GLOOM("环形星雾炮", 150, 50_000, 300, 20, 1_000, PlantKind.GLOOM),
    CATTAIL("全域追踪器", 225, 50_000, 300, 20, 1_425, PlantKind.CATTAIL),
    WINTERMELON("冰陨投手", 200, 50_000, 300, 80, 3_000, PlantKind.LOB_SLOW),
    GOLDMAGNET("星尘磁收器", 50, 30_000, 300, 0, 0, PlantKind.MAGNET),
    SPIKEROCK("重型地刺阵列", 125, 50_000, 900, 40, 1_000, PlantKind.SPIKE),
    COB("轨道巨炮", 500, 50_000, 3_000, 1_800, 15_000, PlantKind.COB),
    IMITATER("拟态模块", 0, 0, 300, 0, 0, PlantKind.UPGRADE)
}

internal enum class PlantKind {
    GENERATOR, TWIN_GENERATOR, SHOT, SLOW_SHOT, DOUBLE_SHOT, TRIPLE_SHOT,
    WALL, BLAST, MINE, PIERCE, EAT, FREEZE, ROW_BLAST, SPIKE, SUPPORT, AQUATIC,
    TORCH, CACTUS, SPLIT_SHOT, STAR_SHOT, PUMPKIN, MAGNET, LOB, LOB_SLOW,
    COFFEE, GARLIC, UMBRELLA, GATLING, GLOOM, CATTAIL, COB, KELP, UPGRADE
}

internal enum class ZombieKind(
    val title: String, val health: Int, val armor: Int, val speed: Float,
    val biteMs: Long, val behavior: ZombieBehavior
) {
    DRIFTER("游荡陨石", 190, 0, .22f, 1_000, ZombieBehavior.WALK),
    SIGNAL("引潮陨石", 190, 0, .27f, 1_000, ZombieBehavior.FAST),
    SHELL("甲壳陨石", 190, 370, .22f, 1_000, ZombieBehavior.ARMOR),
    VAULT("跃迁陨石", 335, 0, .36f, 1_000, ZombieBehavior.VAULT),
    HEAVY("重甲陨石", 190, 1_100, .18f, 1_000, ZombieBehavior.ARMOR),
    DASHER("裂隙陨石", 190, 0, .48f, 1_000, ZombieBehavior.FAST),
    NEWSPAPER("脆页狂奔者", 190, 150, .22f, 1_000, ZombieBehavior.RAGE),
    SCREEN("屏障卫士", 190, 1_100, .22f, 1_000, ZombieBehavior.SHIELD),
    FOOTBALL("冲锋重甲", 190, 1_500, .40f, 1_000, ZombieBehavior.FAST),
    DANCER("共振领航者", 340, 0, .30f, 1_000, ZombieBehavior.SUMMON),
    BACKUP("共振随行者", 190, 0, .22f, 1_000, ZombieBehavior.WALK),
    DUCK("浮圈陨石", 190, 0, .22f, 1_000, ZombieBehavior.WALK),
    SNORKEL("潜行陨石", 190, 0, .25f, 1_000, ZombieBehavior.DIVE),
    ZAMBONI("冰痕压路机", 1_350, 0, .30f, 1_000, ZombieBehavior.CRUSH),
    BOBSLED("冰道滑行队", 760, 300, .52f, 1_000, ZombieBehavior.SLED),
    DOLPHIN("跃海突袭者", 340, 0, .80f, 1_000, ZombieBehavior.VAULT),
    JACK("失控能量盒", 340, 0, .42f, 1_000, ZombieBehavior.EXPLODE),
    BALLOON("浮空气球体", 190, 20, .40f, 1_000, ZombieBehavior.AIRBORNE),
    DIGGER("地下掘进者", 290, 100, .32f, 1_000, ZombieBehavior.DIGGER),
    POGO("连续跃迁者", 340, 0, .52f, 1_000, ZombieBehavior.POGO),
    YETI("霜核游荡者", 1_000, 0, .32f, 1_000, ZombieBehavior.FAST),
    BUNGEE("空投掠夺者", 400, 0, .35f, 1_000, ZombieBehavior.AIRBORNE),
    LADDER("架桥攀爬者", 340, 500, .24f, 1_000, ZombieBehavior.LADDER),
    CATAPULT("远程投掷车", 1_350, 0, .20f, 1_000, ZombieBehavior.CATAPULT),
    GIANT("巨型陨石", 3_000, 0, .12f, 1_000, ZombieBehavior.GIANT),
    IMP("高速碎片", 70, 0, .52f, 800, ZombieBehavior.FAST),
    BOSS("核心吞噬体", 2_400, 500, .12f, 1_000, ZombieBehavior.GIANT)
}

internal enum class ZombieBehavior {
    WALK, FAST, ARMOR, VAULT, RAGE, SHIELD, SUMMON, DIVE, CRUSH, SLED,
    EXPLODE, AIRBORNE, DIGGER, POGO, LADDER, CATAPULT, GIANT
}

internal data class DefensePlantUnit(
    val type: DefensePlant, val row: Int, val col: Int,
    var health: Int = type.health,
    var elapsedMs: Long = if (type.kind == PlantKind.EAT) type.intervalMs else 0L,
    var armed: Boolean = type.kind != PlantKind.MINE,
    var attackMs: Long = 0L
)

internal data class DefenseZombieUnit(
    val id: Long, val type: ZombieKind, var row: Int,
    var x: Float, var health: Int = type.health, var armor: Int = type.armor,
    var biteElapsedMs: Long = 0L, var slowedMs: Long = 0L, var jumped: Boolean = false,
    var specialMs: Long = 0L, var angry: Boolean = false, var underground: Boolean = false,
    var returning: Boolean = false,
    var summoned: Boolean = false, var friendly: Boolean = false, var spawnBudgetCost: Int = 0,
    var swallowedMs: Long = 0L, var swallowedByCol: Int = -1
) {
    val totalHealth get() = health + armor
}

internal data class DefenseShot(
    val row: Int, var x: Float, val damage: Int, val slows: Boolean,
    val splash: Float = 0f, val pierces: Boolean = false, val speed: Float = .004f,
    val direction: Float = 1f, val targetKind: PlantKind = PlantKind.SHOT
)
internal data class FallingStar(
    val id: Long, val row: Int, val col: Int, var progress: Float = 0f, var ageMs: Long = 0L
)
internal data class StarFlight(
    val row: Int, val col: Int, val dropProgress: Float, val delayMs: Long = 0L, var elapsedMs: Long = 0L
)
internal data class DefenseExplosion(
    val row: Int, val x: Float, var remainingMs: Long = 500L,
    val durationMs: Long = 500L, val radiusCells: Float = .8f
)
internal data class DefenseQuestion(
    val problem: MathProblem, val starId: Long, val row: Int, val col: Int, val dropProgress: Float
)

private fun DefenseZombieUnit.behaviorCanBeHitBy(kind: PlantKind): Boolean {
    if (swallowedMs > 0) return false
    if (underground) return kind in setOf(PlantKind.LOB, PlantKind.LOB_SLOW, PlantKind.BLAST, PlantKind.ROW_BLAST, PlantKind.COB)
    return when (type.behavior) {
    ZombieBehavior.AIRBORNE -> kind in setOf(
        PlantKind.CACTUS, PlantKind.CATTAIL, PlantKind.ROW_BLAST,
        PlantKind.BLAST, PlantKind.COB, PlantKind.STAR_SHOT
    )
    ZombieBehavior.DIVE -> kind in setOf(
        PlantKind.LOB, PlantKind.LOB_SLOW, PlantKind.AQUATIC, PlantKind.BLAST, PlantKind.ROW_BLAST
    )
    ZombieBehavior.SHIELD -> kind !in setOf(PlantKind.SHOT, PlantKind.SLOW_SHOT, PlantKind.DOUBLE_SHOT)
    else -> true
    }
}

internal class DefenseEngine(private val random: Random = Random.Default) {
    val plants = mutableListOf<DefensePlantUnit>()
    val zombies = mutableListOf<DefenseZombieUnit>()
    val shots = mutableListOf<DefenseShot>()
    val stars = mutableListOf<FallingStar>()
    val starFlights = mutableListOf<StarFlight>()
    val explosions = mutableListOf<DefenseExplosion>()
    val readyAt = mutableMapOf<DefensePlant, Long>()
    var selected: DefensePlant? = null
    var shovelMode = false
    var question: DefenseQuestion? = null
    var sun = 200
        private set
    var lives = 5
        private set
    var answeredCorrectly = 0
        private set
    var answeredIncorrectly = 0
        private set
    var lastAutoPickupAward = 0
        private set
    var collectedStars = 0
        private set
    var autoPickupCharges = 0
        private set
    var elapsedMs = 0L
        private set
    var finished = false
        private set
    var won = false
        private set
    private var spawnMs = 0L
    private var threatWave = -1
    private var threatUsed = 0
    private var sunMs = 0L
    private var nextId = 1L
    private var previousPlant: DefensePlant? = null

    fun start() {
        if (plants.isEmpty() && zombies.isEmpty()) {
            sun = 200
            lives = 5
            finished = false
            won = false
            elapsedMs = 0L
        }
    }

    fun reset() {
        plants.clear(); zombies.clear(); shots.clear(); stars.clear(); starFlights.clear(); explosions.clear(); readyAt.clear()
        selected = null; shovelMode = false; question = null; sun = 200; lives = 5
        answeredCorrectly = 0; answeredIncorrectly = 0; lastAutoPickupAward = 0
        collectedStars = 0; autoPickupCharges = 0
        elapsedMs = 0; finished = false; won = false
        spawnMs = 0; threatWave = -1; threatUsed = 0; sunMs = 0; nextId = 1; previousPlant = null
    }

    fun step(deltaMs: Long, difficulty: DefenseDifficulty) {
        if (finished || (question != null && !difficulty.keepsRunningWhileAnswering)) return
        val dt = deltaMs.coerceIn(0, 100)
        elapsedMs += dt
        stars.forEach {
            it.ageMs += dt
            it.progress = (it.progress + dt / 2_000f).coerceAtMost(1f)
        }
        stars.removeAll { it.ageMs >= 20_000 }
        starFlights.forEach { it.elapsedMs += dt }
        starFlights.removeAll { it.elapsedMs >= it.delayMs + STAR_FLIGHT_MS }
        explosions.forEach { it.remainingMs -= dt }
        explosions.removeAll { it.remainingMs <= 0 }
        spawnMs += dt
        sunMs += dt
        if (sunMs >= 9_000) {
            sunMs %= 9_000
            createStar(FallingStar(nextId++, random.nextInt(5), random.nextInt(9)))
        }
        val wave = (elapsedMs / 45_000).toInt()
        if (wave != threatWave) {
            threatWave = wave
            threatUsed = 0
            zombies.forEach { it.spawnBudgetCost = 0 }
        }
        val spawnEvery = (difficulty.baseSpawnMs - wave * 750L).coerceAtLeast(difficulty.minSpawnMs)
        if (elapsedMs >= difficulty.firstSpawnMs && spawnMs >= spawnEvery && elapsedMs < 150_000) {
            spawnMs = 0
            val waveBudget = difficulty.waveThreatBudget + wave
            threatUsed += spawnZombie(waveBudget - threatUsed)
        }
        plants.toList().forEach { plant ->
            plant.elapsedMs += dt
            plant.attackMs = (plant.attackMs - dt).coerceAtLeast(0L)
            when (plant.type.kind) {
                PlantKind.GENERATOR, PlantKind.TWIN_GENERATOR -> if (plant.elapsedMs >= plant.type.intervalMs) {
                    plant.elapsedMs %= plant.type.intervalMs
                    repeat(if (plant.type.kind == PlantKind.TWIN_GENERATOR) 2 else 1) {
                        createStar(FallingStar(nextId++, plant.row, plant.col))
                    }
                }
                PlantKind.SHOT, PlantKind.SLOW_SHOT, PlantKind.DOUBLE_SHOT, PlantKind.PIERCE,
                PlantKind.TRIPLE_SHOT, PlantKind.SPLIT_SHOT, PlantKind.STAR_SHOT, PlantKind.CACTUS,
                PlantKind.CATTAIL, PlantKind.GATLING, PlantKind.GLOOM, PlantKind.LOB, PlantKind.LOB_SLOW, PlantKind.COB -> {
                    val targetZombie = zombies.firstOrNull { zombie ->
                        val laneTarget = when (plant.type.kind) {
                            PlantKind.TRIPLE_SHOT -> zombie.row in plant.row - 1..plant.row + 1
                            PlantKind.GLOOM -> zombie.row in plant.row - 1..plant.row + 1 &&
                                zombie.x in (plant.col - 1f)..(plant.col + 1.4f)
                            PlantKind.STAR_SHOT, PlantKind.CATTAIL -> true
                            else -> zombie.row == plant.row
                        }
                        val heldByWall = zombie.x <= plant.col &&
                            plants.any { barrier -> barrier.row == zombie.row && barrier.type.kind == PlantKind.WALL &&
                                zombie.x in (barrier.col - .2f)..(barrier.col + 1.05f) }
                        val directionTarget = plant.type.kind in setOf(PlantKind.SPLIT_SHOT, PlantKind.GLOOM) ||
                            zombie.x > plant.col || heldByWall
                        laneTarget && directionTarget && zombie.behaviorCanBeHitBy(plant.type.kind)
                    }
                    val interval = plant.type.intervalMs
                    if (targetZombie != null && interval > 0 && plant.elapsedMs >= interval) {
                        plant.elapsedMs %= interval
                        if (plant.type.kind != PlantKind.GLOOM && targetZombie.x <= plant.col + .55f) {
                            shots += DefenseShot(targetZombie.row, plant.col + .55f, plant.type.damage,
                                plant.type.kind == PlantKind.SLOW_SHOT || plant.type.kind == PlantKind.LOB_SLOW,
                                splash = if (plant.type.kind in setOf(PlantKind.LOB, PlantKind.LOB_SLOW, PlantKind.COB)) 1f else 0f,
                                pierces = plant.type.kind == PlantKind.PIERCE,
                                speed = if (plant.type.kind in setOf(PlantKind.LOB, PlantKind.LOB_SLOW, PlantKind.COB)) .0025f else .004f,
                                direction = -1f, targetKind = plant.type.kind)
                        } else when (plant.type.kind) {
                            PlantKind.TRIPLE_SHOT -> (plant.row - 1..plant.row + 1).filter { it in 0..4 }
                                .forEach { lane -> shots += DefenseShot(lane, plant.col + .55f, plant.type.damage, false) }
                            PlantKind.STAR_SHOT -> (0..4).filter { lane -> zombies.any { it.row == lane && it.x > plant.col } }
                                .forEach { lane -> shots += DefenseShot(lane, plant.col + .55f, plant.type.damage, false,
                                    targetKind = plant.type.kind) }
                            PlantKind.CATTAIL -> zombies.filter { it.x > plant.col }.minByOrNull { it.x }
                                ?.let { shots += DefenseShot(it.row, plant.col + .55f, plant.type.damage, false,
                                    targetKind = plant.type.kind) }
                            PlantKind.SPLIT_SHOT -> {
                                shots += DefenseShot(plant.row, plant.col + .55f, plant.type.damage, false,
                                    targetKind = plant.type.kind)
                                if (zombies.any { it.row == plant.row && it.x < plant.col }) {
                                    shots += DefenseShot(plant.row, plant.col - .55f, plant.type.damage, false,
                                        direction = -1f, targetKind = plant.type.kind)
                                }
                            }
                            PlantKind.GLOOM -> zombies.filter { it.row in plant.row - 1..plant.row + 1 && it.x in (plant.col - 1f)..(plant.col + 1.4f) }
                                .forEach { damage(it, plant.type.damage) }
                            else -> {
                                val shotCount = when (plant.type.kind) {
                                    PlantKind.DOUBLE_SHOT -> 2
                                    PlantKind.GATLING -> 4
                                    else -> 1
                                }
                                repeat(shotCount) {
                                    val torch = plants.any { it.type.kind == PlantKind.TORCH && it.row == plant.row && it.col > plant.col }
                                    shots += DefenseShot(plant.row, plant.col + .55f,
                                        plant.type.damage / shotCount * if (torch) 2 else 1,
                                        plant.type.kind == PlantKind.SLOW_SHOT || plant.type.kind == PlantKind.LOB_SLOW,
                                        splash = if (plant.type.kind in setOf(PlantKind.LOB, PlantKind.LOB_SLOW, PlantKind.COB)) 1f else 0f,
                                        pierces = plant.type.kind == PlantKind.PIERCE,
                                        speed = if (plant.type.kind in setOf(PlantKind.LOB, PlantKind.LOB_SLOW, PlantKind.COB)) .0025f else .004f,
                                        targetKind = plant.type.kind)
                                }
                            }
                        }
                    }
                }
                PlantKind.BLAST -> {
                    val radius = if (plant.type == DefensePlant.DOOM) 99f else 1.5f
                    if (plant.type == DefensePlant.CHERRY && plant.elapsedMs >= CHERRY_FUSE_MS) {
                        explosions += DefenseExplosion(plant.row, plant.col + .5f,
                            remainingMs = 700L, durationMs = 700L, radiusCells = 1.65f)
                        zombies.filter { it.row in (plant.row - 1)..(plant.row + 1) &&
                            it.x in (plant.col - 1f)..(plant.col + radius) }
                            .forEach { damage(it, plant.type.damage) }
                        plants.remove(plant)
                    } else if (plant.type != DefensePlant.CHERRY && zombies.any {
                            it.row in (plant.row - 1)..(plant.row + 1) && it.x in (plant.col - 1f)..(plant.col + radius)
                        }) {
                        val targets = zombies.filter { it.row in (plant.row - 1)..(plant.row + 1) &&
                            it.x in (plant.col - 1f)..(plant.col + radius) }
                        explosions += DefenseExplosion(plant.row, plant.col + .5f,
                            radiusCells = if (plant.type == DefensePlant.DOOM) 4f else 1.65f)
                        targets.forEach { damage(it, plant.type.damage) }
                        plants.remove(plant)
                    }
                }
                PlantKind.MINE -> {
                    if (plant.elapsedMs >= MINE_ARMING_MS) plant.armed = true
                    if (plant.armed && zombies.any { it.row == plant.row && it.x in (plant.col - 1.05f)..(plant.col + 1.05f) }) {
                        explosions += DefenseExplosion(plant.row, plant.col + .5f)
                        zombies.filter { it.row in (plant.row - 1)..(plant.row + 1) &&
                            it.x in (plant.col - 1.05f)..(plant.col + 1.05f) }
                            .forEach { damage(it, plant.type.damage) }
                        plants.remove(plant)
                    }
                }
                PlantKind.KELP -> zombies.firstOrNull { it.row == plant.row && it.x in (plant.col - .5f)..(plant.col + .8f) }
                    ?.let { it.health = 0; plants.remove(plant) }
                PlantKind.FREEZE -> if (zombies.isNotEmpty() && plant.elapsedMs >= 2_000) {
                    zombies.forEach { it.slowedMs = 4_000 }
                    plants.remove(plant)
                }
                PlantKind.ROW_BLAST -> if (zombies.any { it.row == plant.row }) {
                    zombies.filter { it.row == plant.row }.forEach { damage(it, plant.type.damage) }
                    plants.remove(plant)
                }
                PlantKind.SPIKE -> if (plant.elapsedMs >= 1_000) {
                    plant.elapsedMs %= 1_000
                    zombies.filter { it.row == plant.row && it.x in (plant.col - .2f)..(plant.col + .8f) }
                        .forEach { damage(it, plant.type.damage) }
                }
                PlantKind.EAT -> {
                    if (plant.type == DefensePlant.SQUASH) {
                        zombies.firstOrNull { it.row == plant.row && it.x in (plant.col - .5f)..(plant.col + 1.2f) }
                            ?.let { damage(it, plant.type.damage); plants.remove(plant) }
                    } else if (plant.elapsedMs >= plant.type.intervalMs) {
                        zombies.firstOrNull { it.row == plant.row && !it.friendly && it.totalHealth > 0 && it.swallowedMs <= 0 &&
                            it.type.behavior != ZombieBehavior.AIRBORNE && !it.underground &&
                            it.x in (plant.col - .5f)..(plant.col + 1.55f) }
                            ?.let {
                                it.swallowedMs = CHOMPER_BITE_MS
                                it.swallowedByCol = plant.col
                                plant.elapsedMs = 0
                                plant.attackMs = CHOMPER_BITE_MS
                            }
                    }
                }
                PlantKind.MAGNET -> if (plant.elapsedMs >= 3_000) {
                    plant.elapsedMs = 0
                    zombies.firstOrNull { it.row == plant.row && it.x in (plant.col - 1f)..(plant.col + 3f) && it.armor > 0 }
                        ?.let { it.armor = 0 }
                }
                PlantKind.SUPPORT -> when (plant.type) {
                    DefensePlant.HYPNO -> zombies.firstOrNull { it.row == plant.row && it.x in (plant.col - .5f)..(plant.col + .8f) }
                        ?.let { it.friendly = true; plants.remove(plant) }
                    DefensePlant.BLOVER -> {
                        val cleared = zombies.filter { it.type.behavior == ZombieBehavior.AIRBORNE }
                        cleared.forEach(::releaseSpawnBudget)
                        if (cleared.isNotEmpty()) spawnMs = (spawnEvery - 2_000L).coerceAtLeast(0L)
                        zombies.removeAll { it.type.behavior == ZombieBehavior.AIRBORNE }
                        plants.remove(plant)
                    }
                    else -> Unit
                }
                else -> Unit
            }
        }
        shots.forEach { shot ->
            shot.x += dt * shot.speed * shot.direction
            val hit = zombies.firstOrNull { it.row == shot.row &&
                (if (shot.direction > 0) it.x >= shot.x && it.x - shot.x < .35f
                 else it.x <= shot.x && shot.x - it.x < .35f) &&
                it.behaviorCanBeHitBy(shot.targetKind) }
            if (hit != null) {
                damage(hit, shot.damage)
                if (shot.slows) hit.slowedMs = 3_000
                if (shot.splash > 0) zombies.filter { it.row == hit.row && kotlin.math.abs(it.x - hit.x) <= shot.splash }
                    .filter { it.id != hit.id }.forEach { damage(it, shot.damage / 2) }
                if (!shot.pierces) shot.x = 99f
            }
        }
        shots.removeAll { it.x > 10f || it.x < -.5f }
        for (zombie in zombies.toList()) {
            if (zombie.swallowedMs > 0) {
                zombie.swallowedMs = (zombie.swallowedMs - dt).coerceAtLeast(0L)
                if (zombie.swallowedMs == 0L) { zombie.health = 0; zombie.armor = 0 }
                continue
            }
            if (zombie.friendly) {
                val target = zombies.firstOrNull { !it.friendly && it.row == zombie.row && it.x < zombie.x }
                if (target != null) damage(target, dt.toInt())
                else zombie.x += zombie.type.speed * dt / 1_000f
                continue
            }
            zombie.slowedMs = (zombie.slowedMs - dt).coerceAtLeast(0)
            zombie.specialMs += dt
            if (zombie.type.behavior == ZombieBehavior.SUMMON && zombie.specialMs >= 8_000) {
                zombie.specialMs = 0
                zombies += DefenseZombieUnit(nextId++, ZombieKind.BACKUP, zombie.row, zombie.x + .25f)
            }
            if (zombie.type.behavior == ZombieBehavior.GIANT && !zombie.summoned && zombie.health <= zombie.type.health / 2) {
                zombie.summoned = true
                zombies += DefenseZombieUnit(nextId++, ZombieKind.IMP, zombie.row, zombie.x + .4f)
            }
            if (zombie.type.behavior == ZombieBehavior.DIGGER && !zombie.underground && !zombie.returning && zombie.x < 5f) {
                zombie.underground = true
                zombie.x = -.1f
            }
            if (zombie.type.behavior == ZombieBehavior.EXPLODE) {
                val blastTarget = plants.asSequence()
                    .filter { it.row == zombie.row && zombie.x in (it.col - .2f)..(it.col + 1.05f) }
                    .maxByOrNull { it.col }
                if (blastTarget != null) {
                    plants.remove(blastTarget)
                    zombie.health = 0
                    continue
                }
            }
            if (zombie.type.behavior == ZombieBehavior.RAGE && zombie.armor <= 0) zombie.angry = true
            val frontPlant = if (zombie.underground) null else plants.asSequence()
                .filter { it.row == zombie.row && zombie.x in (it.col - .2f)..(it.col + 1.05f) &&
                    (it.type.kind != PlantKind.MINE || it.armed) }
                .maxWithOrNull(compareBy<DefensePlantUnit> { it.col }
                    .thenBy { if (it.type == DefensePlant.PUMPKIN) 1 else 0 })
            if (frontPlant != null && !(zombie.type.behavior == ZombieBehavior.VAULT && !zombie.jumped)) {
                zombie.biteElapsedMs += dt
                if (zombie.biteElapsedMs >= zombie.type.biteMs) {
                    zombie.biteElapsedMs %= zombie.type.biteMs
                    frontPlant.health -= when {
                        zombie.type.behavior in setOf(ZombieBehavior.CRUSH, ZombieBehavior.GIANT) -> frontPlant.health
                        else -> 100
                    }
                    if (frontPlant.health <= 0) plants.remove(frontPlant)
                    if (frontPlant.type.kind == PlantKind.GARLIC) zombie.row = (zombie.row + 1) % 5
                }
            } else {
                if (zombie.type.behavior in setOf(ZombieBehavior.VAULT, ZombieBehavior.POGO) &&
                    !zombie.jumped && frontPlant != null) {
                    zombie.x = frontPlant.col - .6f
                    zombie.jumped = true
                } else {
                    val slow = if (zombie.slowedMs > 0) .5f else 1f
                    val rage = if (zombie.angry) 2f else 1f
                    val movement = zombie.type.speed * dt / 1_000f * slow * rage
                    when {
                        zombie.underground -> {
                            zombie.x += movement
                            if (zombie.x >= .2f) { zombie.underground = false; zombie.returning = true }
                        }
                        zombie.returning -> {
                            zombie.x += movement
                            if (zombie.x >= 9.1f) zombie.returning = false
                        }
                        else -> zombie.x -= movement
                    }
                }
            }
            if (zombie.x < -.5f) {
                lives--
                zombies.remove(zombie)
                releaseSpawnBudget(zombie)
                if (lives <= 0) finished = true
            }
        }
        val killed = zombies.filter { it.totalHealth <= 0 }
        if (killed.isNotEmpty()) {
            killed.forEach(::releaseSpawnBudget)
            zombies.removeAll { it.totalHealth <= 0 }
            spawnMs = (spawnEvery - 2_000L).coerceAtLeast(0L)
        }
        if (elapsedMs >= 150_000 && zombies.isEmpty()) { finished = true; won = lives > 0 }
    }

    fun cost(type: DefensePlant): Int? = if (type == DefensePlant.IMITATER) previousPlant?.cost else type.cost

    fun select(type: DefensePlant): String {
        if (finished) return "本局已结束"
        if (selected == type) {
            selected = null
            return "已取消选择：${type.title}"
        }
        if (question != null) return "请先完成当前题目"
        val cost = cost(type) ?: return "拟态模块需要先部署一个可复制模块"
        if (sun < cost) return "星能不足：${type.title}需要 $cost 点"
        if ((readyAt[type] ?: 0L) > elapsedMs) return "${type.title}还在冷却"
        shovelMode = false
        selected = type
        return "已选：${type.title} · $cost 星能；点击空格部署"
    }

    fun toggleShovel() { shovelMode = !shovelMode; selected = null }

    fun tapCell(row: Int, col: Int): String {
        if (row !in 0..4 || col !in 0..8) return "请点击轨道格子"
        if (shovelMode) {
            val target = plants.lastOrNull { it.row == row && it.col == col }
            shovelMode = false
            if (target == null) return "这里没有可铲除的模块"
            plants.remove(target)
            return "已铲除：${target.type.title}"
        }
        if (finished) return "本局已结束"
        if (question != null) return "请先完成当前题目"
        val packet = selected ?: return "先选择左侧模块，再点轨道格子"
        val isWater = row == 2
        val aquaticPlant = packet.kind == PlantKind.AQUATIC || packet in setOf(DefensePlant.TANGLE, DefensePlant.SEASHROOM)
        if (aquaticPlant && !isWater) return "${packet.title}只能放在水域"
        val onCell = plants.filter { it.row == row && it.col == col }
        val hasPad = onCell.any { it.type == DefensePlant.LILYPAD }
        if (isWater && packet != DefensePlant.LILYPAD && !aquaticPlant && !hasPad) return "水域需要先部署漂浮轨道板"
        if (packet == DefensePlant.PUMPKIN && onCell.isEmpty()) return "护盾外壳要套在已有模块上"
        if (onCell.isNotEmpty() && packet != DefensePlant.PUMPKIN &&
            !(isWater && hasPad && onCell.all { it.type == DefensePlant.LILYPAD })) return "这个格子已经有模块了"
        val type = if (packet == DefensePlant.IMITATER) previousPlant ?: return "拟态模块需要先部署一个可复制模块" else packet
        val cost = if (packet == DefensePlant.IMITATER) type.cost else packet.cost
        if (sun < cost) return "星能不足：需要 $cost 点"
        if ((readyAt[packet] ?: 0L) > elapsedMs) return "${packet.title}还在冷却"
        sun -= cost
        plants += DefensePlantUnit(type, row, col)
        readyAt[packet] = elapsedMs + if (packet == DefensePlant.IMITATER) type.rechargeMs else packet.rechargeMs
        previousPlant = type
        selected = null
        return "已部署：${type.title}"
    }

    fun tapStar(starId: Long, settings: GameSettings): MathProblem? {
        if (question != null || finished) return null
        val star = stars.firstOrNull { it.id == starId } ?: return null
        question = DefenseQuestion(MathEngine.generate(settings) ?: return null,
            starId, star.row, star.col, star.progress)
        return question?.problem
    }

    fun answer(value: Int, difficulty: DefenseDifficulty): Boolean {
        lastAutoPickupAward = 0
        val pending = question ?: return false
        val star = stars.firstOrNull { it.id == pending.starId }
        stars.removeAll { it.id == pending.starId }
        question = null
        if (finished) return false
        if (value != pending.problem.answer) {
            answeredIncorrectly++
            if (difficulty == DefenseDifficulty.NIGHTMARE) sun = (sun - 25).coerceAtLeast(0)
            return false
        }
        sun += STAR_REWARD
        starFlights += StarFlight(star?.row ?: pending.row, star?.col ?: pending.col,
            star?.progress ?: pending.dropProgress)
        answeredCorrectly++
        if (answeredCorrectly % 5 == 0) {
            lastAutoPickupAward = random.nextInt(2, 4)
            autoPickupCharges += lastAutoPickupAward
            collectAutoPickups()
        }
        collectedStars++
        return true
    }

    fun abandonQuestion() {
        question?.let { pending -> stars.removeAll { it.id == pending.starId } }
        question = null
    }

    fun sunQuestion(id: Long) = stars.firstOrNull { it.id == id }

    private fun createStar(star: FallingStar) {
        if (autoPickupCharges > 0) {
            autoPickupCharges--
            sun += STAR_REWARD
            collectedStars++
            starFlights += StarFlight(star.row, star.col, star.progress)
        } else stars += star
    }

    private fun collectAutoPickups() {
        var delayMs = 0L
        while (autoPickupCharges > 0 && stars.isNotEmpty()) {
            val star = stars.removeAt(0)
            starFlights += StarFlight(star.row, star.col, star.progress, delayMs = delayMs)
            delayMs += 90L
            autoPickupCharges--
            sun += STAR_REWARD
            collectedStars++
        }
    }

    private fun spawnZombie(availableThreat: Int): Int {
        val wave = (elapsedMs / 45_000).toInt()
        val pool = when {
            elapsedMs < 60_000 -> listOf(ZombieKind.DRIFTER)
            elapsedMs < 90_000 -> listOf(ZombieKind.DRIFTER, ZombieKind.SIGNAL, ZombieKind.SHELL)
            elapsedMs < 120_000 -> listOf(ZombieKind.DRIFTER, ZombieKind.SIGNAL, ZombieKind.SHELL,
                ZombieKind.NEWSPAPER, ZombieKind.SCREEN)
            elapsedMs < 150_000 -> listOf(ZombieKind.DRIFTER, ZombieKind.SIGNAL, ZombieKind.SHELL,
                ZombieKind.NEWSPAPER, ZombieKind.SCREEN, ZombieKind.HEAVY, ZombieKind.VAULT, ZombieKind.FOOTBALL)
            wave == 3 -> listOf(ZombieKind.ZAMBONI, ZombieKind.BOBSLED, ZombieKind.JACK,
                ZombieKind.BALLOON, ZombieKind.DIGGER, ZombieKind.POGO, ZombieKind.LADDER)
            else -> listOf(ZombieKind.YETI, ZombieKind.BUNGEE, ZombieKind.CATAPULT,
                ZombieKind.GIANT, ZombieKind.IMP, ZombieKind.DASHER)
        }
        val eligible = pool.filter { it.threatPoints() <= availableThreat }
        if (eligible.isEmpty()) return 0
        val type = if (wave >= 3 && ZombieKind.BOSS.threatPoints() <= availableThreat &&
            random.nextInt(100) < 5) ZombieKind.BOSS else eligible.random(random)
        val row = if (type in setOf(ZombieKind.DUCK, ZombieKind.SNORKEL,
                ZombieKind.BOBSLED, ZombieKind.DOLPHIN)) 2 else random.nextInt(5)
        val threat = type.threatPoints()
        zombies += DefenseZombieUnit(nextId++, type, row, 9.1f, spawnBudgetCost = threat)
        return threat
    }

    private fun releaseSpawnBudget(zombie: DefenseZombieUnit) {
        threatUsed = (threatUsed - zombie.spawnBudgetCost).coerceAtLeast(0)
        zombie.spawnBudgetCost = 0
    }

    private fun ZombieKind.threatPoints() = ((health + armor + 999) / 1_000).coerceAtLeast(1)

    private fun damage(zombie: DefenseZombieUnit, amount: Int) {
        if (zombie.swallowedMs > 0) return
        val armorDamage = minOf(zombie.armor, amount)
        zombie.armor -= armorDamage
        zombie.health -= amount - armorDamage
    }
}
