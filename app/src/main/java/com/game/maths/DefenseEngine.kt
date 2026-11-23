package com.game.maths

import android.content.Context
import kotlin.random.Random

internal const val MINE_ARMING_MS = 6_000L
internal const val STAR_REWARD = 25
private const val LARGE_SUN_REWARD = 75
private const val MARIGOLD_COIN_REWARD = 50
internal const val CHOMPER_BITE_MS = 720L
internal const val CHERRY_FUSE_MS = 650L
internal const val STAR_FLIGHT_MS = 620L

private val SHIELD_BYPASS_SHOTS = setOf(PlantKind.PIERCE, PlantKind.LOB, PlantKind.LOB_SLOW,
    PlantKind.GLOOM, PlantKind.SPIKE, PlantKind.KELP, PlantKind.COB, PlantKind.BLAST,
    PlantKind.ROW_BLAST)
private val DAMAGEABLE_GEAR = setOf(ZombieKind.SHELL, ZombieKind.HEAVY, ZombieKind.NEWSPAPER,
    ZombieKind.SCREEN, ZombieKind.FOOTBALL, ZombieKind.LADDER)
private val TOOL_GEAR = setOf(ZombieKind.DIGGER, ZombieKind.JACK, ZombieKind.POGO)
private val POOL_ZOMBIES = setOf(ZombieKind.DUCK, ZombieKind.SNORKEL, ZombieKind.DOLPHIN)
private val POOL_ROWS = setOf(2, 3)
private val WATER_LANE_ZOMBIES = POOL_ZOMBIES + ZombieKind.ZAMBONI + ZombieKind.BOBSLED
private val MAGNETIZABLE_ZOMBIES = setOf(ZombieKind.HEAVY, ZombieKind.SCREEN, ZombieKind.FOOTBALL,
    ZombieKind.DIGGER, ZombieKind.JACK, ZombieKind.POGO, ZombieKind.LADDER)
private val MUSHROOMS = setOf(DefensePlant.PUFF, DefensePlant.SUNSHROOM, DefensePlant.FUME,
    DefensePlant.SCAREDY, DefensePlant.ICE, DefensePlant.DOOM, DefensePlant.SEASHROOM,
    DefensePlant.GLOOM, DefensePlant.MAGNET, DefensePlant.GOLDMAGNET)

internal enum class DefenseDifficulty(
    val title: String, val keepsRunningWhileAnswering: Boolean,
    val firstSpawnMs: Long, val baseSpawnMs: Long, val minSpawnMs: Long, val waveThreatBudget: Int
) {
    RELAXED("简单", false, 32_000, 30_000, 27_000, 3),
    HARD("困难", true, 26_000, 25_000, 22_000, 4),
    NIGHTMARE("噩梦", true, 20_000, 21_000, 18_000, 5)
}

internal enum class DefenseBattlefield(
    val title: String, val detail: String, val hasPool: Boolean = false,
    val night: Boolean = false, val fog: Boolean = false, val roof: Boolean = false
) {
    LAWN_POOL("草坪 + 泳池", "两侧草地；中央两行需要荷叶", hasPool = true),
    NIGHT_GARDEN("夜间墓园", "月色草地、墓碑与会散开的迷雾", night = true, fog = true),
    ROOFTOP("白天屋顶", "左侧五列斜坡、右侧平顶；直射弹会撞瓦", roof = true),
    DAY_LAWN("白天草坪", "五行草地；天空每 9 秒生成阳光"),
    NIGHT_LAWN("夜晚草坪", "月光与暗色草地；夜间蘑菇自然苏醒", night = true),
    DAY_POOL("白天泳池", "六行场地；中央两行为水路", hasPool = true),
    FOG_POOL("雾夜泳池", "夜色、双水路与移动迷雾", hasPool = true, night = true, fog = true),
    NIGHT_ROOF("夜间屋顶", "暗色瓦顶；左坡挡直射弹，投手越坡", night = true, roof = true);

    val rowCount get() = if (hasPool) 6 else 5
    val waterRows get() = if (hasPool) POOL_ROWS else emptySet()
    val graveCells get() = if (hasPool) setOf(0 to 2, 1 to 6, 4 to 4, 5 to 7)
        else setOf(0 to 2, 1 to 6, 3 to 4, 4 to 7)
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
    SUN("向日葵", 50, 7_500, 240, 0, 24_000, PlantKind.GENERATOR),
    PEA("豌豆射手", 100, 7_500, 300, 20, 1_425, PlantKind.SHOT),
    CHERRY("樱桃炸弹", 150, 50_000, 300, 1_800, 0, PlantKind.BLAST),
    WALL("坚果墙", 50, 30_000, 4_000, 0, 0, PlantKind.WALL),
    MINE("土豆地雷", 25, 30_000, 300, 1_800, 0, PlantKind.MINE),
    SNOW("寒冰射手", 175, 7_500, 300, 20, 1_425, PlantKind.SLOW_SHOT),
    CHOMPER("大嘴花", 150, 7_500, 300, 1_800, 42_000, PlantKind.EAT),
    DOUBLE("双发射手", 200, 7_500, 300, 40, 1_425, PlantKind.DOUBLE_SHOT),
    PUFF("小喷菇", 0, 7_500, 300, 20, 1_425, PlantKind.SHOT),
    SUNSHROOM("阳光菇", 25, 7_500, 240, 0, 24_000, PlantKind.GENERATOR),
    FUME("大喷菇", 75, 7_500, 300, 20, 1_000, PlantKind.PIERCE),
    GRAVE("墓碑吞噬者", 75, 7_500, 300, 0, 0, PlantKind.SUPPORT),
    HYPNO("魅惑菇", 75, 30_000, 300, 0, 0, PlantKind.SUPPORT),
    SCAREDY("胆小菇", 25, 7_500, 300, 20, 1_425, PlantKind.SHOT),
    ICE("寒冰菇", 75, 50_000, 300, 0, 0, PlantKind.FREEZE),
    DOOM("毁灭菇", 125, 50_000, 300, 1_800, 0, PlantKind.BLAST),
    LILYPAD("荷叶", 25, 7_500, 300, 0, 0, PlantKind.AQUATIC),
    SQUASH("窝瓜", 50, 30_000, 300, 1_800, 0, PlantKind.EAT),
    THREEPEATER("三线射手", 325, 7_500, 300, 20, 1_425, PlantKind.TRIPLE_SHOT),
    TANGLE("缠绕海草", 25, 7_500, 300, 1_800, 0, PlantKind.KELP),
    JALAPENO("火爆辣椒", 125, 50_000, 300, 1_800, 0, PlantKind.ROW_BLAST),
    SPIKEWEED("地刺", 100, 7_500, 300, 20, 1_000, PlantKind.SPIKE),
    TORCHWOOD("火炬树桩", 175, 7_500, 1_200, 0, 0, PlantKind.TORCH),
    TALLNUT("高坚果", 125, 30_000, 8_000, 0, 0, PlantKind.WALL),
    SEASHROOM("海蘑菇", 0, 30_000, 300, 20, 1_425, PlantKind.SHOT),
    PLANTERN("灯笼草", 25, 30_000, 300, 0, 0, PlantKind.SUPPORT),
    CACTUS("仙人掌", 125, 7_500, 300, 20, 1_425, PlantKind.CACTUS),
    BLOVER("三叶草", 100, 7_500, 300, 0, 0, PlantKind.SUPPORT),
    SPLITPEA("分裂豆", 125, 7_500, 300, 20, 1_425, PlantKind.SPLIT_SHOT),
    STARFRUIT("杨桃", 125, 7_500, 300, 20, 1_425, PlantKind.STAR_SHOT),
    PUMPKIN("南瓜头", 125, 30_000, 4_000, 0, 0, PlantKind.PUMPKIN),
    MAGNET("磁力菇", 100, 7_500, 300, 0, 0, PlantKind.MAGNET),
    CABBAGE("卷心菜投手", 100, 7_500, 300, 40, 2_000, PlantKind.LOB),
    FLOWERPOT("花盆", 25, 7_500, 300, 0, 0, PlantKind.SUPPORT),
    KERNEL("玉米投手", 100, 7_500, 300, 20, 2_000, PlantKind.LOB_SLOW),
    COFFEE("咖啡豆", 75, 7_500, 300, 0, 0, PlantKind.COFFEE),
    GARLIC("大蒜", 50, 7_500, 400, 0, 0, PlantKind.GARLIC),
    UMBRELLA("叶子保护伞", 100, 7_500, 300, 0, 0, PlantKind.UMBRELLA),
    MARIGOLD("金盏花", 50, 7_500, 240, 0, 30_000, PlantKind.GENERATOR),
    MELON("西瓜投手", 300, 7_500, 300, 80, 3_000, PlantKind.LOB),
    GATLING("机枪豌豆", 250, 50_000, 300, 80, 1_425, PlantKind.GATLING),
    TWINSUN("双子向日葵", 150, 50_000, 240, 0, 24_000, PlantKind.TWIN_GENERATOR),
    GLOOM("忧郁菇", 150, 50_000, 300, 20, 1_000, PlantKind.GLOOM),
    CATTAIL("香蒲", 225, 50_000, 300, 20, 1_425, PlantKind.CATTAIL),
    WINTERMELON("冰西瓜投手", 200, 50_000, 300, 80, 3_000, PlantKind.LOB_SLOW),
    GOLDMAGNET("金磁力菇", 50, 50_000, 300, 0, 0, PlantKind.MAGNET),
    SPIKEROCK("地刺王", 125, 50_000, 900, 40, 1_000, PlantKind.SPIKE),
    COB("玉米加农炮", 500, 50_000, 3_000, 1_800, 34_750, PlantKind.COB),
    IMITATER("模仿者", 0, 0, 300, 0, 0, PlantKind.UPGRADE)
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
    DRIFTER("普通僵尸", 190, 0, .22f, 1_000, ZombieBehavior.WALK),
    SIGNAL("旗帜僵尸", 190, 0, .22f, 1_000, ZombieBehavior.WALK),
    SHELL("路障僵尸", 190, 370, .22f, 1_000, ZombieBehavior.ARMOR),
    VAULT("撑杆僵尸", 335, 0, .36f, 1_000, ZombieBehavior.VAULT),
    HEAVY("铁桶僵尸", 190, 1_100, .18f, 1_000, ZombieBehavior.ARMOR),
    DASHER("高速僵尸（本项目原创）", 190, 0, .48f, 1_000, ZombieBehavior.FAST),
    NEWSPAPER("读报僵尸", 190, 150, .22f, 1_000, ZombieBehavior.RAGE),
    SCREEN("铁门僵尸", 190, 1_100, .22f, 1_000, ZombieBehavior.SHIELD),
    FOOTBALL("橄榄球僵尸", 190, 1_500, .40f, 1_000, ZombieBehavior.FAST),
    DANCER("舞王僵尸", 340, 0, .30f, 1_000, ZombieBehavior.SUMMON),
    BACKUP("伴舞僵尸", 190, 0, .22f, 1_000, ZombieBehavior.WALK),
    DUCK("鸭子救生圈僵尸", 190, 0, .22f, 1_000, ZombieBehavior.WALK),
    SNORKEL("潜水僵尸", 190, 0, .25f, 1_000, ZombieBehavior.DIVE),
    ZAMBONI("冰车僵尸", 1_350, 0, .30f, 1_000, ZombieBehavior.CRUSH),
    BOBSLED("雪橇僵尸小队", 760, 300, .52f, 1_000, ZombieBehavior.SLED),
    DOLPHIN("海豚骑士僵尸", 340, 0, .80f, 1_000, ZombieBehavior.VAULT),
    JACK("小丑僵尸", 340, 0, .42f, 1_000, ZombieBehavior.EXPLODE),
    BALLOON("气球僵尸", 190, 20, .40f, 1_000, ZombieBehavior.AIRBORNE),
    DIGGER("矿工僵尸", 290, 100, .32f, 1_000, ZombieBehavior.DIGGER),
    POGO("跳跳僵尸", 340, 0, .52f, 1_000, ZombieBehavior.POGO),
    YETI("雪人僵尸", 1_000, 0, .32f, 1_000, ZombieBehavior.YETI),
    BUNGEE("蹦极僵尸", 400, 0, .35f, 1_000, ZombieBehavior.BUNGEE),
    LADDER("扶梯僵尸", 340, 500, .24f, 1_000, ZombieBehavior.LADDER),
    CATAPULT("投篮僵尸", 1_350, 0, .20f, 1_000, ZombieBehavior.CATAPULT),
    GIANT("巨人僵尸", 3_000, 0, .12f, 1_000, ZombieBehavior.GIANT),
    IMP("小鬼僵尸", 70, 0, .52f, 800, ZombieBehavior.FAST),
    BOSS("僵王博士", 8_000, 1_500, .12f, 1_000, ZombieBehavior.BOSS)
}

internal enum class ZombieBehavior {
    WALK, FAST, ARMOR, VAULT, RAGE, SHIELD, SUMMON, DIVE, CRUSH, SLED,
    EXPLODE, AIRBORNE, BUNGEE, DIGGER, POGO, LADDER, CATAPULT, GIANT, YETI, BOSS
}

internal data class DefensePlantUnit(
    var type: DefensePlant, val row: Int, val col: Int,
    var health: Int = type.health,
    var elapsedMs: Long = if (type.kind == PlantKind.EAT) type.intervalMs else 0L,
    var armed: Boolean = type.kind != PlantKind.MINE,
    var attackMs: Long = 0L, var frightened: Boolean = false,
    var awake: Boolean = type !in MUSHROOMS,
    var ageMs: Long = 0L,
    var laddered: Boolean = false, var activated: Boolean = false,
    var targetZombieId: Long = -1L, var disguiseType: DefensePlant? = null,
    var transformingMs: Long = 0L
)

private fun DefenseZombieUnit.hasMetalGear() = !equipmentRemoved && type in MAGNETIZABLE_ZOMBIES

internal data class DefenseZombieUnit(
    val id: Long, val type: ZombieKind, var row: Int,
    var x: Float, var health: Int = type.health, var armor: Int = type.armor,
    var biteElapsedMs: Long = 0L, var slowedMs: Long = 0L, var jumped: Boolean = false,
    var specialMs: Long = 0L, var angry: Boolean = false, var underground: Boolean = false,
    var returning: Boolean = false,
    var summoned: Boolean = false, var friendly: Boolean = false, var spawnBudgetCost: Int = 0,
    var equipmentRemoved: Boolean = false,
    var swallowedMs: Long = 0L, var swallowedByCol: Int = -1, var stunnedMs: Long = 0L,
    var actionMs: Long = 0L, var jumpingMs: Long = 0L, var retreating: Boolean = false,
    var diveUnderwater: Boolean = true, var targetRow: Int = -1, var targetCol: Int = -1,
    var attackTriggered: Boolean = false, var gearLostMs: Long = 0L,
    var capturedPlantType: DefensePlant? = null
) {
    val totalHealth get() = health + armor
}

internal data class DefenseShot(
    val row: Int, var x: Float, var damage: Int, var slows: Boolean,
    var splash: Float = 0f, val pierces: Boolean = false, val speed: Float = .004f,
    val direction: Float = 1f, val targetKind: PlantKind = PlantKind.SHOT,
    val splashRows: Int = 0, val launchX: Float = x, val landingX: Float = x,
    val flightDurationMs: Long = 0L, var flightElapsedMs: Long = 0L,
    val arcHeightCells: Float = 0f, var arcOffsetCells: Float = 0f,
    val sourceType: DefensePlant = DefensePlant.PEA,
    val homing: Boolean = false, var targetId: Long = -1L,
    var lane: Float = row.toFloat(), var targetX: Float = landingX, var targetLane: Float = row.toFloat(),
    val vectorShot: Boolean = false, val laneDirection: Float = 0f,
    val stuns: Boolean = false, val butter: Boolean = false,
    val roofBlockAtX: Float? = null,
    val impactAtEnd: Boolean = false, val impactRow: Int = row, val impactX: Float = landingX,
    var burning: Boolean = false, var spent: Boolean = false,
    val hitIds: MutableSet<Long> = mutableSetOf()
)
internal data class FallingStar(
    val id: Long, val row: Int, val col: Int, var progress: Float = 0f, var ageMs: Long = 0L,
    val value: Int = STAR_REWARD, val coin: Boolean = false, val largeSun: Boolean = false
)
internal data class StarFlight(
    val row: Int, val col: Int, val dropProgress: Float, val delayMs: Long = 0L,
    var elapsedMs: Long = 0L, val largeSun: Boolean = false
)
internal data class DefenseExplosion(
    val row: Int, val x: Float, var remainingMs: Long = 500L,
    val durationMs: Long = 500L, val radiusCells: Float = .8f,
    val kind: DefenseEffectKind = DefenseEffectKind.BLAST, val endRow: Int = row,
    val endX: Float = x
)
internal enum class DefenseEffectKind { BLAST, FREEZE, FLAME_ROW, WIND, MAGNET, CHARM, WAKE, SQUASH, KELP, TRANSFORM, SHIELD, ROOF_IMPACT, MOWER }
internal data class DefenseZombieShot(
    val row: Int, val col: Int, val targetRow: Int, val targetCol: Int,
    var remainingMs: Long = 650L, val durationMs: Long = 650L, val damage: Int = 160
)
internal data class DefenseQuestion(
    val problem: MathProblem, val starId: Long, val row: Int, val col: Int,
    val dropProgress: Float, val value: Int = STAR_REWARD,
    val largeSun: Boolean = false, val challengeHint: String? = null
)

private fun DefenseZombieUnit.behaviorCanBeHitBy(kind: PlantKind): Boolean {
    if (swallowedMs > 0) return false
    if (underground) return kind in setOf(PlantKind.LOB, PlantKind.LOB_SLOW, PlantKind.BLAST, PlantKind.ROW_BLAST, PlantKind.COB)
    return when (type.behavior) {
    ZombieBehavior.AIRBORNE, ZombieBehavior.BUNGEE -> kind in setOf(
        PlantKind.CACTUS, PlantKind.CATTAIL, PlantKind.ROW_BLAST,
        PlantKind.BLAST, PlantKind.COB, PlantKind.STAR_SHOT
    )
    ZombieBehavior.DIVE -> !diveUnderwater || kind in setOf(
        PlantKind.LOB, PlantKind.LOB_SLOW, PlantKind.AQUATIC, PlantKind.BLAST, PlantKind.ROW_BLAST
    )
    else -> true
    }
}

internal class DefenseEngine(
    private val random: Random = Random.Default,
    val battlefield: DefenseBattlefield = DefenseBattlefield.LAWN_POOL,
    val campaignLevel: DefenseCampaignLevel? = null
) {
    val plants = mutableListOf<DefensePlantUnit>()
    val zombies = mutableListOf<DefenseZombieUnit>()
    val shots = mutableListOf<DefenseShot>()
    val stars = mutableListOf<FallingStar>()
    val starFlights = mutableListOf<StarFlight>()
    val explosions = mutableListOf<DefenseExplosion>()
    val zombieShots = mutableListOf<DefenseZombieShot>()
    val craters = mutableSetOf<Pair<Int, Int>>()
    val gravestones = mutableSetOf<Pair<Int, Int>>()
    val iceTrails = mutableMapOf<Pair<Int, Int>, Long>()
    val mowerRows = initialMowerRows().toMutableSet()
    val readyAt = mutableMapOf<DefensePlant, Long>()
    var selected: DefensePlant? = null
    var cobTargeting: DefensePlantUnit? = null
    var shovelMode = false
    var question: DefenseQuestion? = null
    var sun = campaignLevel?.startingSun ?: 200
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
    var plantHitSoundEvents = 0L
        private set
    var zombieBiteSoundEvents = 0L
        private set
    var finished = false
        private set
    var won = false
        private set
    private var spawnMs = 0L
    private var threatWave = -1
    private var threatUsed = 0
    private var spawnGroups = 0
    private var sunMs = 0L
    private var regularSunsSinceLarge = 0
    private var nextLargeSunChallengeUsesRange = true
    private var nextId = 1L
    var imitaterTarget: DefensePlant? = null
        private set
    private var fogClearedUntilMs = 0L
    private var bossSpawned = false
    private val triggeredCampaignEvents = mutableSetOf<Int>()
    private val mowerSpawnBlockedUntilMs = mutableMapOf<Int, Long>()

    private fun initialMowerRows(): Set<Int> = if (battlefield.roof) emptySet() else
        (campaignLevel?.activeRows ?: (0 until battlefield.rowCount).toSet())
            .filter { it in 0 until battlefield.rowCount && it !in battlefield.waterRows }.toSet()

    fun campaignWarning(): String? {
        val level = campaignLevel ?: return null
        if (level.id == 50 && elapsedMs in 122_000L..130_000L) return "首领即将到达：留意战场提示"
        val warning = level.spawnEvents.withIndex().firstOrNull { (index, event) ->
            index !in triggeredCampaignEvents && elapsedMs in
                ((event.notBeforeSec - event.telegraphSec).coerceAtLeast(0) * 1_000L)..(event.notBeforeSec * 1_000L)
        }?.value ?: return null
        return "即将出现：${warning.zombie.title}"
    }

    private fun createShot(
        plant: DefensePlantUnit, target: DefenseZombieUnit, row: Int = target.row,
        damage: Int = plant.type.damage, direction: Float = 1f,
        vectorShot: Boolean = false, laneDirection: Float = 0f, offsetCells: Float = 0f
    ): DefenseShot {
        val kind = plant.type.kind
        val butter = plant.type == DefensePlant.KERNEL && random.nextFloat() < .25f
        val lobbed = kind in setOf(PlantKind.LOB, PlantKind.LOB_SLOW, PlantKind.COB) &&
            (battlefield.roof || kotlin.math.abs(target.x - (plant.col + .5f)) > .6f)
        val homing = kind == PlantKind.CATTAIL
        val launchX = plant.col + when {
            homing -> .5f + offsetCells
            vectorShot -> .5f
            else -> .55f * direction + offsetCells * direction
        }
        val landingX = target.x
        val splash = when (plant.type) {
            DefensePlant.MELON, DefensePlant.WINTERMELON -> .5f
            DefensePlant.COB -> 1f
            else -> 0f
        }
        return DefenseShot(
            row = row, x = launchX, damage = if (butter) damage * 2 else damage,
            slows = kind == PlantKind.SLOW_SHOT || plant.type == DefensePlant.WINTERMELON,
            splash = splash, pierces = kind == PlantKind.PIERCE,
            speed = if (homing) .008f else if (lobbed) .0025f else .004f,
            direction = direction, targetKind = kind,
            splashRows = if (plant.type == DefensePlant.MELON || plant.type == DefensePlant.WINTERMELON ||
                plant.type == DefensePlant.COB) 1 else 0,
            launchX = launchX, landingX = landingX,
            flightDurationMs = if (lobbed) (kotlin.math.abs(landingX - launchX) / .0025f).toLong().coerceAtLeast(350L) else 0L,
            sourceType = plant.type,
            homing = homing, targetId = if (homing) target.id else -1L,
            lane = if (homing || vectorShot) plant.row.toFloat() else row.toFloat(),
            targetX = landingX, targetLane = target.row.toFloat(),
            vectorShot = vectorShot, laneDirection = laneDirection,
            stuns = butter, butter = butter,
            roofBlockAtX = if (battlefield.roof && !lobbed && !homing &&
                kind !in setOf(PlantKind.PIERCE, PlantKind.GLOOM) && plant.col < 4 && direction > 0f
            ) plant.col + 1f else null,
            arcHeightCells = when (plant.type) {
                DefensePlant.COB -> 1.25f
                DefensePlant.MELON, DefensePlant.WINTERMELON -> 1f
                else -> if (lobbed) .75f else 0f
            }
        )
    }

    private fun DefenseZombieUnit.isInTile(row: Int, col: Int) =
        this.row == row && x >= col && x < col + 1f

    fun isFogged(row: Int, col: Int): Boolean {
        if (!battlefield.fog || elapsedMs < 30_000L || fogClearedUntilMs > elapsedMs) return false
        return plants.none { it.type == DefensePlant.PLANTERN &&
            kotlin.math.abs(it.row - row) <= 2 && kotlin.math.abs(it.col - col) <= 2 }
    }

    private fun isProtected(targetRow: Int, targetCol: Int): Boolean = plants.any {
        it.type == DefensePlant.UMBRELLA && kotlin.math.abs(it.row - targetRow) <= 1 &&
            kotlin.math.abs(it.col - targetCol) <= 1
    }

    private fun resolveZombieShot(shot: DefenseZombieShot) {
        val target = plants.lastOrNull { it.row == shot.targetRow && it.col == shot.targetCol &&
            it.type != DefensePlant.FLOWERPOT }
        if (target == null) return
        if (isProtected(target.row, target.col)) {
            explosions += DefenseExplosion(target.row, target.col + .5f, 500L, 500L, .8f,
                DefenseEffectKind.SHIELD)
        } else {
            target.health -= shot.damage
            target.attackMs = 220L
            if (target.health <= 0) plants.remove(target)
        }
    }

    fun start() {
        if (plants.isEmpty() && zombies.isEmpty()) {
            sun = campaignLevel?.startingSun ?: 200
            lives = 5
            finished = false
            won = false
            elapsedMs = 0L
            plantHitSoundEvents = 0L
            zombieBiteSoundEvents = 0L
            regularSunsSinceLarge = 0
            nextLargeSunChallengeUsesRange = true
            spawnMs = 0L; threatWave = -1; threatUsed = 0; spawnGroups = 0
            bossSpawned = false
            mowerRows.clear(); mowerRows.addAll(initialMowerRows())
            mowerSpawnBlockedUntilMs.clear()
            if (battlefield.night && !battlefield.roof) gravestones.addAll(battlefield.graveCells)
        }
    }

    fun reset() {
        plants.clear(); zombies.clear(); shots.clear(); zombieShots.clear(); stars.clear(); starFlights.clear(); explosions.clear(); craters.clear(); gravestones.clear(); iceTrails.clear(); readyAt.clear()
        mowerRows.clear(); mowerRows.addAll(initialMowerRows()); mowerSpawnBlockedUntilMs.clear()
        selected = null; cobTargeting = null; shovelMode = false
        selected = null; shovelMode = false; question = null; sun = campaignLevel?.startingSun ?: 200; lives = 5
        answeredCorrectly = 0; answeredIncorrectly = 0; lastAutoPickupAward = 0
        collectedStars = 0; autoPickupCharges = 0
        elapsedMs = 0; finished = false; won = false
        plantHitSoundEvents = 0L; zombieBiteSoundEvents = 0L
        spawnMs = 0; threatWave = -1; threatUsed = 0; spawnGroups = 0
        sunMs = 0; nextId = 1; imitaterTarget = null
        regularSunsSinceLarge = 0; nextLargeSunChallengeUsesRange = true
        fogClearedUntilMs = 0; bossSpawned = false
        triggeredCampaignEvents.clear()
        if (battlefield.night && !battlefield.roof) gravestones.addAll(battlefield.graveCells)
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
        explosions.forEach { it.remainingMs -= dt }
        explosions.removeAll { it.remainingMs <= 0 }
        starFlights.forEach { it.elapsedMs += dt }
        starFlights.removeAll { it.elapsedMs >= it.delayMs + STAR_FLIGHT_MS }
        zombieShots.forEach { it.remainingMs -= dt }
        zombieShots.filter { it.remainingMs <= 0 }.toList().forEach(::resolveZombieShot)
        zombieShots.removeAll { it.remainingMs <= 0 }
        if (fogClearedUntilMs < elapsedMs) fogClearedUntilMs = 0L
        iceTrails.entries.removeAll { it.value <= elapsedMs }
        spawnMs += dt
        sunMs += dt
        val skyStarInterval = campaignLevel?.skyStarIntervalMs ?: 9_000L
        if (sunMs >= skyStarInterval) {
            sunMs %= skyStarInterval
            activeSpawnRows().randomOrNull(random)?.let { row ->
                createStar(FallingStar(nextId++, row, random.nextInt(9)))
            }
        }
        val hasFinalBoss = campaignLevel?.id == 50 || campaignLevel == null &&
            battlefield == DefenseBattlefield.ROOFTOP
        if (hasFinalBoss && elapsedMs >= 130_000 && !bossSpawned) {
            activeSpawnRows().randomOrNull(random)?.let { row ->
                bossSpawned = true
                zombies += DefenseZombieUnit(nextId++, ZombieKind.BOSS, row, 9.1f)
            }
        }
        val wave = (elapsedMs / 45_000).toInt()
        if (wave != threatWave) {
            threatWave = wave
            threatUsed = 0
            zombies.forEach { it.spawnBudgetCost = 0 }
        }
        val spawnEvery = (difficulty.baseSpawnMs - wave * 750L).coerceAtLeast(difficulty.minSpawnMs)
        if (elapsedMs >= difficulty.firstSpawnMs && spawnMs >= spawnEvery && elapsedMs < 150_000) {
            val waveBudget = (difficulty.waveThreatBudget + wave + (campaignLevel?.budgetOffset ?: 0)).coerceAtLeast(1)
            val groupSize = when {
                spawnGroups == 0 -> 1
                elapsedMs < 90_000 -> 2
                elapsedMs < 120_000 -> 3
                else -> 4
            }
            val availableThreat = waveBudget - threatUsed
            val dueEvent = campaignLevel?.spawnEvents?.withIndex()?.firstOrNull { (index, event) ->
                index !in triggeredCampaignEvents && elapsedMs >= event.notBeforeSec * 1_000L
            }
            val guaranteed = dueEvent?.value?.takeIf {
                it.zombie.threatPoints() * it.count <= availableThreat
            }
            val spawnCount = guaranteed?.count ?: groupSize
            val spawnedThreat = spawnGroup(availableThreat, spawnCount,
                guaranteed?.let { event -> List(event.count) { event.zombie } }.orEmpty())
            spawnMs = if (spawnedThreat > 0) 0L else (spawnEvery - 1_000L).coerceAtLeast(0L)
            if (guaranteed != null && spawnedThreat >= guaranteed.zombie.threatPoints() * guaranteed.count) {
                triggeredCampaignEvents += dueEvent.index
            }
            threatUsed += spawnedThreat
            if (spawnedThreat > 0) spawnGroups++
        }
        plants.toList().forEach { plant ->
            plant.elapsedMs += dt
            plant.attackMs = (plant.attackMs - dt).coerceAtLeast(0L)
            if (plant.transformingMs > 0) {
                plant.transformingMs = (plant.transformingMs - dt).coerceAtLeast(0L)
                if (plant.transformingMs == 0L) {
                    plant.type = plant.disguiseType ?: plant.type
                    plant.health = plant.type.health
                    plant.elapsedMs = 0L
                    plant.ageMs = 0L
                    plant.awake = battlefield.night || plant.type !in MUSHROOMS
                }
                return@forEach
            }
            if (!plant.awake) return@forEach
            plant.ageMs += dt
            when (plant.type.kind) {
                PlantKind.GENERATOR, PlantKind.TWIN_GENERATOR -> if (plant.elapsedMs >= plant.type.intervalMs) {
                    plant.elapsedMs %= plant.type.intervalMs
                    plant.attackMs = 350L
                    repeat(if (plant.type.kind == PlantKind.TWIN_GENERATOR) 2 else 1) {
                        val value = when {
                            plant.type == DefensePlant.MARIGOLD -> MARIGOLD_COIN_REWARD
                            plant.type == DefensePlant.SUNSHROOM && plant.ageMs < 120_000L -> STAR_REWARD / 2
                            else -> STAR_REWARD
                        }
                        createStar(FallingStar(nextId++, plant.row, plant.col, value = value,
                            coin = plant.type == DefensePlant.MARIGOLD))
                    }
                }
                PlantKind.SHOT, PlantKind.SLOW_SHOT, PlantKind.DOUBLE_SHOT, PlantKind.PIERCE,
                PlantKind.TRIPLE_SHOT, PlantKind.SPLIT_SHOT, PlantKind.STAR_SHOT, PlantKind.CACTUS,
                PlantKind.CATTAIL, PlantKind.GATLING, PlantKind.GLOOM, PlantKind.LOB, PlantKind.LOB_SLOW -> {
                    val targetZombie = if (plant.type.kind == PlantKind.CATTAIL) {
                        val eligible = zombies.filter { it.totalHealth > 0 &&
                            it.behaviorCanBeHitBy(PlantKind.CATTAIL) }
                        eligible.filter { it.type.behavior == ZombieBehavior.AIRBORNE }.minByOrNull { it.x }
                            ?: eligible.minByOrNull { it.x }
                    } else if (plant.type.kind == PlantKind.SPLIT_SHOT) {
                        zombies.filter { it.row == plant.row && it.totalHealth > 0 &&
                            it.behaviorCanBeHitBy(PlantKind.SPLIT_SHOT) }
                            .minByOrNull { if (it.x >= plant.col + .5f) it.x - (plant.col + .5f) else plant.col + .5f - it.x }
                    } else zombies.firstOrNull { zombie ->
                        if (zombie.totalHealth <= 0) return@firstOrNull false
                        val laneTarget = when (plant.type.kind) {
                            PlantKind.TRIPLE_SHOT -> zombie.row in plant.row - 1..plant.row + 1
                            PlantKind.GLOOM -> zombie.row in plant.row - 1..plant.row + 1 &&
                                zombie.x in (plant.col - .5f)..(plant.col + 1.5f)
                            PlantKind.STAR_SHOT, PlantKind.CATTAIL -> true
                            else -> zombie.row == plant.row
                        }
                        val heldByWall = zombie.x <= plant.col &&
                            plants.any { barrier -> barrier.row == zombie.row && barrier.type.kind == PlantKind.WALL &&
                                zombie.x in (barrier.col - .2f)..(barrier.col + 1.05f) }
                        val directionTarget = plant.type.kind in setOf(PlantKind.SPLIT_SHOT, PlantKind.STAR_SHOT, PlantKind.GLOOM) ||
                            zombie.x > plant.col || heldByWall
                        val maxRange = when (plant.type) {
                            DefensePlant.PUFF, DefensePlant.SEASHROOM -> 3.5f
                            DefensePlant.FUME -> 4.5f
                            else -> Float.MAX_VALUE
                        }
                        val frightened = plant.type == DefensePlant.SCAREDY && zombies.any {
                            it.totalHealth > 0 && it.row in plant.row - 1..plant.row + 1 &&
                                it.x in (plant.col - .5f)..(plant.col + 1.5f)
                        }
                        plant.frightened = frightened
                        laneTarget && directionTarget && zombie.x <= plant.col + maxRange && !frightened &&
                            zombie.behaviorCanBeHitBy(plant.type.kind)
                    }
                    val interval = plant.type.intervalMs
                    if (targetZombie != null && interval > 0 && plant.elapsedMs >= interval) {
                        plant.elapsedMs %= interval
                        plant.attackMs = 260L
                        if (plant.type.kind !in setOf(PlantKind.GLOOM, PlantKind.CATTAIL, PlantKind.STAR_SHOT, PlantKind.SPLIT_SHOT) && targetZombie.x < plant.col) {
                            shots += createShot(plant, targetZombie, direction = -1f)
                        } else when (plant.type.kind) {
                            PlantKind.TRIPLE_SHOT -> (plant.row - 1..plant.row + 1).filter { it in 0 until battlefield.rowCount }
                                .forEach { lane -> zombies.filter { it.row == lane && it.x > plant.col && it.totalHealth > 0 }
                                    .minByOrNull { it.x }?.let { shots += createShot(plant, it, row = lane) } }
                            PlantKind.STAR_SHOT -> listOf(1f to 0f, 1f to -1f, 1f to 1f, 0f to -1f, 0f to 1f)
                                .forEach { (xDir, laneDir) -> shots += createShot(plant, targetZombie,
                                    vectorShot = true, direction = xDir, laneDirection = laneDir) }
                            PlantKind.CATTAIL -> repeat(2) { shots += createShot(plant, targetZombie,
                                offsetCells = (it.toFloat() - .5f) * .12f) }
                            PlantKind.SPLIT_SHOT -> {
                                zombies.filter { it.row == plant.row && it.x > plant.col && it.totalHealth > 0 }
                                    .minByOrNull { it.x }?.let { shots += createShot(plant, it) }
                                zombies.filter { it.row == plant.row && it.x < plant.col && it.totalHealth > 0 }
                                    .maxByOrNull { it.x }?.let { back -> repeat(2) {
                                        shots += createShot(plant, back, direction = -1f)
                                    } }
                            }
                            PlantKind.GLOOM -> zombies.filter { it.row in plant.row - 1..plant.row + 1 &&
                                it.x in (plant.col - .5f)..(plant.col + 1.5f) }
                                .forEach { damage(it, plant.type.damage) }
                            else -> {
                                val shotCount = when (plant.type.kind) {
                                    PlantKind.DOUBLE_SHOT -> 2
                                    PlantKind.GATLING -> 4
                                    else -> 1
                                }
                                repeat(shotCount) { shotIndex ->
                                    shots += createShot(plant, targetZombie,
                                        damage = plant.type.damage / shotCount,
                                        offsetCells = shotIndex * .1f)
                                }
                            }
                        }
                    }
                }
                PlantKind.COB -> Unit
                PlantKind.BLAST -> {
                    if (plant.type == DefensePlant.CHERRY && plant.elapsedMs >= CHERRY_FUSE_MS) {
                        val firstBlastCol = maxOf(0, plant.col - 1).toFloat()
                        val afterLastBlastCol = (plant.col + 2).toFloat()
                        explosions += DefenseExplosion(plant.row, plant.col + .5f,
                            remainingMs = 700L, durationMs = 700L, radiusCells = 1.5f)
                        zombies.filter { it.row in (plant.row - 1)..(plant.row + 1) &&
                            it.x >= firstBlastCol && it.x < afterLastBlastCol }
                            .forEach { damage(it, plant.type.damage) }
                        plants.remove(plant)
                    } else if (plant.type == DefensePlant.DOOM && plant.elapsedMs >= CHERRY_FUSE_MS) {
                        val centerX = plant.col + .5f
                        val targets = zombies.filter {
                            val dx = it.x - centerX
                            val dy = (it.row - plant.row).toFloat()
                            dx * dx + dy * dy <= 3.5f * 3.5f
                        }
                        explosions += DefenseExplosion(plant.row, centerX, radiusCells = 3.5f)
                        targets.forEach { damage(it, plant.type.damage) }
                        for (r in 0 until battlefield.rowCount) for (c in 0..8) {
                            val dx = c + .5f - centerX
                            val dy = (r - plant.row).toFloat()
                            if (dx * dx + dy * dy <= 3.5f * 3.5f) craters += r to c
                        }
                        plants.remove(plant)
                    }
                }
                PlantKind.MINE -> {
                    if (plant.elapsedMs >= MINE_ARMING_MS) plant.armed = true
                    if (plant.armed && zombies.any { it.isInTile(plant.row, plant.col) }) {
                        explosions += DefenseExplosion(plant.row, plant.col + .5f, radiusCells = .5f)
                        zombies.filter { it.isInTile(plant.row, plant.col) }
                            .forEach { damage(it, plant.type.damage) }
                        plants.remove(plant)
                    }
                }
                PlantKind.KELP -> zombies.firstOrNull { it.isInTile(plant.row, plant.col) }
                    ?.let {
                        plantHitSoundEvents++
                        it.swallowedMs = 450L; it.swallowedByCol = plant.col
                        plant.attackMs = 450L
                        explosions += DefenseExplosion(plant.row, plant.col + .5f, 450L, 450L, .65f,
                            DefenseEffectKind.KELP)
                        plants.remove(plant)
                    }
                PlantKind.FREEZE -> if (plant.elapsedMs >= 300) {
                    zombies.forEach { it.stunnedMs = 4_000; it.slowedMs = 5_000 }
                    explosions += DefenseExplosion(2, 4.5f, 700L, 700L, 5f, DefenseEffectKind.FREEZE, 2, 4.5f)
                    plants.remove(plant)
                }
                PlantKind.ROW_BLAST -> if (zombies.any { it.row == plant.row }) {
                    zombies.filter { it.row == plant.row }.forEach { damage(it, plant.type.damage) }
                    explosions += DefenseExplosion(plant.row, 0f, 700L, 700L, .55f, DefenseEffectKind.FLAME_ROW, plant.row, 9f)
                    plants.remove(plant)
                }
                PlantKind.SPIKE -> if (plant.elapsedMs >= 1_000) {
                    plant.elapsedMs %= 1_000
                    zombies.filter { it.isInTile(plant.row, plant.col) }
                        .forEach { damage(it, plant.type.damage); plant.attackMs = 180L }
                }
                PlantKind.EAT -> {
                    if (plant.type == DefensePlant.SQUASH) {
                        if (!plant.activated) zombies.firstOrNull { it.row == plant.row &&
                            it.totalHealth > 0 && !it.friendly && it.x in plant.col.toFloat()..(plant.col + 2f) }
                            ?.let { target ->
                                plant.activated = true
                                plant.targetZombieId = target.id
                                plant.attackMs = 420L
                                plant.elapsedMs = 0L
                                explosions += DefenseExplosion(target.row, target.x, 420L, 420L, .75f,
                                    DefenseEffectKind.SQUASH)
                            }
                        else if (plant.activated && plant.attackMs == 0L) {
                            zombies.firstOrNull { it.id == plant.targetZombieId && it.totalHealth > 0 }
                                ?.let { target -> zombies.filter { victim -> victim.isInTile(target.row, target.x.toInt()) }
                                    .forEach { damage(it, plant.type.damage) } }
                            plants.remove(plant)
                        }
                    } else if (plant.elapsedMs >= plant.type.intervalMs) {
                        zombies.firstOrNull { it.row == plant.row && !it.friendly && it.totalHealth > 0 && it.swallowedMs <= 0 &&
                            it.type.behavior != ZombieBehavior.AIRBORNE && !it.underground &&
                            it.x >= plant.col + 1f && it.x < plant.col + 2f }
                            ?.let {
                                plantHitSoundEvents++
                                it.swallowedMs = CHOMPER_BITE_MS
                                it.swallowedByCol = plant.col
                                plant.elapsedMs = 0
                                plant.attackMs = CHOMPER_BITE_MS
                            }
                    }
                }
                PlantKind.MAGNET -> if (plant.type != DefensePlant.GOLDMAGNET && plant.elapsedMs >= 3_000) {
                    plant.elapsedMs = 0
                    val metalTarget = zombies.asSequence()
                        .filter { it.row == plant.row && it.x in (plant.col - 1f)..(plant.col + 3f) && it.hasMetalGear() && !it.friendly }
                        .minByOrNull { kotlin.math.abs(it.x - (plant.col + .5f)) }
                    if (metalTarget != null) {
                        metalTarget.let { target ->
                            plant.attackMs = 650L
                            explosions += DefenseExplosion(plant.row, plant.col + .5f, 650L, 650L, .35f,
                                DefenseEffectKind.MAGNET, target.row, target.x)
                            target.equipmentRemoved = true
                            target.gearLostMs = 650L
                            target.armor = 0
                            if (target.type.behavior == ZombieBehavior.DIGGER && target.underground) {
                                target.underground = false
                                target.returning = true
                                target.x = .2f
                            }
                            if (target.type.behavior == ZombieBehavior.POGO) target.jumped = true
                        }
                    } else plants.asSequence().filter { it.laddered && it.row == plant.row &&
                        kotlin.math.abs(it.col - plant.col) <= 3 }.minByOrNull { kotlin.math.abs(it.col - plant.col) }
                        ?.let { target ->
                            target.laddered = false
                            plant.attackMs = 650L
                            explosions += DefenseExplosion(plant.row, plant.col + .5f, 650L, 650L, .35f,
                                DefenseEffectKind.MAGNET, target.row, target.col + .5f)
                        }
                }
                PlantKind.SUPPORT -> when (plant.type) {
                    DefensePlant.HYPNO -> zombies.firstOrNull { it.row == plant.row && it.x in (plant.col - .5f)..(plant.col + .8f) }
                        ?.let {
                            it.friendly = true
                            plant.attackMs = 500L
                            explosions += DefenseExplosion(plant.row, plant.col + .5f, 500L, 500L, .8f,
                                DefenseEffectKind.CHARM)
                            plants.remove(plant)
                        }
                    DefensePlant.BLOVER -> {
                        val cleared = zombies.filter { it.type.behavior == ZombieBehavior.AIRBORNE }
                        cleared.forEach(::releaseSpawnBudget)
                        if (cleared.isNotEmpty()) spawnMs =
                            (spawnEvery - (campaignLevel?.postKillSpawnDelayMs ?: 2_000L)).coerceAtLeast(0L)
                        zombies.removeAll { it.type.behavior == ZombieBehavior.AIRBORNE }
                        fogClearedUntilMs = elapsedMs + 12_000L
                        plant.attackMs = 650L
                        explosions += DefenseExplosion(2, 4.5f, 650L, 650L, 5f, DefenseEffectKind.WIND, 0, 4.5f)
                        plants.remove(plant)
                    }
                    DefensePlant.GRAVE -> if (plant.elapsedMs >= 1_250L) {
                        gravestones.remove(plant.row to plant.col)
                        plant.attackMs = 250L
                        plants.remove(plant)
                    }
                    else -> Unit
                }
                else -> Unit
            }
        }
        shots.forEach { shot ->
            val previousX = shot.x
            val previousLane = shot.lane
            var roofImpact = false
            var homingTarget: DefenseZombieUnit? = null
            if (shot.homing) {
                homingTarget = zombies.firstOrNull { it.id == shot.targetId && it.totalHealth > 0 }
                    ?: zombies.filter { it.totalHealth > 0 && it.behaviorCanBeHitBy(PlantKind.CATTAIL) }
                        .filter { it.type.behavior == ZombieBehavior.AIRBORNE }.minByOrNull { it.x }
                    ?: zombies.filter { it.totalHealth > 0 && it.behaviorCanBeHitBy(PlantKind.CATTAIL) }.minByOrNull { it.x }
                if (homingTarget == null) shot.spent = true else {
                    shot.targetId = homingTarget.id
                    shot.targetX = homingTarget.x
                    shot.targetLane = homingTarget.row.toFloat()
                    val dx = shot.targetX - shot.x
                    val dy = shot.targetLane - shot.lane
                    val distance = kotlin.math.sqrt(dx * dx + dy * dy)
                    val travel = dt * shot.speed
                    if (distance <= travel) {
                        shot.x = shot.targetX
                        shot.lane = shot.targetLane
                    } else if (distance > 0f) {
                        shot.x += dx / distance * travel
                        shot.lane += dy / distance * travel
                    }
                }
            } else if (shot.flightDurationMs > 0) {
                shot.flightElapsedMs = (shot.flightElapsedMs + dt).coerceAtMost(shot.flightDurationMs)
                val progress = shot.flightElapsedMs.toFloat() / shot.flightDurationMs
                shot.x = shot.launchX + (shot.landingX - shot.launchX) * progress
                shot.lane = shot.row.toFloat() + (shot.impactRow - shot.row) * progress
                shot.arcOffsetCells = 4f * shot.arcHeightCells * progress * (1f - progress)
            } else if (shot.vectorShot) {
                val length = kotlin.math.sqrt(1f + shot.laneDirection * shot.laneDirection)
                shot.x += dt * shot.speed * shot.direction / length
                shot.lane += dt * shot.speed * shot.laneDirection / length
            } else shot.x += dt * shot.speed * shot.direction
            shot.roofBlockAtX?.let { blockAt ->
                if (previousX < blockAt && shot.x >= blockAt) {
                    shot.x = blockAt
                    roofImpact = true
                }
            }
            if (!shot.burning && shot.sourceType in setOf(DefensePlant.PEA, DefensePlant.SNOW,
                    DefensePlant.DOUBLE, DefensePlant.THREEPEATER, DefensePlant.SPLITPEA, DefensePlant.GATLING)) {
                val torch = plants.firstOrNull { it.type.kind == PlantKind.TORCH && it.row == shot.row }
                if (torch != null) {
                    val torchX = torch.col + .5f
                    if (torchX in minOf(previousX, shot.x)..maxOf(previousX, shot.x)) {
                        shot.burning = true
                        shot.damage *= 2
                        shot.slows = false
                        shot.splash = .7f
                    }
                }
            }
            if (shot.impactAtEnd && shot.flightDurationMs > 0 && shot.flightElapsedMs >= shot.flightDurationMs) {
                explosions += DefenseExplosion(shot.impactRow, shot.impactX, radiusCells = 1.5f)
                zombies.filter { it.row in shot.impactRow - 1..shot.impactRow + 1 &&
                    kotlin.math.abs(it.x - shot.impactX) <= 1f }
                    .forEach { damage(it, DefensePlant.COB.damage) }
                shot.spent = true
            }
            val hit = if (shot.spent || shot.impactAtEnd) null else if (shot.homing) homingTarget?.takeIf {
                kotlin.math.sqrt((it.x - shot.x) * (it.x - shot.x) +
                    (it.row - shot.lane) * (it.row - shot.lane)) <= .35f
            } else if (shot.vectorShot) zombies.firstOrNull { zombie ->
                if (zombie.totalHealth <= 0 || zombie.id in shot.hitIds ||
                    !zombie.behaviorCanBeHitBy(shot.targetKind)) return@firstOrNull false
                val dx = shot.x - previousX
                val dy = shot.lane - previousLane
                val lengthSquared = dx * dx + dy * dy
                val t = if (lengthSquared == 0f) 0f else (((zombie.x - previousX) * dx +
                    (zombie.row - previousLane) * dy) / lengthSquared).coerceIn(0f, 1f)
                val hitX = previousX + t * dx
                val hitLane = previousLane + t * dy
                val distanceX = zombie.x - hitX
                val distanceLane = zombie.row - hitLane
                distanceX * distanceX + distanceLane * distanceLane <= .35f * .35f
            } else zombies.firstOrNull { it.row == shot.row &&
                it.totalHealth > 0 && it.id !in shot.hitIds &&
                it.x in (minOf(previousX, shot.x) - .35f)..(maxOf(previousX, shot.x) + .35f) &&
                it.behaviorCanBeHitBy(shot.targetKind) }
            if (hit != null) {
                shot.hitIds += hit.id
                damage(hit, shot.damage, shot.targetKind)
                if (shot.slows) hit.slowedMs = 3_000
                if (shot.stuns) hit.stunnedMs = 5_000
                if (shot.splash > 0) zombies.filter { kotlin.math.abs(it.row - hit.row) <= shot.splashRows &&
                    kotlin.math.abs(it.x - hit.x) <= shot.splash }
                    .filter { it.id != hit.id }.forEach {
                        damage(it, shot.damage / 2, shot.targetKind)
                        if (shot.slows) it.slowedMs = 3_000
                    }
                if (!shot.pierces) shot.spent = true
            }
            if (roofImpact) {
                explosions += DefenseExplosion(shot.row, shot.x, 220L, 220L, .22f,
                    DefenseEffectKind.ROOF_IMPACT)
                shot.spent = true
            }
            if (shot.flightDurationMs > 0 && shot.flightElapsedMs >= shot.flightDurationMs && !shot.pierces) shot.spent = true
        }
        shots.removeAll { it.spent || it.x > 10f || it.x < -.5f ||
            (it.vectorShot && it.lane !in -.5f..(battlefield.rowCount - .5f)) }
        for (zombie in zombies.toList()) stepZombie(zombie, dt)
        val killed = zombies.filter { it.totalHealth <= 0 }
        if (killed.isNotEmpty()) {
            killed.forEach(::releaseSpawnBudget)
            zombies.removeAll { it.totalHealth <= 0 }
            spawnMs = (spawnEvery - (campaignLevel?.postKillSpawnDelayMs ?: 2_000L)).coerceAtLeast(0L)
        }
        if (elapsedMs >= 150_000 && zombies.isEmpty()) { finished = true; won = lives > 0 }
    }

    private fun stepZombie(zombie: DefenseZombieUnit, dt: Long) {
        if (zombie.totalHealth <= 0) return
        if (zombie.swallowedMs > 0) {
            zombie.swallowedMs = (zombie.swallowedMs - dt).coerceAtLeast(0L)
            if (zombie.swallowedMs == 0L) { zombie.health = 0; zombie.armor = 0 }
            return
        }
        zombie.actionMs = (zombie.actionMs - dt).coerceAtLeast(0L)
        zombie.jumpingMs = (zombie.jumpingMs - dt).coerceAtLeast(0L)
        zombie.gearLostMs = (zombie.gearLostMs - dt).coerceAtLeast(0L)
        zombie.stunnedMs = (zombie.stunnedMs - dt).coerceAtLeast(0L)
        zombie.slowedMs = (zombie.slowedMs - dt).coerceAtLeast(0L)
        if (zombie.stunnedMs > 0) return
        zombie.specialMs += dt
        if (zombie.friendly) {
            val target = zombies.firstOrNull { !it.friendly && it.row == zombie.row && it.x < zombie.x }
            if (target != null) damage(target, dt.toInt()) else zombie.x += zombie.type.speed * dt / 1_000f
            return
        }

        when (zombie.type.behavior) {
            ZombieBehavior.SUMMON -> if (zombie.specialMs >= 8_000L) {
                zombie.specialMs %= 8_000L
                zombie.actionMs = 900L
                zombies += DefenseZombieUnit(nextId++, ZombieKind.BACKUP, zombie.row, zombie.x + .25f)
            }
            ZombieBehavior.GIANT -> if (!zombie.summoned && zombie.health <= zombie.type.health / 2) {
                zombie.summoned = true
                zombie.actionMs = 900L
                zombies += DefenseZombieUnit(nextId++, ZombieKind.IMP, zombie.row, zombie.x + .55f,
                    jumpingMs = 700L, jumped = true)
            }
            ZombieBehavior.BOSS -> {
                if (!zombie.summoned && zombie.health <= zombie.type.health / 2) {
                    zombie.summoned = true
                    zombie.actionMs = 1_000L
                    repeat(2) { offset -> zombies += DefenseZombieUnit(nextId++, ZombieKind.IMP,
                        (zombie.row + offset).coerceIn(0, battlefield.rowCount - 1), zombie.x + .7f + offset * .2f,
                        jumpingMs = 800L, jumped = true) }
                }
                if (zombie.specialMs >= 2_600L) {
                    zombie.specialMs = 0L
                    plants.filter { it.type != DefensePlant.FLOWERPOT }.randomOrNull(random)?.let { target ->
                        zombie.targetRow = target.row
                        zombie.targetCol = target.col
                        zombie.actionMs = 550L
                        zombieShots += DefenseZombieShot(zombie.row, zombie.x.toInt().coerceIn(0, 8),
                            target.row, target.col, damage = 240)
                    }
                }
            }
            ZombieBehavior.CATAPULT -> if (zombie.specialMs >= 2_300L) {
                plants.filter { it.row == zombie.row && it.col < zombie.x && it.type != DefensePlant.FLOWERPOT }
                    .maxByOrNull { it.col }?.let { target ->
                        zombie.specialMs = 0L
                        zombie.actionMs = 500L
                        zombie.targetRow = target.row
                        zombie.targetCol = target.col
                        zombieShots += DefenseZombieShot(zombie.row, zombie.x.toInt().coerceIn(0, 8),
                            target.row, target.col)
                    }
            }
            ZombieBehavior.YETI -> if (zombie.specialMs >= 10_000L) zombie.retreating = true
            ZombieBehavior.DIGGER -> if (!zombie.equipmentRemoved && !zombie.underground &&
                !zombie.returning && zombie.x < 5f) {
                zombie.underground = true
                zombie.x = -.1f
                zombie.actionMs = 450L
            }
            else -> Unit
        }

        if (zombie.type == ZombieKind.DANCER && zombie.specialMs % 8_000L >= 7_000L)
            zombie.actionMs = maxOf(zombie.actionMs, 180L)
        if (zombie.type.behavior == ZombieBehavior.EXPLODE && !zombie.equipmentRemoved) {
            val blastTarget = plants.asSequence().filter { it.row == zombie.row &&
                it.type != DefensePlant.FLOWERPOT && zombie.x in (it.col - .2f)..(it.col + 1.05f) }
                .maxByOrNull { it.col }
            if (blastTarget != null) {
                plants.filter { it.row in blastTarget.row - 1..blastTarget.row + 1 &&
                    kotlin.math.abs(it.col - blastTarget.col) <= 1 && it.type != DefensePlant.FLOWERPOT }
                    .toList().forEach { it.health = 0; plants.remove(it) }
                explosions += DefenseExplosion(blastTarget.row, blastTarget.col + .5f,
                    500L, 500L, 1.5f)
                zombie.health = 0
                return
            }
        }
        if (zombie.type.behavior == ZombieBehavior.RAGE && zombie.armor <= 0) zombie.angry = true

        if (zombie.type.behavior == ZombieBehavior.BUNGEE) {
            if (zombie.retreating) {
                zombie.x += zombie.type.speed * dt / 1_000f
                if (zombie.x > 9.8f) zombie.health = 0
                return
            }
            if (zombie.attackTriggered) {
                if (zombie.actionMs == 0L) zombie.retreating = true
                return
            }
            if (zombie.targetCol < 0 && zombie.x < 7.5f) {
                val candidate = plants.filter { it.type != DefensePlant.FLOWERPOT }
                    .minWithOrNull(compareBy<DefensePlantUnit> { kotlin.math.abs(it.col + .5f - zombie.x) }
                        .thenBy { kotlin.math.abs(it.row - zombie.row) })
                if (candidate == null) { zombie.retreating = true; return }
                zombie.targetRow = candidate.row
                zombie.targetCol = candidate.col
            }
            val target = plants.firstOrNull { it.row == zombie.targetRow && it.col == zombie.targetCol &&
                it.type != DefensePlant.FLOWERPOT }
            if (target != null) {
                if (isProtected(target.row, target.col)) {
                    explosions += DefenseExplosion(target.row, target.col + .5f, 500L, 500L, .9f,
                        DefenseEffectKind.SHIELD)
                    zombie.retreating = true
                    return
                }
                zombie.x -= zombie.type.speed * dt / 1_000f
                if (zombie.x <= target.col + .65f) {
                    zombie.attackTriggered = true
                    zombie.actionMs = 1_000L
                    zombie.capturedPlantType = target.type
                    plants.remove(target)
                }
                return
            }
            zombie.targetCol = -1
        }

        if (zombie.type.behavior == ZombieBehavior.BUNGEE && zombie.attackTriggered && zombie.actionMs > 0) return
        if (zombie.underground) {
            zombie.x += zombie.type.speed * dt / 1_000f
            if (zombie.x >= .2f) { zombie.underground = false; zombie.returning = true }
            return
        }
        if (zombie.retreating) {
            zombie.x += zombie.type.speed * dt / 1_000f
            if (zombie.x > 9.8f) zombie.health = 0
            return
        }

        val flying = zombie.type.behavior == ZombieBehavior.AIRBORNE ||
            zombie.type.behavior == ZombieBehavior.BUNGEE
        val frontPlant = if (flying || zombie.underground) null else plants.asSequence()
            .filter { it.row == zombie.row && it.type != DefensePlant.FLOWERPOT &&
                zombie.x in (it.col - .2f)..(it.col + 1.05f) &&
                (it.type.kind != PlantKind.MINE || it.armed) }
            .maxWithOrNull(compareBy<DefensePlantUnit> { it.col }
                .thenBy { if (it.type == DefensePlant.PUMPKIN) 1 else 0 })

        if (zombie.type.behavior == ZombieBehavior.DIVE && frontPlant != null) zombie.diveUnderwater = false
        if (zombie.type.behavior == ZombieBehavior.LADDER && frontPlant != null &&
            !zombie.equipmentRemoved && !frontPlant.laddered) {
            frontPlant.laddered = true
            zombie.equipmentRemoved = true
            zombie.armor = 0
            zombie.actionMs = 700L
            zombie.x = frontPlant.col - .6f
            return
        }
        if (frontPlant?.laddered == true) {
            zombie.x = frontPlant.col - .6f
            return
        }
        if (zombie.type.behavior == ZombieBehavior.DIVE && frontPlant == null &&
            zombie.x > 0f && zombie.specialMs % 1_000L < dt) zombie.diveUnderwater = true

        val canJump = zombie.type.behavior == ZombieBehavior.VAULT && !zombie.jumped ||
            zombie.type == ZombieKind.DOLPHIN && !zombie.jumped ||
            zombie.type.behavior == ZombieBehavior.POGO && !zombie.equipmentRemoved
        val blockedJump = frontPlant?.type == DefensePlant.TALLNUT
        val waitingToJump = canJump && frontPlant != null && !blockedJump &&
            (zombie.type.behavior == ZombieBehavior.POGO && zombie.jumpingMs == 0L ||
                zombie.type.behavior != ZombieBehavior.POGO && !zombie.jumped)
        if (frontPlant != null && !waitingToJump) {
            // Split coarse damage into five smaller chew ticks while keeping roughly the same DPS.
            val biteIntervalMs = (zombie.type.biteMs / 5L).coerceAtLeast(1L)
            zombie.biteElapsedMs += dt
            if (zombie.biteElapsedMs >= biteIntervalMs) {
                zombie.biteElapsedMs %= biteIntervalMs
                zombie.actionMs = 260L
                if (zombie.type.behavior == ZombieBehavior.CRUSH && frontPlant.type == DefensePlant.SPIKEWEED) {
                    frontPlant.health = 0
                    zombie.health = 0
                } else if (zombie.type.behavior == ZombieBehavior.CRUSH && frontPlant.type == DefensePlant.SPIKEROCK) {
                    zombie.health = 0
                } else {
                    if (zombie.type.behavior != ZombieBehavior.CRUSH) zombieBiteSoundEvents++
                    frontPlant.health -= when {
                        zombie.type.behavior == ZombieBehavior.CRUSH || zombie.type.behavior == ZombieBehavior.GIANT ||
                            zombie.type.behavior == ZombieBehavior.BOSS -> frontPlant.health
                        else -> 20
                    }
                }
                if (frontPlant.health <= 0) plants.remove(frontPlant)
                if (frontPlant.type.kind == PlantKind.GARLIC) {
                    val grassRows = (0 until battlefield.rowCount).filter { it !in battlefield.waterRows }
                    val rows = if (zombie.row in battlefield.waterRows) grassRows.filter {
                        kotlin.math.abs(it - zombie.row) == 1
                    } else listOf(zombie.row - 1, zombie.row + 1).filter { it in grassRows }
                    if (rows.isNotEmpty()) {
                        val oldRow = zombie.row
                        zombie.row = rows.minByOrNull { kotlin.math.abs(it - oldRow) }!!
                        explosions += DefenseExplosion(oldRow, frontPlant.col + .5f, 350L, 350L,
                            .45f, DefenseEffectKind.TRANSFORM, zombie.row, frontPlant.col + .5f)
                    }
                }
            }
        } else if (waitingToJump) {
            zombie.x = frontPlant.col - .6f
            zombie.jumpingMs = if (zombie.type.behavior == ZombieBehavior.POGO) 700L else 560L
            zombie.actionMs = zombie.jumpingMs
            if (zombie.type.behavior != ZombieBehavior.POGO) zombie.jumped = true
            if (zombie.type == ZombieKind.DOLPHIN) zombie.equipmentRemoved = true
        } else {
            val slow = if (zombie.slowedMs > 0) .5f else 1f
            val rage = if (zombie.angry) 2f else 1f
            val baseSpeed = when {
                zombie.type.behavior == ZombieBehavior.VAULT && zombie.jumped -> ZombieKind.DRIFTER.speed
                zombie.type == ZombieKind.DOLPHIN && zombie.jumped -> ZombieKind.DRIFTER.speed
                zombie.type.behavior == ZombieBehavior.POGO && zombie.equipmentRemoved -> ZombieKind.DRIFTER.speed
                else -> zombie.type.speed
            }
            val movement = baseSpeed * dt / 1_000f * slow * rage
            when {
                zombie.returning -> {
                    zombie.x += movement
                    if (zombie.x >= 9.1f) zombie.returning = false
                }
                else -> zombie.x -= movement
            }
            if (zombie.type.behavior == ZombieBehavior.CRUSH) {
                val tile = zombie.x.toInt().coerceIn(0, 8)
                iceTrails[zombie.row to tile] = elapsedMs + 30_000L
            }
        }
        if (zombie.type == ZombieKind.BOBSLED && (zombie.row to zombie.x.toInt().coerceIn(0, 8)) !in iceTrails)
            zombie.slowedMs = maxOf(zombie.slowedMs, 500L)
        if (zombie.x < -.5f) {
            val mowerTriggered = mowerRows.remove(zombie.row)
            if (mowerTriggered) {
                mowerSpawnBlockedUntilMs[zombie.row] = elapsedMs + 10_000L
                explosions += DefenseExplosion(zombie.row, .1f, 1_400L, 1_400L, 0f, DefenseEffectKind.MOWER)
                zombies.filter { it.row == zombie.row }.forEach { it.health = 0; it.armor = 0 }
            } else lives--
            zombies.remove(zombie)
            releaseSpawnBudget(zombie)
            if (lives <= 0) finished = true
        }
    }

    fun cost(type: DefensePlant): Int? = if (type == DefensePlant.IMITATER) imitaterTarget?.cost else type.cost
    fun rechargeMs(type: DefensePlant): Long =
        if (type == DefensePlant.IMITATER) imitaterTarget?.rechargeMs ?: 0L else type.rechargeMs

    fun select(type: DefensePlant): String {
        if (finished) return "本局已结束"
        if (type == DefensePlant.LILYPAD && !battlefield.hasPool) return "当前战场没有水域"
        if (type == DefensePlant.FLOWERPOT && !battlefield.roof) return "花盆只在屋顶战场使用"
        if (type == DefensePlant.GRAVE && !battlefield.night) return "墓碑吞噬者只在夜间墓园使用"
        if (type == DefensePlant.PLANTERN && !battlefield.fog) return "灯笼草只在有迷雾的战场使用"
        if (type == DefensePlant.UMBRELLA && !battlefield.roof) return "叶子保护伞只在有空投攻击的屋顶战场使用"
        if (selected == type) {
            selected = null
            imitaterTarget = null
            return "已取消选择：${type.title}"
        }
        if (question != null) return "请先完成当前题目"
        if (type == DefensePlant.IMITATER) {
            shovelMode = false
            cobTargeting = null
            selected = type
            imitaterTarget = null
            return "已选择模仿者：再点一张植物卡选择复制对象"
        }
        val cost = cost(type) ?: return "模仿者需要先选择复制对象"
        if (sun < cost) return "阳光不足：${type.title}需要 $cost 点"
        if ((readyAt[type] ?: 0L) > elapsedMs) return "${type.title}还在冷却"
        shovelMode = false
        cobTargeting = null
        selected = type
        return "已选：${type.title} · $cost 阳光；点击空格部署"
    }

    fun chooseImitaterTarget(type: DefensePlant): String {
        if (selected != DefensePlant.IMITATER) return "请先选择模仿者卡片"
        if (type == DefensePlant.IMITATER) return "模仿者不能复制自己"
        if (question != null) return "请先完成当前题目"
        if (sun < type.cost) return "阳光不足：复制${type.title}需要 ${type.cost} 点"
        if ((readyAt[DefensePlant.IMITATER] ?: 0L) > elapsedMs) return "模仿者还在冷却"
        imitaterTarget = type
        return "复制目标已选：${type.title}；点击空格部署"
    }

    fun toggleShovel() { shovelMode = !shovelMode; selected = null; imitaterTarget = null; cobTargeting = null }

    fun tapCell(row: Int, col: Int): String {
        if (row !in 0 until battlefield.rowCount || col !in 0..8) return "请点击轨道格子"
        if (campaignLevel != null && row !in campaignLevel.activeRows) return "本关尚未开放这条路线"
        if (shovelMode) {
            val target = plants.lastOrNull { it.row == row && it.col == col }
            shovelMode = false
            if (target == null) return "这里没有可铲除的植物"
            plants.remove(target)
            return "已铲除：${target.type.title}"
        }
        if (finished) return "本局已结束"
        if (question != null) return "请先完成当前题目"
        if ((row to col) in craters) return "奇点爆炸留下的坑洼无法种植"
        cobTargeting?.let { cob ->
            if (cob !in plants) { cobTargeting = null; return "巨炮已移除" }
            if (cob.elapsedMs < cob.type.intervalMs) {
                return "巨炮装填中：${((cob.type.intervalMs - cob.elapsedMs + 999) / 1_000)}秒后可发射"
            }
            val launchX = cob.col + .5f
            val impactX = col + .5f
            shots += DefenseShot(row = cob.row, x = launchX, damage = 0, slows = false,
                targetKind = PlantKind.COB, speed = .0025f, launchX = launchX, landingX = impactX,
                sourceType = DefensePlant.COB,
                flightDurationMs = (kotlin.math.abs(impactX - launchX) / .0025f).toLong().coerceAtLeast(350L),
                arcHeightCells = 1.25f, impactAtEnd = true, impactRow = row, impactX = impactX)
            cob.elapsedMs = 0L
            cobTargeting = null
            return "巨炮已发射，落点范围 3×3"
        }
        val packet = selected
        val tappedCob = plants.lastOrNull { it.row == row && it.col == col && it.type == DefensePlant.COB }
        if (tappedCob != null && packet != DefensePlant.PUMPKIN) {
            if (tappedCob.elapsedMs < tappedCob.type.intervalMs) {
                return "巨炮装填中：${((tappedCob.type.intervalMs - tappedCob.elapsedMs + 999) / 1_000)}秒后可发射"
            }
            selected = null
            cobTargeting = tappedCob
            return "巨炮已就绪：再点一次轨道格选择落点"
        }
        if (packet == null) return "先选择左侧植物，再点种植位置"
        val type = if (packet == DefensePlant.IMITATER)
            imitaterTarget ?: return "先点一张植物卡选择模仿者的复制对象"
        else packet
        val isWater = row in battlefield.waterRows
        val aquaticPlant = type.kind == PlantKind.AQUATIC || type in setOf(
            DefensePlant.TANGLE, DefensePlant.SEASHROOM, DefensePlant.CATTAIL)
        if (aquaticPlant && !battlefield.hasPool) return "${type.title}需要泳池战场"
        if (aquaticPlant && !isWater) return "${type.title}只能放在水域"
        val onCell = plants.filter { it.row == row && it.col == col }
        val hasPad = onCell.any { it.type == DefensePlant.LILYPAD }
        if (type == DefensePlant.CATTAIL && !hasPad) return "香蒲必须种在荷叶上"
        if (isWater && type != DefensePlant.LILYPAD && !aquaticPlant && !hasPad) return "水域需要先种荷叶"
        if (type == DefensePlant.GRAVE && (row to col) !in gravestones) return "墓碑吞噬者只能种在墓碑上"
        if (type != DefensePlant.GRAVE && (row to col) in gravestones) return "先用墓碑吞噬者清理这里的墓碑"
        if (battlefield.roof && type != DefensePlant.FLOWERPOT && type != DefensePlant.PUMPKIN &&
            onCell.none { it.type == DefensePlant.FLOWERPOT }) return "屋顶需要先种花盆，再种其他植物"
        if (type == DefensePlant.FLOWERPOT && !battlefield.roof) return "花盆只在屋顶战场使用"
        if (type == DefensePlant.COFFEE) {
            val target = onCell.firstOrNull { it.type in MUSHROOMS && !it.awake }
                ?: return "咖啡豆需要种在沉睡的蘑菇上"
            val cost = if (packet == DefensePlant.IMITATER) type.cost else packet.cost
            if (sun < cost) return "阳光不足：需要 $cost 点"
            if ((readyAt[packet] ?: 0L) > elapsedMs) return "${packet.title}还在冷却"
            sun -= cost
            readyAt[packet] = elapsedMs + type.rechargeMs
            target.awake = true
            target.elapsedMs = 0L
            target.attackMs = 650L
            explosions += DefenseExplosion(row, col + .5f, 650L, 650L, .7f, DefenseEffectKind.WAKE)
            if (packet == DefensePlant.IMITATER)
                explosions += DefenseExplosion(row, col + .5f, 800L, 800L, .9f, DefenseEffectKind.TRANSFORM)
            selected = null
            imitaterTarget = null
            return "已唤醒：${target.type.title}"
        }
        if (packet == DefensePlant.COB) {
            val kernel = onCell.firstOrNull { it.type == DefensePlant.KERNEL }
                ?: return "玉米加农炮需要由同一行相邻的两株玉米投手合成"
            val partner = plants.firstOrNull { it.row == row && it.type == DefensePlant.KERNEL &&
                kotlin.math.abs(it.col - kernel.col) == 1 }
                ?: return "玉米加农炮需要由同一行相邻的两株玉米投手合成"
            if (sun < type.cost) return "阳光不足：需要 ${type.cost} 点"
            if ((readyAt[type] ?: 0L) > elapsedMs) return "${type.title}还在冷却"
            val anchorCol = minOf(kernel.col, partner.col)
            plants.remove(kernel); plants.remove(partner)
            sun -= type.cost
            plants += DefensePlantUnit(type, row, anchorCol)
            readyAt[type] = elapsedMs + type.rechargeMs
            selected = null
            return "两株玉米投手已合成为玉米加农炮；初次装填需 35 秒"
        }
        if (type == DefensePlant.PUMPKIN && onCell.none {
                it.type != DefensePlant.LILYPAD && it.type != DefensePlant.FLOWERPOT })
            return "南瓜头要套在已有植物上"
        if (onCell.isNotEmpty() && type != DefensePlant.PUMPKIN &&
            !(isWater && hasPad && type != DefensePlant.LILYPAD && onCell.all { it.type == DefensePlant.LILYPAD }) &&
            !(battlefield.roof && type != DefensePlant.FLOWERPOT && onCell.all { it.type == DefensePlant.FLOWERPOT }))
            return "这个格子已经种了植物"
        val cost = if (packet == DefensePlant.IMITATER) type.cost else packet.cost
        if (sun < cost) return "阳光不足：需要 $cost 点"
        if ((readyAt[packet] ?: 0L) > elapsedMs) return "${packet.title}还在冷却"
        sun -= cost
        val unit = DefensePlantUnit(if (packet == DefensePlant.IMITATER) DefensePlant.IMITATER else type, row, col)
        if (packet == DefensePlant.IMITATER) {
            unit.disguiseType = type
            unit.transformingMs = 800L
            unit.attackMs = 800L
        } else if (type == DefensePlant.GRAVE) {
            unit.attackMs = 1_250L
        }
        if (battlefield.night && type in MUSHROOMS) unit.awake = true
        plants += unit
        readyAt[packet] = elapsedMs + if (packet == DefensePlant.IMITATER) type.rechargeMs else packet.rechargeMs
        selected = null
        imitaterTarget = null
        return "已种下：${type.title}"
    }

    fun tapStar(starId: Long, settings: GameSettings): MathProblem? {
        if (question != null || finished) return null
        val star = stars.firstOrNull { it.id == starId } ?: return null
        val base = settings.normalized()
        var challengeHint: String? = null
        var usedRangeStep: Boolean? = null
        var problem: MathProblem? = null
        if (star.largeSun) {
            val rangeLimit = when {
                base.maxNumber < 10 -> minOf(10, base.maxNumber + 5)
                else -> (base.maxNumber.toLong() + maxOf(10, (base.maxNumber + 1) / 2))
                    .coerceAtMost(1_000_000L).toInt()
            }
            val rangeStep = if (rangeLimit > base.maxNumber) {
                val rangeSettings = base.copy(maxNumber = rangeLimit).normalized()
                val rangeLabel = if (rangeSettings.minNumber == 0) "范围扩到 $rangeLimit 以内"
                    else "范围扩到 ${rangeSettings.minNumber}～$rangeLimit"
                Triple(rangeSettings, rangeLabel, true)
            } else null
            val complexityStep = if (base.terms < 5)
                Triple(base.copy(terms = base.terms + 1), "多算一步（${base.terms + 1}个数）", false)
            else null
            val canAlternate = rangeStep != null && complexityStep != null
            val candidates = if (nextLargeSunChallengeUsesRange)
                listOfNotNull(rangeStep, complexityStep) else listOfNotNull(complexityStep, rangeStep)
            val baseRange = base.minNumber..base.maxNumber
            for ((candidate, hint, usesRange) in candidates) {
                var attempts = if (usesRange) 12 else 1
                while (attempts-- > 0) {
                    val generated = MathEngine.generate(candidate, random) ?: break
                    val usesExpandedRange = generated.start !in baseRange || generated.answer !in baseRange ||
                        generated.steps.any { it.operand !in baseRange }
                    if (!usesRange || usesExpandedRange) {
                        problem = generated
                        challengeHint = hint
                        usedRangeStep = usesRange
                        break
                    }
                }
                if (problem != null) break
            }
            if (problem == null) return null
            if (canAlternate && usedRangeStep != null)
                nextLargeSunChallengeUsesRange = !usedRangeStep
        } else {
            problem = MathEngine.generate(base, random) ?: return null
        }
        question = DefenseQuestion(problem, starId, star.row, star.col, star.progress,
            star.value, star.largeSun, challengeHint)
        return problem
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
        sun += pending.value
        starFlights += StarFlight(star?.row ?: pending.row, star?.col ?: pending.col,
            star?.progress ?: pending.dropProgress, largeSun = pending.largeSun)
        answeredCorrectly++
        if (answeredCorrectly % 3 == 0) {
            lastAutoPickupAward = 2
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
        val produced = when {
            star.coin -> star
            star.largeSun -> star.also { regularSunsSinceLarge = 0 }
            regularSunsSinceLarge >= 4 || random.nextInt(5) == 0 -> {
                regularSunsSinceLarge = 0
                star.copy(value = LARGE_SUN_REWARD, largeSun = true)
            }
            else -> star.also { regularSunsSinceLarge++ }
        }
        if (produced.coin && plants.any { it.type == DefensePlant.GOLDMAGNET && it.awake }) {
            sun += produced.value
            collectedStars++
            starFlights += StarFlight(produced.row, produced.col, produced.progress)
            return
        }
        if (autoPickupCharges > 0 && !produced.largeSun) {
            autoPickupCharges--
            sun += produced.value
            collectedStars++
            starFlights += StarFlight(produced.row, produced.col, produced.progress)
        } else stars += produced
    }

    private fun collectAutoPickups() {
        var delayMs = 0L
        while (autoPickupCharges > 0) {
            val index = stars.indexOfFirst { !it.largeSun }
            if (index < 0) break
            val star = stars.removeAt(index)
            starFlights += StarFlight(star.row, star.col, star.progress, delayMs = delayMs)
            delayMs += 90L
            autoPickupCharges--
            sun += star.value
            collectedStars++
        }
    }

    private fun activeSpawnRows() = campaignLevel?.activeRows
        ?.filter { it in 0 until battlefield.rowCount }
        ?: (0 until battlefield.rowCount).toList()

    private fun zombieSpawnRows(type: ZombieKind): List<Int> {
        val activeRows = activeSpawnRows()
        val safeRows = activeRows.filter { (mowerSpawnBlockedUntilMs[it] ?: 0L) <= elapsedMs }
        return when {
            type == ZombieKind.BOBSLED -> iceTrails.keys.map { it.first }.distinct().filter { it in safeRows }
            type in POOL_ZOMBIES && battlefield.hasPool -> battlefield.waterRows.filter { it in safeRows }
            type == ZombieKind.ZAMBONI && battlefield.hasPool -> safeRows.filter { it !in battlefield.waterRows }
            else -> safeRows
        }
    }

    private fun spawnZombie(availableThreat: Int, forced: ZombieKind? = null): Int {
        val wave = (elapsedMs / 45_000).toInt()
        val pool = when (battlefield) {
            DefenseBattlefield.LAWN_POOL -> when {
                wave == 0 -> listOf(ZombieKind.DRIFTER)
                wave == 1 -> listOf(ZombieKind.DRIFTER, ZombieKind.SIGNAL, ZombieKind.SHELL,
                    ZombieKind.NEWSPAPER, ZombieKind.DUCK)
                else -> listOf(ZombieKind.DRIFTER, ZombieKind.SIGNAL, ZombieKind.SHELL,
                    ZombieKind.NEWSPAPER, ZombieKind.HEAVY, ZombieKind.VAULT, ZombieKind.FOOTBALL,
                    ZombieKind.ZAMBONI, ZombieKind.BOBSLED, ZombieKind.DOLPHIN, ZombieKind.DUCK, ZombieKind.SNORKEL)
            }
            DefenseBattlefield.NIGHT_GARDEN -> when {
                wave == 0 -> listOf(ZombieKind.DRIFTER, ZombieKind.SHELL)
                wave == 1 -> listOf(ZombieKind.DRIFTER, ZombieKind.SIGNAL, ZombieKind.SHELL,
                    ZombieKind.NEWSPAPER, ZombieKind.SCREEN, ZombieKind.DANCER)
                else -> listOf(ZombieKind.DRIFTER, ZombieKind.SHELL, ZombieKind.NEWSPAPER,
                    ZombieKind.SCREEN, ZombieKind.DANCER, ZombieKind.JACK, ZombieKind.BALLOON,
                    ZombieKind.DIGGER, ZombieKind.POGO, ZombieKind.YETI)
            }
            DefenseBattlefield.ROOFTOP -> when {
                wave == 0 -> listOf(ZombieKind.DRIFTER, ZombieKind.SHELL)
                wave == 1 -> listOf(ZombieKind.DRIFTER, ZombieKind.SIGNAL, ZombieKind.HEAVY,
                    ZombieKind.NEWSPAPER, ZombieKind.FOOTBALL)
                else -> listOf(ZombieKind.DRIFTER, ZombieKind.HEAVY, ZombieKind.VAULT,
                    ZombieKind.FOOTBALL, ZombieKind.BUNGEE, ZombieKind.LADDER,
                    ZombieKind.CATAPULT, ZombieKind.GIANT)
            }
            DefenseBattlefield.DAY_LAWN -> when {
                wave == 0 -> listOf(ZombieKind.DRIFTER)
                else -> listOf(ZombieKind.DRIFTER, ZombieKind.SIGNAL, ZombieKind.SHELL,
                    ZombieKind.VAULT, ZombieKind.HEAVY, ZombieKind.NEWSPAPER)
            }
            DefenseBattlefield.NIGHT_LAWN -> when {
                wave == 0 -> listOf(ZombieKind.DRIFTER, ZombieKind.SHELL)
                else -> listOf(ZombieKind.DRIFTER, ZombieKind.SIGNAL, ZombieKind.SHELL,
                    ZombieKind.NEWSPAPER, ZombieKind.SCREEN, ZombieKind.FOOTBALL, ZombieKind.DANCER)
            }
            DefenseBattlefield.DAY_POOL -> when {
                wave == 0 -> listOf(ZombieKind.DRIFTER, ZombieKind.DUCK)
                else -> listOf(ZombieKind.DRIFTER, ZombieKind.SIGNAL, ZombieKind.SHELL,
                    ZombieKind.DUCK, ZombieKind.SNORKEL, ZombieKind.ZAMBONI,
                    ZombieKind.BOBSLED, ZombieKind.DOLPHIN)
            }
            DefenseBattlefield.FOG_POOL -> when {
                wave == 0 -> listOf(ZombieKind.DRIFTER, ZombieKind.DUCK)
                else -> listOf(ZombieKind.DRIFTER, ZombieKind.SHELL, ZombieKind.NEWSPAPER,
                    ZombieKind.DUCK, ZombieKind.SNORKEL, ZombieKind.DOLPHIN, ZombieKind.JACK,
                    ZombieKind.BALLOON, ZombieKind.DIGGER, ZombieKind.POGO)
            }
            DefenseBattlefield.NIGHT_ROOF -> when {
                wave == 0 -> listOf(ZombieKind.DRIFTER, ZombieKind.SHELL)
                else -> listOf(ZombieKind.DRIFTER, ZombieKind.HEAVY, ZombieKind.VAULT,
                    ZombieKind.BUNGEE, ZombieKind.LADDER, ZombieKind.CATAPULT, ZombieKind.GIANT)
            }
        }
        val allowed = campaignLevel?.allowedZombies ?: ZombieKind.entries.toSet()
        val pendingIntroductions = campaignLevel?.spawnEvents.orEmpty().filterIndexed { index, _ ->
            index !in triggeredCampaignEvents
        }.map { it.zombie }.toSet()
        val candidates = if (forced != null) listOf(forced) else
            pool.filter { it in allowed && it !in pendingIntroductions }
        val eligible = candidates.filter { it.threatPoints() <= availableThreat &&
            (it != ZombieKind.BOBSLED || iceTrails.isNotEmpty()) }
        if (eligible.isEmpty()) return 0
        val type = eligible.random(random)
        val row = zombieSpawnRows(type).randomOrNull(random) ?: return 0
        val threat = type.threatPoints()
        zombies += DefenseZombieUnit(nextId++, type, row, 9.1f, spawnBudgetCost = threat)
        return threat
    }

    private fun spawnGroup(availableThreat: Int, size: Int, forced: List<ZombieKind> = emptyList()): Int {
        var remainingThreat = availableThreat
        var usedThreat = 0
        val groupRows = mutableMapOf<Int, Int>()
        repeat(size) {
            val threat = spawnZombie(remainingThreat, forced.getOrNull(it))
            if (threat == 0) return usedThreat
            val zombie = zombies.last()
            if ((groupRows[zombie.row] ?: 0) > 0) {
                if (zombie.type in WATER_LANE_ZOMBIES) {
                    zombie.x += (groupRows[zombie.row] ?: 0) * .45f
                } else {
                    val otherRow = zombieSpawnRows(zombie.type).firstOrNull { it !in groupRows }
                    if (otherRow != null) zombie.row = otherRow
                    else zombie.x += (groupRows[zombie.row] ?: 0) * .45f
                }
            }
            groupRows[zombie.row] = (groupRows[zombie.row] ?: 0) + 1
            usedThreat += threat
            remainingThreat -= threat
        }
        return usedThreat
    }

    private fun releaseSpawnBudget(zombie: DefenseZombieUnit) {
        threatUsed = (threatUsed - zombie.spawnBudgetCost).coerceAtLeast(0)
        zombie.spawnBudgetCost = 0
    }

    private fun ZombieKind.threatPoints() = when (this) {
        ZombieKind.DRIFTER, ZombieKind.SIGNAL, ZombieKind.DUCK, ZombieKind.BACKUP, ZombieKind.IMP -> 1
        ZombieKind.SHELL, ZombieKind.VAULT, ZombieKind.NEWSPAPER, ZombieKind.DANCER,
        ZombieKind.SNORKEL, ZombieKind.DOLPHIN, ZombieKind.JACK, ZombieKind.BALLOON,
        ZombieKind.DIGGER, ZombieKind.POGO, ZombieKind.BUNGEE, ZombieKind.YETI, ZombieKind.LADDER -> 2
        ZombieKind.GIANT -> 4
        ZombieKind.BOSS -> 6
        else -> ((health + armor + 499) / 500).coerceAtLeast(1)
    }

    private fun damage(zombie: DefenseZombieUnit, amount: Int, source: PlantKind? = null) {
        if (zombie.swallowedMs > 0) return
        plantHitSoundEvents++
        val hadGear = zombie.armor > 0
        if (zombie.type == ZombieKind.SCREEN && source in SHIELD_BYPASS_SHOTS) {
            zombie.health -= amount
        } else if (zombie.type == ZombieKind.NEWSPAPER && source in SHIELD_BYPASS_SHOTS) {
            zombie.armor = (zombie.armor - amount).coerceAtLeast(0)
            zombie.health -= amount
        } else {
            val armorDamage = minOf(zombie.armor, amount)
            zombie.armor -= armorDamage
            zombie.health -= amount - armorDamage
        }
        if (hadGear && zombie.armor == 0 && zombie.type in DAMAGEABLE_GEAR) {
            zombie.equipmentRemoved = true
            zombie.gearLostMs = 650L
            if (zombie.type == ZombieKind.NEWSPAPER) {
                zombie.angry = true
                zombie.stunnedMs = maxOf(zombie.stunnedMs, 800L)
            }
        }
        if (hadGear && zombie.armor == 0 && zombie.type in TOOL_GEAR) {
            zombie.equipmentRemoved = true
            zombie.gearLostMs = 650L
            if (zombie.type == ZombieKind.DIGGER && zombie.underground) {
                zombie.underground = false
                zombie.returning = true
                zombie.x = .2f
            }
        }
    }
}
