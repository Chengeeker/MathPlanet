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
    onBack: () -> Unit, battlefield: DefenseBattlefield = DefenseBattlefield.LAWN_POOL,
    initialZombie: ZombieKind? = null, initialPlant: DefensePlant? = null
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
            GuideTab("植物", tab == 0) { tab = 0 }
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
                    Text("花费 ${if (plant == DefensePlant.IMITATER) "按复制对象" else "${plant.cost} 阳光"}  ·  冷却 ${if (plant == DefensePlant.IMITATER) "按复制对象" else "${plant.rechargeMs / 1_000}s"}  ·  耐久 ${plant.health}",
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
                    GuideSection("当前战场：${battlefield.title}", battlefield.detail)
                    GuideSection("陆地与泳池", if (battlefield.hasPool)
                        "第 1、2、4、5 行是陆地。第 3 行为水域，普通植物和香蒲需先种荷叶；海蘑菇、缠绕海草可直接种水面。"
                        else "当前没有水域，荷叶、海蘑菇、缠绕海草和香蒲无法部署。")
                    if (battlefield.night) GuideSection("夜间与墓碑", "蘑菇会自动醒来；墓碑阻挡种植，选墓碑吞噬者后点击墓碑可清理。迷雾在战斗 30 秒后出现，灯笼草照亮附近 5×5 格，三叶草可暂时吹散。")
                    if (battlefield.roof) GuideSection("屋顶与花盆", "每个屋顶格先种花盆，才能再种其他植物；花盆是承载底座，不会被僵尸啃掉。")
                    GuideSection("阳光收集", "普通阳光答对 +$STAR_REWARD；阳光菇前 120 秒每份 +${STAR_REWARD / 2}。每份普通阳光有 20% 概率变成大阳光，连续 4 份普通后第 5 份保底出现；大阳光答对 +75，必须手动答题，自动收集不会领取。大阳光题只提高一项：扩大数字范围或多算一步，并随当前题目设置变化。答错或放弃只失去当前这份阳光。")
                    GuideSection("种植顺序", "左侧选植物，再点击空格种下。选择铲除后点击已有植物可移除。选中植物或僵尸可直接打开对应图鉴。")
                    GuideSection("难度与暂停", "简单：32 秒首敌、基础间隔 30 秒；困难：26 秒、25 秒；噩梦：20 秒、21 秒。波次会逐步加快。简单答题暂停，困难/噩梦答题不停战；噩梦答错扣 25 阳光。")
                    GuideSection("经典单位交互", "磁力菇可吸走金属护具与工具；叶子保护伞覆盖周围 3×3 格，能挡蹦极抓取和篮球。屋顶战场会出现投篮、蹦极与僵王博士；夜间墓园会出现夜间专属单位。")
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
            Text("${entry.cost} 阳光 · ${entry.roleLabel()}",
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
    DefensePlant.SUN -> "向日葵周期产出阳光；本模式以答题晶体代替阳光，答对后领取。"
    DefensePlant.PEA -> "豌豆射手向同一行发射豌豆，造成持续伤害。"
    DefensePlant.CHERRY -> "樱桃炸弹短暂引爆，对周围 3×3 范围造成大量伤害后消失。"
    DefensePlant.WALL -> "坚果墙耐久很高，能挡住同一行的僵尸，为后排争取时间。"
    DefensePlant.MINE -> "部署后充能 ${MINE_ARMING_MS / 1_000} 秒；充能期间不会被敌人啃食。充能完成后，敌人进入本格才触发，只伤害同格目标。"
    DefensePlant.SNOW -> "寒冰射手发射冰豌豆，命中后降低僵尸移动速度。"
    DefensePlant.CHOMPER -> "大嘴花吞掉前方一只地面僵尸；吞噬后需要较长时间恢复。"
    DefensePlant.DOUBLE -> "双发射手每次连续发射两颗豌豆。"
    DefensePlant.PUFF -> "小喷菇免费，孢子射程约 3 格；白天沉睡，需用咖啡豆唤醒。"
    DefensePlant.SUNSHROOM -> "阳光菇白天沉睡；唤醒后周期产出阳光。本模式前 120 秒每颗答题晶体价值 25，之后为 50。"
    DefensePlant.FUME -> "大喷菇白天沉睡；唤醒后向前喷射约 4 格烟雾，穿透并命中沿途僵尸，也能绕过铁门。"
    DefensePlant.GRAVE -> "夜间墓园中会出现墓碑。选墓碑吞噬者后点击墓碑，它会咬碎墓碑并消失。"
    DefensePlant.HYPNO -> "魅惑菇被僵尸吃掉时魅惑该僵尸，使其转为友军。"
    DefensePlant.SCAREDY -> "胆小菇是低花费长程射手；白天沉睡，唤醒后若周围 3×3 格出现僵尸会躲藏并停火。"
    DefensePlant.ICE -> "寒冰菇白天沉睡；唤醒后冻结全场 4 秒，再减速 5 秒后消失。"
    DefensePlant.DOOM -> "毁灭菇白天沉睡；唤醒后延迟 ${CHERRY_FUSE_MS / 1_000f} 秒，以约 3.5 格半径爆炸并留下不可种植的弹坑。"
    DefensePlant.LILYPAD -> "荷叶铺在水面后，可在同一格种植普通植物。"
    DefensePlant.SQUASH -> "窝瓜压扁前方近距离的僵尸，攻击后消失。"
    DefensePlant.THREEPEATER -> "三线射手同时向本行及上下相邻行发射豌豆。"
    DefensePlant.TANGLE -> "缠绕海草只能种在水中；僵尸进入同格时将其拉下水。"
    DefensePlant.JALAPENO -> "火爆辣椒清除整行僵尸后消失。"
    DefensePlant.SPIKEWEED -> "地刺持续扎伤踩过本格的僵尸；撞上普通冰车时会与其同归于尽。"
    DefensePlant.TORCHWOOD -> "火炬树桩将经过它的豌豆变成伤害更高的火球，并清除冰冻减速。"
    DefensePlant.TALLNUT -> "高坚果耐久高于坚果墙，能挡路且不会被撑杆跳过。"
    DefensePlant.SEASHROOM -> "海蘑菇免费、只能种在水中，孢子射程约 3 格；白天沉睡，需用咖啡豆唤醒。"
    DefensePlant.PLANTERN -> "夜间墓园的迷雾在战斗 30 秒后出现；灯笼草照亮周围 5×5 格，让其中单位重新可见。"
    DefensePlant.CACTUS -> "仙人掌发射尖刺，可攻击气球等空中僵尸。"
    DefensePlant.BLOVER -> "三叶草吹走气球等空中僵尸，并可驱散迷雾；本次只处理场上空中目标。"
    DefensePlant.SPLITPEA -> "分裂豆向前发射一颗豌豆；后方出现僵尸时，会向后连射两颗。"
    DefensePlant.STARFRUIT -> "杨桃从自身位置向左、上、下及右上/右下五个方向发射星星。"
    DefensePlant.PUMPKIN -> "南瓜头套在已有植物外保护它；只能覆盖种植，不能单独放置。"
    DefensePlant.MAGNET -> "磁力菇白天沉睡，需用咖啡豆唤醒；之后每 3 秒吸走本行附近最近的一件金属装备，也能吸掉已架设的梯子。可吸铁桶、铁门、橄榄球头盔、矿工镐、小丑盒和跳跳杆；路障不是金属，不会被吸走。"
    DefensePlant.CABBAGE -> "卷心菜投手白天沉睡；唤醒后抛投卷心菜，攻击越过坚果墙后的僵尸。"
    DefensePlant.FLOWERPOT -> "屋顶上的每株植物都需要花盆承载；先铺盆，再在同一格种植物。花盆不会替植物承受啃咬。"
    DefensePlant.KERNEL -> "玉米投手白天沉睡；唤醒后投掷玉米，约 25% 概率改抛黄油，造成更高伤害并定身 5 秒。"
    DefensePlant.COFFEE -> "咖啡豆唤醒一株沉睡蘑菇；点选咖啡豆后再点蘑菇所在格，不额外占格。"
    DefensePlant.GARLIC -> "大蒜被啃食后会改变僵尸的行进路线。"
    DefensePlant.UMBRELLA -> "叶子保护伞保护周围 3×3 格的植物，挡住蹦极僵尸抓取和投篮僵尸的篮球。"
    DefensePlant.MARIGOLD -> "金盏花周期产出金币；本模式以答题晶体代替金币。"
    DefensePlant.MELON -> "西瓜投手抛出西瓜，对命中点附近同一行的 3×1 范围造成溅射。"
    DefensePlant.GATLING -> "机枪豌豆每轮连续发射四颗豌豆。"
    DefensePlant.TWINSUN -> "双子向日葵每个生产周期产出两份阳光；本模式对应两颗答题晶体。"
    DefensePlant.GLOOM -> "忧郁菇白天沉睡；唤醒后向自身及上下相邻行喷射近距离烟雾。"
    DefensePlant.CATTAIL -> "香蒲必须种在荷叶上；每轮发射两枚追踪尖刺，优先攻击空中僵尸。"
    DefensePlant.WINTERMELON -> "冰西瓜投手白天沉睡；唤醒后抛出冰西瓜，攻击同一行 3×1 范围并减速命中者。"
    DefensePlant.GOLDMAGNET -> "金盏花会产出星币；有金磁力菇时星币自动收集并换成阳光，不弹出答题题目。"
    DefensePlant.SPIKEROCK -> "地刺王比地刺耐久和伤害更高；撞上冰车时摧毁车辆并保留下来。"
    DefensePlant.COB -> "两台同一行相邻的玉米投手可合成玉米加农炮；装填 34.75 秒后点选炮台和落点，造成 3×3 爆炸。"
    DefensePlant.IMITATER -> "选中模仿者后再选一张植物卡，模仿者会复制该卡并播放约 0.8 秒的面具变身动画。"
}

private fun DefensePlant.useText() = when (this) {
    DefensePlant.LILYPAD, DefensePlant.TANGLE, DefensePlant.SEASHROOM -> "只能部署在中间水域。普通植物要先在同格种荷叶。"
    DefensePlant.CATTAIL -> "香蒲只能种在中央水域的荷叶上。"
    DefensePlant.PUMPKIN -> "先在陆地或荷叶上种一株植物，再选南瓜头并点同一格。"
    DefensePlant.COFFEE -> "先种任一沉睡蘑菇，再选咖啡豆并点蘑菇所在格。"
    DefensePlant.COB -> "先在同一行相邻种两株玉米投手，再选玉米加农炮卡并点其中一株升级。"
    DefensePlant.IMITATER -> "先选模仿者，再点卡组中想复制的植物卡，最后点空格部署；费用和冷却按复制对象计算。"
    DefensePlant.GRAVE -> "只在夜间墓园点击墓碑部署；没有墓碑的格子无法放置。"
    DefensePlant.PLANTERN -> "灯笼草在夜间墓园照亮周围 5×5 格；三叶草可短暂清除全场迷雾。"
    DefensePlant.FLOWERPOT -> "只在屋顶战场使用；先把花盆放在空格，再将其他植物种在盆上。"
    DefensePlant.UMBRELLA -> "种在屋顶花盆上；覆盖周围 3×3 格，保护植物免受蹦极和篮球攻击。"
    DefensePlant.GOLDMAGNET -> "与金盏花搭配；自动吸收金盏花产出的星币。"
    else -> "选中卡片后点击空格部署；阳光不足、冷却中或地形不符时会提示原因。"
}

private fun DefensePlant.counterText() = when (kind) {
    PlantKind.GENERATOR, PlantKind.TWIN_GENERATOR -> "不造成攻击伤害；需要搭配射手或坚果墙等防御植物保护。"
    PlantKind.WALL, PlantKind.PUMPKIN -> "只能拖延僵尸，无法将其消灭；空中僵尸与特殊攻击需其他应对。"
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
    ZombieBehavior.BUNGEE -> "空中抓取"
    ZombieBehavior.DIGGER -> "地下绕行"
    ZombieBehavior.LADDER -> "架梯越障"
    ZombieBehavior.CATAPULT -> "远程投射"
    ZombieBehavior.YETI -> "限时撤离"
    ZombieBehavior.BOSS -> "首领攻击"
}

private fun ZombieKind.guideText() = when (this) {
    ZombieKind.DRIFTER -> "普通僵尸沿所在行前进，遇到植物会停下啃食。"
    ZombieKind.SIGNAL -> "旗帜僵尸标记僵尸进攻波次；本模式按普通僵尸速度和耐久处理。"
    ZombieKind.SHELL -> "路障僵尸的路障提供额外耐久，先于本体承受伤害。路障不是金属，磁力菇无法吸走；被击碎后只剩普通僵尸。"
    ZombieKind.VAULT -> "撑杆僵尸会跳过遇到的第一个植物，落地后失去撑杆并以普通速度前进。"
    ZombieKind.HEAVY -> "铁桶僵尸的铁桶提供大量额外耐久；铁桶可被打掉，也可被磁力菇吸走。"
    ZombieKind.DASHER -> "高速僵尸是本项目原创单位，没有经典版对应角色；它以较高速度沿行推进。"
    ZombieKind.NEWSPAPER -> "读报僵尸先由报纸吸收伤害；报纸被打掉后会停顿片刻，再进入愤怒状态并加速。报纸不是金属。"
    ZombieKind.SCREEN -> "铁门僵尸的金属铁门先承受普通豌豆伤害；大喷菇、投手、地刺和爆炸可绕过铁门直接伤本体，磁力菇能吸走铁门。夜间墓园会出现它。"
    ZombieKind.FOOTBALL -> "橄榄球僵尸移动很快，头盔提供额外耐久；头盔可被打掉或被磁力菇吸走。"
    ZombieKind.DANCER -> "舞王僵尸会跳舞并定期召唤同一行的伴舞；夜间墓园会出现它。"
    ZombieKind.BACKUP -> "伴舞僵尸跟随舞王僵尸出现；本模式沿普通路线推进。"
    ZombieKind.DUCK -> "鸭子救生圈僵尸只能在水面行进，是水路的基础僵尸。"
    ZombieKind.SNORKEL -> "潜水僵尸在水中潜行，普通直线豌豆无法命中；投手、范围攻击等仍可攻击它。"
    ZombieKind.ZAMBONI -> "冰车僵尸以高耐久快速碾过植物；普通地刺会与它同归于尽，地刺王能摧毁它并保留下来。"
    ZombieKind.BOBSLED -> "雪橇僵尸小队沿冰车留下的冰道高速滑行；本模式将小队合并为一个高耐久单位，只会在冰道生成。"
    ZombieKind.DOLPHIN -> "海豚骑士僵尸会跃过第一个植物；失去海豚后速度降至普通僵尸水平。"
    ZombieKind.JACK -> "小丑僵尸靠近植物时会引爆手中的小丑盒，对附近植物造成爆炸伤害；磁力菇吸走盒子后，它不再引爆。"
    ZombieKind.BALLOON -> "气球僵尸从空中越过地面防线；仙人掌、香蒲、三叶草和范围攻击可应对它。"
    ZombieKind.DIGGER -> "矿工僵尸钻到地下绕过前排，再从另一侧现身；磁力菇或打掉矿工镐都会使它现身并折返。"
    ZombieKind.POGO -> "跳跳僵尸会连续借跳跳杆越过普通植物；高坚果挡住它，磁力菇吸走跳杆后它失去跳跃并减速。"
    ZombieKind.YETI -> "雪人僵尸生命值高、速度较快；出现一段时间后会尝试向右撤离。"
    ZombieKind.BUNGEE -> "蹦极僵尸从空中垂下并抓走一株植物，然后飞回；叶子保护伞覆盖时会挡住它。"
    ZombieKind.LADDER -> "扶梯僵尸把梯子架在遇到的植物上并越过；后续僵尸可沿梯子通过，磁力菇能移走已架好的梯子。"
    ZombieKind.CATAPULT -> "投篮僵尸周期性向前方植物投篮球；叶子保护伞覆盖的植物不会受伤。"
    ZombieKind.GIANT -> "巨人僵尸生命值很高，会挥棒一击摧毁挡路植物；生命降至一半时把小鬼僵尸投进战场。"
    ZombieKind.IMP -> "小鬼僵尸体型小、生命较低但移动很快，常由巨人僵尸召唤。"
    ZombieKind.BOSS -> "僵王博士是屋顶战场的最终首领，会向植物投射攻击；生命降至一半时投出两只小鬼僵尸。"
}

private fun ZombieKind.counterText() = when (behavior) {
    ZombieBehavior.AIRBORNE -> "使用仙人掌、香蒲、三叶草或范围攻击；普通豌豆通常无法命中。"
    ZombieBehavior.DIVE -> "使用投手、范围攻击或水生植物；潜水僵尸在水下时普通豌豆无法命中。"
    ZombieBehavior.SHIELD -> "使用大喷菇、投手、地刺或爆炸绕过铁门；磁力菇可吸走铁门。"
    ZombieBehavior.VAULT, ZombieBehavior.POGO -> "用地刺或范围攻击布防；磁力菇可吸走跳跳僵尸的跳跳杆。"
    ZombieBehavior.CRUSH, ZombieBehavior.GIANT, ZombieBehavior.BOSS, ZombieBehavior.ARMOR -> "用穿透、减速、爆发和地刺组合处理厚护甲/高生命。"
    ZombieBehavior.SUMMON -> "尽快集中火力击败召唤者，减少随行单位累积。"
    else -> "观察它所在路线，使用持续输出并为高生命目标准备减速或爆发。"
}
