package boards.entities

// Entities holds the main data for the class and provides manipulator functions

data class Board(
    var id: Int = 0,
    var name: String,
    var desc: String,
    var canDelete: Boolean = false,
)

fun MutableList<Board>.addBoard(element: Board): Boolean {
    this.add(element)
    this.reindex()
    return true
}

fun MutableList<Board>.removeBoard(element: Board): Boolean {
    this.remove(element)
    this.reindex()
    return true
}

private fun MutableList<Board>.reindex() {
    var count = 1
    for (board in this) {
        board.id = count++
    }
}
