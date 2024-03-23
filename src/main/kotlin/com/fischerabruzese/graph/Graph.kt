package com.fischerabruzese.graph

import java.util.*
import kotlin.math.ln
import kotlin.random.Random
@Suppress("unused")

/**
 * Represents a **mutable** graph data structure. The graph may contain cycles,
 * and is therefore not strictly a DAG. The graph permits [Any] elements as the vertex
 * type and does **not** permit nullable types.
 *
 * Implementations can store vertices in an Adjacency Matrix, Adjacency List,
 * or a different way.
 *
 * All implimentation of Graphs contain the following algorithms that
 * are either defined here (and are open for optimization in subclasses) or
 * must be defined in its entirety in the specific implementation.
 *
 * Graph's contain methods for clustering and pathing, including:
 *  * Dijkstra's Algorithm
 *  * Breadth First Search
 *  * Depth First Search
 *  * HCS (Highly Connected Subgraphs)
 *
 * @author Skylar Abruzese
 * @author Paul Fischer
 */
abstract class Graph<E : Any> : Iterable<E> {

    /*---------------- FUNCTIONALITY ----------------*/
    /**
     * @return The number of vertices in the graph.
     */
    open fun size() : Int {
        return getVertices().size
    }

    /**
     * The edge getter for graphs.
     *
     * @param from The source of the edge.
     * @param to The destination of the edge.
     * @return The weight of the directed edge between the two vertices, or null if no edge exists.
     * @throws NoSuchElementException if the one or more of the elements do not exist in the graph
     */
    abstract operator fun get(from : E, to : E) : Int?

    /**
     * @param from The vertex to start from.
     * @param to The vertex to end at.
     * @param value The weight to set on the edge between the two vertices.
     * @return The previous weight of the edge between the two vertices, or null if no edge existed.
     * @throws NoSuchElementException if [from] or [to] aren't in this graph
     */
    abstract operator fun set(from : E, to : E, value : Int) : Int?

    /**
     * @return a set of the vertices in the graph.
     */
    abstract fun getVertices(): Set<E>

    /**
     * @return a set of the directed Edges (source to destination) in the graph.
     */
    abstract fun getEdges():  Set<Pair<E,E>>

    /**
     * @return An iterator over the vertices in the graph. No order is guaranteed.
     */
    override fun iterator(): Iterator<E> {
        return getVertices().iterator()
    }

    /**
     * Adds a new, unconnected vertex to the graph. Will skip any vertex that is already present in the graph
     * @param vertex The vertex to add to the graph
     * @return true if the vertex is successfully added to the graph and false if the vertex was already present in the graph.
     */
    fun add(vararg vertex: E): Boolean {
        return addAll(vertex.toList()).isNotEmpty()
    }

    /**
     * Adds a collection of vertices to the graph. Will skip all vertices that are already present in the graph
     * @param vertices The collection of vertices to add to the graph.
     * @return a collection of all elements that already existed in this graph
     */
    abstract fun addAll(vertices: Collection<E>): Collection<E>

    /**
     * Removes a vertex from the graph and removes all edges it was a part of.
     * @param vertex The vertices to remove from the graph.
     * @return true if the vertex is successfully removed from the graph and false if the vertex is not present in the graph.
     */
    fun remove(vararg vertex : E): Boolean {
        return removeAll(vertex.toList()).isNotEmpty()
    }

    /**
     * Removes all vertices in a collection from the graph and removes all edges they were a part of. Ignores any vertices that already don't exist.
     * @param vertices The collection of vertices to remove from the graph.
     * @return a collection of the vertices that were already not present in the graph
     */
    abstract fun removeAll(vertices : Collection<E>): Collection<E>

    /**
     * Removes an edge from the given vertices
     * @param from the source of the edge
     * @param to the destination of the edge
     * @return the weight of the edge removed, null if edge already didn't exist
     * @throws IllegalArgumentException if either the [source][from] or [destination][to] don't exist in this graph
     */
    abstract fun removeEdge(from: E, to: E): Int?

    /**
     * Removes all edges between the given vertices
     * @param v1 one end of the edges
     * @param v2 the other end of the edges
     * @return the number of edges removed
     */
    fun disconnect(v1: E, v2: E) : Int {
        var numDisconnected = 0
        if(removeEdge(v1,v2) != null) numDisconnected++
        if(removeEdge(v2,v1) != null) numDisconnected++
        return numDisconnected
    }

    /**
     * Removes all edges in this graph.
     */
    open fun clearEdges() {
        for(from in this){
            for(to in this){
                removeEdge(from, to)
            }
        }
    }

    /**
     * Checks if the given vertex is already in the graph
     * @param vertex the vertex to check
     * @return true if the graph contains this vertex false if the graph doesn't contain this vertex
     */
    abstract fun contains(vertex: E): Boolean

    /**
     * Retrieves all the vertices that [source] has an outbound connection to
     * @param source the source vertex of the neighbors
     * @return a collection of vertices that are connected to the given vertex
     * @throws NoSuchElementException if [source] is not in this graph
     */
    open fun neighbors(source: E): Collection<E> {
        val neighbors = mutableListOf<E>()
        val vertices = getVertices()
        for (vert in vertices){
            if (get(source, vert) != null) neighbors.add(vert) //get will throw the error if source doesn't exist
        }
        return neighbors
    }

    /**
     * @return the amount of connection between 2 vertices. Always between 0 and 2.
     * @throws NoSuchElementException if [v1] or [v2] doesn't exist in this graph
     */
    abstract fun countEdgesBetween(v1: E, v2: E): Int

    /**
     * Retrieve the vertices reachable from [source] via [path]
     * @param source the source that the returned vertices are reachable from
     * @return a collection of every vertex reachable from [source]
     * @throws IllegalArgumentException if [source] doesn't exist in this graph
     */
    abstract fun getConnected(source: E): Collection<E>

    /**
     * Note that this does **not** preform a deep copy and the objects themselves are **not** copied. Reference's will still point to the same object.
     * @return a new graph identical to this one.
     */
    open fun copy(): Graph<E>{
        return subgraph(getVertices())
    }

    /**
     * Returns a graph containing only a subset of vertices. Maintains all connections to vertices within the specified subset.
     *
     * Note that the objects themselves are **not** copied onto the subgraph. Reference's will still reference the same object.
     *
     * @param vertices the subset of vertices the subgraph should contain
     * @return a new graph containing only the specified vertices
     * @throws IllegalArgumentException if any [vertices] does not exist in this graph
     */
    abstract fun subgraph(vertices: Collection<E>):Graph<E>

    /**
     * Joins [other] on to this graph including edges.
     * Any vertices in [other] that are also in this graph will not be added, but connections to those vertices will still be added.
     * @param other the graph to add to this graph
     */
    fun union(other: Graph<E>) {
        addAll(other.getVertices())
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

    /**
     * Produces a new graph identical to this one, but where each vertex is modified with the given transform
     * @param transform the transform to apply to each vertex
     * @param R the type of elements this graph's vertices will be transformed to and that will be in the new graph
     * @return a clone of this graph with the given transform
     */
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

            clusters += AMGraph(remainingVertices.take(size)).apply {
                randomize(intraClusterConnectedness, minEdgeWeight, maxEdgeWeight, true, random)
            }
            remainingVertices = LinkedList(remainingVertices.subList(size, remainingVertices.size))
        }
        clusters += AMGraph(remainingVertices).apply {
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
        clearEdges()
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
     * Used for clustering
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
     * Uses Highly Connected Subgraph (with Kargers for finding min-cut) algorithm to find clusters in this graph. Implementation may or may not merge singletons TODO:FIX
     * @param connectedness the minimum proportion of vertices that must be connected to form a cluster
     * @param kargerness the number of probabilistic attempts at finding the min-cut before the best option is taken
     * @return a collection of subgraphs that are the clusters of this graph
     * @throws InterruptedException since clustering is an expensive algorithm it will throw an exception when the thread is interrupted during calculation instead of completing the calculation
     */
    open fun getClusters(connectedness: Double = 0.5, kargerness: Int = 1000): Collection<Graph<E>>{
        return mergeSingletons(highlyConnectedSubgraphs(connectedness, kargerness))
    }

    /**
     * TODO: WRITE THIS JAVADOC
     */
    abstract fun highlyConnectedSubgraphs(connectedness: Double = 0.5, kargerness: Int = 1000): Collection<Graph<E>>

    private fun mergeSingletons(clusters: Collection<Graph<E>>):  Collection<Graph<E>>{
        val clusters = ArrayList(clusters)

        //singletons to remove
        val removeQueue = LinkedList<Graph<E>>()

        //for each singleton
        for (cluster in clusters) {
            if (Thread.currentThread().isInterrupted) throw InterruptedException()
            if (cluster.size() != 1) continue //not a singleton

            val singleton = cluster.getVertices().first()

            //aka hcc
            var highestConnectedCluster = cluster
            var hccInbounds = LinkedList<E>()
            var hccOutbounds = LinkedList<E>()

            //find hcc
            for (neighborCluster in clusters) {
                if (neighborCluster === cluster) continue

                val obConnections = LinkedList<E>()
                val ibConnections = LinkedList<E>()
                for (v in neighborCluster) {
                    if (this[v, singleton] != null) ibConnections.add(v)
                    if (this[singleton, v] != null) obConnections.add(v)
                }

                val highestConnectedness = hccInbounds.size + hccOutbounds.size
                val newConnectedness = ibConnections.size + obConnections.size
                if (newConnectedness > highestConnectedness || (newConnectedness == highestConnectedness && neighborCluster.size() > highestConnectedCluster.size())) {
                    hccInbounds = ibConnections
                    hccOutbounds = obConnections
                    highestConnectedCluster = neighborCluster
                }
            }

            //if the singleton has no connections to another cluster and is still the default value, do nothing
            if (highestConnectedCluster === cluster) continue

            //merge singleton into hcc
            highestConnectedCluster.add(singleton)
            for (ib in hccInbounds) {
                highestConnectedCluster[ib, singleton] = this[ib, singleton]!!
            }
            for (ob in hccOutbounds) {
                highestConnectedCluster[singleton, ob] = this[singleton, ob]!!
            }

            //remove singleton cluster
            removeQueue.add(cluster)
        }
        clusters.removeAll(removeQueue)

        return clusters
    }

    /**
     * @see getClusters
     * @param confidence the desired probability of finding the min-cut during each iteration of Kargers algorithm
     * @throws InterruptedException since clustering is an expensive algorithm it will throw an exception when the thread is interrupted during calculation instead of completing the calculation
     * @throws IllegalArgumentException if confidence is not between 0.0 and 1.0 exclusive
     */
    @Deprecated("Confidence calculations are severely inaccurate", ReplaceWith("getClusters(kargerness: Int)"), DeprecationLevel.WARNING)
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