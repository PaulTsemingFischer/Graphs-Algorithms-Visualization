//TODO: Graph presets
//TODO: Figure out the issue with OG dijkstra with disjoint graphs
//TODO: make kargerness a confidence %

package com.fischerabruzese.graph

import java.math.BigInteger
import java.util.*
import kotlin.math.ln
import kotlin.math.min
import kotlin.math.pow
import kotlin.random.Random

class AMGraph<E:Any> private constructor(dummy:Int, outboundConnections : List<Pair<E,Iterable<Pair<E,Int>>>?>) : Graph<E>() { //dummy is to avoid conflicting signatures, the constructor is private, so it never sees the light of day
    /**
     * Represents a directed graph with non-negative edge weights.
     * @param E The type of the vertices in the graph.
     * @param vertex the vertices to include in the graph
     */
    constructor(vararg vertex: E) : this(0,
        vertex.map { it to emptyList<Pair<E, Int>>() }
    )

    /**
     * Alternate constructors for creating graphs
     */
    companion object {
        /**
         * Creates a graph from vertices with outbound connections
         * @param outboundConnections: pairs containing the vertex paired with an array of all vertices it has an outbound connection to paired with that connections weight
         */
        @JvmName("graphOfOutboundConnectionsVararg")
        fun <E:Any> graphOf(vararg outboundConnections: Pair<E, Iterable<Pair<E, Int>>>) = graphOf(outboundConnections.toList())

        /**
         * Creates a graph from a list of vertices with outbound connections
         * @param outboundConnectionsList: A list of pairs containing the vertex paired with an array of all vertices it has an outbound connection to paired with that connections weight
         */
        @JvmName("graphOfOutboundConnectionsList")
        fun <E:Any> graphOf(outboundConnectionsList: List<Pair<E, Iterable<Pair<E, Int>>>?>) = AMGraph(0,outboundConnectionsList)

        /**
         * Creates a graph from vertices with outbound connections with no weights
         * @param connections: pairs containing the vertex paired with an array of all vertices it has an outbound connection to
         */
        @JvmName("graphOfConnectionsVararg")
        fun <E:Any> graphOf(vararg connections: Pair<E, Iterable<E>?>) = fromConnections(connections.toList())

        /**
         * Creates a graph from vertices with outbound connections with no weights
         * @param connectionsList: A list of pairs containing the vertex paired with an array of all vertices it has an outbound connection to
         */
        @JvmName("graphOfConnectionsList")
        fun <E:Any> fromConnections(connectionsList: List<Pair<E, Iterable<E>?>>) = AMGraph(0,
            connectionsList.map {
                it.first to (it.second?.map { it2 -> it2 to 1 } ?: emptyList())
            }
        )

        /**
         * @param verticesList: A collection of vertices to add to the graph
         */
        fun <E:Any> fromCollection(verticesList: Collection<E>) = AMGraph(0, verticesList.map { it to emptyList() })
    }

    /**
     * Constructs an empty graph with no vertices
     */
    constructor() : this(0, emptyList())


    /**
     * An ordered collection of all the vertices in this graph. It's location in this array is referred to as the vertex 'id'
     */
    private var vertices: ArrayList<E> = ArrayList()// Vert index --> E

    /**
     * An ordered matrix of the edges in the graph. The `[i]` `[j]` element is the weight of the edge from vertex id `[i]` to vertex id `[j]`
     */
    private var edgeMatrix: Array<IntArray> // [Vert index][Vert index] --> edge weight

    /**
     * Converts a vertex to its index in [vertices] to avoid slow searching for id
     */
    private val indexLookup = HashMap<E, Int>() // E --> Vert index

    /**
     * A cached table of the shortest paths from every vertex to every other vertex.
     * dijkstraTables`[source]` `[destination]` = Pair(previous vertex id, distance from source)
     * This will be null when all paths are invalid, one vertex will be null if it hasn't been calculated. If the vertex is not in the table then it has no path.
     */
    private var dijkstraTables: Array<Array<Pair<Int, Int>>?>?

    /*------------------ FUNCTIONALITY ------------------*/

    override fun size() = vertices.size

    init {
        //Add vertices to vertices and indexLookup
        for (connections in outboundConnections) {
            if (connections == null) continue
            if (indexLookup.putIfAbsent(connections.first, vertices.size) == null) {
                vertices.add(connections.first)
            }
            for (outboundEdge in connections.second) {
                if (indexLookup.putIfAbsent(outboundEdge.first, vertices.size) == null) {
                    vertices.add(outboundEdge.first)
                }
            }
        }

        dijkstraTables = null
        edgeMatrix = Array(size()) { IntArray(size()) { -1 } }

        //Add edges
        for (connections in outboundConnections) {
            if (connections == null) continue
            for (outboundEdge in connections.second) {
                try {
                    if (outboundEdge.second <= 0) throw IllegalArgumentException()
                    else edgeMatrix[indexLookup[connections.first]!!][indexLookup[outboundEdge.first]!!] =
                        outboundEdge.second
                } catch (e: IllegalArgumentException) {
                    edgeMatrix[indexLookup[connections.first]!!][indexLookup[outboundEdge.first]!!] = 1
                }
            }
        }
    }

    override operator fun get(from: E, to: E): Int? {
        return indexLookup[from]?.let { f ->
            indexLookup[to]?.let { t ->
                get(f, t)
            }
        }
    }

    /**
     * For faster internal access to the edges, since we frequently already have the id's
     */
    private fun get(from: Int, to: Int): Int? {
        return if (edgeMatrix[from][to] == -1) null
        else edgeMatrix[from][to]
    }

    override operator fun set(from: E, to: E, value: Int): Int? {
        return set(indexLookup[from]!!, indexLookup[to]!!, value)
    }

    /**
     * For faster internal access to the edges, since we frequently already have the id's
     */
    private fun set(from: Int, to: Int, value: Int): Int? {
        dijkstraTables = null
        return get(from, to).also { edgeMatrix[from][to] = value }
    }

    override fun remove(from: E, to: E): Int? {
        return remove(indexLookup[from]!!, indexLookup[to]!!)
    }

    /**
     * For faster internal access to the edges, since we frequently already have the id's
     */
    private fun remove(from: Int, to: Int): Int? {
        return set(from, to, -1)
    }

    override fun contains(vertex: E): Boolean {
        return indexLookup.containsKey(vertex)
    }

    override fun iterator(): Iterator<E> = vertices.iterator()

    override fun getVertices(): Set<E> {
        return vertices.toSet()
    }

    override fun getEdges(): Set<Pair<E, E>> {
        val edges = mutableSetOf<Pair<E,E>>()
        for(from in edgeMatrix.indices){
            for(to in edgeMatrix[from].indices){
                if(edgeMatrix[from][to] >= 0) edges.add(vertices[from] to vertices[to])
            }
        }
        return edges
    }

    override fun clearConnections() {
        edgeMatrix = Array(size()) { IntArray(size()) { -1 } }
        dijkstraTables = null
    }

    override fun addAll(verts: Collection<E>) {
        for (vert in verts) indexLookup[vert] = size()
        vertices.addAll(verts)

        edgeMatrix = Array(size()) { i ->
            IntArray(size()) { j ->
                edgeMatrix.getOrNull(i)?.getOrNull(j) ?: -1
            }
        }
    }

    //TODO: PAUL COMMENT THIS METHOD, JAVADOC ALREADY IN GRAPH.KT
    override fun removeAll(verts: Collection<E>) {
        val vertexToRemove = Array(size()) { false }
        for (vertex in verts) {
            val id = indexLookup.remove(vertex) ?: continue
            vertexToRemove[id] = true
        }
        for(i in vertexToRemove.indices.reversed()){
            if(vertexToRemove[i]) vertices.removeAt(i)
        }

        val newEdgeMatrix = Array(size()) { IntArray(size()) { -1 } }
        var fromOffset = 0

        for (from in edgeMatrix.indices) {
            if (vertexToRemove[from])
                fromOffset++
            else {
                var toOffset = 0
                for (to in edgeMatrix.indices) {
                    if (vertexToRemove[to])
                        toOffset++
                    else
                        newEdgeMatrix[from - fromOffset][to - toOffset] = edgeMatrix[from][to]
                }
            }
        }
        edgeMatrix = newEdgeMatrix
        dijkstraTables = null
    }

    override fun<R : Any> mapVertices(transform: (vertex: E) -> R) : Graph<R> {
        val newGraph = AMGraph<R>()
        newGraph.edgeMatrix = edgeMatrix.map { it.clone() }.toTypedArray()
        newGraph.vertices = ArrayList(vertices.mapIndexed{index, e -> transform(e).also{ r -> newGraph.indexLookup[r] = index}})
        return newGraph
    }

    override fun toString(): String {
        val string = StringBuilder()
        for (destinations in edgeMatrix) {
            string.append("[")
            for (weight in destinations) {
                string.append("${weight.let { if (it == -1) " " else it }}][")
            }
            string.deleteRange(string.length - 2, string.length)
            string.append("]\n")
        }
        return string.toString()
    }

    override fun neighbors(vertex: E): Collection<E> {
        val vertexId = indexLookup[vertex]!!
        val neighbors = mutableListOf<E>()
        for (vert in vertices.indices){
            if (get(vertexId, vert) != -1) neighbors.add(vertices[vert])
        }
        return neighbors
    }

    override fun bidirectionalConnections(v1: E, v2: E): Int{
        var connections = 0
        get(v1,v2)?.let{connections++}
        get(v2,v1)?.let{connections++}
        return connections
    }

    override fun copy(): AMGraph<E> {
        val newGraph = AMGraph<E>()
        for(v in vertices) newGraph.add(v)
        for(src in edgeMatrix.indices){
            for(dest in edgeMatrix[src].indices){
                if(get(src, dest) != null){
                    newGraph.set(src, dest, get(src, dest)!!)
                }
            }
        }
        return newGraph
    }

    override fun subgraph(verts: Collection<E>): AMGraph<E> {
        return subgraphFromIds(vertices.map { indexLookup[it]!! })
    }

    /**
     * For faster internal access to the edges, since we frequently already have the id's
     */
    private fun subgraphFromIds(verts : Collection<Int>):AMGraph<E>{ //This method could be so much clearer, but I just love inline function 💕
        return graphOf(verts.map { from ->
            vertices[from] to ArrayList<Pair<E,Int>>().apply{verts.forEach{ t ->
                get(from,t)?.let{ add(vertices[t] to it) }
            }}
        })
    }


    /*------------------ RANDOMIZATION ------------------*/

    override fun randomize(probability: Double, minWeight: Int, maxWeight: Int, allowDisjoint: Boolean, random: Random) { //when removed add default values
        for (i in edgeMatrix.indices) {
            for (j in edgeMatrix.indices) {
                if (random.nextDouble() < probability) {
                    set(i, j, random.nextInt(minWeight,maxWeight))
                } else {
                    set(i, j, -1)
                }
            }
        }
        if(!allowDisjoint) mergeDisjoint(minWeight, maxWeight, random)
    }

    override fun mergeDisjoint(minWeight: Int, maxWeight: Int, random: Random){
        val bidirectional = this.getBidirectionalUnweighted() as AMGraph<E>
        var vertex = random.nextInt(size())
        var unreachables : List<Int>

        //Runs while there are any unreachable vertices from `vertex` (store all the unreachable ones in `unreachables`)
        //Vertices --> Unreachable non-self vertices --> Unreachable non-self id's
        while(vertices.filterIndexed { id, _ -> id != vertex && bidirectional.path(vertex, id).isEmpty() }.map{indexLookup[it]!!}.also{ unreachables = it }.isNotEmpty()){
            val from : Int
            val to : Int
            val weight = random.nextInt(minWeight, maxWeight)
            if(random.nextBoolean()) {
                from = vertex
                to = unreachables.random(random)
            }
            else {
                from = unreachables.random(random)
                to = vertex
            }
            edgeMatrix[from][to] = weight
            bidirectional.set(from, to, 1)
            bidirectional.set(to, from, 1)

            vertex = random.nextInt(size())
            unreachables = emptyList()
        }
    }

    /*------------------ PATHING ------------------*/
    /*BFS and DFS */
    /**
     * Finds a path between two vertices using a depth first search.
     * @param start The vertex to start from.
     * @param dest The vertex to end at.
     * @return A list of vertices representing the path between the two vertices.
     */
    fun depthFirstSearch(start: E, dest: E): List<E> {
        return dfsRecursive(indexLookup[start]!!, indexLookup[dest]!!, BooleanArray(size())).map { vertices[it] }
    }

    /**
     * Finds a path between two vertices using a breadth first search.
     * @param start The vertex to start from.
     * @param dest The vertex to end at.
     * @return A list of vertices representing the path between the two vertices.
     */
    fun breadthFirstSearch(start: E, dest: E): List<E> = search(false, start, dest)


    /**
     * Finds a path between two vertices using either depth or breadth.
     * @param depth true will use depth first search false will use breadth first search
     * @param start The vertex to start from.
     * @param dest The vertex to end at.
     * @return A list of vertices representing the path between the two vertices.
     */
    private fun search(depth: Boolean, start: E, dest: E): List<E> {
        val dest = indexLookup[dest]!!
        val start = indexLookup[start]!!
        val q = LinkedList<Int>()
        val prev = IntArray(size()) { -1 }

        q.addFirst(start)
        prev[start] = -2

        while (!q.isEmpty()) {
            val curPath = q.pop()

            for ((ob, dist) in edgeMatrix[curPath].withIndex()) {

                if (prev[ob] < 0 && dist != -1 && ob != curPath) {
                    if (depth)
                        q.addFirst(ob)
                    else //breadth
                        q.addLast(ob)
                    prev[ob] = curPath
                    if (ob == dest) break
                }
            }
        }
        return LinkedList<E>().apply {
            if (prev[dest] == -1) return@apply

            var next: Int = dest
            while (next != start) {
                addFirst(vertices[next])
                next = prev[next]
            }
            addFirst(vertices[start])
        }
    }

    private fun dfsRecursive(src: Int, dest: Int, visited: BooleanArray): LinkedList<Int> {
        visited[src] = true

        for ((ob, dist) in edgeMatrix[src].withIndex()) {

            if (!visited[ob] && dist != -1) {
                if (ob == dest)
                    return LinkedList<Int>().apply { addFirst(ob); addFirst(src) }
                dfsRecursive(ob, dest, visited).let {
                    if (it.isNotEmpty())
                        return it.apply { addFirst(src) }
                }
            }
        }
        return LinkedList()
    }

    /* DIJKSTRA'S */
    override fun path(from: E, to: E): List<E> {
        return path(indexLookup[from]!!,indexLookup[to]!!).map { vertices[it] }
    }

    /**
     * Allows specification of [useSimpleAlgorithm] to use dijkstra's without a fib heap which preforms slightly better, but is currently non-functional on disjoint graphs.
     */
    @Deprecated("Simple Algorithm has a unresolved logic flaw. Use path(from: E, to: E) instead", ReplaceWith("path(from, to)"))
    fun path(from: E, to: E, useSimpleAlgorithm: Boolean): List<E> {
        return path(indexLookup[from]!!,indexLookup[to]!!).map { vertices[it] }
    }

    @Suppress("DEPRECATION_ERROR")
    /**
     * @return A list of vertex id's representing the path between from and to where [from] is the first element and [to] is the last. Returns an empty list if there is no path
     */
    private fun path(from: Int, to: Int, useSimpleAlgorithm: Boolean = false): List<Int>{
        return try {
            tracePath(
                from,
                to,
                if (useSimpleAlgorithm) getDijkstraTableSimple(from) else getDijkstraTable(from)
            )
        } catch (e: IndexOutOfBoundsException) { //More nodes were added that are disjoint and not in cached tables (we know there's no path)
            emptyList()
        }
    }


    override fun distance(from: E, to: E): Int {
        val fromIndex = indexLookup[from]!!
        val toIndex = indexLookup[to]!!
        return (getDijkstraTable(fromIndex))[toIndex].second
    }

    /**
     * Implements a Fibonacci Heap in Dijkstra's algorithm to queue vertices for search.
     */
    private fun dijkstraFibHeap(from: Int, to: Int? = null): Array<Pair<Int, Int>> {
        //Initialize each vertex's info mapped to ids
        val prev = IntArray(size()) { -1 }
        val dist = IntArray(size()) { Int.MAX_VALUE }
        dist[from] = 0

        //PriorityQueue storing Priority = dist, Value = id
        val heap = FibonacciHeap<Int, Int>()

        //Store Queue's nodes for easy search/updates
        val nodeCollection = Array<Node<Int, Int>?>(size()) { null }
        nodeCollection[from] = heap.insert(dist[from], from)

        //loop forever, or until we have visited to
        while (to == null || heap.minimum() == to) {

            //store and remove next node, mark as visited, break if empty
            val cur = heap.extractMin() ?: break

            //iterate through potential outbound connections
            for ((i, edge) in edgeMatrix[cur].withIndex()) {

                //relax all existing connections
                if (edge != -1
                    //table update required if it's the shortest path (so far)
                    && dist[cur] + edge < dist[i]
                ) {

                    //update
                    dist[i] = dist[cur] + edge
                    prev[i] = cur

                    //re-prioritize node or create and add it
                    if (nodeCollection[i] != null)
                        heap.decreaseKey(nodeCollection[i]!!, dist[i])
                    else nodeCollection[i] = heap.insert(dist[i], i)
                }
            }
        }
        return prev.zip(dist).toTypedArray()
    }

    @Deprecated("This algorithm is flawed and should not be used. Use path(from: E, to: E) instead", ReplaceWith("path(from, to)"), level = DeprecationLevel.ERROR)
    /**
     * @precondition: If "to" is null, finds every path from "from", else only the path from "from" to "to" is accurate
     * @postcondition: Both Int.MAX_VALUE and -1 indicates no path
     * @return An array of (previous vertex index, distance)
     */
    private fun dijkstra(from: Int, to: Int? = null): Array<Pair<Int, Int>> {
        val distance = IntArray(size()) { Int.MAX_VALUE }
        val prev = IntArray(size()) { -1 }
        val visited = BooleanArray(size()) { false }

        distance[from] = 0
        while (to == null || !visited[to]) {
            //Determine the next vertex to visit
            var currVert = visited.indexOfFirst { !it } //Finds first unvisited
            if (currVert == -1 || distance[currVert] == Int.MAX_VALUE) break //All visited
            for (i in currVert + 1 until visited.size) {
                if (!visited[i] && distance[i] < distance[currVert]) {
                    currVert = i
                }
            }
            //Update distances and previous
            val currDist = distance[currVert]
            for ((i, edge) in edgeMatrix[currVert].withIndex()) {
                if (!visited[i] && edge != -1 && currDist + edge < distance[i]) {
                    distance[i] = (currDist + edgeMatrix[currVert][i])
                    prev[i] = currVert
                }
            }
            //Update visited
            visited[currVert] = true
        }
        return prev.zip(distance).toTypedArray() //funky function
    }

    /**
     * Attempts to retrieve the dijkstra's table from the cache. If it is not cached, it will create it using [dijkstraFibHeap]. If the table is created, it will be cached.
     * @return The table retrieved from the DijkstraTable
     */
    private fun getDijkstraTable(fromIndex: Int): Array<Pair<Int, Int>> {
        if(dijkstraTables == null) dijkstraTables = Array(size()) {null}
        if (dijkstraTables!![fromIndex] == null) dijkstraTables!![fromIndex] = dijkstraFibHeap(fromIndex)
        return dijkstraTables!![fromIndex]!!
    }

    @Suppress("DEPRECATION_ERROR")
    @Deprecated("This algorithm is flawed and should not be used. Use getDijkstraTable(fromIndex) instead", ReplaceWith("getDijkstraTable(fromIndex)"), level = DeprecationLevel.ERROR)
    private fun getDijkstraTableSimple(fromIndex: Int): Array<Pair<Int, Int>> {
        if(dijkstraTables == null) dijkstraTables = Array(size()) {null}
        if (dijkstraTables!![fromIndex] == null) dijkstraTables!![fromIndex] = dijkstra(fromIndex)
        return dijkstraTables!![fromIndex]!!
    }

    /**
     *  Goes through the Dijkstra's table and returns a list of the path between from and to if it exists, and returns an empty list otherwise.
     */
    private fun tracePath(from: Int, to: Int, dijkstraTable: Array<Pair<Int, Int>>): List<Int> {
        //println("from: $from to: $to ${dijkstraTable.contentDeepToString()}")
        val path = LinkedList<Int>()
        path.add(to)
        var curr = to
        while (path.firstOrNull() != from) {
            path.addFirst(dijkstraTable[curr].run { curr = first; first })//.also{println("curr path: $path")}
            if (path[0] == -1) {
                path.removeFirst(); break
            }
        }
        return if (path.first() == from) path else emptyList()
    }

    override fun getConnected(vertex: E): List<E> {
        return getConnected(indexLookup[vertex]!!).map{ vertices[it] }
    }

    //Returns a list of all connected vertices by ID
    private fun getConnected(vertex: Int): List<Int> {
        val connected = ArrayList<Int>()
        for(id in vertices.indices){
            if (id == vertex || path(id, vertex).isNotEmpty())
                connected.add(id)
        }
        return connected
    }

    /*------------------ COLORING ------------------*/

    @Deprecated("This algorithm was never finished and should not be used", level = DeprecationLevel.HIDDEN)
    fun color(maxColors: Int): Array<List<E>>? {
        require(maxColors > 0)
        val max = BigInteger(maxColors.toString()).pow(size() + 1)
        var colors = BigInteger.ZERO //storing this array as an int for FANCY iteration

        fun getColor(index: Int): Int {
            return colors.divide(maxColors.toDouble().pow(index.toDouble()).toInt().toBigInteger())
                .mod(maxColors.toBigInteger()).toInt()
        }

        fun check(): Boolean {
            for (vert in vertices) {
                for ((ob, w) in edgeMatrix[indexLookup[vert]!!].withIndex()) {
                    if (w != -1 && getColor(ob) == getColor(indexLookup[vert]!!)) {
                        return false
                    }
                }
            }
            return true
        }

        while (!check()) {
            if (colors == max) return null
            colors = colors.plus(BigInteger.ONE)
        }

        return ArrayList<ArrayList<E>>().apply {
            for (vert in vertices.indices) {
                while (getColor(vert) >= size()) {
                    add(ArrayList())
                }
                this[getColor(vert)].addLast(vertices[vert])
            }
        }.toTypedArray()
    }

    /*------------------ CLUSTERING ------------------*/

    override fun getClusters(connectedness: Double, kargerness: Int) : Collection<AMGraph<E>>{
        val minCut = karger(kargerness)
        //check if minCut size is acceptable or there's no cut (ie there's only 1 node in the graph)
        if(minCut.size >= connectedness * size() || minCut.size == -1) return listOf(this)

        val clusters = ArrayList<AMGraph<E>>()
        val subgraph1 = subgraphFromIds(minCut.cluster1)
        val subgraph2 = subgraphFromIds(minCut.cluster2)

        clusters.addAll(subgraph1.getClusters(connectedness, kargerness))
        clusters.addAll(subgraph2.getClusters(connectedness, kargerness))

        return clusters
    }

    /**
     * @param numAttempts the number of attempts of cuts it should try before it picks the lowest one
     * @return the smallest cut found in [numAttempts] iterations that results in the largest min clyster
     */
    private fun karger(numAttempts: Int) : Cut {
        var bestCut = minCut()

        repeat(numAttempts - 1){
            bestCut = minCut().takeIf{
                it < bestCut
            } ?: bestCut
        }

        return bestCut
    }

    fun test(){
        fun calculateNumRuns(numVerts: Int, pDesired: Double): Int {
            val pMinCutSuccess = 1.0 / (numVerts * numVerts / 2 - numVerts / 2)
            val requiredIterations = ln(1 - pDesired) / ln(1 - pMinCutSuccess)
            return requiredIterations.toInt()
        }
        fun confidenceAfterIterations(numVerts: Int, iterations: Int): Double {
            val pMinCutSuccess = 1.0 / (numVerts * numVerts / 2 - numVerts / 2)
            val confidence = 1 - (1 - pMinCutSuccess).pow(iterations.toDouble())
            return confidence
        }

        val minCuts: Int = karger(1000000).size
        val numRuns = calculateNumRuns(size(), 0.88)

        var successes = 0
        repeat(10000){
            val minCutSize = karger(1).size
            println("Min cut size: $minCutSize, Best min cut: $minCuts")
            if(minCutSize == minCuts) successes++
        }
        println("Num runs: $numRuns")
        println("Success proportion: ${successes.toDouble()/10000}")
        println("Confidence: ${confidenceAfterIterations(size(), 1)}% of time")
    }

    /**
     * @param size the number of cuts necessary to separate the minCut
     * @param cluster1 the first cluster created by the cut
     * @param cluster2 the second cluster created by the cut
     * Cuts are compared by their desirability for our clustering algorithm
     * A desirable cut has a small value and is defined by a small size and a large min cluster
     */
    private data class Cut(val size: Int, val cluster1: Collection<Int>, val cluster2: Collection<Int>) : Comparable<Cut>{
        fun minCluster() = min(cluster1.size, cluster2.size)
        override fun compareTo(other: Cut): Int {
            return(this.size - other.size).let{
                if(it == 0) other.minCluster() - this.minCluster()
                else it
            }
        }
    }

    /**
     * @return a probabilistic attempt at finding the minimum cuts to make 2 disjoint graphs from this graph
     */
    private fun minCut() : Cut {
        //'from' > 'to' in edges
        var edges: MutableList<Pair<Int, Int>> = ArrayList()
        //Initializing edges from edge-matrix, triangular to ensure from > to
        for (from in 1 until size()) {
            for (to in 0 until from) {
                if (edgeMatrix[from][to] > -1) edges.add(from to to)
                if (edgeMatrix[to][from] > -1) edges.add(from to to)
            }
        }

        //If the nodes index contains a list whose last value that is not itself, it is a reference
        //If a node contains a list with last()==itself, it is a cluster head
        val nodeRedirection = Array(size()) { LinkedList<Int>().apply { add(it) } }

        //Navigates through references until it finds the redirected value
        fun getLinkedCluster(node: Int): List<Int> {
            if (nodeRedirection[node].last() == node) return nodeRedirection[node]
            return getLinkedCluster(nodeRedirection[node].last())
        }

        var numNodes = size()

        //Randomize edge list
        randomizeList(edges) //randomize prefers an array list
        edges = LinkedList(edges) //turn into a queue so we can pop

        //Delete the next edge and merge the two vertices (or clusters) into one
        fun collapse() {
            val edge = edges.pop()
            //from and to are the merged values (if they've previously been merged)
            val cluster1 = getLinkedCluster(edge.first)
            val cluster2 = getLinkedCluster(edge.second)

            if (cluster1 == cluster2) return //If both nodes are in the same cluster, do nothing

            //Redirect the cluster with the smaller max node into the cluster with the larger max node
            val bigHead = maxOf(cluster1.last(), cluster2.last())
            val smallHead = minOf(cluster1.last(), cluster2.last())

            nodeRedirection[bigHead].addAll(0, nodeRedirection[smallHead]) //Merge smallHead's cluster into bigHead's cluster
            nodeRedirection[smallHead] = LinkedList<Int>().apply { add(bigHead) } //Make smallHead redirect to bigHead

            //Finished collapsing 2 clusters into 1
            numNodes--
        }

        fun getClusters(): List<LinkedList<Int>> = nodeRedirection.filterIndexed{ id, cluster -> cluster.last() == id}

        fun getCut(): Cut {
            require(numNodes == 2)
            val clusters = getClusters()

            //count from all possible connections
            var edgesToCut = 0
            for (from in clusters[0]) {
                for (to in clusters[1]) {
                    if (edgeMatrix[from][to] > -1) edgesToCut++
                    if (edgeMatrix[to][from] > -1) edgesToCut++
                }
            }
            return Cut(edgesToCut, clusters[0], clusters[1])
        }

        //we cut the connections when we've collapsed everything into 2 nodes
        while (numNodes > 2 && edges.isNotEmpty()) {
            collapse()
        }
        return when(numNodes) {
            1 -> Cut(-1, emptyList(), emptyList()) //SINGLE NODE CASE
            2 -> getCut() //REGULAR CASE
            else -> {//DISJOINT CASE
                val clusters = getClusters()
                val cluster1 = clusters.subList(0, clusters.size/2).flatten()
                val cluster2 = clusters.subList(clusters.size/2, clusters.size).flatten()
                Cut(0, cluster1, cluster2)
            }
        }
    }

    private fun<T> randomizeList(list: MutableList<T>) {
        fun swap(index1: Int, index2: Int) {
            list[index1] = list.set(index2,list[index1])
        }

        for(i in list.indices){
            swap(i,Random.nextInt(i,list.size))
        }
    }
}