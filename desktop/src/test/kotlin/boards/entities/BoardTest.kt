package boards.entities
import kotlin.test.*

class BoardTest() {
    var boardList: MutableList<Board> = mutableListOf()
    var board1: Board = Board(1, "name1", "desc1")
    var board2: Board = Board(2, "name2", "desc2")

    @Test
    fun addBoard() {
        val oldCount = boardList.size
        boardList.addBoard(board1)
        assertEquals(oldCount + 1, boardList.size)
     // NOTE: Reindex not tested; logic expected to change when database is implemented
    }

    @Test
    fun removeBoard() {
        boardList.addBoard(board1)
        val oldCount = boardList.size
        boardList.removeBoard(board1)
        assertEquals(oldCount - 1, boardList.size)
    }

    @Test
    fun updateBoard() {
        boardList.addBoard(board1)
        val oldCount = boardList.size
        boardList.updateBoard(board1, "newName", "newDesc")
        assertEquals(oldCount, boardList.size)
        assertEquals("newName", boardList[0].name)
        assertEquals("newDesc", boardList[0].desc)
    }

}