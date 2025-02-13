package individual_board.view
import androidx.compose.runtime.mutableStateListOf
import shared.ISubscriber
import individual_board.model.Model;
import individual_board.entities.Note


class ViewModel(private val model: Model): ISubscriber {
    val noteList = mutableMapOf<Int, MutableList<Note>>()

    init{
        model.subscribe(this)
    }

    override fun update() {
        println("IDK")
    }
}