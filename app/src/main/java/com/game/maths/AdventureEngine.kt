package com.game.maths

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.max
import kotlin.random.Random

enum class SoloGame(val title: String, val emoji: String, val subtitle: String) {
    SHOP("小小商店", "🛒", "学习 1、5、10、20、50、100 元纸币"),
    REVIEW("错题专项复习", "📒", "集中练习需要巩固的题目")
}
enum class BattleGame(val title: String) { LIGHTNING("⚡ 闪电抢答赛"), STRATEGY("🧠 巧思挑战赛") }
enum class SkillTopic { MIXED, CARRY, BORROW, MONEY }
enum class ShopRole { CUSTOMER, CASHIER }
data class SkillQuestion(
    val a: Int, val b: Int, val add: Boolean = true,
    val price: Int = 0, val paid: Int = 0, val role: ShopRole = ShopRole.CUSTOMER,
    val rawExpression: String = "", val rawAnswer: Int = 0,
    val sourceKey: String = ""
) {
    val isMoney get() = price > 0
    val answer get() = if (rawExpression.isNotEmpty()) rawAnswer else if (isMoney) {
        if (role == ShopRole.CUSTOMER) price else paid - price
    } else if (add) a + b else a - b
    val expression get() = if (rawExpression.isNotEmpty()) rawExpression else if (isMoney) {
        if (role == ShopRole.CUSTOMER) "商品 ¥$price · 请付款"
        else "商品 ¥$price · 收到 ¥$paid · 请找零"
    } else "$a ${if (add) "+" else "−"} $b = ?"
    val learningKey get() = if (sourceKey.isNotEmpty()) sourceKey else if (isMoney) {
        "MONEY:${role.name}:$price:$paid"
    } else "${if (add) "ADD" else "SUB"}:$a:$b"
    val strategyStep get() = if (add) 10 - a else b
    val resultBreak get() = 10 - b + (a - 10)
}
object AdventureBank {
    /** All operands, results, listed prices and given payments stay inside 0..maxNumber. */
    fun generate(
        topic: SkillTopic, random: Random = Random.Default,
        weakKeys: Set<String> = emptySet(), maxNumber: Int = 20,
        shopRole: ShopRole? = null
    ): SkillQuestion {
        val cap = maxNumber.coerceIn(3, 1_000_000)
        val candidates = when (topic) {
            SkillTopic.CARRY -> carry(cap).ifEmpty { sampleArithmetic(cap, random) }
            SkillTopic.BORROW -> borrow(cap).ifEmpty { sampleArithmetic(cap, random) }
            SkillTopic.MONEY -> money(cap, shopRole)
            SkillTopic.MIXED -> sampleArithmetic(cap, random)
        }
        val weak = candidates.filter { it.learningKey in weakKeys }
        return if (weak.isNotEmpty() && random.nextInt(4) == 0) weak.random(random)
        else candidates.random(random)
    }

    fun makeRound(
        topic: SkillTopic, count: Int, weakKeys: Set<String> = emptySet(),
        random: Random = Random.Default, maxNumber: Int = 20,
        shopRole: ShopRole? = null
    ): List<SkillQuestion> {
        val result = mutableListOf<SkillQuestion>()
        repeat(count.coerceIn(1, 30)) {
            var next = generate(topic, random, weakKeys, maxNumber, shopRole)
            for (attempt in 0 until 40) {
                if (next.learningKey !in result.takeLast(7).map { it.learningKey }) break
                next = generate(topic, random, weakKeys, maxNumber, shopRole)
            }
            result += next
        }
        return result
    }

    /** The configured learning range is the ONLY numeric limit on questions. */
    private fun sampleArithmetic(cap: Int, random: Random): List<SkillQuestion> {
        val upper = cap.coerceAtLeast(3)
        return List(90) {
            if (random.nextBoolean()) {
                val a = random.nextInt(1, upper)
                SkillQuestion(a, random.nextInt(1, upper - a + 1))
            } else {
                val a = random.nextInt(2, upper + 1)
                SkillQuestion(a, random.nextInt(1, a + 1), add = false)
            }
        }
    }

    private fun carry(cap: Int): List<SkillQuestion> =
        (4..9).flatMap { a ->
            (2..9).mapNotNull { b ->
                val sum = a + b
                if (sum <= 10 || sum > minOf(cap, 20)) null
                else SkillQuestion(a, b)
            }
        }

    private fun borrow(cap: Int): List<SkillQuestion> =
        (11..minOf(19, cap)).flatMap { a ->
            (2..9).mapNotNull { b ->
                if (a % 10 >= b) null
                else SkillQuestion(a, b, add = false)
            }
        }

    private fun money(cap: Int, shopRole: ShopRole?): List<SkillQuestion> {
        // Prices remain practical everyday amounts, never exceeding the learning cap.
        val upper = minOf(cap, 200)
        val givenAmounts = (2..minOf(cap, 20)).toList() +
            listOf(25, 30, 40, 50, 60, 70, 80, 90, 100, 120, 150, 200)
                .filter { it <= cap }
        val results = ArrayList<SkillQuestion>()
        for (price in 1..upper) {
            if (shopRole != ShopRole.CASHIER) {
                results += SkillQuestion(0, 0, price = price, role = ShopRole.CUSTOMER)
            }
            if (shopRole != ShopRole.CUSTOMER) {
                val higher = givenAmounts.filter { it > price }
                higher.forEach { given ->
                    results += SkillQuestion(0, 0, price = price, paid = given, role = ShopRole.CASHIER)
                }
            }
        }
        return results.ifEmpty { listOf(SkillQuestion(0, 0, price = 1, paid = 2, role = ShopRole.CASHIER)) }
    }
}

data class TrainingRecord(
    val game: String, val question: String, val key: String,
    val correct: Boolean, val methodCorrect: Boolean, val elapsedMs: Long,
    val timestamp: Long = System.currentTimeMillis()
)
object TrainingHistory {
    private const val PREF = "math_planet_history"
    private const val KEY = "practice_v1"
    fun read(context: Context): List<TrainingRecord> = runCatching {
        val raw = context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString(KEY, "[]")
        val array = JSONArray(raw)
        (0 until array.length()).map { i ->
            val o = array.getJSONObject(i)
            TrainingRecord(o.getString("game"), o.getString("question"),
                o.getString("key"), o.getBoolean("correct"), o.optBoolean("method", false),
                o.optLong("elapsed", 0), o.getLong("timestamp"))
        }
    }.getOrDefault(emptyList())

    fun enabled(context: Context): Boolean =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getBoolean("record_enabled", true)
    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
            .putBoolean("record_enabled", enabled).apply()
    }
    fun add(context: Context, record: TrainingRecord) {
        if (!enabled(context)) return
        val list = (read(context) + record).takeLast(3000)
        val array = JSONArray()
        list.forEach { r ->
            array.put(JSONObject().put("game", r.game).put("question", r.question)
                .put("key", r.key).put("correct", r.correct)
                .put("method", r.methodCorrect).put("elapsed", r.elapsedMs)
                .put("timestamp", r.timestamp))
        }
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit().putString(KEY, array.toString()).apply()
    }
    fun clear(context: Context) =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().remove(KEY).apply()

    fun clearMistakes(context: Context) {
        val array = JSONArray()
        read(context).filter { it.correct && it.methodCorrect }.forEach { r ->
            array.put(JSONObject().put("game", r.game).put("question", r.question)
                .put("key", r.key).put("correct", r.correct)
                .put("method", r.methodCorrect).put("elapsed", r.elapsedMs)
                .put("timestamp", r.timestamp))
        }
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putString(KEY, array.toString()).apply()
    }

    fun weakKeys(context: Context): Set<String> =
        mistakes(currentGameRecords(read(context))).map { it.key }.toSet()

    fun mistakes(records: List<TrainingRecord>): List<TrainingRecord> {
        return records.groupBy { it.key }.values.mapNotNull { attempts ->
            if (attempts.last().correct && attempts.last().methodCorrect) null
            else attempts.lastOrNull { !it.correct || !it.methodCorrect }
        }.sortedByDescending { it.timestamp }
    }
    /** Keep old version data in storage but exclude retired modes from the current dashboard. */
    fun currentGameRecords(records: List<TrainingRecord>): List<TrainingRecord> =
        records.mapNotNull { record ->
            // Older app releases used longer titles. Group their statistics under
            // the new short titles without modifying any persisted raw record.
            val title = when {
                record.game == "自由算术探险" -> "算数探险"
                record.game.startsWith("基础加减法对决·") ->
                    record.game.replaceFirst("基础加减法对决·", "计算对决·")
                else -> record.game
            }
            if (title == "算数探险" || title == SoloGame.SHOP.title ||
                title == SoloGame.REVIEW.title ||
                title.startsWith("计算对决·") ||
                BattleGame.entries.any { title.startsWith("${it.title}·") }
            ) record.copy(game = title) else null
        }

    fun duration(records: List<TrainingRecord>): Long =
        records.sumOf { max(0L, it.elapsedMs) }
}
