package com.fischerabruzese.graphsFX

import java.math.BigInteger
import java.util.*

private class HuffmanTree<T : Comparable<T>>(private var valueFrequencies: List<ValueAndFrequency<T>>) {
    val rootNode: Node<T>
    val leafNodes = ArrayList<Node<T>>()

    init {
        valueFrequencies = valueFrequencies.sorted() //sort list of chars
        val valueFrequencyNodes: ArrayList<Node<T>> = ArrayList() //arraylist of nodes
        for (valueFrequency in valueFrequencies) {
            valueFrequencyNodes.add(Node(valueFrequency)) //filling the arraylist with leaf nodes
        }


        leafNodes.addAll(valueFrequencyNodes)
        if(valueFrequencyNodes.size == 1) { //special case where we have to add an arbitrary parent to create an encoding path otherwise the encoding will have no path to create binary from
            rootNode = Node(ValueAndFrequency(null ,valueFrequencyNodes[0].data.frequency), valueFrequencyNodes[0])
        }
        else rootNode = createTree(valueFrequencyNodes)
    }

    //Precondition: valueFrequencyNodes must be sorted
    private fun createTree(valueFrequencyNodes: ArrayList<Node<T>>) : Node<T>{
        if(valueFrequencyNodes.size == 1) return valueFrequencyNodes[0]

        val firstNode = valueFrequencyNodes[0]
        val secondNode = valueFrequencyNodes[1]

        valueFrequencyNodes.remove(firstNode)
        valueFrequencyNodes.remove(secondNode)

        val combinedNode = Node(combinedValue(firstNode.data, secondNode.data), firstNode, secondNode)
        addNodeInPlace(valueFrequencyNodes, combinedNode)
        firstNode.parentAndIsRightNode = Pair(combinedNode, false)
        secondNode.parentAndIsRightNode = Pair(combinedNode, true)

        return createTree(valueFrequencyNodes)
    }

    private fun combinedValue(first: ValueAndFrequency<T>, second: ValueAndFrequency<T>) : ValueAndFrequency<T> {
        return ValueAndFrequency(null, first.frequency + second.frequency)
    }

    private fun addNodeInPlace(valueFrequencyNodes: ArrayList<Node<T>>, node: Node<T>) : ArrayList<Node<T>>{

        for((insertionIndex, item) in valueFrequencyNodes.withIndex()){
            if(node.data < item.data){
                valueFrequencyNodes.add(insertionIndex, node)
                return valueFrequencyNodes
            }
        }

        valueFrequencyNodes.add(node)
        return valueFrequencyNodes
    }

    data class Node<T : Comparable<T>>(
        var data: ValueAndFrequency<T>,
      val left: Node<T>? = null,
      val right: Node<T>? = null,
      var parentAndIsRightNode: Pair<Node<T>, Boolean>? = null
    )
}

private data class ValueAndFrequency<T : Comparable<T>>(val value: T?, var frequency: Int) : Comparable<ValueAndFrequency<T>> {
    override fun compareTo(other: ValueAndFrequency<T>): Int {
        val frequencyDifference: Int = this.frequency - other.frequency
        if(frequencyDifference == 0 && this.value != null && other.value != null){
            return value.compareTo(other.value)
        }
        return frequencyDifference
    }

    override fun toString() : String{
        if (value != null){
            return if(value.javaClass.toString() == "class java.lang.Character" && value == '\n'){
                "\\n:$frequency"
            } else {
                "$value:$frequency"
            }
        }
        return "$frequency"
    }
}

fun encode(str: String): BigInteger {
    //Encoding
    val frequencyArray = createFrequencyArray(str)
    val huffmanTree = HuffmanTree(frequencyArray)
    val itemHashMap = getEncoding(huffmanTree)
    val encodedText = createEncodedBinary(str, itemHashMap)
    val encodedHeader = getHeaderEncoding(frequencyArray)

    val string = booleanArrayToString(encodedHeader + encodedText)
    return BigInteger(string, 2)
}

fun main() {
    println(encode("testing"))
}

private fun booleanArrayToString(arr: BooleanArray): String {
    val result = StringBuilder()
    for(i in arr.indices){
        result.append(if(arr[i]) "1" else "0")
    }
    return result.toString()
}

//finalSize == 0 for variable length(will return an integer number of bytes)
private fun intToBooleanList(inputInt: Int,  finalSize: Int = 0) : List<Boolean> {
    var i = inputInt
    val binaryRepresentedInt = LinkedList<Boolean>()
    while (i > 0) {
        binaryRepresentedInt.addFirst(i and 1 == 1)
        i = (i shr 1)
    }

    //Padding zeros until it is an integer number of bytes
    if(finalSize == 0) {
        while (binaryRepresentedInt.size % 8 != 0) {
            binaryRepresentedInt.addFirst(false)
        }
    }
    else{
        while (binaryRepresentedInt.size < finalSize) {
            binaryRepresentedInt.addFirst(false)
        }
    }
    return binaryRepresentedInt
}
private fun shortToBooleanList(inputShort: Short, finalSize: Int = 0) : List<Boolean> {
    return intToBooleanList(inputShort.toInt(), finalSize)
}


private fun createFrequencyArray(passage: String) : ArrayList<ValueAndFrequency<Char>> {
    val valFrequencies: ArrayList<ValueAndFrequency<Char>> = ArrayList()
    for (c in passage.toCharArray()){
        var itemFound = false
        for (item in valFrequencies){
            if (c == item.value){
                item.frequency++
                itemFound = true
                break
            }
        }
        if(!itemFound) valFrequencies.add(ValueAndFrequency(c, 1))
    }
    return valFrequencies
}

private fun <T : Comparable<T>> getEncoding(huffmanTree: HuffmanTree<T>): HashMap<T, BooleanArray> {
    val leafNodes = huffmanTree.leafNodes
    val itemEncodings: HashMap<T, BooleanArray> = HashMap()

    for (node in leafNodes) {
        val itemArray = getBooleanArray(node, ArrayList())
        itemEncodings[node.data.value!!] = itemArray
    }

    return itemEncodings
}

private fun <T : Comparable<T>> createEncodedBinary(input : String, hashMap: HashMap<T, BooleanArray>) : BooleanArray {
    var dataNumBits = 0
    val data = ArrayList<BooleanArray>()
    for (c in input.toCharArray()){
        @Suppress("UNCHECKED_CAST") val binaryValue: BooleanArray? = hashMap[c as T]
        data.add(binaryValue!!) //it won't be null rightttttt
        dataNumBits += binaryValue.size
    }
    var compressedDataIterator = 0
    val compressedData = BooleanArray(dataNumBits)
    for(i in data.indices){
        for(j in 0..<data[i].size){
            compressedData[compressedDataIterator++] = data[i][j] //There has to be a better way than using an iterator right?
        }
    }
    return compressedData
}

private fun getHeaderEncoding(frequencyArray: ArrayList<ValueAndFrequency<Char>>): BooleanArray {
    /*
    Parts of a header encoding
      - 32 bits for the int representing the length of the header in bits
      - A boolean array with each bit representing the identity of the bytes following the array, 0 = char byte, 1 = frequency byte
      - Alternating sets of groups of char bytes and groups of int bytes representing the frequency of each character
     */

    val headerEncoding = LinkedList<Boolean>()
    val headerByteKey = LinkedList<Boolean>()

    for (valFreq in frequencyArray){
        val valEncode = shortToBooleanList(valFreq.value!!.code.toShort())
        val freqEncode = intToBooleanList(valFreq.frequency)

        //Updating headerByteDecodeKey
        for(i in valEncode.indices step 8){
            headerByteKey.add(false)
        }
        for(i in freqEncode.indices step 8){
            headerByteKey.add(true)
        }

        //Adding to main header
        headerEncoding.addAll(valEncode)
        headerEncoding.addAll(freqEncode)
    }

    //Adding booleanArray representing the identity(char vs frequency) of each bit
    headerEncoding.addAll(0, headerByteKey)

    //Adding length of header
    headerEncoding.addAll(0, intToBooleanList(headerEncoding.size + 32, 32))

    return headerEncoding.toBooleanArray()
}



private fun <T : Comparable<T>> getBooleanArray(node: HuffmanTree.Node<T>, bitArrList: ArrayList<Boolean>) : BooleanArray {
    if (node.parentAndIsRightNode == null) {
        bitArrList.reverse()
        return BooleanArray(bitArrList.size) { bitArrList[it] }
    }
    bitArrList.add(node.parentAndIsRightNode!!.second)
    return getBooleanArray(node.parentAndIsRightNode!!.first, bitArrList)
}