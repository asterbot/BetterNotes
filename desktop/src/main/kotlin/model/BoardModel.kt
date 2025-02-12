package model;
import entities.Board;
import entities.addBoard;
import entities.removeBoard;

class BoardModel : IPublisher(){
    var boardList = mutableListOf<Board>();

    init {
        boardList = mutableListOf(
            Board(id=1, name="CS346", desc="App Development"),
            Board(id=2, name="CS341", desc="Algorithms"),
            Board(id=3, name="CS370", desc="Numerical Computation"),
        );
    }

    fun add(board: Board) {
        boardList.addBoard(board);
        notifySubscribers();
    }

    fun del(board: Board) {
        boardList.removeBoard(board);
        notifySubscribers();
    }

    fun save() {
        // Save to file
    }

}
