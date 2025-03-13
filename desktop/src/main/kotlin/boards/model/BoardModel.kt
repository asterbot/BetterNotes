package boards.model;
import boards.entities.*
import shared.ConnectionManager
import shared.IPublisher
import shared.individualBoardModel
import shared.persistence.IPersistence

class BoardModel(val persistence: IPersistence) : IPublisher(){
    var boardList = mutableListOf<Board>();

    init {
        persistence.connect()
        if (ConnectionManager.isConnected) {
            boardList = persistence.readBoards().toMutableList()
            notifySubscribers()
        }
        println(boardList)
    }

    fun initialize(){
        // Called when there is a reconnection
        if (ConnectionManager.isConnected) {
            boardList = persistence.readBoards().toMutableList()
            notifySubscribers()
        }
    }


    /*
    For all the functions below, first modify local data structures, then do same on DB and then notifySubscribers()
    */

    fun add(board: Board) {
        boardList.addBoard(board);
        individualBoardModel.noteDict[board.id]= mutableListOf()

        if (ConnectionManager.isConnected) {
            persistence.addBoard(board);
        }

        notifySubscribers();
    }

    fun del(board: Board) {
        boardList.removeBoard(board);
        individualBoardModel.noteDict.remove(board.id);

        if (ConnectionManager.isConnected) {
            persistence.deleteBoard(board.id, board.notes);
        }

        notifySubscribers();
    }

    fun update(board: Board, name: String, desc: String) {
        boardList.updateBoard(board, name, desc);

        if (ConnectionManager.isConnected) {
            persistence.updateBoard(board.id, name, desc, board.notes);
        }

        notifySubscribers();
    }


}
