package com.fischerabruzese.graph

import java.util.*
import kotlin.math.ln
import kotlin.random.Random
@Suppress("unused")

abstract class Graph<E : Any> : Iterable<E> {

    /*---------------- FUNCTIONALITY ----------------*/
    /**
     * @return The number of vertices in the graph.
     */
    open fun size() : Int{
        return getVertices().size
    }

    /**
     * @param from The vertex to start from.
     * @param to The vertex to end at.
     * @return The weight of the edge between the two vertices, or null if no edge exists.
     *
     * @throws NoSuchElementException if the one or more of the elements do not exist in the graph
     */
    abstract operator fun get(from : E, to : E) : Int?

    /**
     * @param from The vertex to start from.
     * @param to The vertex to end at.
     * @param value The weight to set on the edge between the two vertices.
     * @return The previous weight of the edge between the two vertices, or null if no edge existed.
     */
    abstract operator fun set(from : E, to : E, value : Int) : Int?

    /**
     * @return A set of the vertices in the graph.
     */
    abstract fun getVertices() : Set<E>

    /**
     * @return A set of the Edges in the graph.
     */
    abstract fun getEdges() :  Set<Pair<E,E>>

    /**
     * @return An iterator over the vertices in the graph. The order is not guaranteed.
     */
    override fun iterator(): Iterator<E> {
        return getVertices().iterator()
    }

    /**
     * Adds a vertex to the graph.
     * @param verts The vertices to add to the graph.
     */
    fun add(vararg verts : E){
        addAll(verts.toList())
    }

    /**
     * Adds a collection of vertices to the graph.
     * @param verts The collection of vertices to add to the graph.
     */
    abstract fun addAll(verts : Collection<E>)

    /**
     * Removes a vertex from the graph.
     * @param verts The vertices to remove from the graph.
     */
    fun remove(vararg verts : E){
        removeAll(verts.toList())
    }

    /**
     * Removes a collection of vertices from the graph.
     * @param verts The collection of vertices to remove from the graph.
     */
    abstract fun removeAll(verts : Collection<E>)

    /**
     * Removes the edge from the given vertices
     * @param from the source of the edge
     * @param to the destination of the ege
     * @return the weight of the edge removed, null if it didn't exist
     */
    abstract fun remove(from: E, to: E): Int?

    /**
     * Removes any edges between the given vertices
     * @param v1 one end of the edges
     * @param v2 the other end of the edges
     * @return the number of edges removed
     */
    fun disconnect(v1: E, v2: E) : Int {
        var numDisconnected = 0
        if(remove(v1,v2) != null) numDisconnected++
        if(remove(v2,v1) != null) numDisconnected++
        return numDisconnected
    }

    /**
     * Removes all the connection in the graph.
     */
    open fun clearConnections() {
        for(from in this){
            for(to in this){
                remove(from, to)
            }
        }
    }

    /**
     * Checks if the given vertex is already in the graph
     * @param vertex the vertex to check
     * @return true if the vertex exists in the graph
     */
    abstract fun contains(vertex: E): Boolean

    /**
     * @param vertex the source vertex of the neighbors
     * @return a collection of vertices that are connected to the given vertex
     */
    open fun neighbors(vertex: E): Collection<E> {
        val neighbors = mutableListOf<E>()
        val vertices = getVertices()
        for (vert in vertices){
            if (get(vertex, vert) != -1) neighbors.add(vert)
        }
        return neighbors
    }

    abstract fun bidirectionalConnections(v1: E, v2: E): Int

    /**
     * @param vertex the source of the collection
     * @return a collection of every vertex reachable from [vertex]
     */
    abstract fun getConnected(vertex: E): Collection<E>

    /**
     * @return a new graph identical to this
     */
    abstract fun copy(): Graph<E>

    /**
     * Returns a graph containing only the subset of vertices specified. Maintains all connections to vertices within the specified subset.
     * @param verts the subset of vertices the subgraph should contain
     * @return a new graph containing only the specified vertices
     */
    abstract fun subgraph(verts: Collection<E>):Graph<E>

    fun union(other: Graph<E>) {
        for(v in other){
            add(v)
        }
        for((f,t) in other.getEdges()){
            other[f, t]?.let { this[f,t] = it }
        }
    }

    /**
     * @param predicate a lambda that evaluates whether to include the given vertex
     * @return a new graph containing only vertices matching the given predicate.
     */
    inline fun filter(predicate: (vertex: E) -> Boolean) : Graph<E>{
        return subgraph(getVertices().filter{predicate(it)})
    }

    open fun<R : Any> mapVertices(transform: (vertex: E) -> R) : Graph<R> {
        val graph = AMGraph<R>()
        graph.addAll(this.map(transform))
        for(f in this){
            for(t in this){
                if(this[f,t] != null){
                    graph[transform(f),transform(t)] = this[f,t]!!
                }
            }
        }
        return graph
    }

    override fun hashCode(): Int {
        return Objects.hash(getVertices(), getEdges())
    }

    /**
     * @return true if the graphs contain the same vertices with the same connections and is the same type of graph
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Graph<*>) return false
        if (getVertices() != other.getVertices()) return false
        if (getEdges() != other.getEdges()) return false
        return true
    }

    override fun toString(): String{
        return getEdges().toString()
    }
    /*---------------- RANDOMIZATION ----------------*/
    /**
     * Sets random connections between vertices between 0 and maxWeight.
     * @param probability The probability of a connection being made. Must be between 0 and 1. Only accounts for initial connections formed, not any additional ones to prevent disjoint vertices.
     * @param minWeight The minimum weight of a connection(inclusive).
     * @param maxWeight The maximum weight of a connection(exclusive).
     * @param allowDisjoint Makes sure that there's always at least one connection per vertex
     * @param random A random object that determines what graph is constructed
     */
    open fun randomize(probability: Double, minWeight: Int, maxWeight: Int, allowDisjoint: Boolean = true, random: Random = Random){
        for (from in this) {
            for (to in this) {
                if (random.nextDouble() < probability) {
                    set(from, to, random.nextInt(1,maxWeight))
                } else {
                    set(from, to, -1)
                }
            }
        }
        if(!allowDisjoint) mergeDisjoint(minWeight, maxWeight, random)
    }
    fun randomize(probability: Double, maxWeight: Int, allowDisjoint: Boolean = true, random: Random = Random) = randomize(probability, 1, maxWeight, allowDisjoint, random)

    //TODO: Consider refactoring to new class
    /**
     * Randomizes the edges in a graph to create clusters. [interClusterConnectedness]/[intraClusterConnectedness] is essentially the clusteriness of the graph.
     * @param numClusters The number of clusters to split the graph into.
     * @param maxEdgeWeight The maximum weight of a connection(exclusive).
     * @param intraClusterConnectedness The probability of a connection being made within a cluster. Must be between 0 and 1.
     * @param interClusterConnectedness The probability of a connection being made between clusters. Must be between 0 and 1.
     * @param random A random object that determines what graph is constructed
     */
    fun randomizeWithCluster(numClusters: Int,
                             minEdgeWeight: Int,
                             maxEdgeWeight: Int,
                             intraClusterConnectedness: Double,
                             interClusterConnectedness: Double,
                             random: Random = Random) {
        var remainingVertices = LinkedList(getVertices())
        val clusters = LinkedList<Graph<E>>()

        val vertsPerCluster = size()/numClusters

        for(cluster in 0 until numClusters-1){
           val size = random.nextInt(
                vertsPerCluster - (size()/10),
                vertsPerCluster + (size()/10) + 1
            ).coerceIn(1 until remainingVertices.size - (numClusters-1 - cluster)) //ensure we have enough for numClusters

            clusters += AMGraph.fromCollection(remainingVertices.take(size)).apply {
                randomize(intraClusterConnectedness, minEdgeWeight, maxEdgeWeight, true, random)
            }
            remainingVertices = LinkedList(remainingVertices.subList(size, remainingVertices.size))
        }
        clusters += AMGraph.fromCollection(remainingVertices).apply {
            randomize(intraClusterConnectedness, minEdgeWeight, maxEdgeWeight, true, random)
        }

        val mergedGraph = AMGraph<E>()
        for(g in clusters){
            mergedGraph.union(g)
        }

        for(fromCluster in clusters){
            for(fromVertex in fromCluster){
                for(toCluster in clusters) {
                    if(toCluster == fromCluster) continue //don't do an innerConnection
                    for (toVertex in toCluster) {
                        if (random.nextDouble() < interClusterConnectedness){
                            mergedGraph[fromVertex, toVertex] = random.nextInt(minEdgeWeight, maxEdgeWeight)
                        }
                    }
                }
            }
        }

        becomeCloneOf(mergedGraph)
    }

    //TODO: javadoc
    fun becomeCloneOf(graph: Graph<E>){
        clearConnections()
        removeAll(getVertices())
        union(graph)
    }

    /**
     *  Adds edges to the current so that the graph is not disjoint. Randomly adds edges that will merge 2 disjoint sections until everything is joined.
     *  @param maxWeight the highest value weight these edges should connect with
     *  @param random the random object that determines which edges get added and the weights
     */
    open fun mergeDisjoint(minWeight: Int, maxWeight: Int, random: Random = Random){
        val vertices = getVertices()
        val bidirectional = getBidirectionalUnweighted()
        var src = vertices.random()
        var unreachables : List<E>

        //Runs while there are any unreachable vertices from `vertex` (store all the unreachable ones in `unreachables`)
        //Vertices --> Unreachable non-self vertices --> Unreachable non-self id's
        while(vertices.filter { dest -> dest != src && bidirectional.path(src, dest).isEmpty() }.also{ unreachables = it }.isNotEmpty()){
            val weight = random.nextInt(minWeight, maxWeight)

            val (from, to) =
                if (random.nextBoolean())
                    src to unreachables.random(random)
                else unreachables.random(random) to src

            set(from,to,weight)
            bidirectional[from, to] = 1
            bidirectional[to, from] = 1

            src = vertices.random()
            unreachables = emptyList()
        }
    }

    /**
     * @return A copy of this graph with every connection being bidirectional with a weight of 1
     */
    protected fun getBidirectionalUnweighted() : Graph<E>{
        val newGraph = AMGraph<E>()
        for(v in this) newGraph.add(v)
        for(src in this){
            for(dest in this){
                if(get(src, dest) != null){
                    newGraph[src, dest] = 1
                    newGraph[dest, src] = 1
                }
            }
        }
        return newGraph
    }

    /*---------------- PATHING ----------------*/
    /**
     * Finds the shortest path between two vertices using dijkstra's algorithm.
     * @param from The vertex to start from.
     * @param to The vertex to end at.
     * @return A list of vertices representing the shortest path between the two vertices.
     */
    abstract fun path(from : E, to : E) : List<E>

    /**
     * Finds the shortest distance between two vertices using dijkstra's algorithm.
     * @param from The vertex to start from.
     * @param to The vertex to end at.
     * @return The distance between the two vertices.
     */
    abstract fun distance(from : E, to : E) : Int

    /*---------------- CLUSTERING ----------------*/

    /**
     * Uses Highly Connected Subgraph (with Kargers for finding min-cut) algorithm to find clusters in this graph
     * @param connectedness the minimum proportion of vertices that must be connected to form a cluster
     * @param kargerness the number of probabilistic attempts at finding the min-cut before the best option is taken
     * @return a collection of subgraphs that are the clusters of this graph
     */
    abstract fun getClusters(connectedness: Double = 0.5, kargerness: Int = 1000): Collection<Graph<E>>

    /**
     * @see getClusters
     * @param confidence the desired probability of finding the min-cut during each iteration of Kargers algorithm
     */
    open fun getClusters(connectedness: Double = 0.25, confidence: Double){
        require(confidence < 1.0 && confidence > 0.0)

        fun calculateNumRuns(n: Int, pDesired: Double): Int {
            val p = 1.0 / (n * n / 2 - n / 2)
            val t = ln(1 - pDesired) / ln(1 - p)
            return t.toInt()
        }

        getClusters(connectedness, calculateNumRuns(size(), 1.0 - confidence))
    }
}