package boards.view
import androidx.compose.runtime.mutableStateListOf


import shared.ISubscriber
import boards.model.Model;
import boards.entities.Board


class ViewModel(private val model: Model): ISubscriber {
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
