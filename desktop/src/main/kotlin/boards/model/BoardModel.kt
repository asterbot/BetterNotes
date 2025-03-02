package boards.model;
import boards.entities.*
import shared.IPublisher

class BoardModel : IPublisher(){
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

    fun update(board: Board, name: String, desc: String) {
        boardList.updateBoard(board, name, desc);
        notifySubscribers();
    }

    fun newBoardId(): Int{
        // The board ID for a new board to be added
        return boardList.size
    }

    fun save() {
        // Save to file
    }

}
