package com.game.maths

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun DefenseGuidebook(
    onBack: () -> Unit, initialZombie: ZombieKind? = null, initialPlant: DefensePlant? = null
) {
    var tab by remember { mutableIntStateOf(if (initialZombie != null) 1 else 0) }
    var plant by remember { mutableStateOf(initialPlant ?: DefensePlant.PEA) }
    var zombie by remember { mutableStateOf(initialZombie ?: ZombieKind.DRIFTER) }
    Column(Modifier.fillMaxSize().background(Color(0xFF17223F)).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("←", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Black,
                modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onBack).padding(horizontal = 12.dp, vertical = 6.dp))
            Column(Modifier.weight(1f)) {
                Text("单位图鉴", color = Color(0xFFFFC65A), fontSize = 22.sp, fontWeight = FontWeight.Black)
                Text("看图识别 · 了解用途、限制与应对方式", color = Color.White, fontSize = 11.sp)
            }
            GuideTab("防御模块", tab == 0) { tab = 0 }
            Spacer(Modifier.width(6.dp))
            GuideTab("来袭单位", tab == 1) { tab = 1 }
            Spacer(Modifier.width(6.dp))
            GuideTab("地形规则", tab == 2) { tab = 2 }
            Spacer(Modifier.width(8.dp))
            Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFC65A))) {
                Text("返回", color = Color(0xFF253453))
            }
        }
        Row(Modifier.fillMaxSize().weight(1f), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            LazyColumn(Modifier.weight(.43f).fillMaxHeight().clip(RoundedCornerShape(15.dp))
                .background(Color(0xFF263654)).padding(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (tab == 0) items(DefensePlant.entries) { entry ->
                    GuideListPlant(entry, plant == entry) { plant = entry }
                } else if (tab == 1) items(ZombieKind.entries) { entry ->
                    GuideListZombie(entry, zombie == entry) { zombie = entry }
                }
            }
            Column(Modifier.weight(.57f).fillMaxHeight().clip(RoundedCornerShape(15.dp))
                .background(Color(0xFFF4F0DF)).padding(13.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(7.dp)) {
                if (tab == 0) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                        PlantPortrait(plant, Modifier.size(72.dp))
                        Column {
                            Text(plant.title, color = Color(0xFF253453), fontSize = 23.sp, fontWeight = FontWeight.Black)
                            Text(plant.roleLabel(), color = Color(0xFF4D6681), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Text("花费 ${if (plant == DefensePlant.IMITATER) "按复制对象" else "${plant.cost} 星能"}  ·  冷却 ${if (plant == DefensePlant.IMITATER) "按复制对象" else "${plant.rechargeMs / 1_000}s"}  ·  耐久 ${plant.health}",
                        color = Color(0xFF253453), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    if (plant.intervalMs > 0) Text("${if (plant.kind == PlantKind.GENERATOR || plant.kind == PlantKind.TWIN_GENERATOR) "生产周期" else "攻击间隔"}：${plant.intervalMs / 1_000f} 秒",
                        color = Color(0xFF253453), fontSize = 11.sp)
                    GuideSection("它会做什么", plant.guideText())
                    GuideSection("怎么使用", plant.useText())
                    GuideSection("克制与限制", plant.counterText())
                } else if (tab == 1) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                        ZombiePortrait(zombie, Modifier.size(72.dp))
                        Column {
                            Text(zombie.title, color = Color(0xFF253453), fontSize = 23.sp, fontWeight = FontWeight.Black)
                            Text(zombie.roleLabel(), color = Color(0xFF4D6681), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Text("生命 ${zombie.health}  ·  护甲 ${zombie.armor}  ·  速度 ${zombie.speed} 格/秒  ·  啃食间隔 ${zombie.biteMs / 1_000f}s",
                        color = Color(0xFF253453), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    GuideSection("它会做什么", zombie.guideText())
                    GuideSection("如何应对", zombie.counterText())
                } else {
                    Text("这关的轨道", color = Color(0xFF253453), fontSize = 21.sp, fontWeight = FontWeight.Black)
                    GuideSection("陆地轨道", "第 1、2、4、5 轨是陆地。普通防御模块放在空格；同一格不能叠放两个普通模块。")
                    GuideSection("中央水域", "第 3 轨是水域。先放漂浮轨道板，才能把普通模块放到同一格；水生孢子灯和缠绕模块可直接部署在水面。")
                    GuideSection("星能收集", "天空或资源模块会生成星能晶体。等它落稳后点击，再完成一道当前设置的数学题；答对 +$STAR_REWARD，答错或放弃只失去这颗晶体。")
                    GuideSection("布防顺序", "左侧选模块，点击轨道空格部署。选择铲除后点已有模块可移除。选中单位或敌人可直接打开对应图鉴。")
                    GuideSection("难度与暂停", "轻松：首波 20 秒，之后每 13 秒出怪并逐步加快；标准：14 秒、每 10 秒；挑战：9 秒、每 7 秒且答题不停战。敌人间隔随波次渐进缩短。轻松/标准答题时暂停。")
                    GuideSection("本关未启用", "当前只有白天草坪和一条中央水域；夜间、雾、屋顶、墓碑等环境能力不会在本关生效，图鉴条目会注明。")
                }
            }
        }
    }
}

@Composable
private fun GuideTab(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(label, color = if (selected) Color(0xFF253453) else Color.White,
        fontWeight = FontWeight.Bold, fontSize = 12.sp,
        modifier = Modifier.clip(RoundedCornerShape(10.dp))
            .background(if (selected) Color(0xFFFFC65A) else Color(0xFF34486B))
            .clickable(onClick = onClick).padding(horizontal = 11.dp, vertical = 10.dp))
}

@Composable
private fun GuideListPlant(entry: DefensePlant, selected: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(10.dp))
        .background(if (selected) Color(0xFFFFC65A) else Color(0xFF34486B))
        .clickable(onClick = onClick).padding(horizontal = 7.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        PlantPortrait(entry, Modifier.size(36.dp))
        Column(Modifier.weight(1f)) {
            Text(entry.title, color = if (selected) Color(0xFF253453) else Color.White,
                fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${entry.cost} 星能 · ${entry.roleLabel()}",
                color = if (selected) Color(0xFF253453) else Color(0xFFD2DFEF), fontSize = 9.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun GuideListZombie(entry: ZombieKind, selected: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(10.dp))
        .background(if (selected) Color(0xFFFFC65A) else Color(0xFF34486B))
        .clickable(onClick = onClick).padding(horizontal = 7.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        ZombiePortrait(entry, Modifier.size(36.dp))
        Column(Modifier.weight(1f)) {
            Text(entry.title, color = if (selected) Color(0xFF253453) else Color.White,
                fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("生命 ${entry.health} · 护甲 ${entry.armor}",
                color = if (selected) Color(0xFF253453) else Color(0xFFD2DFEF), fontSize = 9.sp)
        }
    }
}

@Composable
private fun GuideSection(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, color = Color(0xFF3D745F), fontSize = 12.sp, fontWeight = FontWeight.Black)
        Text(body, color = Color(0xFF253453), fontSize = 13.sp, lineHeight = 18.sp)
    }
}

private fun DefensePlant.roleLabel() = when (kind) {
    PlantKind.GENERATOR, PlantKind.TWIN_GENERATOR -> "资源生产"
    PlantKind.SHOT, PlantKind.SLOW_SHOT, PlantKind.DOUBLE_SHOT, PlantKind.TRIPLE_SHOT,
    PlantKind.GATLING, PlantKind.CACTUS, PlantKind.SPLIT_SHOT, PlantKind.STAR_SHOT, PlantKind.CATTAIL -> "远程攻击"
    PlantKind.WALL, PlantKind.PUMPKIN -> "防护"
    PlantKind.BLAST, PlantKind.MINE, PlantKind.ROW_BLAST -> "爆发攻击"
    PlantKind.EAT, PlantKind.KELP -> "近身一次性攻击"
    PlantKind.FREEZE, PlantKind.PIERCE, PlantKind.LOB, PlantKind.LOB_SLOW, PlantKind.GLOOM, PlantKind.COB -> "特殊攻击"
    PlantKind.SPIKE -> "地面持续伤害"
    PlantKind.SUPPORT, PlantKind.AQUATIC, PlantKind.TORCH, PlantKind.MAGNET, PlantKind.COFFEE,
    PlantKind.GARLIC, PlantKind.UMBRELLA, PlantKind.UPGRADE -> "辅助/地形"
}

private fun DefensePlant.guideText() = when (this) {
    DefensePlant.SUN -> "周期生成一颗星能晶体；晶体仍需答题收取。"
    DefensePlant.PEA -> "向右发射单发直线脉冲，适合基础持续输出。"
    DefensePlant.CHERRY -> "邻近敌人时爆发，对周围一片轨道造成高伤害后消失。"
    DefensePlant.WALL -> "高耐久挡住本轨敌人，为后排争取攻击时间。"
    DefensePlant.MINE -> "部署后充能 ${MINE_ARMING_MS / 1_000} 秒；充能期间不会被敌人啃食。充能完成后，同轨或相邻轨敌人进入附近时爆炸，对范围内目标造成高伤害。"
    DefensePlant.SNOW -> "直线射击并使命中的敌人减速。"
    DefensePlant.CHOMPER -> "近距离吞噬一个敌人，吞噬后需要较长时间恢复。"
    DefensePlant.DOUBLE -> "每次攻击连续发射两发直线脉冲。"
    DefensePlant.PUFF -> "零花费短程孢子射手；目标靠近后才会攻击。"
    DefensePlant.SUNSHROOM -> "周期产出星能晶体；低花费、耐久较低。"
    DefensePlant.FUME -> "发射穿透雾束，可同时打到同一路线上的多个目标。"
    DefensePlant.GRAVE -> "用于清理墓碑；当前草坪没有墓碑，本关暂不生效。"
    DefensePlant.HYPNO -> "接触敌人后将其转为友军；只影响一个近身目标。"
    DefensePlant.SCAREDY -> "低花费远程射手；当前原型尚未实现受惊停火规则。"
    DefensePlant.ICE -> "触发一次全场冻结/减速，然后消失。"
    DefensePlant.DOOM -> "大范围高伤害奇点，影响范围覆盖全场后消失。"
    DefensePlant.LILYPAD -> "铺在水面后，允许普通陆生模块部署在同一格。"
    DefensePlant.SQUASH -> "压扁前方近距离目标后消失。"
    DefensePlant.THREEPEATER -> "同时向本轨及上下相邻轨道发射脉冲。"
    DefensePlant.TANGLE -> "水域近身时缠住并移除一个敌人。"
    DefensePlant.JALAPENO -> "清除本轨所有敌人后消失。"
    DefensePlant.SPIKEWEED -> "持续伤害经过本格的地面敌人；可阻挡地面压路类单位。"
    DefensePlant.TORCHWOOD -> "增强同轨经过它的普通脉冲伤害。"
    DefensePlant.TALLNUT -> "比普通护墙更耐打，专门拖住突破型敌人。"
    DefensePlant.SEASHROOM -> "水域短程孢子射手，免费但只能放在水面。"
    DefensePlant.PLANTERN -> "用于照亮迷雾；当前关卡没有迷雾，本关暂不生效。"
    DefensePlant.CACTUS -> "直线射击，可攻击气球等空中目标。"
    DefensePlant.BLOVER -> "驱散当前空中敌人后消失。"
    DefensePlant.SPLITPEA -> "前方和后方都有目标时可双向射击。"
    DefensePlant.STARFRUIT -> "向多条路线发射星形脉冲。"
    DefensePlant.PUMPKIN -> "套在已有模块外增加一层耐久护壳；不能单独放置。"
    DefensePlant.MAGNET -> "周期剥除近范围敌人的护甲。"
    DefensePlant.CABBAGE -> "抛物线投射，对落点附近造成溅射伤害。"
    DefensePlant.FLOWERPOT -> "屋顶关卡用于承载模块；当前草坪没有屋顶，本关暂不生效。"
    DefensePlant.KERNEL -> "抛出黏性弹，命中后降低目标速度。"
    DefensePlant.COFFEE -> "用于唤醒睡眠中的蘑菇；当前日间草坪没有睡眠状态。"
    DefensePlant.GARLIC -> "被啃食后将敌人引向相邻路线。"
    DefensePlant.UMBRELLA -> "用于保护邻近模块免受空投掠夺；当前版本的拦截范围仍待完善。"
    DefensePlant.MARIGOLD -> "周期生成额外星能晶体。"
    DefensePlant.MELON -> "抛出重型能量果，对落点及邻近敌人造成范围伤害。"
    DefensePlant.GATLING -> "每次攻击连续发射四发脉冲。"
    DefensePlant.TWINSUN -> "每个生产周期同时生成两颗星能晶体。"
    DefensePlant.GLOOM -> "攻击自身周围及相邻路线的近距离敌人。"
    DefensePlant.CATTAIL -> "自动锁定场上目标，可跨路线并攻击空中单位。"
    DefensePlant.WINTERMELON -> "抛出冰陨石造成范围伤害并减速。"
    DefensePlant.GOLDMAGNET -> "吸引并剥除附近敌人的金属护甲。"
    DefensePlant.SPIKEROCK -> "强化地刺，持续伤害地面敌人并阻挡压路类单位。"
    DefensePlant.COB -> "蓄力后轰击一大片区域，冷却时间很长。"
    DefensePlant.IMITATER -> "复制最近部署的模块；需要先成功部署一个目标模块。"
}

private fun DefensePlant.useText() = when (this) {
    DefensePlant.LILYPAD, DefensePlant.TANGLE, DefensePlant.SEASHROOM -> "只能部署在中间水域。普通模块要先在同格铺浮板。"
    DefensePlant.PUMPKIN -> "先在陆地或已有浮板的水域放置目标，再选护盾外壳并点同一格。"
    DefensePlant.IMITATER -> "先部署要复制的模块，再选拟态模块；它复制最近部署对象的费用与能力。"
    DefensePlant.GRAVE, DefensePlant.PLANTERN, DefensePlant.FLOWERPOT, DefensePlant.COFFEE -> "该能力需要特定场景条件；详情见本关限制。"
    else -> "选中卡片后点击空格部署；星能不足、冷却中或地形不符时会提示原因。"
}

private fun DefensePlant.counterText() = when (kind) {
    PlantKind.GENERATOR, PlantKind.TWIN_GENERATOR -> "不造成攻击伤害；需要搭配射手或阻挡模块保护。"
    PlantKind.WALL, PlantKind.PUMPKIN -> "只能拖延敌人，无法清除敌人；空中敌人与特殊投射需其他应对。"
    PlantKind.SUPPORT, PlantKind.AQUATIC, PlantKind.TORCH, PlantKind.COFFEE,
    PlantKind.GARLIC, PlantKind.UMBRELLA, PlantKind.UPGRADE -> guideText()
    else -> "注意目标路线、射程和敌方特殊抗性；图鉴条目说明本关生效限制。"
}

private fun ZombieKind.roleLabel() = when (behavior) {
    ZombieBehavior.WALK -> "普通推进"
    ZombieBehavior.FAST -> "高速推进"
    ZombieBehavior.ARMOR, ZombieBehavior.SHIELD -> "护甲突破"
    ZombieBehavior.VAULT, ZombieBehavior.POGO -> "跳跃越障"
    ZombieBehavior.RAGE -> "失去护甲后加速"
    ZombieBehavior.SUMMON -> "召唤增援"
    ZombieBehavior.DIVE -> "水域潜行"
    ZombieBehavior.CRUSH, ZombieBehavior.GIANT -> "重型碾压"
    ZombieBehavior.SLED -> "水域滑行"
    ZombieBehavior.EXPLODE -> "近程爆炸"
    ZombieBehavior.AIRBORNE -> "空中单位"
    ZombieBehavior.DIGGER -> "地下绕行"
    ZombieBehavior.LADDER -> "架梯越障（原型）"
    ZombieBehavior.CATAPULT -> "远程投射（原型）"
}

private fun ZombieKind.guideText() = when (this) {
    ZombieKind.DRIFTER -> "普通敌人，沿所在轨道向核心推进并啃食阻挡物。"
    ZombieKind.SIGNAL -> "移动略快的基础敌人。"
    ZombieKind.SHELL -> "带少量护甲，护甲先承受伤害。"
    ZombieKind.VAULT -> "遇到首个阻挡物时跳过它。"
    ZombieKind.HEAVY -> "护甲较厚、速度偏慢，能承受持续火力。"
    ZombieKind.DASHER -> "高速沿轨道推进。"
    ZombieKind.NEWSPAPER -> "护甲被打光后进入愤怒状态并加速。"
    ZombieKind.SCREEN -> "屏障可抵挡普通直线脉冲。"
    ZombieKind.FOOTBALL -> "高护甲并快速推进。"
    ZombieKind.DANCER -> "周期召唤随行者。"
    ZombieKind.BACKUP -> "随领航者出现的普通支援单位。"
    ZombieKind.DUCK -> "沿水域推进的基础敌人。"
    ZombieKind.SNORKEL -> "水域潜行单位，普通直线射击难以命中。"
    ZombieKind.ZAMBONI -> "重型压路单位，能快速摧毁挡路模块。"
    ZombieKind.BOBSLED -> "成队高速滑过水域路线。"
    ZombieKind.DOLPHIN -> "快速跃过阻挡模块。"
    ZombieKind.JACK -> "接近防线时触发爆炸，摧毁邻近模块。"
    ZombieKind.BALLOON -> "空中单位，免疫多数地面直线攻击。"
    ZombieKind.DIGGER -> "进入地下绕过防线，再从另一侧回到战场。"
    ZombieKind.POGO -> "遇到阻挡时连续跳跃越障。"
    ZombieKind.YETI -> "高生命值的高速重型敌人。"
    ZombieKind.BUNGEE -> "空投单位原型；当前简化版按空中目标处理。"
    ZombieKind.LADDER -> "带护甲的架梯单位；完整搭梯/梯子可攀爬规则尚未实现。"
    ZombieKind.CATAPULT -> "远程投射单位；当前版本暂未实现完整投石行为。"
    ZombieKind.GIANT -> "巨型单位，生命较高并能重击阻挡物；受伤后投出高速碎片。"
    ZombieKind.IMP -> "生命低但移动很快的小型支援敌人。"
    ZombieKind.BOSS -> "本模式首领原型，拥有大量生命与重击能力。"
}

private fun ZombieKind.counterText() = when (behavior) {
    ZombieBehavior.AIRBORNE -> "使用尖晶拦截器、全域追踪器或风暴驱散器；普通脉冲通常无法命中。"
    ZombieBehavior.DIVE -> "用抛物线投手、范围攻击或水域模块；本关潜行规则为简化实现。"
    ZombieBehavior.SHIELD -> "使用穿透、抛物线或范围攻击绕过正面屏障。"
    ZombieBehavior.VAULT, ZombieBehavior.POGO -> "利用地刺、范围攻击或跳跃后方布防；单格护墙会被越过。"
    ZombieBehavior.CRUSH, ZombieBehavior.GIANT, ZombieBehavior.ARMOR -> "用穿透、减速、爆发和地刺组合处理厚护甲/高生命。"
    ZombieBehavior.SUMMON -> "尽快集中火力击败召唤者，减少随行单位累积。"
    else -> "观察它所在路线，使用持续输出并为高生命目标准备减速或爆发。"
}
