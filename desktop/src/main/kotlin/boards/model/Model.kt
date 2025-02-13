package boards.model;
import boards.entities.Board;
import boards.entities.addBoard;
import boards.entities.removeBoard;
import shared.IPublisher

class Model : IPublisher(){
    var boardList = mutableListOf<Board>();

    // Convention: id's start from 1
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

    fun newBoardId(): Int{
        // The board ID for a new board to be added
        return boardList.size + 1
    }

    fun save() {
        // Save to file
    }

}
