package fdg_layout

data class Node<NodeDataType>(
    var pos: Vec, // relative pos from [0, 1], later scaled by screen size
    var force: Vec = Vec(0f, 0f),
    val mass: Float = 1f,
    val data: NodeDataType // the actual data stored in each Node
)

data class Edge(
    val id1: Int,
    val id2: Int,
)

data class Vec(var x: Float, var y: Float) {
    fun copy() = Vec(x, y)

    fun add(other: Vec): Vec {
        x += other.x
        y += other.y
        return this
    }
    fun sub(other: Vec): Vec {
        x -= other.x
        y -= other.y
        return this
    }
    fun mult(factor: Float): Vec {
        x *= factor
        y *= factor
        return this
    }
    fun div(factor: Float): Vec {
        x /= factor
        y /= factor
        return this
    }

    fun mag(): Float {
        return kotlin.math.sqrt(x * x + y * y)
    }

    fun lerp(target: Vec, alpha: Float) {
        x += alpha * (target.x - x)
        y += alpha * (target.y - y)
    }
}