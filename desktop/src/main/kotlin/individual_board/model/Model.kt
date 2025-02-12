package individual_board.model
import boards.entities.Board
import individual_board.entities.Note
import individual_board.entities.addNote
import individual_board.entities.removeNote
import shared.IPublisher


class Model(boardId: Int) : IPublisher() {
    var noteList = mutableListOf<Note>()

    fun getNoteListFromBoardId(boardId: Int): MutableList<Note> {
        if (boardId == 1) {
            return mutableListOf(
                Note(
                    id = 1,
                    title = "CS346 Course Logistics",
                    desc = "Course Outline, Course Schedule, Grading Policy, etc.",
                ),
                Note(
                    id = 2,
                    title = "Agile Development",
                    desc = "",
                ),
            )
        }


    }

    init {
        getNoteListFromBoardId(boardId)
    }


}

//package boards.model;
//import boards.entities.Board;
//import boards.entities.addBoard;
//import boards.entities.removeBoard;
//import shared.IPublisher
//
//class Model : IPublisher(){
//    var boardList = mutableListOf<Board>();
//
//    init {
//        boardList = mutableListOf(
//            Board(id=1, name="CS346", desc="App Development"),
//            Board(id=2, name="CS341", desc="Algorithms"),
//            Board(id=3, name="CS370", desc="Numerical Computation"),
//        );
//    }
//
//    fun add(board: Board) {
//        boardList.addBoard(board);
//        notifySubscribers();
//    }
//
//    fun del(board: Board) {
//        boardList.removeBoard(board);
//        notifySubscribers();
//    }
//
//    fun save() {
//        // Save to file
//    }
//
//}
