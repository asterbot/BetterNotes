package view
import androidx.compose.runtime.mutableStateListOf


import model.ISubscriber
import model.Model
import model.BoardModel;
import entities.Board


class ViewModel(private val model: BoardModel): ISubscriber {
    val boardList = mutableStateListOf<Board>()

    init{
        model.subscribe(this)
    }

    override fun update(){
        boardList.clear()
        for (board in model.boardList){
            boardList.add(board)
        }
    }

}