package com.fischerabruzese.graphsFX

class Position(private var _x: Double, private var _y: Double) {

    const val UPPERBOUND = 0.1
    const val LOWERBOUND = 0.0

    var x: Double
        get() = _x
        set(value) {constrain(value)}

    var y: Double
        get() = _y
        set(value) {constrain(value)}

    operator fun component1() = x
    operator fun component2() = y

    private fun constrain(value: Double) = when {
        (value > UPPERBOUND) -> UPPERBOUND
        (value < LOWERBOUND) -> LOWERBOUND
        else -> value
    }

    fun addConstrained(other: Position): Position {
        return Position(x + other.x, y + other.y)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Position
        return x == other.x && y == other.y
    }
}
