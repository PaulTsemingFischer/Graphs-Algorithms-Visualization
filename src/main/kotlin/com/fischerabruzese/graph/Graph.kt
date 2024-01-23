package com.fischerabruzese.graph

abstract class Graph<E : Any> : Iterable<E> {

    abstract fun size() : Int
    abstract operator fun get(from : E, to : E) : Int?
    abstract operator fun set(from : E, to : E, value : Int) : Int?
    abstract fun getVertices() : Set<E>
    abstract fun add(vararg verts : E)
    abstract fun remove(vararg verts : E)
    abstract fun path(from : E, to : E) : List<E>?
    abstract fun distance(from : E, to : E) : Int
}