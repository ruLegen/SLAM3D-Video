package com.mag.slam3dvideo.utils


object Theme {
    fun createRoundRectDrawable(rad: kotlin.Int, defaultColor: kotlin.Int): android.graphics.drawable.Drawable {
        val defaultDrawable: android.graphics.drawable.ShapeDrawable = android.graphics.drawable.ShapeDrawable(android.graphics.drawable.shapes.RoundRectShape(kotlin.floatArrayOf(rad.toFloat(), rad.toFloat(), rad.toFloat(), rad.toFloat(), rad.toFloat(), rad.toFloat(), rad.toFloat(), rad.toFloat()), null, null))
        defaultDrawable.paint.color = defaultColor
        return defaultDrawable
    }
    fun createRoundRectDrawable(topRad: kotlin.Int, bottomRad: kotlin.Int, defaultColor: kotlin.Int): android.graphics.drawable.Drawable {
        val defaultDrawable: android.graphics.drawable.ShapeDrawable = android.graphics.drawable.ShapeDrawable(android.graphics.drawable.shapes.RoundRectShape(kotlin.floatArrayOf(topRad.toFloat(), topRad.toFloat(), topRad.toFloat(), topRad.toFloat(), bottomRad.toFloat(), bottomRad.toFloat(), bottomRad.toFloat(), bottomRad.toFloat()), null, null))
        defaultDrawable.paint.color = defaultColor
        return defaultDrawable
    }
}

