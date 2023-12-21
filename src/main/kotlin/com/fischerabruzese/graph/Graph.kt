package com.fischerabruzese.graph

abstract class Graph<E : Any> : Iterable<E> {

    abstract operator fun get(from : E, to : E) : Int?
    abstract operator fun set(from : E, to : E, value : Int) : Int?
    abstract fun getVerticies() : Set<E>

}