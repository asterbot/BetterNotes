package graph_ui

import individual_board.entities.Note


// "Node" is a "Note" displayed on a "Board"
// - pos: relative coordinates between [0, 1] which can be scaled by the canvas size
// - mass: "Articles" have mass = 1, "Sections" have mass = 1 + num(children)
data class Node(
    var pos: Vec,
    var force: Vec = Vec(0f, 0f),
    val mass: Float = 1f,
    val note: Note
)

data class Edge(
    val id1: Int,
    val id2: Int,
    val maxDist: Float = 0.1f // currently unused
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

    // magnitude
    fun mag(): Float {
        return kotlin.math.sqrt(x * x + y * y)
    }

    // linear interpolation
    fun lerp(target: Vec, alpha: Float) {
        x = x + alpha * (target.x - x)
        y = y + alpha * (target.y - y)
    }
}