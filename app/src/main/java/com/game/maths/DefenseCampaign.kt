package com.game.maths

import android.content.Context

internal enum class DefenseCampaignTheme(val title: String, val battlefield: DefenseBattlefield) {
    LAWN("白天草坪", DefenseBattlefield.DAY_LAWN),
    NIGHT("夜晚草坪", DefenseBattlefield.NIGHT_LAWN),
    POOL("白天泳池", DefenseBattlefield.DAY_POOL),
    FOG_POOL("雾夜泳池", DefenseBattlefield.FOG_POOL),
    ROOF("屋顶", DefenseBattlefield.ROOFTOP)
}

internal data class CampaignSpawnEvent(
    val notBeforeSec: Int,
    val deadlineSec: Int,
    val zombie: ZombieKind,
    val count: Int = 1,
    val telegraphSec: Int = 8
)

internal data class DefenseCampaignLevel(
    val id: Int,
    val theme: DefenseCampaignTheme,
    val battlefield: DefenseBattlefield,
    val title: String,
    val objective: String,
    val difficulty: Int,
    val activeRows: Set<Int>,
    val allowedPlants: Set<DefensePlant>,
    val allowedZombies: Set<ZombieKind>,
    val spawnEvents: List<CampaignSpawnEvent>,
    val budgetOffset: Int,
    val startingSun: Int = 200,
    val skyStarIntervalMs: Long = 9_000L,
    val postKillSpawnDelayMs: Long = 2_000L
) {
    val chapter get() = (id - 1) / 10 + 1
    val numberInChapter get() = (id - 1) % 10 + 1
    val label get() = "$chapter-${numberInChapter}"
    val columns get() = 9
    val waterRows get() = battlefield.waterRows
}

internal object DefenseCampaign {
    val themes = DefenseCampaignTheme.entries

    private val titles = listOf(
        listOf("单轨启程", "三线观察", "首次轻甲", "全场布防", "反弹试炼", "跃迁来袭", "双波耐守", "重甲检验", "路线综合", "补给转换"),
        listOf("夜幕初临", "墓碑阻路", "屏障来袭", "夜间协防", "遗迹敲击", "高速冲线", "墓园混编", "召唤同伴", "多线合围", "夜行补给"),
        listOf("水陆初遇", "双域布防", "潜行水路", "六路协作", "微型风暴", "冰道突袭", "水陆混编", "越水冲锋", "泳池合围", "潮汐补给"),
        listOf("浓雾初临", "灯塔侦察", "空中来袭", "视野协防", "暗箱抉择", "后排绕袭", "雾中混编", "连续跃迁", "混合总测", "闪电补给"),
        listOf("倾斜屋顶", "抛物线阵地", "梯架突破", "屋顶布防", "后排保卫", "远程投射", "三向夹击", "巨型来袭", "终盘演练", "星球首领")
    )

    private val objectives = listOf(
        listOf("只守中间一行；先收集阳光，再布置基础攻击。", "开放三行，练习把资源分给不同路线。", "首次出现轻甲；用持续输出处理，不要求特定植物。", "开放五行，练习撤除和重排防线。", "以同轨聚集与范围处理为重点，降低常规波次压力。", "首次出现会越过前排的跃迁敌，需留下第二道防线。", "延长战斗节奏；让产能和攻击同时成形。", "首次出现重甲；观察其耐久并保持持续火力。", "把普通、轻甲、跃迁敌分组出现，避免同一波堆满。", "补给受限下维持稳定防线，预留即时应对资源。"),
        listOf("切换夜间经济与场景；先学会使用夜间单位。", "少量障碍占格，安排清障与防守的先后。", "首次出现屏障敌，给穿透或破甲留出解法。", "在夜间规则下组合已见敌，保留图鉴查看入口。", "以障碍与短时处理为重点，压低同时来敌数量。", "首次出现高速重型冲线者，提前布置减速或应急火力。", "增加墓园路线压力，不同时叠加视野限制。", "首次出现召唤型敌，优先识别并处理召唤来源。", "组合已见的墓碑、屏障与召唤机制，分波预告。", "有限补给下应对夜间敌群，保留关键防守卡。"),
        listOf("出现陆地与水路；水路部署需要先放荷叶。", "开始同时维护陆地和水路，避免单侧堆防。", "首次出现潜水敌，关注水面提示并准备专门反制。", "增加路线覆盖要求，但控制单路敌人密度。", "以小体型密集来敌为主题，明确数量与伤害反馈。", "首次出现压路冰道，提前观察它改变的路线格。", "组合冰道与泳池敌，留出恢复和重新布防时间。", "首次出现高速越水敌，限制同一波的数量。", "汇合潜行、越水与陆地敌，每波突出一个主威胁。", "有限水陆补给下完成全场覆盖，荷叶供给保持稳定。"),
        listOf("夜间、水路和轻雾同时出现；先看清警示再布防。", "首次强化视野管理，灯塔揭示范围必须清楚。", "首次出现空中敌，同时提供可用的拦截方式。", "在雾中安排侦察与水陆防守，避免随机盲打。", "以逐步揭示信息为重点，答题资源可换取观察机会。", "首次出现后排绕袭，标明回场路线并预留反向防线。", "组合已见敌人，但敌人位置始终可识别。", "首次出现连续跃迁者，限制数量并展示可用反制。", "阶段综合关；不同波次轮换主威胁并明确预警。", "短时闪光揭示战场；保留路线、方向和危险提示。"),
        listOf("屋顶需要花盆；空投落点先预告再结算。", "使用曲射攻击适应屋顶地形，避免只依赖直射。", "首次出现架梯敌，说明受影响的防线与处理方法。", "组合屋顶敌人，检验前几关建立的阵地。", "重点保护后排；空投目标提前标出并限制数量。", "首次出现远程投射，显示目标区与落点。", "混合远程、架梯和常规敌，错开高压时段。", "首次出现巨型高耐久敌，提供多段预警和处理窗口。", "最终首领前的综合复习，不加入未教学机制。", "夜间屋顶首领关；按阶段提示危险路线并留出反制窗口。")
    )

    private val starRatings = listOf(
        listOf(1, 1, 2, 2, 2, 2, 2, 3, 3, 2),
        listOf(2, 2, 3, 3, 2, 3, 3, 3, 4, 3),
        listOf(2, 2, 3, 3, 2, 4, 3, 4, 4, 3),
        listOf(3, 3, 4, 4, 3, 4, 4, 4, 5, 4),
        listOf(3, 3, 4, 4, 3, 4, 4, 5, 5, 5)
    )

    private data class Introduction(val chapter: Int, val level: Int, val zombies: List<ZombieKind>)

    private val introductions = listOf(
        Introduction(1, 3, listOf(ZombieKind.SHELL)),
        Introduction(1, 6, listOf(ZombieKind.VAULT)),
        Introduction(1, 8, listOf(ZombieKind.HEAVY)),
        Introduction(2, 1, listOf(ZombieKind.NEWSPAPER)),
        Introduction(2, 3, listOf(ZombieKind.SCREEN)),
        Introduction(2, 6, listOf(ZombieKind.FOOTBALL)),
        Introduction(2, 8, listOf(ZombieKind.DANCER, ZombieKind.BACKUP)),
        Introduction(3, 1, listOf(ZombieKind.DUCK)),
        Introduction(3, 3, listOf(ZombieKind.SNORKEL)),
        Introduction(3, 6, listOf(ZombieKind.ZAMBONI, ZombieKind.BOBSLED)),
        Introduction(3, 8, listOf(ZombieKind.DOLPHIN)),
        Introduction(4, 1, listOf(ZombieKind.JACK)),
        Introduction(4, 3, listOf(ZombieKind.BALLOON)),
        Introduction(4, 6, listOf(ZombieKind.DIGGER)),
        Introduction(4, 8, listOf(ZombieKind.POGO)),
        Introduction(5, 1, listOf(ZombieKind.BUNGEE)),
        Introduction(5, 3, listOf(ZombieKind.LADDER)),
        Introduction(5, 6, listOf(ZombieKind.CATAPULT)),
        Introduction(5, 8, listOf(ZombieKind.GIANT)),
        Introduction(5, 10, listOf(ZombieKind.BOSS))
    )

    private data class PlantUnlock(val levelId: Int, val plant: DefensePlant)
    private val plantUnlocks = listOf(
        PlantUnlock(1, DefensePlant.PEA), PlantUnlock(2, DefensePlant.SUN),
        PlantUnlock(3, DefensePlant.CHERRY), PlantUnlock(4, DefensePlant.WALL),
        PlantUnlock(6, DefensePlant.MINE), PlantUnlock(7, DefensePlant.SNOW),
        PlantUnlock(8, DefensePlant.CHOMPER), PlantUnlock(9, DefensePlant.DOUBLE),
        PlantUnlock(11, DefensePlant.PUFF), PlantUnlock(12, DefensePlant.SUNSHROOM),
        PlantUnlock(13, DefensePlant.FUME), PlantUnlock(14, DefensePlant.GRAVE),
        PlantUnlock(16, DefensePlant.HYPNO), PlantUnlock(17, DefensePlant.SCAREDY),
        PlantUnlock(18, DefensePlant.ICE), PlantUnlock(19, DefensePlant.DOOM),
        PlantUnlock(21, DefensePlant.LILYPAD), PlantUnlock(22, DefensePlant.SQUASH),
        PlantUnlock(23, DefensePlant.THREEPEATER), PlantUnlock(24, DefensePlant.TANGLE),
        PlantUnlock(26, DefensePlant.JALAPENO), PlantUnlock(27, DefensePlant.SPIKEWEED),
        PlantUnlock(28, DefensePlant.TORCHWOOD), PlantUnlock(29, DefensePlant.TALLNUT),
        PlantUnlock(31, DefensePlant.SEASHROOM), PlantUnlock(32, DefensePlant.PLANTERN),
        PlantUnlock(33, DefensePlant.CACTUS), PlantUnlock(34, DefensePlant.BLOVER),
        PlantUnlock(36, DefensePlant.SPLITPEA), PlantUnlock(37, DefensePlant.STARFRUIT),
        PlantUnlock(38, DefensePlant.PUMPKIN), PlantUnlock(39, DefensePlant.MAGNET),
        PlantUnlock(41, DefensePlant.CABBAGE), PlantUnlock(42, DefensePlant.FLOWERPOT),
        PlantUnlock(43, DefensePlant.KERNEL), PlantUnlock(44, DefensePlant.COFFEE),
        PlantUnlock(46, DefensePlant.GARLIC), PlantUnlock(47, DefensePlant.UMBRELLA),
        PlantUnlock(48, DefensePlant.MARIGOLD), PlantUnlock(49, DefensePlant.MELON)
    )

    val levels: List<DefenseCampaignLevel> = (1..50).map { id ->
        val chapter = (id - 1) / 10
        val local = (id - 1) % 10 + 1
        val levelIntro = introductions.firstOrNull { it.chapter == chapter + 1 && it.level == local }
        val available = introductions.filter { it.chapter < chapter + 1 ||
            it.chapter == chapter + 1 && it.level <= local }.flatMap { it.zombies }.toMutableSet()
        available += ZombieKind.DRIFTER
        if (id >= 2) available += ZombieKind.SIGNAL
        val theme = themes[chapter]
        val battlefield = when (id) {
            35 -> DefenseBattlefield.NIGHT_GARDEN
            50 -> DefenseBattlefield.NIGHT_ROOF
            else -> theme.battlefield
        }
        val rows = when {
            chapter == 0 && local == 1 -> setOf(2)
            chapter == 0 && local in 2..3 -> setOf(1, 2, 3)
            else -> (0 until battlefield.rowCount).toSet()
        }
        val availablePlants = plantUnlocks.filter { it.levelId <= id }.mapTo(mutableSetOf()) { it.plant }
        // The original 5-1 supplies pots during play; this deck-based mode needs them before the reward unlock.
        if (id == 41) availablePlants += DefensePlant.FLOWERPOT
        val eventAt = when (local) {
            1, 2 -> 32
            in 3..6 -> 48
            else -> 78
        }
        val event = levelIntro?.zombies?.filterNot { it in setOf(ZombieKind.BOSS, ZombieKind.BACKUP) }
            ?.mapIndexed { index, zombie ->
            val startsAt = eventAt + index * 30
            CampaignSpawnEvent(startsAt, startsAt + 40, zombie, telegraphSec = 8)
        }.orEmpty()
        val budgetOffset = when (local) {
            1, 5 -> -1
            7, 8, 9, 10 -> 1
            else -> 0
        }
        DefenseCampaignLevel(
            id = id,
            theme = theme,
            battlefield = battlefield,
            title = titles[chapter][local - 1],
            objective = objectives[chapter][local - 1],
            difficulty = starRatings[chapter][local - 1],
            activeRows = rows,
            allowedPlants = availablePlants,
            allowedZombies = available,
            spawnEvents = event,
            budgetOffset = budgetOffset.coerceIn(-2, 2),
            startingSun = if (id == 1) 150 else 200,
            postKillSpawnDelayMs = (10_000L - starRatings[chapter][local - 1] * 1_000L).coerceAtLeast(5_000L)
        )
    }

    fun level(id: Int) = levels.getOrNull(id - 1)
    fun defaultDeck(level: DefenseCampaignLevel): List<DefensePlant> {
        val required = when {
            level.battlefield.hasPool -> DefensePlant.LILYPAD
            level.battlefield.roof -> DefensePlant.FLOWERPOT
            else -> null
        }
        return (level.allowedPlants.filter { it != required }.take(7) +
            listOfNotNull(required?.takeIf { it in level.allowedPlants })).take(8)
    }
}

internal object DefenseCampaignProgress {
    private const val FILE = "star_guard_campaign"
    private const val KEY_UNLOCKED = "unlocked_level"

    fun unlockedThrough(context: Context): Int = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        .getInt(KEY_UNLOCKED, 1).coerceIn(1, 50)

    fun complete(context: Context, id: Int) {
        val preferences = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val next = (id + 1).coerceAtMost(50)
        if (next > preferences.getInt(KEY_UNLOCKED, 1)) {
            preferences.edit().putInt(KEY_UNLOCKED, next).apply()
        }
    }
}
