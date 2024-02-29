//TODO: Separate randomize avg connections and probability so that you can do decimal avg connections
//TODO: Make randomize depend on the toggle
//TODO: Graph presets
//TODO: Figure out the issue with OG dijkstra
//TODO: Write linkedListOf extension function
//TODO: make kargerness a confidence %
//TODO: Make edit edge work on non-existant edges

//TODO: COMMENTING -> Comment pathing stuff not already commented in Graph.kt

package com.fischerabruzese.graph

import java.math.BigInteger
import java.util.LinkedList
import kotlin.math.min
import kotlin.random.Random
import kotlin.math.pow

/**
 * Represents a directed graph with non-negative edge weights.
 * @param E The type of the vertices in the graph.
 */
class AMGraph<E:Any> private constructor(dummy:Int, outboundConnections : List<Pair<E,Iterable<Pair<E,Int>>>?>) : Graph<E>() { //dummy is to avoid conflicting signatures, the constructor is private, so it never sees the light of day
    /**
     * Represents a directed graph with non-negative edge weights.
     * @param E The type of the vertices in the graph.
     * @param vertex the vertices to include in the graph
     */
    constructor(vararg vertex: E) : this(0,
        vertex.map { it to emptyList<Pair<E, Int>>() }
    )

    companion object {
        @JvmName("graphOfOutboundConnectionsVararg")
        fun <E:Any> graphOf(vararg outboundConnections: Pair<E, Iterable<Pair<E, Int>>>) = graphOf(outboundConnections.toList())

        @JvmName("graphOfOutboundConnectionsList")
        fun <E:Any> graphOf(outboundConnectionsList: List<Pair<E, Iterable<Pair<E, Int>>>?>) = AMGraph(0,outboundConnectionsList)
        
        @JvmName("graphOfConnectionsVararg")
        fun <E:Any> graphOf(vararg connections: Pair<E, Iterable<E>?>) = fromConnections(connections.toList())

        @JvmName("graphOfConnectionsList")
        fun <E:Any> fromConnections(connectionsList: List<Pair<E, Iterable<E>?>>) = AMGraph(0,
            connectionsList.map {
                it.first to (it.second?.map { it2 -> it2 to 1 } ?: emptyList())
            }
        )
        fun <E:Any> fromCollection(verticesList: Collection<E>) = AMGraph(0, verticesList.map { it to emptyList() })
    }
    constructor() : this(0, emptyList())


    private var vertices: ArrayList<E> = ArrayList()// Vert index --> E
    private var edgeMatrix: Array<IntArray> // [Vert index][Vert index] --> edge weight
    private val indexLookup = HashMap<E, Int>() // E --> Vert index
    private var dijkstraTables: Array<Array<Pair<Int, Int>>?>? // Vert index --> cached dijkstra table(pair of previous, distance to every vertex)

    /*------------------ FUNCTIONALITY ------------------*/

    override fun size() = vertices.size

    init {
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
        dijkstraTables = Array(size()) { null }
        edgeMatrix = Array(size()) { IntArray(size()) { -1 } }

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
    private fun get(from: Int, to: Int): Int? {
        return if (edgeMatrix[from][to] == -1) null
        else edgeMatrix[from][to]
    }

    override operator fun set(from: E, to: E, value: Int): Int? {
        return set(indexLookup[from]!!, indexLookup[to]!!, value)
    }
    private fun set(from: Int, to: Int, value: Int): Int? {
        dijkstraTables = null
        return get(from, to).also { edgeMatrix[from][to] = value }
    }

    override fun remove(from: E, to: E): Int? {
        return remove(indexLookup[from]!!, indexLookup[to]!!)
    }
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
        dijkstraTables = Array(size()) { null }
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

    override fun removeAll(verts: Collection<E>) {
        val vertexToRemove = Array(size()) { false }
        for (vertex in verts) {
            val id = indexLookup.remove(vertex) ?: continue
            vertexToRemove[id] = true
            vertices.removeAt(id)
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
        dijkstraTables = Array(size()) { null }
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
    private fun subgraphFromIds(verts : Collection<Int>):AMGraph<E>{ //This could be so much clearer, but I just love inline function 💕
        return graphOf(verts.map { from ->
            vertices[from] to ArrayList<Pair<E,Int>>().apply{verts.forEach{ t ->
                get(from,t)?.let{ add(vertices[t] to it) }
            }}
        })
    }
    /*------------------ RANDOMIZATION ------------------*/

    override fun randomize(avgConnectionsPerVertex: Int, minWeight: Int, maxWeight: Int, allowDisjoint: Boolean, random: Random) { //when inheritance removed add default values
        val probability = avgConnectionsPerVertex.toDouble()  / size()
        randomize(probability, minWeight, maxWeight, allowDisjoint, random)
    }

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
        if(!allowDisjoint) mergeDisjoint(maxWeight, random)
    }

    override fun mergeDisjoint(maxWeight: Int, random: Random){
        val bidirectional = this.getBidirectionalUnweighted() as AMGraph<E>
        var vertex = random.nextInt(size())
        var unreachables : List<Int>

        //Runs while there are any unreachable vertices from `vertex` (store all the unreachable ones in `unreachables`)
        //Vertices --> Unreachable non-self vertices --> Unreachable non-self id's
        while(vertices.filterIndexed { id, _ -> id != vertex && bidirectional.path(vertex, id).isEmpty() }.map{indexLookup[it]!!}.also{ unreachables = it }.isNotEmpty()){
            val from : Int
            val to : Int
            val weight = random.nextInt(maxWeight)
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
     * Finds a path between two vertices using either depth or breadth.
     * @param depth true will use depth first search false will use breadth first search
     * @param start The vertex to start from.
     * @param dest The vertex to end at.
     * @return A list of vertices representing the path between the two vertices.
     */
    private fun search(depth: Boolean, start: E, dest: E): List<E> {
        val dest = indexLookup[dest]!!
        val q = LinkedList<Int>()
        val prev = IntArray(size()) { -1 }

        q.addFirst(indexLookup[start]!!)
        prev[indexLookup[start]!!] = -2

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
            var next: Int = dest
            if (prev[dest] != -1) {
                while (next != indexLookup[start]!!) {
                    addFirst(vertices[next])
                    next = prev[next]
                }
                addFirst(start)
            }
        }
    }

    /**
     * Finds a path between two vertices using a depth first search.
     * @param start The vertex to start from.
     * @param dest The vertex to end at.
     * @return A list of vertices representing the path between the two vertices.
     */
    fun depthFirstSearch(start: E, dest: E): List<E> = search(true, start, dest)

    /**
     * Finds a path between two vertices using a breadth first search.
     * @param start The vertex to start from.
     * @param dest The vertex to end at.
     * @return A list of vertices representing the path between the two vertices.
     */
    fun breadthFirstSearch(start: E, dest: E): List<E> = search(false, start, dest)

    /**
     * Finds a path between two vertices using a depth first search.
     * @param start The vertex to start from.
     * @param dest The vertex to end at.
     * @return A list of vertices representing the path between the two vertices.
     */
    fun depthFirstSearch2(start: E, dest: E): List<E> {
        return depthFirstSearch2(indexLookup[start]!!, indexLookup[dest]!!, BooleanArray(size())).map { vertices[it] }
    }
    private fun depthFirstSearch2(vertex: Int, dest: Int, visited: BooleanArray): LinkedList<Int> {
        visited[vertex] = true
        for ((ob, dist) in edgeMatrix[vertex].withIndex()) {
            if (!visited[ob] && dist != -1) {
                if (ob == dest) return LinkedList<Int>().apply { addFirst(ob); addFirst(vertex) }
                depthFirstSearch2(ob, dest, visited).let { if (it.isNotEmpty()) return it.apply { addFirst(vertex) } }
            }
        }
        return LinkedList()
    }

    /**
     * Finds a path between two vertices using a breadth first search.
     * @param start The vertex to start from.
     * @param dest The vertex to end at.
     * @return A list of vertices representing the path between the two vertices.
     */
    fun breadthFirstSearch2(start: E, dest: E): List<E> {
        return breadthFirstSearch2(indexLookup[start]!!, indexLookup[dest]!!).map { vertices[it] }
    }

    private fun breadthFirstSearch2(vertex: Int, dest: Int): List<Int> {
        val q = LinkedList<Int>()
        val prev = IntArray(size()) { -1 }
        prev[vertex] = -2 //-2 represents start
        q.add(vertex)
        while (!q.isEmpty()) {
            val currVer = q.pop()
            for ((ob, dist) in edgeMatrix[currVer].withIndex()) {
                if (prev[ob] == -1 && dist != -1) {
                    q.addLast(ob)
                    prev[ob] = currVer
                }
            }
        }
        val path = LinkedList<Int>()
        var curr = dest
        path.addFirst(dest)
        while (prev[curr] != -2 && prev[curr] != -1) {
            path.addFirst(prev[curr])
            curr = prev[curr]
        }
        if (path.first() == vertex) return path
        return emptyList()
    }

    /* DIJKSTRA'S */
    override fun path(from: E, to: E): List<E> {
        return path(from, to, false)
    }
    fun path(from: E, to: E, useSimpleAlgorithm: Boolean = false): List<E> {
        return path(indexLookup[from]!!,indexLookup[to]!!).map { vertices[it] }
    }

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
     * Implements a Fibonacci Heap in Dijkstra's algorithm to queue vertices.
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

    private fun getDijkstraTable(fromIndex: Int): Array<Pair<Int, Int>> {
        if(dijkstraTables == null) dijkstraTables = Array(size()) {null}
        if (dijkstraTables!![fromIndex] == null) dijkstraTables!![fromIndex] = dijkstraFibHeap(fromIndex)
        return dijkstraTables!![fromIndex]!!
    }

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

    internal fun clearDijkstraCache() {
        dijkstraTables = null
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
    /**
     * Colors the graph
     */
    private fun color(maxColors: Int): Array<List<E>>? {
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

fun main() {
    val graph = AMGraph.graphOf(listOf(2 to listOf(1 to 1, 3 to 3), 0 to listOf(1 to 1, 3 to 3, 5 to 2)))
    graph.add(7)
    println(graph.path(2, 5, true))
    graph.add(9)
    println(graph.path(2, 5, true))
}