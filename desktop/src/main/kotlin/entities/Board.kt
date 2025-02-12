package entities

data class Board(
    var id: Int = 0,
    var name: String,
    var desc: String
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
    var count = 0
    for (board in this) {
        board.id = count++
    }
}