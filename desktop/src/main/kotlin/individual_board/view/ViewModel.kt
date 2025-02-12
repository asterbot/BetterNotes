package individual_board.view
import androidx.compose.runtime.mutableStateListOf
import shared.ISubscriber
import individual_board.model.Model;
import individual_board.entities.Note


class ViewModel(private val model: Model): ISubscriber {
    val noteList = mutableStateListOf<Note>()

    init{
        model.subscribe(this)
    }

    override fun update(){
        noteList.clear()
        for (note in model.noteList){
            noteList.add(note)
        }
    }
}