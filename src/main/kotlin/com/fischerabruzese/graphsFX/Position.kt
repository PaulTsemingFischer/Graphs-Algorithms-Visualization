package com.fischerabruzese.graphsFX

open class Position(x: Double,
                    y: Double,
                    bounds: Boolean = true,
                    private val upperBound: Double = if(!bounds) Double.MAX_VALUE else 1.0,
                    private val lowerBound: Double = if(!bounds) Double.MIN_VALUE else 0.0
) {

    open val x: Double = constrain(x)
    //get() = _x
        //set(value) {constrain(value)}

    open val y: Double = constrain(y)
    //get() = _y
        //set(value) {constrain(value)}


    operator fun component1() = x
    operator fun component2() = y

    protected fun constrain(value: Double) = when {
        (value > upperBound) -> upperBound
        (value < lowerBound) -> lowerBound
        else -> value
    }

    /** Adding will return a [Displacement] if either Position is a Displacement */
    open operator fun plus(other: Position): Position {
        return if (this is Displacement || other is Displacement)
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
class Displacement(private val _x: Double,
                   private val _y: Double,
): Position(_x, _y, false) {
    override var x: Double
        get() = _x
        set(value) {constrain(value)}

    override var y: Double
        get() = _y
        set(value) {constrain(value)}

    init {
        x = _x
        y = _y
    }

    operator fun plusAssign(other: Position) {
        x += other.x
        y += other.y
    }
}
