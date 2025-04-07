package boards.view
import androidx.compose.runtime.mutableStateListOf


import shared.ISubscriber
import boards.model.BoardModel;
import boards.entities.Board


class BoardViewModel(private val model: BoardModel): ISubscriber {
    val boardList = mutableStateListOf<Board>()


    init{
        model.subscribe(this)
        update()
    }

    override fun update(){
        boardList.clear()
        for (board in model.boardList){
            boardList.add(board)
        }
    }
}
