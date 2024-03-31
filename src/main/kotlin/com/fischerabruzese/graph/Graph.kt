package com.fischerabruzese.graph

import java.io.Serializable
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Represents a **mutable** graph data structure. A graph is directed and does
 * not permit negative edge weights.The graph may contain cycles, and is
 * therefore not strictly a DAG. The graph permits [Any] elements as the vertex
 * type and does **not** permit nullable types.
 *
 * Implementations can store vertices in an Adjacency Matrix, Adjacency List,
 * or a different way.
 *
 * All implementation of Graphs contain the following algorithms that
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
abstract class Graph<E : Any> : Iterable<E>, Serializable {

    /*---------------- FUNCTIONALITY ----------------*/
    /**
     * @return The number of vertices/elements in the graph.
     */
    open fun size(): Int {
        return getVertices().size
    }

    /**
     * The edge getter for graphs.
     *
     * @param from The source of the edge
     * @param to The destination of the edge
     *
     * @return The weight of the directed edge between the two vertices, or
     *         null if no edge exists
     *
     * @throws NoSuchElementException if the one or more of the elements do not
     *         exist in the graph
     */
    abstract operator fun get(from: E, to: E): Int?

    /**
     * Sets the weight of the given edge.
     *
     * @param from The vertex to start from
     * @param to The vertex to end at
     * @param value The weight to set on the edge between the two vertices
     *
     * @return The previous weight of the edge between the two vertices, or
     *         null if no edge existed.
     *
     * @throws NoSuchElementException if [from] or [to] aren't in this graph
     */
    abstract operator fun set(from: E, to: E, value: Int): Int?

    /**
     * @return a set of the vertices in the graph.
     */
    abstract fun getVertices(): Set<E>

    /**
     * @return a set of the directed Edges (source to destination) in the
     *         graph.
     */
    abstract fun getEdges(): Set<Pair<E, E>>

    /**
     * @return An iterator over the vertices in the graph. No order is
     *         guaranteed.
     */
    override fun iterator(): Iterator<E> {
        return getVertices().iterator()
    }

    /**
     * Adds a new, unconnected vertex to the graph. Will skip any vertex that
     * is already present in the graph.
     *
     * @param vertex The vertex to add to the graph
     *
     * @return true if the vertex is successfully added to the graph and false
     *         if the vertex was already present in the graph
     */
    fun add(vararg vertex: E): Boolean {
        return addAll(vertex.toList()).isNotEmpty()
    }

    /**
     * Adds a collection of vertices to the graph. Will skip all vertices that
     * are already present in the graph.
     *
     * @param vertices The collection of vertices to add to the graph
     *
     * @return a collection of all elements that already existed in this graph
     */
    abstract fun addAll(vertices: Collection<E>): Collection<E>

    /**
     * Removes a vertex from the graph and removes all edges it was a part of.
     *
     * @param vertex The vertices to remove from the graph
     *
     * @return true if the vertex is successfully removed from the graph and
     *         false if the vertex is not present in the graph
     */
    fun remove(vararg vertex: E): Boolean {
        return removeAll(vertex.toList()).isNotEmpty()
    }

    /**
     * Removes all vertices in a collection from the graph and removes all
     * edges they were a part of. Ignores any vertices that already don't
     * exist.
     *
     * @param vertices the collection of vertices to remove from the graph
     *
     * @return a collection of the vertices that were already not present in
     *         the graph
     */
    abstract fun removeAll(vertices: Collection<E>): Collection<E>

    /**
     * Removes an edge from the given vertices.
     *
     * @param from the source of the edge
     * @param to the destination of the edge
     *
     * @return the weight of the edge removed, null if edge already didn't
     *         exist
     *
     * @throws IllegalArgumentException if [from] or [to] don't exist in this
     *         graph
     */
    abstract fun removeEdge(from: E, to: E): Int?

    /**
     * Removes all edges between the given vertices.
     *
     * @param v1 one end of the edges
     * @param v2 the other end of the edges
     *
     * @return the number of edges removed
     */
    fun disconnect(v1: E, v2: E): Int {
        var numDisconnected = 0
        if (removeEdge(v1, v2) != null) numDisconnected++
        if (removeEdge(v2, v1) != null) numDisconnected++
        return numDisconnected
    }

    /**
     * Removes all edges in this graph.
     */
    open fun clearEdges() {
        for (from in this) {
            for (to in this) {
                removeEdge(from, to)
            }
        }
    }

    /**
     * Checks if the given vertex is already in the graph
     *
     * @param vertex the vertex to check
     *
     * @return true if the graph contains this vertex false if the graph
     *         doesn't contain this vertex
     */
    abstract fun contains(vertex: E): Boolean

    /**
     * Retrieves all the vertices that [source] has an outbound connection to
     *
     * @param source the source vertex of the neighbors
     *
     * @return a collection of vertices that are connected to the given vertex
     *
     * @throws NoSuchElementException if [source] is not in this graph
     */
    open fun neighbors(source: E): Collection<E> {
        val neighbors = mutableListOf<E>()
        val vertices = getVertices()
        for (vert in vertices) {
            if (get(source, vert) != null) neighbors.add(vert) //get will throw the error if source doesn't exist
        }
        return neighbors
    }

    /**
     * @return the amount of connection between 2 vertices. Always between 0
     *         and 2.
     *
     * @throws NoSuchElementException if [v1] or [v2] doesn't exist in this
     *         graph
     */
    abstract fun countEdgesBetween(v1: E, v2: E): Int

    /**
     * Retrieve the vertices reachable from [source] via [path].
     *
     * @param source the source that the returned vertices are reachable from
     *
     * @return a collection of every vertex reachable from [source]
     *
     * @throws IllegalArgumentException if [source] doesn't exist in this graph
     */
    abstract fun getConnected(source: E): Collection<E>

    /**
     * Note that this does **not** preform a deep copy and the objects
     * themselves are **not** copied. Reference's will still point to the same
     * object.
     *
     * @return a new graph identical to this one.
     */
    open fun copy(): Graph<E> {
        return subgraph(getVertices())
    }

    /**
     * Returns a graph containing only a subset of vertices. Maintains all
     * connections to vertices within the specified subset.
     *
     * Note that the objects themselves are **not** copied onto the subgraph.
     * Reference's will still reference the same object.
     *
     * @param vertices the subset of vertices the subgraph should contain
     *
     * @return a new graph containing only the specified vertices
     *
     * @throws IllegalArgumentException if any [vertices] does not exist in
     *         this graph
     */
    abstract fun subgraph(vertices: Collection<E>): Graph<E>

    /**
     * Joins [other] on to this graph including edges. Any vertices in [other]
     * that are also in this graph will not be added, but connections to those
     * vertices will still be added.
     *
     * @param other the graph to add to this graph
     */
    fun union(other: Graph<E>) {
        addAll(other.getVertices())
        for ((f, t) in other.getEdges()) {
            other[f, t]?.let { this[f, t] = it }
        }
    }

    /**
     * Builds a filtered graph based on the given [predicate]
     *
     * @param predicate a lambda that evaluates whether to include the given
     *        vertex
     *
     * @return a new graph containing only vertices matching the given
     *         predicate.
     */
    inline fun filter(predicate: (vertex: E) -> Boolean): Graph<E> {
        return subgraph(getVertices().filter { predicate(it) })
    }

    /**
     * Produces a new graph identical to this one, but where each vertex is
     * modified with the given transform
     *
     * @param transform the transform to apply to each vertex
     * @param R the type of elements this graph's vertices will be transformed
     *        to and that will be in the new graph
     *
     * @return a clone of this graph with the given transform
     */
    open fun <R : Any> mapVertices(transform: (vertex: E) -> R): Graph<R> {
        val graph = AMGraph<R>()
        graph.addAll(this.map(transform))
        for (f in this) {
            for (t in this) {
                if (this[f, t] != null) {
                    graph[transform(f), transform(t)] = this[f, t]!!
                }
            }
        }
        return graph
    }

    override fun hashCode(): Int {
        return Objects.hash(getVertices(), getEdges())
    }

    /**
     * @return true if the graphs contain the same vertices with the same
     *         connections and is the same type of graph
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Graph<*>) return false
        if (getVertices() != other.getVertices()) return false
        if (getEdges() != other.getEdges()) return false
        return true
    }

    override fun toString(): String {
        return getEdges().toString()
    }
    /*---------------- RANDOMIZATION ----------------*/
    /**
     * For all possible edges: randomly sets the edge to the given weight with
     * the probability [p][probability]
     *
     * @param probability The probability of a connection being made. Must be
     *        between 0 and 1. Only accounts for initial connections formed,
     *        not any additional ones to prevent disjoint vertices.
     * @param minWeight The minimum weight of a connection(inclusive).
     * @param maxWeight The maximum weight of a connection(exclusive).
     * @param allowDisjoint Makes sure that there's always at least one
     *        connection per vertex
     * @param random A random object that determines what graph is constructed
     */
    open fun randomize(
        probability: Double,
        minWeight: Int,
        maxWeight: Int,
        allowDisjoint: Boolean = true,
        random: Random = Random
    ) {
        for (from in this) {
            for (to in this) {
                if (random.nextDouble() < probability) {
                    set(from, to, random.nextInt(1, maxWeight))
                } else {
                    set(from, to, -1)
                }
            }
        }
        if (!allowDisjoint) mergeDisjoint(minWeight, maxWeight, random)
    }

    /**
     * Equivalent to [randomize] with minWeight = 1
     *
     * @see randomize
     */
    fun randomize(probability: Double, maxWeight: Int, allowDisjoint: Boolean = true, random: Random = Random) =
        randomize(probability, 1, maxWeight, allowDisjoint, random)

    /**
     * [Randomizes][randomize] multiple graphs with
     * p = [intraClusterConnectedness]; Then combines the graph and connects
     * the possible edges between them with p = [interClusterConnectedness].
     *
     * [inter][interClusterConnectedness]/[intra][intraClusterConnectedness] is
     * essentially the clusteriness of the graph.
     *
     * @param numClusters The number of clusters to split the graph into.
     * @param maxEdgeWeight The maximum weight of a connection(exclusive).
     * @param intraClusterConnectedness The probability of a connection being
     *        made within a cluster. Must be between 0 and 1.
     * @param interClusterConnectedness The probability of a connection being
     *        made between clusters. Must be between 0 and 1.
     * @param random A random object that determines what graph is constructed
     */
    fun randomizeWithCluster(
        numClusters: Int,
        minEdgeWeight: Int,
        maxEdgeWeight: Int,
        intraClusterConnectedness: Double,
        interClusterConnectedness: Double,
        random: Random = Random
    ) {
        var remainingVertices = LinkedList(getVertices())
        val clusters = LinkedList<Graph<E>>()

        val vertsPerCluster = size() / numClusters

        for (cluster in 0 until numClusters - 1) {
            val size = random.nextInt(
                vertsPerCluster - (size() / 10),
                vertsPerCluster + (size() / 10) + 1
            )
                .coerceIn(1 until remainingVertices.size - (numClusters - 1 - cluster)) //ensure we have enough for numClusters

            clusters += AMGraph(remainingVertices.take(size)).apply {
                randomize(intraClusterConnectedness, minEdgeWeight, maxEdgeWeight, true, random)
            }
            remainingVertices = LinkedList(remainingVertices.subList(size, remainingVertices.size))
        }
        clusters += AMGraph(remainingVertices).apply {
            randomize(intraClusterConnectedness, minEdgeWeight, maxEdgeWeight, true, random)
        }

        val mergedGraph = AMGraph<E>()
        for (g in clusters) {
            mergedGraph.union(g)
        }

        for (fromCluster in clusters) {
            for (fromVertex in fromCluster) {
                for (toCluster in clusters) {
                    if (toCluster == fromCluster) continue //don't do an innerConnection
                    for (toVertex in toCluster) {
                        if (random.nextDouble() < interClusterConnectedness) {
                            mergedGraph[fromVertex, toVertex] = random.nextInt(minEdgeWeight, maxEdgeWeight)
                        }
                    }
                }
            }
        }

        becomeCloneOf(mergedGraph)
    }

    /**
     * Erases this graph makes its edges and vertices identical to
     * [the other graph][graph].
     *
     * @param graph the graph to become the clone of
     */
    fun becomeCloneOf(graph: Graph<E>) {
        clearEdges()
        removeAll(getVertices())
        union(graph)
    }

    /**
     *  Adds edges to the current so that the graph is not disjoint. Works by
     *  randomly adding edges that will merge 2 disjoint sections until
     *  everything is joined.
     *
     *  @param minWeight the lowest value weight these edges should connect
     *         with
     *  @param maxWeight the highest value weight these edges should connect
     *         with
     *  @param random the random object that determines which edges get added
     *         and the weights
     */
    open fun mergeDisjoint(minWeight: Int, maxWeight: Int, random: Random = Random) {
        val vertices = getVertices()
        val bidirectional = getBidirectionalUnweighted()
        var src = vertices.random()
        var unreachables: List<E>

        //Runs while there are any unreachable vertices from `vertex` (store all the unreachable ones in `unreachables`)
        //Vertices --> Unreachable non-self vertices --> Unreachable non-self id's
        while (vertices.filter { dest -> dest != src && bidirectional.path(src, dest).isEmpty() }
                .also { unreachables = it }.isNotEmpty()) {
            val weight = random.nextInt(minWeight, maxWeight)

            val (from, to) =
                if (random.nextBoolean())
                    src to unreachables.random(random)
                else unreachables.random(random) to src

            set(from, to, weight)
            bidirectional[from, to] = 1
            bidirectional[to, from] = 1

            src = vertices.random()
            unreachables = emptyList()
        }
    }

    /**
     * Clustering helper method. Since there is no undirected implementation of
     * this graph. We imitate one here.
     *
     * @return A copy of this graph with every connection being bidirectional
     *         with a weight of 1
     */
    protected fun getBidirectionalUnweighted(): Graph<E> {
        val newGraph = AMGraph<E>()
        for (v in this) newGraph.add(v)
        for (src in this) {
            for (dest in this) {
                if (get(src, dest) != null) {
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
     *
     * @param from The vertex to start from. Source.
     * @param to The vertex to end at. Destination.
     *
     * @return A list of vertices representing the shortest path between the
     *         two vertices. If no path exists, an empty array otherwise the
     *         list will start the source and end with the destination
     */
    abstract fun path(from: E, to: E): List<E>

    /**
     * Finds a path between two vertices using Depth First Search.
     *
     * @param source The vertex to start from.
     * @param destination The vertex to find/end at.
     *
     * @return A list of vertices representing the discovered path between the
     *         two vertices. If no path exists, an empty array otherwise the
     *         list will start the source and end with the destination
     */
    open fun depthFirstSearch(source: E, destination: E): List<E> {
        return search(true, source, destination)
    }

    /**
     * Finds a path between two vertices using Breadth First Search.
     * This will return the shortest path in terms of number of vertices
     * visited.
     *
     * @param source The vertex to start from.
     * @param destination The vertex to find/end at.
     *
     * @return A list of vertices representing the BFS path between the
     *         two vertices. If no path exists, an empty array otherwise the
     *         list will start the source and end with the destination
     */
    open fun breadthFirstSearch(source: E, destination: E): List<E> {
        return search(false, source, destination)
    }

    protected open fun search(depth: Boolean, source: E, destination: E): List<E> {
        val q = LinkedList<E>()
        val prev = HashMap<E, E>(size())

        q.addFirst(source)
        prev[source] = source

        while (!q.isEmpty()) {
            val curPath = q.pop()

            for (outboundVertex in getConnected(curPath)) {

                if (!prev.contains(outboundVertex) && outboundVertex != curPath) {
                    if (depth)
                        q.addFirst(outboundVertex)
                    else //breadth
                        q.addLast(outboundVertex)
                    prev[outboundVertex] = curPath
                    if (outboundVertex == destination) break
                }
            }
        }
        return LinkedList<E>().apply {
            if (!prev.contains(destination)) return@apply

            var next = prev[destination]!!
            while (next != source) {
                addFirst(next)
                next = prev[next] ?: throw IllegalStateException("Destination found but no path exists")
            }
            addFirst(source)
        }
    }

    /**
     * Finds the shortest distance between two vertices using dijkstra's
     * algorithm.
     *
     * @param from The vertex to start from.
     * @param to The vertex to end at.
     *
     * @return The distance between the two vertices. [Int.MAX_VALUE] if no
     *         path exists.
     */
    abstract fun distance(from: E, to: E): Int

    /*---------------- CLUSTERING ----------------*/

    /**
     * Default implementation uses Highly Connected Subgraph then a singleton
     * removal algorithm to find clusters in this graph. Implementation may be
     * altered in subclasses.
     *
     * @param connectedness the minimum proportion of vertices that must be
     *        connected to form a cluster in HCS
     * @param kargerness the number of probabilistic attempts at finding the
     *        min-cut before the best option is taken
     *
     * @return a collection of subgraphs that are the clusters of this graph
     *         via the given parameters
     *
     * @throws InterruptedException since clustering is an expensive algorithm
     *         it will throw an exception when the thread is interrupted during
     *         calculation instead of completing the calculation
     */
    open fun getClusters(connectedness: Double = 0.5, kargerness: Int = 1000): Collection<Graph<E>> {
        return mergeSingletons(highlyConnectedSubgraphs(connectedness, kargerness))
    }

    /**
     * Replaces kargerness with confidence in [getClusters].
     *
     * @see getClusters
     *
     * @param confidence The confidence that this answer is the true HCS; more
     *        specifically, the desired probability of finding the correct
     *        min-cut during each iteration of Karger's algorithm.
     *
     * @return a collection of subgraphs that are the clusters of this graph
     *         via the given parameters
     *
     * @throws InterruptedException since clustering is an expensive algorithm
     *         it will throw an exception when the thread is interrupted during
     *         calculation instead of completing the calculation
     * @throws IllegalArgumentException if confidence is not between 0.0 and
     *         1.0 exclusive
     */
    fun getClusters(connectedness: Double = 0.5, confidence: Double): Collection<Graph<E>> {
        return getClusters(connectedness, kargerness = estimateRequiredKargerness(confidence))
    }

    /**
     * Uses an **approximation** equation to provide a kargerness that we are
     * *at least* [pUpperBound] confident in correctly finding the min-cut.
     *
     * @param pUpperBound an upperbound for the confidence that is desired
     *        however it's not a guaranteed upper-bound.
     *
     * @return the kargerness required to be [p][pUpperBound] confident in the
     *         specified clustering
     */
    fun estimateRequiredKargerness(pUpperBound: Double): Int {
        require(pUpperBound > 0 && pUpperBound < 1)
        return (0.380468110736 * (size() + 3.67357512953) * ln(-1 / (pUpperBound - 1))).roundToInt()
    }

    /**
     * Uses an **approximation** equation to provide the confidence that we
     * should have in [kargerness] at this graph size in correctly finding
     * the min-cut.
     *
     * **Note that this uses the same equation as [estimateRequiredKargerness]
     * and will produce an upper bound for p that is likely higher than the
     * true p**
     *
     * @param kargerness the kargerness for [getClusters] to estimate the
     *        confidence of.
     *
     * @return the estimated confidence in the specified clustering
     *
     * @see AMGraph.findKargerSuccessRate
     */
    fun estimateClusteringConfidence(kargerness: Int): Double {
        return 1-(0.07219812725.pow(kargerness / (size() + 3.67357512953))).coerceIn(0.0..1.0)
    }

    /**
     * HCS clustering algorithm finds all the subgraphs with n vertices such
     * that the minimum cut of those subgraphs contain more than
     * [connectedness]*sizeOfTheGraph edges, and identifies them as clusters.
     *
     * This implementation uses Karger's algorithm (see
     * [karger's][AMGraph.karger]) with a repeat value of [kargerness]
     *
     * @param connectedness the minimum edges (this*graphSize) that must be
     *        connected to consider a vertex clustered
     * @param kargerness The number of iterations of [min-cut][AMGraph.minCut]
     *        to try before taking the best one
     *
     * @return the subgraphs representing the clusters generated by this
     *         algorithm
     */
    abstract fun highlyConnectedSubgraphs(connectedness: Double = 0.5, kargerness: Int = 1000): Collection<Graph<E>>

    /**
     * Given clusters merges singletons into the cluster that they're most
     * connected to.
     *
     * @param clusters the unmerged clusters
     * @param extraPasses the number of time singletons should be checked. If merge
     * singletons are creating new large clusters this will assign singletons
     * to clusters that might not have been as big when they were initially
     * checked. Usually a value of 1 or 2 is sufficient.
     *
     * @return the clusters with singletons merged into the cluster that
     *         they're most connected to
     */
    private fun mergeSingletons(clusters: Collection<Graph<E>>, extraPasses: Int = 1): Collection<Graph<E>> {
        var passes = extraPasses + 1

        val clusters = ArrayList(clusters)

        val singletons = ArrayList<E>(clusters.size)

        do {
            passes--

            //Cleans up existing singletons (will do nothing on first pass)
            for(singleton in singletons) {
                var hcc: Graph<E>? = null
                var highScore = 0
                var connections = LinkedList<Pair<E,E>>()
                var oldCluster: Graph<E>? = null

                for(cluster in clusters){
                    val theseConnections = LinkedList<Pair<E,E>>()
                    for(v in cluster){
                        if(v === singleton){
                            oldCluster = cluster
                            continue
                        }
                        if(this[v,singleton] != null) theseConnections += v to singleton
                        if(this[singleton,v] != null) theseConnections += singleton to v
                    }

                    if (theseConnections.size > highScore || (theseConnections.size == highScore && cluster.size() > (oldCluster?.size() ?:-1))){
                        hcc = cluster
                        highScore = theseConnections.size
                        connections = theseConnections
                    }
                }

                if (hcc === oldCluster) continue

                oldCluster!!.remove(singleton)

                hcc!!.add(singleton)

                for(c in connections) {
                    hcc[c.first, c.second] = this[c.first, c.second]!!
                }
            }

            //we're going to find new singletons
            singletons.clear()

            //singletons to remove
            val removeQueue = LinkedList<Graph<E>>()

            //Finds and removes singletons
            for (cluster in clusters) {
                if (Thread.currentThread().isInterrupted) throw InterruptedException()
                if (cluster.size() != 1) continue //not a singleton

                val singleton = cluster.getVertices().first()

                //aka hcc
                var highScore = 1
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

                    val score = ibConnections.size + obConnections.size
                    if (score > highScore || (score == highScore && neighborCluster.size() >= highestConnectedCluster.size() )) {
                        highScore = score
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

                //If you have a valid cluster to merge into lets verify it's the correct one.
                singletons.addLast(singleton)
            }
            clusters.removeAll(removeQueue)

        } while(passes > 0)

        return clusters
    }

    /**
     * Generate a key for the graph that contains its current vertices, edges,
     * and weights. This key can typically be read by a constructor of
     * subclasses to reproduce the graph.
     *
     * @return a string containing the information in the current graph.
     */
    fun getKey(): String {
        val vertStr = ArrayList(getVertices()).joinToString(separator = "|")

        val edges: String = getEdges().map { Triple(it.first, it.second, this[it.first,it.second]) }.joinToString(separator = "|") { triple ->
            "${triple.first}#${triple.second}#${triple.third}"
        }

        val finalString = "$vertStr@$edges"

        return finalString
    }
}