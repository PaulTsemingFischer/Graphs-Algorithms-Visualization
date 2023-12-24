package com.fischerabruzese.graph

import java.util.LinkedList
import java.util.PriorityQueue

/* Construct a graph with outboundConnections containing:
        outboundConnection(Source, ArrayOf(Destination, Weight))
*/
class AMGraph<E:Any>(vararg outboundConnections : Pair<E,Iterable<Pair<E,Int>>>?) : Graph<E>() {

//    constructor(vararg outboundConnectionsUnweighted: Pair<E, Iterable<E>>) : this(
//        *outboundConnectionsUnweighted.map { (source, destinations) ->
//            source to destinations.map { it to 1 }
//        }.toTypedArray()
//    )

    constructor(vararg vertices: E, usingVertexContructor : Boolean) : this(
        *vertices.map { it to emptyList<Pair<E, Int>>()}.toTypedArray()
    )

    internal constructor() : this(
        null
    )

    //do we need to store the index? is the index the location in the array?
    data class Vertex<E>(val item : E)

    private var vertices : ArrayList<Vertex<E>> = ArrayList()
    private var edgeMatrix : Array<IntArray>
    private val indexLookup = HashMap<E, Int>()

    init {
        for(connections in outboundConnections){
            if(connections == null) continue
            if(indexLookup.putIfAbsent(connections.first, vertices.size) == null){
                vertices.add(Vertex(connections.first))
            }
            for(outboundEdge in connections.second){
                if(indexLookup.putIfAbsent(outboundEdge.first, vertices.size) == null){
                    vertices.add(Vertex(outboundEdge.first))
                }
            }
        }
        edgeMatrix = Array(vertices.size) {IntArray(vertices.size) {-1} }

        for(connections in outboundConnections) {
            if(connections == null) continue
            for(outboundEdge in connections.second){
                edgeMatrix[indexLookup[connections.first]!!][indexLookup[outboundEdge.first]!!] = outboundEdge.second
            }
        }
    }

    override operator fun get(from : E, to : E ) : Int? {
        return indexLookup[from]?.let { f ->
            indexLookup[to]?.let { t ->
                get(f, t)
            }
        }
    }

    private fun get(from : Int, to : Int) : Int? {
        return if (edgeMatrix[from][to] == -1) null
        else edgeMatrix[from][to]
    }

    override operator fun set(from: E, to: E, value: Int): Int? {
        return indexLookup[from]?.let { f ->
            indexLookup[to]?.let { t ->
                set(f, t, value)
            }
        }
    }

    private fun set(from : Int, to : Int, value : Int) : Int? {
        return get(from, to).also { edgeMatrix[from][to] = value }
    }

    override fun iterator(): Iterator<E> {
        TODO("Not yet implemented")
    }

    override fun getVerticies(): Set<E> {
        return vertices.map { it.item }.toSet()
    }

    override fun toString(): String {
        val string = StringBuilder()
        for(destinations in edgeMatrix){
            string.append("[")
            for (weight in destinations){
                string.append("${weight.let{if(it == -1) " " else it}}][")
            }
            string.deleteRange(string.length - 2, string.length)
            string.append("]\n")
        }
        return string.toString()
    }

    fun randomize(chanceFilled : Double, maxWeight: Int){
        for(i in edgeMatrix.indices) {
            for (j in edgeMatrix.indices) {
                if (chanceFilled > Math.random()) {
                    set(i, j, (1..maxWeight).random())
                } else {
                    set(i, j, -1)
                }
            }
        }
    }

    //Pre-condition: "from" is a valid vertex
    fun getAllDijkstra(from : E) : Array<Pair<List<E>, Int>> {
        val fromIndex = indexLookup[from]!!
        return dikstra(fromIndex, null).let{
            it.mapIndexed{ index, pair ->
                getPath(fromIndex, index, it).map {vertex ->  vertices[vertex].item } to pair.second
            }
        }.toTypedArray()
    }

    fun getDijkstra(from: E, to: E) : Pair<List<E>, Int>{
        val fromIndex = indexLookup[from]!!
        val toIndex = indexLookup[to]!!
        return dikstra(fromIndex, toIndex).let{
            getPath(fromIndex, toIndex, it).map{vertex -> vertices[vertex].item} to it[toIndex].second
        }
    }

    fun getDijkstraWeight(from: E, to: E) : Int{
        val fromIndex = indexLookup[from]!!
        val toIndex = indexLookup[to]!!
        return dikstra(fromIndex, toIndex)[toIndex].second
    }

    fun getDijkstraPath(from: E, to: E) : List<E>{
        return getDijkstra(from, to).first
    }

    fun getAllDijkstras(from : E) : Array<Pair<List<E>, Int>> {
        val fromIndex = indexLookup[from]!!
        return abruzeseDijkstras(fromIndex).let{
            it.mapIndexed{ index, pair ->
                abruzeseGetPath(index, it).map {vertex ->  vertices[vertex].item } to 0
            }
        }.toTypedArray()
    }

    private fun abruzeseDijkstras(from : Int): Array<LinkedList<Int>> {
        data class DikstraVertex(val location : Int, var previous : DikstraVertex?, var distFromSrc : Int, var visited : Boolean)
        var start : DikstraVertex? = null
        val dikstrasVertices = List(vertices.size) { i -> DikstraVertex(i, null, Int.MAX_VALUE, false).also { if(i == from) start = it; it.distFromSrc = 0} }

        val que = LinkedList<DikstraVertex>()
        que.add(start!!)
        while(que.size != 0) {
            val cur = que.peek()
            for ((next, distToNext) in edgeMatrix[cur.location].withIndex()) {
                if (next != -1 && !dikstrasVertices[next].visited && dikstrasVertices[next].distFromSrc < cur.distFromSrc + distToNext) {
                    dikstrasVertices[next].previous = cur
                    dikstrasVertices[next].distFromSrc = cur.distFromSrc + distToNext
                    que.add(dikstrasVertices[next])
                }
            }
            que.poll().visited = true
        }
        //the index in the array is the path it is to
        return Array(vertices.size) { i ->
            var path = LinkedList<Int>()
            var j = i
            while(dikstrasVertices[j] != start){
                if(dikstrasVertices[j].previous == null) {
                    path = emptyList<Int>() as LinkedList<Int>
                    break
                }
                else j = dikstrasVertices[j].previous!!.location
                path.addFirst(dikstrasVertices[j].location)
            }
            path
        }
    }
    private fun abruzeseGetPath(to: Int, table : Array<LinkedList<Int>>): List<Int> {
        return table[to]
    }


    //Pre-condition: If "to" is null, finds every path from "from", else only the path from "from" to "to" is accurate
    //Post-condition: A Int.MAX_VALUE in distance indicates unreachable, a -1 in Prev indicates no path
    //Post-condition: Returns an array of (previous vertex index, distance)
    private fun dikstra(from : Int, to : Int?) : Array<Pair<Int, Int>> {
        val distance = IntArray(vertices.size) { Int.MAX_VALUE }
        val prev = IntArray(vertices.size) { -1 }
        val visited = BooleanArray(vertices.size) { false }

        distance[from] = 0
        while(to == null || !visited[to]){
            //Determine the next vertex to visit
            var currVert = visited.indexOfFirst{!it} //Finds first unvisited
            if(currVert == -1 || distance[currVert] == Int.MAX_VALUE) break //All visited//TODO: might cause issues
            for(i in currVert + 1 until visited.size){//TODO: +1 might cause issues
                if(!visited[i] && distance[i] < distance[currVert]){
                    currVert = i
                }
            }

            //Update distances and previous
            val currDist = distance[currVert]
            for(i in visited.indices){
                val edge = edgeMatrix[currVert][i]
                if(!visited[i] && edge != -1 && currDist + edge < distance[i]){
                    distance[i] = (currDist + edgeMatrix[currVert][i])
                    prev[i] = currVert
                }
            }

            //Update visited
            visited[currVert] = true
        }
        return prev.zip(distance).toTypedArray() //funky function
    }

    private fun getPath(from: Int, to: Int, dikstraTable: Array<Pair<Int, Int>>): List<Int> {
        val path = LinkedList<Int>()
        path.add(to)
        var curr = to
        while(path.firstOrNull() != from){
            path.addFirst(dikstraTable[curr].first.also{curr = it})
        }
        return path
    }
}