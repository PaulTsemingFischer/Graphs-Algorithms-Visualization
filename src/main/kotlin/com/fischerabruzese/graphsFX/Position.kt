package com.fischerabruzese.graphsFX

open class Position(x: Double, y: Double) {

    open val x: Double = constrain(x)
    open val y: Double = constrain(y)

    operator fun component1() = x
    operator fun component2() = y

    protected open fun constrain(value: Double) = when {
        (value > 1) -> 1.0
        (value < 0) -> 0.0
        else -> value
    }

    /** Adding will return a [Position] if either is a Position */
    open operator fun plus(other: Position): Position {
        return if (this is Displacement && other is Displacement)
            Displacement(x + other.x, y + other.y)
        else Position(x + other.x, y + other.y)
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
class Displacement(override var x: Double, override var y: Double, val forceCapPerPos: Double, val forceCapPerNeg: Double): Position(x, y){
    override fun constrain(value: Double) = when {
        (value > forceCapPerPos) -> forceCapPerPos
        (value < forceCapPerNeg) -> forceCapPerNeg
        else -> value
    }

    override operator fun plus(other: Position): Displacement {
        x += other.x
        y += other.y
        return this
    }
}