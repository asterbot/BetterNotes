package boards.model;
import boards.entities.*
import shared.ConnectionManager
import shared.IPublisher
import shared.dbQueue
import shared.individualBoardModel
import shared.persistence.Create
import shared.persistence.Delete
import shared.persistence.IPersistence
import shared.persistence.Update
import java.time.Instant

class BoardModel(val persistence: IPersistence) : IPublisher(){
    var boardList = mutableListOf<Board>();
    var sortFunc: (Board, Board) -> Int = { b1, b2 -> b1.name.compareTo(b2.name) }
    var currentSortType: String = "Title"

    init {
        persistence.connect()
    }

    fun initialize(){
        // Called when there is a reconnection
        persistence.connect()
        if (ConnectionManager.isConnected) {
            boardList = persistence.readBoards().toMutableList()
            notifySubscribers()
        }
    }

    // Sorting functions
    private fun sortBoardList() {
        boardList.sortWith(sortFunc)
    }

    fun sortByTitle(reverse: Boolean = false) {
        currentSortType = "Title"
        sortFunc = { b1, b2 -> b1.name.compareTo(b2.name) }
        sortBoardList()
        if (reverse) boardList.reverse()
        notifySubscribers()
    }

    fun sortByDatetimeAccessed(reverse: Boolean = false) {
        currentSortType = "Time Accessed"
        sortFunc = { b1, b2 -> b2.datetimeAccessed.compareTo(b1.datetimeAccessed) }
        sortBoardList()
        if (reverse) boardList.reverse()
        notifySubscribers()
    }

    fun sortByDatetimeCreated(reverse: Boolean = false) {
        currentSortType = "Time Created"
        sortFunc = { b1, b2 -> b2.datetimeCreated.compareTo(b1.datetimeCreated) }
        sortBoardList()
        if (reverse) boardList.reverse()
        notifySubscribers()
    }

    fun sortByDatetimeUpdated(reverse: Boolean = false) {
        currentSortType = "Time Updated"
        sortFunc = { b1, b2 -> b2.datetimeUpdated.compareTo(b1.datetimeUpdated) }
        sortBoardList()
        if (reverse) boardList.reverse()
        notifySubscribers()
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
        else{
            dbQueue.addToQueue(Create(persistence, board))
        }

        notifySubscribers();
    }

    fun del(board: Board) {
        boardList.removeBoard(board);
        individualBoardModel.noteDict.remove(board.id);

        if (ConnectionManager.isConnected) {
            persistence.deleteBoard(board.id, board.notes);
        }
        else{
            dbQueue.addToQueue(Delete(persistence, board, noteListDependency = board.notes))
        }

        notifySubscribers();
    }

    fun update(board: Board, name: String, desc: String) {
        boardList.updateBoard(board, name, desc);

        if (ConnectionManager.isConnected) {
            persistence.updateBoard(board.id, name, desc, board.notes);
        }
        else{
            dbQueue.addToQueue(Update(persistence, board,
                mutableMapOf("name" to name, "desc" to desc, "notes" to board.notes)))
        }

        notifySubscribers();
    }

    fun updateAccessed(board: Board) {
        board.datetimeAccessed = Instant.now().toString()
        if (ConnectionManager.isConnected) {
            persistence.updateBoardAccessed(board.id)
        }
    }
}
