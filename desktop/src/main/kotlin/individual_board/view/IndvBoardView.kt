package individual_board.view
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import article.view.ArticleScreen
import boards.entities.Board
import boards.view.BoardViewScreen
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import fdg_layout.*
import individual_board.entities.Note
import kotlinx.coroutines.launch
import org.bson.types.ObjectId
import shared.*
import kotlin.random.Random

data class IndividualBoardScreen(
    val board: Board
): Screen{
    @Composable
    override fun Content() {
        IndividualBoardView(board)
    }
}

@Composable
internal fun Float.pxToDp(): Dp {
    return (this / LocalDensity.current.density).dp
}


@Composable
fun NoteButton(
    note: Note,
    board: Board,
    onDelete: (Note) -> Unit,
    onEdit: (Note) -> Unit
) {
    val navigator = LocalNavigator.currentOrThrow
    Box (
        modifier = Modifier.padding(15.dp),
    ) {
        Button(
            onClick = {
                println("DEBUG: Clicked ${note.title}")
                individualBoardModel.updateNoteAccessed(note, board)
                ScreenManager.push(navigator, ArticleScreen(board, note))
            },
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                // TODO: change colours based on tags? later
                containerColor = Colors.medTeal
            )
        ) {
            Column(
                modifier = Modifier.padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    note.title,
                    textAlign = TextAlign.Center,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    note.desc,
                    textAlign = TextAlign.Center,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        ActionMenu(
            onEdit = { onEdit(note) },
            onDelete = { onDelete(note) },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
        )
    }
}

fun initializeNotesByNoteListBuilder(fdgLayoutModel: FdgLayoutModel<Note>, noteList: List<Note>) {
    val objectIdToIndex = mutableMapOf<ObjectId, Int>()

    // nodes
    noteList.forEachIndexed { index, note ->
        val newNode = Node(
            pos = Vec(
                x = Random.nextFloat() * (fdgLayoutModel.startDisMultiplier * fdgLayoutModel.canvasWidth * 2) - (fdgLayoutModel.startDisMultiplier * fdgLayoutModel.canvasWidth),
                y = Random.nextFloat() * (fdgLayoutModel.startDisMultiplier * fdgLayoutModel.canvasHeight * 2) - (fdgLayoutModel.startDisMultiplier * fdgLayoutModel.canvasHeight)
            ),
            mass = 5f,
            data = note // assuming your Node uses `data` not `note`
        )
        fdgLayoutModel.nodes.add(newNode)
        objectIdToIndex[note.id] = index
    }

    // edges
    noteList.forEach { note ->
        val thisIndex = objectIdToIndex[note.id] ?: return@forEach
        note.relatedNotes.forEach { childId ->
            val childIndex = objectIdToIndex[childId]
            if (childIndex != null && fdgLayoutModel.edges.none { it.id1 == childIndex && it.id2 == thisIndex }) {
                fdgLayoutModel.edges.add(Edge(id1 = thisIndex, id2 = childIndex))
            }
        }
    }
}

@Composable
fun IndividualBoardView(
    board: Board,
) {
    val navigator = LocalNavigator.currentOrThrow

    val drawerState = rememberDrawerState(DrawerValue.Open)
    val drawerScope = rememberCoroutineScope()

    individualBoardViewModel = IndvBoardViewModel(individualBoardModel, board.id)

    var noteList by remember { mutableStateOf(individualBoardViewModel) }

    LaunchedEffect(noteList) {
        fdgLayoutModel.initializeGraph {
            initializeNotesByNoteListBuilder(this, noteList.noteList)
        }
    }

    // Sorting for notes
    var selectedSort by remember { mutableStateOf(individualBoardModel.currentSortType) }
    var reverseOrder by remember { mutableStateOf(individualBoardModel.currentIsReversed) }
    val sortOptions = listOf("Title", "Last Created", "Last Updated", "Last Accessed")
    var expandedSort by remember { mutableStateOf(false) }

    fun applySorting(type: String, reverse: Boolean) {
        when (type) {
            "Title" -> individualBoardModel.sortByTitle(board.id, reverse)
            "Last Created" -> individualBoardModel.sortByDatetimeCreated(board.id, reverse)
            "Last Updated" -> individualBoardModel.sortByDatetimeUpdated(board.id, reverse)
            "Last Accessed" -> individualBoardModel.sortByDatetimeAccessed(board.id, reverse)
        }
    }

    LaunchedEffect(selectedSort, reverseOrder) {
        applySorting(selectedSort, reverseOrder)
    }

    // Searching / filtering for notes
    var query by remember { mutableStateOf("") }
    val filteredNotes = if (query.isBlank()) {
        noteList.noteList
    } else {
        noteList.noteList.filter {
            it.title.contains(query, ignoreCase = true) ||
                    (it.desc?.contains(query, ignoreCase = true)?: false ||
                            it.relatedNotes.any { relatedNoteId ->
                                noteList.noteList.any { relatedNote ->
                                    relatedNote.id == relatedNoteId && relatedNote.title.contains(query, ignoreCase = true)
                                }
                            }
                            )
        }
    }

    val searchFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        searchFocusRequester.requestFocus()
    }

    val openAddSectionDialog = remember { mutableStateOf(false) }
    val openAddArticleDialog = remember { mutableStateOf(false) }
    val noteToEdit = remember { mutableStateOf<Note?>(null) }
    val noteToDelete = remember { mutableStateOf<Note?>(null) }

    fun addNote(title: String, desc: String, type: String, relatedNotes: List<Note>){
        var relatedNotesIds = mutableListOf<ObjectId>()
        for (note in relatedNotes){
            relatedNotesIds.add(note.id)
        }
        individualBoardModel.addNote(Note(ObjectId(), title, desc, relatedNotes = relatedNotesIds), board)
        fdgLayoutModel.initializeGraph {
            initializeNotesByNoteListBuilder(this, noteList.noteList)
        }
    }

    fun deleteNote(note: Note) {
        individualBoardModel.del(note, board)
        fdgLayoutModel.initializeGraph {
            initializeNotesByNoteListBuilder(this, noteList.noteList)
        }
    }

    fun editNote(note:Note, title: String, desc: String, relatedNotes: List<Note>){
        var relatedNotesIds = mutableListOf<ObjectId>()
        for (note in relatedNotes){
            relatedNotesIds.add(note.id)
        }
        individualBoardModel.updateNote(note, board.id, title, desc, relatedNotesIds)
        fdgLayoutModel.initializeGraph {
            initializeNotesByNoteListBuilder(this, noteList.noteList)
        }
    }


    Box(modifier = Modifier.fillMaxSize()
        .onPreviewKeyEvent {
            when {
                (it.key == Key.Escape) -> {
                    ScreenManager.push(navigator, BoardViewScreen())
                    true
                }
                else -> false
            }
        }
    ) {
        ModalNavigationDrawer(
            scrimColor = Colors.black.copy(alpha=.2f),
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    drawerContainerColor = Colors.veryLightTeal,
                    modifier = Modifier
                        .widthIn(min = 400.dp)
                        .fillMaxWidth(0.5f)
                ) {
                    IconButton(
                        onClick = {
                            drawerScope.launch {
                                drawerState.close()
                            }
                        },
                        modifier = Modifier.align(Alignment.End).padding(8.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close drawer")
                    }
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = board.name,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.h4,
                            modifier = Modifier.fillMaxWidth(0.8f)
                                .padding(8.dp),
                        )

                        Text(
                            text = board.desc,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.body1,
                            modifier = Modifier.fillMaxWidth(0.8f)
                                .padding(8.dp),
                        )

                        TextButton(
                            onClick = { ScreenManager.push(navigator, BoardViewScreen()) },
                            modifier = Modifier.padding(2.dp),
                            colors = textButtonColours()
                        ) {
                            Text("Back to All Boards")
                        }


                        // Search notes
                        OutlinedTextField(
                            value = query,
                            onValueChange = { newQuery -> query = newQuery },
                            label = { Text("Search notes") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = Colors.medTeal
                                )
                            },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .padding(8.dp)
                                .focusRequester(searchFocusRequester)
                            ,
                            colors = outlinedTextFieldColours()
                        )
                        // Sorting
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text("Sort by: ", modifier = Modifier.padding(end = 8.dp))

                            Box {
                                Text(
                                    text = selectedSort,
                                    modifier = Modifier
                                        .background(Color.LightGray, shape = RoundedCornerShape(4.dp))
                                        .padding(8.dp)
                                        .clickable {
                                            expandedSort = true
                                        }
                                )
                                DropdownMenu(
                                    expanded = expandedSort,
                                    onDismissRequest = { expandedSort = false },
                                    containerColor = Colors.veryLightTeal
                                ) {
                                    sortOptions.forEach { option ->
                                        DropdownMenuItem(
                                            text = { Text(option) },
                                            onClick = {
                                                selectedSort = option
                                                expandedSort = false
                                                applySorting(selectedSort, reverseOrder)
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Text("Reverse: ")

                            Switch(
                                checked = reverseOrder,
                                onCheckedChange = {
                                    reverseOrder = it
                                    applySorting(selectedSort, reverseOrder)
                                },
                                colors = switchColours()
                            )
                        }


                        BoxWithConstraints(
                            Modifier.fillMaxSize()
                                .padding(15.dp)
                                .background(Colors.lightGrey.times(1.03f).copy(red = Colors.lightGrey.red))
                                .weight(1f)
                        ) {
                            val maxWidthDp = maxWidth
                            val columnWidth = 300.dp // adjust to whatever size makes sense for your grid items
                            val columns = (maxWidthDp / columnWidth).toInt().coerceAtLeast(1)

                            val state = rememberLazyGridState()
                            LazyVerticalGrid(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(10.dp),
                                state = state,
                                columns = GridCells.Fixed(columns),
                            ) {
                                if (noteList.noteList.isEmpty() || filteredNotes.isEmpty())
                                    item(span = { GridItemSpan(maxLineSpan) }) {
                                    Column(
                                            modifier = Modifier.padding(top=15.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                        Text(
                                            text = "No notes available",
                                            fontSize = 20.sp,
                                            modifier = Modifier.padding(vertical = 30.dp),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                                else {
                                    for (note in filteredNotes) {
                                        item {
                                            NoteButton(
                                                note = note,
                                                board = board,
                                                onDelete = { noteToDelete.value = note },
                                                onEdit = { noteToEdit.value = note }
                                            )
                                        }
                                    }
                                }
                            }
                            VerticalScrollbar(
                                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                                adapter = rememberScrollbarAdapter(scrollState = state)
                            )

                            AddNoteMenu(
                                onAddArticle = { openAddArticleDialog.value = true },
                                modifier = Modifier
                                    .align(Alignment.BottomEnd) // bottom-right pos
                                    .padding(16.dp)
                            )
                        }
                    }
                }
            },
            gesturesEnabled = false,
            modifier = Modifier.onPreviewKeyEvent {
                when {
                    (it.isCtrlPressed && it.key == Key.Minus) -> {
                        drawerScope.launch {
                            drawerState.close()
                        }
                        true
                    }
                    (it.isCtrlPressed && it.key == Key.Equals) -> {
                        drawerScope.launch {
                            drawerState.open()
                        }
                        true
                    }
                    else -> false
                }
            }
        ) {
            FdgLayoutView(
                graphViewModel = fdgLayoutViewModel,
                onNodeClick = { note ->
                    individualBoardModel.updateNoteAccessed(note, board)
                    ScreenManager.push(navigator, ArticleScreen(board, note))
                },
                getLabel = { node -> node.title },
                getColor = { node -> Colors.darkTeal },
            )
            IconButton(
                onClick = {
                    drawerScope.launch {
                        drawerState.open()
                    }
                },
                modifier = Modifier.align(Alignment.TopStart)
                    .padding(start = 8.dp, top = 55.dp, end = 5.dp, bottom = 5.dp)

            ) {
                Icon(Icons.Default.Menu, contentDescription = "Open drawer")
            }
        }

        when {
            openAddArticleDialog.value -> {
                AddNoteDialog(
                    type = "Note",
                    onDismissRequest = { openAddArticleDialog.value = false },
                    onConfirmation = { title, desc, relatedNotes ->
                        addNote(title, desc, "article", relatedNotes)
                        openAddArticleDialog.value = false
                        println("relatedNotes: $relatedNotes")
                    },
                    onGetOtherNotes = { query ->
                        noteList.noteList.filter {
                            it.title.contains(query, ignoreCase = true)
                        }
                    }
                )
            }

            noteToEdit.value != null -> {
                EditNoteDialog(
                    noteTitle = noteToEdit.value?.title ?: "",
                    noteDesc = noteToEdit.value?.desc ?: "",
                    onDismissRequest = { noteToEdit.value = null },
                    onConfirmation = { title, desc, relatedNotes ->
                        editNote(noteToEdit.value as Note, title, desc, relatedNotes)
                        noteToEdit.value = null
                    },
                    initialRelatedNotes = noteList.noteList.filter { note ->
                        noteToEdit.value?.relatedNotes?.contains(note.id) == true
                    },
                    onGetOtherNotes = { query ->
                        noteList.noteList.filter {
                            it.title.contains(query, ignoreCase = true) && it.id != noteToEdit.value?.id
                        }
                    }
                )
            }

            noteToDelete.value != null -> {
                ConfirmationDialog(
                    onDismissRequest = { noteToDelete.value = null }, // Cancel deletion
                    onConfirmation = {
                        deleteNote(noteToDelete.value!!)
                        noteToDelete.value = null
                    },
                    dialogTitle = "Delete ${noteToDelete.value?.title}",
                    dialogText = "Are you sure you want to '${noteToDelete.value?.title}'? This action cannot be undone."
                )
            }
        }
    }
}
