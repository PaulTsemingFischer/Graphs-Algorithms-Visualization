//TODO: Graph presets
//TODO: Kargerness %

package com.fischerabruzese.graph

import java.math.BigInteger
import java.util.*
import kotlin.NoSuchElementException
import kotlin.math.min
import kotlin.math.pow
import kotlin.random.Random


/**
 * Adjacency Matrix implimentation of [Graph]. Improves on efficiency of
 * [Graph]'s operation where improvements can be made from direct access to
 * adjacency matrix. Permits [Any] elements as the vertex type and does **not**
 * permit nullable types.
 *
 * This implimentation provides constant time edge and vertex search via
 * [get][AMGraph.get] and [contains][AMGraph.contains]; O(n^2) vertex addition
 * and removal via [add][AMGraph.add] and [remove][AMGraph.remove]; Constant
 * time edge addition and removal via [set][AMGraph.set] and [removeEdge];
 *
 * The efficiencies for project algorithms are as follows:
 *  * Dijkstra's Algorithm ->  O(V^2*log(V))
 *  * Breadth First Search -> O(V^2)
 *  * Depth First Search -> O(V^2)
 *  * HCS (Highly Connected Subgraphs) -> TODO
 *
 * **Note that this implementation is not synchronized.** If multiple threads
 * access a [AMGraph] concurrently and at least one of the threads modifies the
 * graph structurally, it *must* be synchronized externally.
 *
 * The iterators returned by this class's [iterator] and methods are
 * *fail-fast*: if the graph is structurally modified in any way  at any time
 * after the iterator is created, the iterator will throw a
 * [ConcurrentModificationException]. However, these exceptions are based on
 * the structure of [ArrayList]'s fail-fast implementation. Therefore, it would
 * be wrong to write a program that depended on this exception for
 * its correctness: *the fail-fast behavior of iterators
 * should be used only to detect bugs.*
 *
 * @author Paul Fischer
 * @author Skylar Abruzese
 * @see Graph
 */
class AMGraph<E:Any> private constructor(dummy:Int, outboundConnections : Collection<Pair<E,Iterable<Pair<E,Int>>>?>) : Graph<E>() { //dummy is to avoid conflicting signatures, the constructor is private, so it never sees the light of day
    /**
     * Represents a directed graph with non-negative edge weights.
     * @param E The type of the vertices in the graph.
     * @param vertices the vertices to include in the graph
     */
    constructor(vertices: Collection<E>) : this(0,
        vertices.map { it to emptyList<Pair<E, Int>>() }
    )

    /**
     * Constructs an empty graph with no vertices
     */
    constructor() : this(0, emptyList())

    /**
     * Alternate constructors and calculation/testing methods
     */
    companion object {
        /**
         * Constructs a new [AMGraph] containing all vertices mentioned in [weightedConnections] with their corresponding edges.
         *
         * --
         *
         * *note that you may have to specify the parameter name (example attached) to avoid being confused with graphOf-unweightedConnections where the [type][E] is [Pair]*
         *
         * - **[AMGraph].graphOf( [weightedConnections] = Collection...)**
         *
         * @param weightedConnections A collection containing all the edges to be added to the new graph.
         * Collection Format:
         *
         * - **[Source][E] paired to a [Collection] of its [Destination][E]&[Weight][Int] 's**
         *
         * @param E The type of the vertices in this graph.
         */
        @JvmName("graphOfOutboundConnectionsList")
        fun <E:Any> graphOf(weightedConnections: Collection<Pair<E, Iterable<Pair<E, Int>>>?>) = AMGraph(0, weightedConnections)

        /**
         * Constructs a new [AMGraph] containing all vertices mentioned in [connections] with edges of weight 1 between the specified vertices.
         *
         * --
         *
         * *note that you may have to specify the parameter name (example attached) to avoid being confused with graphOf-weightedConnections where the [type][E] is [Pair]*
         *
         * - **[AMGraph].graphOf( [connections] = Collection...)**
         *
         * @param connections A collection containing all the edges to be added to the new graph.
         * Collection Format:
         *
         * - **[Source][E] paired to a [Collection] of its [Destination][E]'s**
         *
         * @param E The type of the vertices in this graph.
         */
        @JvmName("graphOfConnectionsList")
        fun <E:Any> graphOf(connections: Collection<Pair<E, Iterable<E>?>>) = AMGraph(0,
            connections.map {
                it.first to (it.second?.map { it2 -> it2 to 1 } ?: emptyList())
            }
        )

        /**
         * Made for testing purposes. Finds the success rate of running Kargers Algorithm with min-cut repeated [kargerness] times for the given [graph].
         * This will repeat Kargers on the graph until it produces the wrong answer. It will calculate the proportion of successes from this, averaging together the results over the [totalRepetitions].
         * @param graph The graph you want to test.
         * @param kargerness The kargerness that you want to test.
         * @param totalRepetitions The total amount of wrong kargers found before calculating the success rate. The higher the value, the slower, but higher confidence answer.
         * @param updateInterval Prints the current values at the specified interval. Can be useful for long calculations. Setting this below totalRepetitions will automatically turn [printing] on unless otherwise specified.
         * @param printing Prints the answer and [updateInterval]s to the console.
         * @return The proportion of times kargers failed given the inputs above.
         * @throws IllegalStateException Very (and I mean very) rarely or under extremely extreme circumstances will the min-cut used to verify a correct min-cut be incorrect and throw this exception.
         * @author Skylar Abruzese
         */
        fun<E:Any> findKargerSuccessRate(
            graph: AMGraph<E>,
            kargerness: Int,
            totalRepetitions: Int,
            updateInterval: Int = totalRepetitions+1,
            printing: Boolean = updateInterval < totalRepetitions
        ): Double {
            //Find real min-cut
            val realMinCut: Int = graph.karger(5000).size

            //Loop Variables
            var i = 1
            var failCount = 1
            var prevFailCount = 1

            val list: LinkedList<E>
            while (i <= totalRepetitions) {
                //Check if we need to provide an update
                if(i%updateInterval == 0) {
                    val deltaFails = failCount-prevFailCount
                    val intervalAvgFail = deltaFails/updateInterval.toDouble()
                    val totalAvgFail = failCount/i.toDouble()
                    if(printing) println("${i-updateInterval}-$i:".padEnd((totalRepetitions.toString().length)*2 + 2) + "${((intervalAvgFail-1) / intervalAvgFail).toString().padEnd(25)}|| Current Average: ${(totalAvgFail-1)/totalAvgFail}")
                    prevFailCount = failCount
                }
                var minCutAttempts = 1
                while(true) {
                    val minCutAttempt = graph.karger(kargerness).size
                    if (minCutAttempt < realMinCut) throw IllegalStateException("Finding real min-cut (against all odds) failed")
                    if (minCutAttempt > realMinCut) { //Failed min cut
                        failCount += minCutAttempts
                        break
                    }
                    minCutAttempts++
                }
                i++
            }

            val avgFailCount = failCount/totalRepetitions.toDouble()
            val pSuccess = (avgFailCount-1) / avgFailCount

            if(printing) print("|V| = ${graph.size()}, kargerness = $kargerness, p-success = $pSuccess")
            return pSuccess
        }

        /**
         * Made for testing purposes. Finds the worst case success rate of running Kargers Algorithm with min-cut repeated [kargerness] times for a graph of size [graphSize].
         * This will repeat Kargers on a graph with the specified size and only 1 correct min-cut until it produces the wrong answer. It will calculate the proportion of successes from this, averaging together the results over the [totalRepetitions].
         * @param graphSize This size of the graph you want to test.
         * @param kargerness The kargerness that you want to test.
         * @param totalRepetitions The total amount of wrong kargers found before calculating the success rate. The higher the value, the slower, but higher confidence answer.
         * @param updateInterval Prints the current values at the specified interval. Can be useful for long calculations. Setting this below totalRepetitions will automatically turn [printing] on unless otherwise specified.
         * @param printing Prints the answer and [updateInterval]s to the console.
         * @return The proportion of times kargers failed given the inputs above.
         * @throws IllegalStateException Very (and I mean very) rarely or under extremely extreme circumstances will the min-cut used to verify a correct min-cut be incorrect and throw this exception.
         * @see findKargerSuccessRate
         */
        fun findKargerSuccessRate(
            graphSize: Int,
            kargerness: Int,
            totalRepetitions: Int,
            updateInterval: Int = totalRepetitions+1,
            printing: Boolean = updateInterval < totalRepetitions
        ): Double {
            /* GRAPH CREATION */
            val verts = (0 until graphSize).toList();
            val graph = AMGraph(verts)
            graph.randomize(1.0, 0, 1)
            (graph as Graph<Int>).removeEdge(
                from = 0,
                to = 1
            )
            return findKargerSuccessRate(graph, kargerness, totalRepetitions, updateInterval, printing)
        }
    }


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
                get(f, t) ?: return null
            } ?: throw NoSuchElementException("To Element {$to} does not exist")
        } ?: throw NoSuchElementException("From Element {$from} does not exist")
    }

    /**
     * For faster internal access to the edges, since we frequently already have the id's
     */
    private fun get(from: Int, to: Int): Int? {
        return if (edgeMatrix[from][to] == -1) null
        else edgeMatrix[from][to]
    }

    override operator fun set(from: E, to: E, value: Int): Int? {
        val f = indexLookup[from] ?: throw NoSuchElementException()
        val t = indexLookup[to] ?: throw NoSuchElementException()
        return set(f,t,value)
    }

    /**
     * For faster internal access to the edges, since we frequently already have the id's
     */
    private fun set(from: Int, to: Int, value: Int): Int? {
        dijkstraTables = null
        return get(from, to).also { edgeMatrix[from][to] = value }
    }

    override fun removeEdge(from: E, to: E): Int? {
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

    override fun clearEdges() {
        edgeMatrix = Array(size()) { IntArray(size()) { -1 } }
        dijkstraTables = null
    }


    override fun addAll(vertices: Collection<E>): Collection<E>{
        val failed = ArrayList<E>()
        for (vert in vertices) {
            if (indexLookup[vert] != null) {
                failed += vert
                continue
            }
            indexLookup[vert] = size()
        }
        this.vertices.addAll(this.vertices)

        edgeMatrix = Array(size()) { i ->
            IntArray(size()) { j ->
                edgeMatrix.getOrNull(i)?.getOrNull(j) ?: -1
            }
        }
        return failed
    }

    override fun removeAll(vertices: Collection<E>): Collection<E> {
        val failed = LinkedList<E>()
        //Marking vertices to remove + removing from hashmap
        val vertexToRemove = Array(size()) { false }
        for (vertex in vertices) {
            val id = indexLookup.remove(vertex)
            if(id == null) {
                failed.add(vertex)
                continue
            }
            vertexToRemove[id] = true
        }

        //Removing vertices from vertices list
        for(i in vertexToRemove.indices.reversed()){
            if(vertexToRemove[i]) this.vertices.removeAt(i)
        }

        //New edge matrix with vertices removed
        val newEdgeMatrix = Array(size()) { IntArray(size()) { -1 } }
        var fromOffset = 0

        //Copy over edges to new edge matrix
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

        //Nuke dijkstra table
        dijkstraTables = null

        return failed
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

    override fun countEdgesBetween(v1: E, v2: E): Int {
        var edges = 0
        get(v1,v2)?.let{edges++}
        get(v2,v1)?.let{edges++}
        return edges
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
        return graphOf(weightedConnections = verts.map { from ->
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
    override fun highlyConnectedSubgraphs(connectedness: Double, kargerness: Int) : Collection<AMGraph<E>>{
        val minCut = karger(kargerness)
        //check if minCut size is acceptable or there's no cut (ie there's only 1 node in the graph)
        if(minCut.size >= connectedness * size() || minCut.size == -1) return listOf(this)

        val clusters = ArrayList<AMGraph<E>>()
        val subgraph1 = subgraphFromIds(minCut.cluster1)
        val subgraph2 = subgraphFromIds(minCut.cluster2)

        clusters.addAll(subgraph1.highlyConnectedSubgraphs(connectedness, kargerness))
        clusters.addAll(subgraph2.highlyConnectedSubgraphs(connectedness, kargerness))

        return clusters
    }

    /**
     * @param numAttempts the number of attempts of cuts it should try before it picks the lowest one
     * @return the smallest cut found in [numAttempts] iterations that results in the largest min clyster
     */
    private fun karger(numAttempts: Int) : Cut {
        var bestCut = minCut()

        repeat(numAttempts - 1){
            if(Thread.currentThread().isInterrupted) throw InterruptedException()
            bestCut = minCut().takeIf{
                it < bestCut
            } ?: bestCut
        }

        return bestCut
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