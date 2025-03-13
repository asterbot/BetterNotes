package individual_board.view
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import shared.ISubscriber
import individual_board.model.IndvBoardModel;
import individual_board.entities.Note
import org.bson.types.ObjectId


class IndvBoardViewModel(private val model: IndvBoardModel, val boardId: ObjectId?): ISubscriber {
    val noteList = mutableStateListOf<Note>()

    init{
        model.subscribe(this)
        update()
    }

    override fun update() {
        noteList.clear()
        for (note in model.noteDict[boardId] ?: emptyList()){
            noteList.add(note)
        }
    }
}