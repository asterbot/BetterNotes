package boards.model;
import boards.entities.Board
import boards.entities.addBoard
import boards.entities.removeBoard
import boards.entities.updateBoard
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
    var currentSortType: String = "Last Accessed"
    var currentIsReversed: Boolean = false

    init {
        persistence.connect()
    }

    fun initialize(){
        // called when there is a reconnection
        persistence.connect()
        if (ConnectionManager.isConnected) {
            boardList = persistence.readBoards().toMutableList()
            notifySubscribers()
        }
    }

    // sorting functions
    private fun sortBoardList() {
        boardList.sortWith(sortFunc)
        if (currentIsReversed) boardList.reverse()
    }

    fun sortByTitle(reverse: Boolean = false) {
        currentSortType = "Title"
        currentIsReversed = reverse
        sortFunc = { b1, b2 -> (b1.name.lowercase()).compareTo(b2.name.lowercase()) }
        sortBoardList()
        notifySubscribers()
    }

    fun sortByDatetimeAccessed(reverse: Boolean = false) {
        currentSortType = "Last Accessed"
        currentIsReversed = reverse
        sortFunc = { b1, b2 -> b2.datetimeAccessed.compareTo(b1.datetimeAccessed) }
        sortBoardList()
        notifySubscribers()
    }

    fun sortByDatetimeCreated(reverse: Boolean = false) {
        currentSortType = "Last Created"
        currentIsReversed = reverse
        sortFunc = { b1, b2 -> b2.datetimeCreated.compareTo(b1.datetimeCreated) }
        sortBoardList()
        notifySubscribers()
    }

    fun sortByDatetimeUpdated(reverse: Boolean = false) {
        currentSortType = "Last Updated"
        currentIsReversed = reverse
        sortFunc = { b1, b2 -> b2.datetimeUpdated.compareTo(b1.datetimeUpdated) }
        sortBoardList()
        notifySubscribers()
    }

    // for all the functions below, first modify local data structures, then do same on DB and then notifySubscribers()

    fun add(board: Board) {
        boardList.addBoard(board);
        individualBoardModel.noteDict[board.id]= mutableListOf()

        if (ConnectionManager.isConnected) {
            persistence.addBoard(board);
        }
        else{
            dbQueue.addToQueue(Create(persistence, board))
        }
        sortBoardList()
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
        sortBoardList()
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
        sortBoardList()
        notifySubscribers();
    }

    fun updateAccessed(board: Board) {
        board.datetimeAccessed = Instant.now().toString()
        if (ConnectionManager.isConnected) {
            persistence.updateBoardAccessed(board.id)
        }
    }
}
