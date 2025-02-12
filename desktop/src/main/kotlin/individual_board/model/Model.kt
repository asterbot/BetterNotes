package individual_board.model
import boards.entities.Board
import individual_board.entities.Note
import individual_board.entities.addNote
import individual_board.entities.removeNote
import individual_board.entities.Section
import individual_board.entities.Article
import individual_board.entities.ContentBlock
import individual_board.entities.MarkdownBlock
import shared.IPublisher


class Model(boardId: Int) : IPublisher() {
    var noteList = mutableListOf<Note>()

    fun getNoteListFromBoardId(boardId: Int): MutableList<Note> {
        if (boardId == 0) {
            var sampleNote1 = Section(
                id = 1,
                title = "CS346 Course Logistics",
                desc = "Course Outline, Course Schedule, Grading Policy, etc.",
            )

            var sampleNote2 = Section(
                id = 2,
                title = "Agile Development",
                desc = "I LOVE AGILE DEVELOPMENT <3",
            )

            val sampleNote3 = Article(
                id = 3,
                title = "Stand-Up Meetings",
                desc = "\uD83E\uDDCD",
                parentNotes = mutableListOf(sampleNote2),
                contentBlocks = mutableListOf(
                    MarkdownBlock(
                        text = "Stand-up meetings are a daily ritual in the agile development process. They are short, focused meetings that are intended to keep the team on track and moving forward. The idea is to have a quick, 15-minute meeting every day to discuss what each team member is working on, what they plan to work on next, and any obstacles they are facing. The goal is to keep the team in sync and to identify and address any issues that may be slowing down progress."
                    ),
                    MarkdownBlock(
                        text = "They must be done standing up. \uD83E\uDDCD \uD83E\uDDCD \uD83E\uDDCD"
                    )
                )
            )

            return mutableListOf(
                sampleNote1,
                sampleNote2,
                sampleNote3
            )
        }
        else {
            return mutableListOf()
        }
    }

    init {
        noteList = getNoteListFromBoardId(boardId)
        println("DEBUG: noteList, $noteList")
    }

    fun addSection(section: Section) {
        noteList.addNote(section)
        notifySubscribers()
    }

    fun addArticle(article: Article) {
        noteList.addNote(article)
        notifySubscribers()
    }

    fun del(note: Note) {
        noteList.removeNote(note)
        notifySubscribers()
    }
}
