package individual_board.view
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.rememberCoroutineScope
import graph_ui.GraphModel
import shared.ISubscriber
import individual_board.model.IndvBoardModel;
import individual_board.entities.Note
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.bson.types.ObjectId
import shared.graphModel

// NOTE: Sort should be in ViewModel as it is a presentation logic
class IndvBoardViewModel(private val model: IndvBoardModel, val boardId: ObjectId?): ISubscriber {
    val noteList = mutableStateListOf<Note>()

    var sortFunc = {note1: Note, note2: Note -> note1.title.compareTo(note2.title)}

    init{
        model.subscribe(this)
        update()
        sortByDatetimeUpdated() // default sort
    }

    fun sortByTitle(){
        sortFunc = {note1: Note, note2: Note -> note1.title.compareTo(note2.title)}
        noteList.sortWith(sortFunc)
    }

    fun sortByDatetimeAccessed(){
        sortFunc = {note1: Note, note2: Note -> note1.datetimeAccessed.compareTo(note2.datetimeAccessed)}
        noteList.sortWith(sortFunc)
    }

    fun sortByDatetimeCreated(){
        sortFunc = {note1: Note, note2: Note -> note1.datetimeCreated.compareTo(note2.datetimeCreated)}
        noteList.sortWith(sortFunc)
    }

    fun sortByDatetimeUpdated(){
        sortFunc = {note1: Note, note2: Note -> note1.datetimeUpdated.compareTo(note2.datetimeUpdated)}
        noteList.sortWith(sortFunc)
    }

    override fun update() {
        noteList.clear()
        for (note in model.noteDict[boardId] ?: emptyList()){
            noteList.add(note)
        }

        noteList.sortWith(sortFunc)
    }
}