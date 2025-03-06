package individual_board.view
import shared.ISubscriber
import individual_board.model.IndvBoardModel;
import individual_board.entities.Note


class ViewModel(private val model: IndvBoardModel): ISubscriber {
    val noteList = mutableMapOf<Int, MutableList<Note>>()

    init{
        model.subscribe(this)
    }

    override fun update() {
        println("IDK")
    }
}