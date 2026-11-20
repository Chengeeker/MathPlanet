package com.game.maths

import kotlin.random.Random

/** Separate, opt-in curriculum pool. None of the classic game generators use these questions. */
internal enum class PracticeStage(val title: String, val limit: Int) {
    PRESCHOOL("幼小衔接 · 大班", 10),
    FIRST("一年级", 20),
    SECOND("二年级", 100),
    THIRD("三年级", 200),
    FOURTH("四年级", 1000)
}

internal enum class PracticeTopic(val title: String) {
    MIXED("综合练习"), STORY("生活应用题"), PICTURE("看图列式"),
    SHAPES("图形与几何"), PATTERN("观察规律"), CHART("统计图表"),
    CLOCK("认识时间")
}

internal enum class PracticeVisual {
    NONE, DOTS, GROUPS, SHAPES, RECTANGLE, BARS, CLOCK
}

internal data class PracticeQuestion(
    val topic: PracticeTopic,
    val prompt: String,
    val answer: Int,
    val unit: String,
    val options: List<Int>,
    val explanation: String,
    val visual: PracticeVisual = PracticeVisual.NONE,
    val a: Int = 0,
    val b: Int = 0,
    val c: Int = 0,
    val hint: String = ""
) {
    init {
        require(options.isEmpty() || (options.size == 4 && options.toSet().size == 4 && answer in options))
        require(answer >= 0)
    }
}

/** Knowledge is gated separately from the classic numeric range. */
internal object PracticeGenerator {
    fun topics(stage: PracticeStage): List<PracticeTopic> = when (stage) {
        PracticeStage.PRESCHOOL -> listOf(
            PracticeTopic.STORY, PracticeTopic.PICTURE, PracticeTopic.SHAPES, PracticeTopic.PATTERN)
        PracticeStage.FIRST -> listOf(
            PracticeTopic.STORY, PracticeTopic.PICTURE, PracticeTopic.SHAPES,
            PracticeTopic.PATTERN, PracticeTopic.CLOCK)
        PracticeStage.SECOND -> listOf(
            PracticeTopic.STORY, PracticeTopic.PICTURE, PracticeTopic.SHAPES,
            PracticeTopic.PATTERN, PracticeTopic.CLOCK, PracticeTopic.CHART)
        else -> PracticeTopic.entries.filter { it != PracticeTopic.MIXED }
    }

    fun make(stage: PracticeStage, topic: PracticeTopic, random: Random = Random.Default): PracticeQuestion {
        val available = topics(stage)
        val chosen = if (topic == PracticeTopic.MIXED) available.random(random)
            else topic.also { require(it in available) }
        val example = when (chosen) {
            PracticeTopic.STORY -> story(stage,random)
            PracticeTopic.PICTURE -> picture(stage,random)
            PracticeTopic.SHAPES -> shape(stage,random)
            PracticeTopic.PATTERN -> pattern(stage,random)
            PracticeTopic.CLOCK -> clock(stage,random)
            PracticeTopic.CHART -> chart(stage,random)
            PracticeTopic.MIXED -> error("Unresolved topic")
        }
        return example.copy(options=if(chosen==PracticeTopic.CLOCK) clockChoices(example.answer,stage,random)
            else choices(example.answer, random))
    }

    /** Randomized visual and arithmetic parameters; no answer is embedded as a label in a picture. */
    fun round(stage: PracticeStage, topic: PracticeTopic, count: Int = 8, random: Random = Random.Default): List<PracticeQuestion> {
        require(count in 1..30)
        val seen = mutableSetOf<String>()
        val result = mutableListOf<PracticeQuestion>()
        repeat(count * 60) {
            if (result.size < count) {
                val q = make(stage,topic,random)
                val key = q.prompt + "|" + q.visual + "|" + q.a + "|" + q.b + "|" + q.c
                if (seen.add(key)) result += q
            }
        }
        check(result.size == count) { "Insufficient unique practice questions" }
        return result
    }

    private fun range(r: Random, min: Int, max: Int): Int = r.nextInt(min, max + 1)

    private fun clockChoices(answer: Int, stage: PracticeStage, r: Random): List<Int> {
        val step=if(stage==PracticeStage.FIRST) 2 else 1
        val candidate=(1..24).filter { if(step==2) it%2==0 else true }
            .sortedBy {kotlin.math.abs(it-answer)}
        return (listOf(answer)+candidate.filter {it!=answer}.take(3)).shuffled(r)
    }

    private fun choices(answer: Int, r: Random): List<Int> {
        val values = linkedSetOf(answer)
        val interval = if(answer >= 50) 10 else if(answer >= 20) 5 else 1
        val offsets = listOf(-2,-1,1,2,-3,3,-4,4,-5,5,-10,10).shuffled(r)
        for (offset in offsets) {
            if (values.size == 4) break
            val next=answer+offset*interval
            if(next >= 0 && next != answer) values += next
        }
        var n=1
        while (values.size < 4) { values += answer+n; n++ }
        return values.shuffled(r)
    }

    private fun story(stage: PracticeStage, r: Random): PracticeQuestion {
        val max = if(stage == PracticeStage.PRESCHOOL) 10 else if(stage == PracticeStage.FIRST) 20 else 100
        // Preschool and first grade never receive multiplication/division.
        val kind = when(stage) {
            PracticeStage.PRESCHOOL -> r.nextInt(2)
            PracticeStage.FIRST -> listOf(0,1,6).random(r)
            else -> r.nextInt(7)
        }
        val contexts=listOf(
            Triple("小恐龙","颗","星星"),Triple("小红","张","贴纸"),
            Triple("小明","块","积木"),Triple("乐乐","支","彩笔"),
            Triple("兔子","根","胡萝卜"))
        val (who,unit,item)=contexts.random(r)
        return when(kind) {
            0 -> {
                val a=range(r,1,max/2);val b=range(r,1,max-a)
                PracticeQuestion(PracticeTopic.STORY,
                    "$who 原来有$a $unit$item，又得到了$b $unit。现在有多少$unit$item？",
                    a+b,unit,emptyList(),
                    "原来有$a $unit，又得到$b $unit，用加法：$a ＋ $b ＝ ${a+b}（$unit）。",
                    a=a,b=b,hint="“又得到”表示数量变多，用加法。")
            }
            1 -> {
                val a=range(r,3,max);val b=range(r,1,a-1)
                PracticeQuestion(PracticeTopic.STORY,
                    "$who 原来有$a $unit$item，送出去$b $unit，还剩多少$unit？",
                    a-b,unit,emptyList(),
                    "从$a ${unit}中拿走$b $unit，用减法：$a － $b ＝ ${a-b}（$unit）。",
                    a=a,b=b,hint="“送出去”表示数量减少，用减法。")
            }
            2 -> {
                val groups=range(r,2,5);val each=range(r,2,5)
                PracticeQuestion(PracticeTopic.STORY,
                    "每个盒子装$each 支铅笔，一共有$groups 个盒子。共有几支铅笔？",
                    groups*each,"支",emptyList(),
                    "$groups 个盒子，每盒$each 支，就是$groups 个$each：$groups × $each ＝ ${groups*each}（支）。",
                    a=groups,b=each,hint="相同数量的几组，可以用乘法。")
            }
            3 -> {
                val n=range(r,2,5);val each=range(r,2,5)
                PracticeQuestion(PracticeTopic.STORY,
                    "把${n*each} 颗糖平均分给$n 个小朋友，每人分得几颗？",
                    each,"颗",emptyList(),
                    "平均分成$n 份，${n*each} ÷ $n ＝ $each（颗）。",
                    a=n*each,b=n,hint="“平均分”表示每个人分得一样多。")
            }
            4 -> {
                val a=range(r,10,max/2);val b=range(r,2,15)
                val c=range(r,1,10)
                val answer=a+b-c
                PracticeQuestion(PracticeTopic.STORY,
                    "图书角原有$a 本书，收到$b 本捐赠，又借出去$c 本。现在有几本？",
                    answer,"本",emptyList(),
                    "先算收到捐赠：$a ＋ $b ＝ ${a+b}。再减去借出的：${a+b} － $c ＝ $answer（本）。",
                    a=a,b=b,c=c,hint="先加进来的，再减去借出去的。")
            }
            5 -> {
                val price=range(r,2,max/2);val paid=range(r,price+1,max)
                PracticeQuestion(PracticeTopic.STORY,
                    "一本练习本卖$price 元，小明给了$paid 元。应找回多少元？",
                    paid-price,"元",emptyList(),
                    "找零＝付出的钱－商品价格：$paid － $price ＝ ${paid-price}（元）。",
                    a=paid,b=price,hint="找回的钱比付出去的钱少。")
            }
            else -> {
                val a=range(r,2,max-2);val b=range(r,1,a-1)
                PracticeQuestion(PracticeTopic.STORY,
                    "哥哥收集了$a 张卡片，妹妹收集了$b 张。哥哥比妹妹多几张？",
                    a-b,"张",emptyList(),
                    "求相差多少，用减法：$a － $b ＝ ${a-b}（张）。",
                    a=a,b=b,hint="“比……多几张”要比较两个数量的差。")
            }
        }
    }

    private fun picture(stage: PracticeStage, r: Random): PracticeQuestion {
        val count=range(r,2,if(stage==PracticeStage.PRESCHOOL) 8 else 12)
        if(stage==PracticeStage.PRESCHOOL || r.nextBoolean()) {
            return PracticeQuestion(PracticeTopic.PICTURE,
                if(r.nextBoolean()) "数一数图上的小圆点，一共有多少个？"
                else "观察图画：黄色小圆点共有几个？",count,"个",emptyList(),
                "从左到右一个一个数，一共有$count 个圆点。",
                PracticeVisual.DOTS,count,hint="可以用手指点着圆点，一个一个数。")
        }
        val groups=range(r,2,4)
        val each=range(r,2,if(stage==PracticeStage.FIRST) 4 else 6)
        return PracticeQuestion(PracticeTopic.PICTURE,
            "图中有$groups 组小圆点，每组$each 个，一共有多少个？",
            groups*each,"个",emptyList(),
            "每组$each 个，有$groups 组：${List(groups){each.toString()}.joinToString(" ＋ ")} ＝ ${groups*each}（个）。",
            PracticeVisual.GROUPS,groups,each,hint="先数每一组，再数一共有几组。")
    }

    private fun shape(stage: PracticeStage, r: Random): PracticeQuestion {
        if(stage == PracticeStage.PRESCHOOL || stage == PracticeStage.FIRST || stage == PracticeStage.SECOND) {
            val count=range(r,2,if(stage==PracticeStage.PRESCHOOL) 6 else 9)
            val type=r.nextInt(3)
            val name=listOf("圆形","正方形","三角形")[type]
            return PracticeQuestion(PracticeTopic.SHAPES,
                "数一数图中有几个$name？",count,"个",emptyList(),
                "图中这些都是$name，一共有$count 个。",
                PracticeVisual.SHAPES,count,type,hint="形状相同，可以一个一个数。")
        }
        val length=range(r,4,12);val width=range(r,2,length-1)
        val area=stage==PracticeStage.FOURTH && r.nextBoolean()
        val answer=if(area) length*width else 2*(length+width)
        return PracticeQuestion(PracticeTopic.SHAPES,
            "长方形长$length 厘米、宽$width 厘米，它的${if(area)"面积" else "周长"}是多少？",
            answer,if(area)"平方厘米" else "厘米",emptyList(),
            if(area) "长方形面积＝长×宽：$length × $width ＝ $answer（平方厘米）。"
            else "周长是四条边的总长度：（$length ＋ $width）× 2 ＝ $answer（厘米）。",
            PracticeVisual.RECTANGLE,length,width,if(area) 1 else 0,
            if(area) "面积是图形内部有多大。" else "周长是绕图形边缘走一圈的长度。")
    }

    private fun pattern(stage: PracticeStage,r:Random):PracticeQuestion {
        val descending=stage==PracticeStage.PRESCHOOL && r.nextBoolean()
        val start=if(descending)range(r,6,10)
            else range(r,1,if(stage==PracticeStage.PRESCHOOL) 5 else 12)
        val step=if(descending) -1 else if(stage==PracticeStage.PRESCHOOL) 1
            else range(r,1,if(stage==PracticeStage.FIRST) 3 else 8)
        val answer=start+4*step
        return PracticeQuestion(PracticeTopic.PATTERN,
            "找规律：$start，${start+step}，${start+2*step}，${start+3*step}，下一个数是几？",
            answer,"",emptyList(),
            if(descending) "每次减少1，所以下一个是${start+3*step} － 1 ＝ $answer。"
            else "每次增加$step，所以下一个数是${start+3*step} ＋ $step ＝ $answer。",
            a=start,b=step,hint="看看相邻两个数字相差多少。")
    }

    private fun clock(stage: PracticeStage,r:Random):PracticeQuestion {
        val h=range(r,1,11)
        val minutes=if(stage==PracticeStage.FIRST) 0 else listOf(0,30).random(r)
        return PracticeQuestion(PracticeTopic.CLOCK,
            "看看钟表：时针和分针表示的是几点？",h*2+minutes/30,"",
            emptyList(),"分针指向${if(minutes==0)"12" else "6"}，时针在$h ${if(minutes==0)"附近" else "和${h+1}之间"}，所以是$h 点${if(minutes==0)"整" else "半"}。",
            PracticeVisual.CLOCK,h,minutes,hint="分针指向12是整点，指向6是半点。")
    }

    private fun chart(stage: PracticeStage,r:Random):PracticeQuestion {
        val a=range(r,2,7);val b=range(r,2,7)
        val c=range(r,2,7)
        val values=listOf(a,b,c)
        val max=values.max()
        val sum=values.sum()
        val askSum=stage==PracticeStage.THIRD || stage==PracticeStage.FOURTH
        return PracticeQuestion(PracticeTopic.CHART,
            if(askSum)"看统计图：三种水果一共卖出多少箱？" else "看统计图：卖得最多的水果有多少箱？",
            if(askSum) sum else max,"箱",emptyList(),
            if(askSum)"香蕉$a 箱、苹果$b 箱、橘子$c 箱，一共$a ＋ $b ＋ $c ＝ $sum（箱）。"
            else "观察三根柱子，最高的柱子代表$max 箱。",
            PracticeVisual.BARS,a,b,c,hint="柱子越高，表示数量越多。")
    }
}
