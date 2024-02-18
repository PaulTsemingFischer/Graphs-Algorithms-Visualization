package com.fischerabruzese.graph

import kotlin.random.Random

abstract class Graph<E : Any> : Iterable<E> {

    abstract fun randomize(probability: Double, maxWeight: Int, fullyConnected: Boolean = false, random: Random = Random)
    abstract fun randomize(avgConnectionsPerVertex: Int, maxWeight: Int, fullyConnected: Boolean = false, random: Random = Random)
    abstract fun size() : Int
    abstract operator fun get(from : E, to : E) : Int?
    abstract operator fun set(from : E, to : E, value : Int) : Int?
    abstract fun getVertices() : Set<E>
    abstract fun add(vararg verts : E)
    abstract fun remove(vararg verts : E)
    abstract fun contains(vertex: E): Boolean

    open fun neighbors(vertex: E): List<E>? {
        val neighbors = mutableListOf<E>()
        val vertices = getVertices()
        for (vert in vertices){
            if (get(vertex, vert) != -1) neighbors.add(vert)
        }
        return neighbors
    }

    abstract fun path(from : E, to : E) : List<E>?
    abstract fun distance(from : E, to : E) : Int
}