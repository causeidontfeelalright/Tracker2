package com.example.tracker2

import java.util.*

class GridMap {
    var width = 0
    var height = 0
    val cells = BitSet()
}

const val MAX_DISTANCE_METERS = 2.0

fun solveForX(y: Int, x1: Int, y1: Int, x2: Int, y2: Int): Double =
    x1 + (y - y1).toDouble() * (x2 - x1).toDouble() / (y2 - y1).toDouble()

class PixelConverter(val meter1: CalibrationLine, val meter2: CalibrationLine,
                     val imgWidth: Int, val imgHeight: Int) {

    private fun calib2xPixel(calibX: Int): Int = scale2int(calibX, imgWidth)
    private fun calib2yPixel(calibY: Int): Int = scale2int(calibY, imgHeight)

    private val meter1Pixel = calib2yPixel(meter1.height)
    private val meter2Pixel = calib2yPixel(meter2.height)

    fun xPixel2distance(xPixel: Int, yPixel: Int): Double {
        val x = (CALIBRATION_MAX * xPixel / imgWidth) - (CALIBRATION_MAX / 2)
        val y = CALIBRATION_MAX * yPixel / imgHeight
        val widthAtY = widthAt(y)
        return x.toDouble() / widthAtY
    }

    fun yPixel2distance(y: Int): Double {
        return when {
            y >= meter1Pixel -> {
                val offset = imgHeight - y
                val range = imgHeight - meter1Pixel
                offset.toDouble() / range.toDouble()
            }
            y >= meter2Pixel -> {
                val offset = meter1Pixel - y
                val range = meter1Pixel - meter2Pixel
                1.0 + offset.toDouble() / range.toDouble()
            }
            else -> {
                MAX_DISTANCE_METERS
            }
        }
    }

    private fun widthAt(height: Int): Double {
        val left = solveForX(height, meter1.xLeft(), meter1.height, meter2.xLeft(), meter2.height)
        val right = solveForX(height, meter1.xRight(), meter1.height, meter2.xRight(), meter2.height)
        return right - left
    }
}

