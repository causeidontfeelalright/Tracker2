package com.example.tracker2

import org.junit.Test
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.util.*
import kotlin.math.pow

val kmeansSamples = arrayListOf(1, 2, 3, 1001, 1002, 1003, 2001, 2002, 2003, 3001, 3002, 3003)
val kmeansLabels = arrayListOf('a', 'a', 'a', 'b', 'b', 'b', 'c', 'c', 'c', 'd', 'd', 'd')

fun intDist(x: Int, y: Int): Double {
    return (x - y).toDouble().pow(2)
}

fun intMean(nums: ArrayList<Int>): Int {
    return nums.sum() / nums.size
}

class UnitTests {
    @Test
    fun histogramTest() {
        val h = Histogram<Char>()
        for (i in 0 until 20) {
            h.bump('a')
        }
        for (i in 0 until 30) {
            h.bump('b')
        }
        assert(h.get('a') == 20)
        assert(h.get('b') == 30)
        assert(h.get('c') == 0)
        assert(h.portion('a') == 0.4)
        assert(h.portion('b') == 0.6)
        assert(h.pluralityLabel() == 'b')
    }

    fun is_approx_eq(value: Double, target: Double, tolerance: Double): Boolean {
        val min = target - tolerance/2
        val max = min + tolerance
        return value in min..max
    }

    @Test
    fun distroTest() {
        val h = Histogram<Char>()
        val d = Distribution<Char>()
        d.add('a', 2.0)
        d.add('b', 4.0)
        for (i in 0 until 10000) {
            h.bump(d.randomPick())
        }
        assert(is_approx_eq(h.portion('a'), 0.33, 0.1))
        assert(is_approx_eq(h.portion('b'), 0.67, 0.1))
    }

    @Test
    fun kmeansTest() {
        val means = KMeans(4, ::intDist, kmeansSamples) { it.sum() / it.size}
        for (target in arrayOf(2, 1002, 2002, 3002)) {
            assert(means.contains(target))
        }
    }

    @Test
    fun knnTest() {
        val classifier = KNN<Int, Char, Double>(::intDist, 3)
        for (i in kmeansLabels.indices) {
            classifier.addExample(kmeansSamples[i], kmeansLabels[i])
        }
        for (test in arrayOf(1002, 3002, 2, 2002).zip(arrayOf('b', 'd', 'a', 'c'))) {
            assert(classifier.labelFor(test.first) == test.second)
        }
    }

    @Test
    fun kmeansUnderflowTest() {
        val means = KMeans(kmeansSamples.size + 1, ::intDist, kmeansSamples) { it.sum() / it.size}
        for (sample in kmeansSamples) {
            assert(means.means.contains(sample))
        }
    }

    @Test
    fun kmeansClassifierTest() {
        val kmeansData = kmeansSamples.zip(kmeansLabels)
        val classifier = KMeansClassifierAggregated(4, ::intDist, kmeansData, ::intMean)
        for (p in arrayOf(Pair(50, 'a'), Pair(400, 'a'), Pair(600, 'b'), Pair(1400, 'b'),
            Pair(1800, 'c'), Pair(2100, 'c'), Pair(2700, 'd'))) {
            assert(classifier.labelFor(p.first) == p.second)
        }
    }

    @Test
    fun kmeansClassifier2Test() {
        val kmeansData = kmeansSamples.zip(kmeansLabels)
        val classifier = KMeansClassifier(2, ::intDist, kmeansData, ::intMean)
        for (p in arrayOf(Pair(50, 'a'), Pair(400, 'a'), Pair(600, 'b'), Pair(1400, 'b'),
            Pair(1800, 'c'), Pair(2100, 'c'), Pair(2700, 'd'))) {
            assert(classifier.labelFor(p.first) == p.second)
        }
    }

    @Test
    fun swapRemoveTest() {
        val nums = arrayListOf(0, 1, 2, 3)
        assert(swapRemove(1, nums) == 1)
        assert(nums.size == 3)
        assert(swapRemove(2, nums) == 2)
        assert(nums.size == 2)
        assert(swapRemove(1, nums) == 3)
        assert(nums.size == 1)
        assert(swapRemove(0, nums) == 0)
        assert(nums.size == 0)
    }

    @Test
    fun fileManagerTest() {
        val projectName = "testProject"
        val filename = "target"
        val filetext1 = "This is a test"
        val filetext2 = "Also a test"
        val label1 = "a"
        val label2 = "b"

        println("Manager base: ${File(".").absolutePath}")
        val manager = FileManager(File("."))
        assert(!manager.projectExists(projectName))
        manager.addProject(projectName)
        assert(manager.projectExists(projectName))

        assert(!manager.labelExists(projectName, label1))
        manager.addLabel(projectName, label1)
        assert(manager.labelExists(projectName, label1))

        val dummyFile = File(filename)
        val writer = PrintWriter(FileWriter(dummyFile))
        writer.println(filetext1)
        writer.close()
        assert(dummyFile.exists())
        manager.moveFileTo(dummyFile, projectName, label1)

        val movedDummy = File(manager.labelDir(projectName, label1), dummyFile.name)
        assert(movedDummy.exists())
        assert(!dummyFile.exists())
        val dummyScanner = Scanner(movedDummy)
        val line = dummyScanner.nextLine()
        assert(line == filetext1)
        dummyScanner.close()

        val writer2 = PrintWriter(FileWriter(dummyFile))
        writer2.println(filetext2)
        writer2.close()
        manager.moveFileTo(dummyFile, projectName, label2)

        assert(manager.allLabelsIn(projectName).contains(label1))
        assert(manager.allLabelsIn(projectName).contains(label2))
        assert(manager.allProjects().contains(projectName))

        assert(manager.deleteLabel(projectName, label1))
        assert(!manager.allLabelsIn(projectName).contains(label1))

        assert(manager.deleteProject(projectName))
        assert(!manager.allProjects().contains(projectName))
    }

    @Test
    fun historyTest() {
        val file = File("testfile")
        val history = CommandHistory(file.name)
        history.add(" ")
        for (i in 1..10) {
            history.add(" ")
            history.add("two")
            if (i % 2 == 0) {
                history.add("three")
            }
            if (i % 3 == 0) {
                history.add("one")
            }
        }

        val sorted = history.mostPopular()
        assert(sorted.size == 3)
        assert(sorted[0] == "two")
        assert(sorted[1] == "three")
        assert(sorted[2] == "one")

        val history2 = CommandHistory(file.name)
        assert(history == history2)

        file.delete()
    }

    @Test
    fun minHeightTest() {
        val example1 = arrayListOf(27, 27, 27, 27, 27, 13, 13, 13, 13, 13, 13, 11, 11, 10, 10, 10, 10, 11, 10, 12, 12, 12, 11, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 27, 27, 27, 27)
        val highest1 = highestPoint(example1)
        assert(highest1 == Pair(18, 10))

        val example2 = arrayListOf(16, 13, 16, 13, 16, 12, 14, 13, 13, 10, 10, 10, 13, 9, 9, 9, 9, 9, 9, 9, 10, 10, 11, 11, 12, 12, 12, 12, 12, 12, 12, 5, 1, 1, 5, 6, 8, 9, 10, 12)
        val highest2 = highestPoint(example2)
        assert(highest2 == Pair(32, 1))
    }

    @Test
    fun pixelConverterTest() {
        val converter = PixelConverter(CalibrationLine(60, 64), CalibrationLine(52, 30), 40, 30)
        val groundline1 = arrayListOf(17, 17, 17, 17, 17, 16, 16, 17, 16, 15, 15, 16, 15, 15, 15, 15, 15, 15, 15, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 21, 20, 21, 21, 21, 21, 21, 21, 21, 22, 22)
        val xs1 = arrayListOf(-1.0638297872340425,-1.0212765957446808,-0.9574468085106383,-0.9148936170212766,-0.851063829787234,-1.1094890510948905,-1.0218978102189782,-0.7021276595744681,-0.8759124087591241,-1.302325581395349,-1.1627906976744187,-0.6715328467153284,-0.9302325581395349,-0.8372093023255814,-0.6976744186046512,-0.6046511627906976,-0.46511627906976744,-0.37209302325581395,-0.23255813953488372,-0.6666666666666666,0.0,0.4444444444444444,1.1111111111111112,1.5555555555555556,2.2222222222222223,2.6666666666666665,3.3333333333333335,3.7777777777777777,4.444444444444445,0.20657276995305165,0.27932960893854747,0.2535211267605634,0.28169014084507044,0.3004694835680751,0.3286384976525822,0.3474178403755869,0.3755868544600939,0.39436619718309857,0.37735849056603776,0.3941299790356394)
        val ys1 = arrayListOf(1.3333333333333333,1.3333333333333333,1.3333333333333333,1.3333333333333333,1.3333333333333333,1.6666666666666665,1.6666666666666665,1.3333333333333333,1.6666666666666665,2.0,2.0,1.6666666666666665,2.0,2.0,2.0,2.0,2.0,2.0,2.0,2.0,2.0,2.0,2.0,2.0,2.0,2.0,2.0,2.0,2.0,0.75,0.8333333333333334,0.75,0.75,0.75,0.75,0.75,0.75,0.75,0.6666666666666666,0.6666666666666666)
        val groundline2 = arrayListOf(26, 26, 24, 24, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 23, 26, 24, 15, 21, 16, 19, 20, 20, 21, 23, 22, 5, 11, 2, 4, 3, 1, 1, 1, 10, 17, 17)
        val xs2 = arrayListOf(-0.28653295128939826,-0.27507163323782235,-0.30201342281879195,-0.28859060402684567,-0.22922636103151864,-0.2177650429799427,-0.20057306590257878,-0.18911174785100288,-0.17191977077363896,-0.16045845272206305,-0.14326647564469913,-0.1318051575931232,-0.11461318051575932,-0.10315186246418338,-0.08595988538681948,-0.07449856733524356,-0.05730659025787966,-0.06060606060606061,-0.02865329512893983,-0.020134228187919462,0.0,0.018779342723004695,0.145985401459854,0.09120521172638436,0.11173184357541899,0.1340782122905028,0.14084507042253522,0.12878787878787878,0.16771488469601678,-0.17886178861788618,-0.6578947368421053,-0.16314199395770393,-0.22099447513812154,-0.21548821548821548,-0.19635343618513323,-0.20757363253856942,-0.2244039270687237,-0.8275862068965517,0.9574468085106383,1.0)
        val ys2 = arrayListOf(0.3333333333333333,0.3333333333333333,0.5,0.5,0.3333333333333333,0.3333333333333333,0.3333333333333333,0.3333333333333333,0.3333333333333333,0.3333333333333333,0.3333333333333333,0.3333333333333333,0.3333333333333333,0.3333333333333333,0.3333333333333333,0.3333333333333333,0.3333333333333333,0.5833333333333334,0.3333333333333333,0.5,2.0,0.75,1.6666666666666665,0.9166666666666666,0.8333333333333334,0.8333333333333334,0.75,0.5833333333333334,0.6666666666666666,2.0,2.0,2.0,2.0,2.0,2.0,2.0,2.0,2.0,1.3333333333333333,1.3333333333333333)
        val groundline3 = arrayListOf(18, 17, 17, 7, 18, 17, 17, 17, 17, 17, 18, 18, 16, 15, 15, 15, 16, 15, 15, 15, 15, 17, 17, 17, 18, 18, 18, 17, 18, 18, 18, 18, 16, 14, 14, 14, 14, 14, 17, 17)
        val xs3 = arrayListOf(-0.78125,-1.0212765957446808,-0.9574468085106383,0.46112600536193027,-0.625,-0.8085106382978723,-0.7446808510638298,-0.7021276595744681,-0.6382978723404256,-0.5957446808510638,-0.390625,-0.359375,-0.583941605839416,-0.8372093023255814,-0.6976744186046512,-0.6046511627906976,-0.291970802919708,-0.37209302325581395,-0.23255813953488372,-0.13953488372093023,0.0,0.0425531914893617,0.10638297872340426,0.14893617021276595,0.15625,0.1875,0.234375,0.3617021276595745,0.3125,0.34375,0.390625,0.421875,0.8759124087591241,7.111111111111111,7.777777777777778,8.222222222222221,8.88888888888889,9.333333333333334,0.9574468085106383,1.0)
        val ys3 = arrayListOf(1.0,1.3333333333333333,1.3333333333333333,2.0,1.0,1.3333333333333333,1.3333333333333333,1.3333333333333333,1.3333333333333333,1.3333333333333333,1.0,1.0,1.6666666666666665,2.0,2.0,2.0,1.6666666666666665,2.0,2.0,2.0,2.0,1.3333333333333333,1.3333333333333333,1.3333333333333333,1.0,1.0,1.0,1.3333333333333333,1.0,1.0,1.0,1.0,1.6666666666666665,2.0,2.0,2.0,2.0,2.0,1.3333333333333333,1.3333333333333333)

        val groundlines = arrayOf(groundline1, groundline2, groundline3)
        val xlines = arrayOf(xs1, xs2, xs3)
        val ylines = arrayOf(ys1, ys2, ys3)
        for (g in 0..2) {
            for (x in 0 until groundlines.size) {
                val x1 = converter.xPixel2distance(x, groundlines[g][x])
                val y1 = converter.yPixel2distance(groundlines[g][x])
                assert(x1 == xlines[g][x])
                assert(y1 == ylines[g][x])
            }
        }
    }

    @Test
    fun solveForXTest() {
        assert(solveForX(0, 1, 2, 1, 3) == 1.0)
        assert(solveForX(3, 1, 1, 2, 2) == 3.0)
    }
}
