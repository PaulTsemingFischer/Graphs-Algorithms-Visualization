package com.fischerabruzese.graphsFX

import kotlin.math.max
import kotlin.math.min

open class Position(x: Double, y: Double) {
    open val x: Double by lazy { constrain(x) }
    open val y: Double by lazy { constrain(y) }

    operator fun component1() = x
    operator fun component2() = y

    protected open fun constrain(value: Double) = when {
        (value > 1) -> 1.0
        (value < 0) -> 0.0
        else -> value
    }

    /** Adding will return a [Position] if either is a Position */
    open operator fun plus(other: Position): Position {
        return if (this is Displacement || other is Displacement)
            Displacement(x + other.x, y + other.y)
        else Position(x + other.x, y + other.y)
    }

    operator fun minus(other: Position): Position {
        return if (javaClass != other.javaClass)
            Position(other.x - x, other.y - y)
        else Displacement(other.x - x, other.y - y)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Position
        return x == other.x && y == other.y
    }

    override fun hashCode(): Int {
        var result = x.hashCode()
        result = 31 * result + y.hashCode()
        return result
    }
}

/** Equivalent to [Position] but with no bounds and mutable */
class Displacement(
    override var x: Double,
    override var y: Double,
    private val enforceMaximum: Double = Double.MAX_VALUE,
    private val enforceMinimum: Double = Double.MIN_VALUE
) : Position(x, y) {
    override fun constrain(value: Double) = when {
        (value > enforceMaximum) -> enforceMaximum
        (value < enforceMinimum) -> enforceMinimum
        else -> value
    }

    fun constrainBetween(max: Double, min: Double): Boolean {
        val orgX = x
        val orgY = y
        x = max(min(x, max), min)
        y = max(min(y, max), min)
        return (orgX != x || orgY != y)
    }

    operator fun plusAssign(other: Position) {
        x += other.x
        y += other.y
    }
}