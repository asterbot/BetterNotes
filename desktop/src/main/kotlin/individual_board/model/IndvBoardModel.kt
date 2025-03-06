package individual_board.model
import individual_board.entities.Note
import individual_board.entities.addNote
import individual_board.entities.removeNote
import individual_board.entities.Section
import article.entities.*
import shared.IPublisher


class IndvBoardModel() : IPublisher() {
    // maps board ID to list of notes

    var noteDict = mutableMapOf<Int, MutableList<Note>>(
        1 to mutableListOf(
            Section(
                id = 1,
                title = "CS346 Course Logistics",
                desc = "Course Outline, Course Schedule, Grading Policy, etc.",
            ),
            Section(
                id = 2,
                title = "Agile Development",
                desc = "I LOVE AGILE DEVELOPMENT <3",
            ),
            Article(
                id = 3,
                title = "Stand-Up Meetings",
                desc = "\uD83E\uDDCD",
                contentBlocks = mutableListOf(
                    MarkdownBlock(
                        text = "Stand-up meetings are a daily ritual in the agile development process. They are short, focused meetings that are intended to keep the team on track and moving forward. The idea is to have a quick, 15-minute meeting every day to discuss what each team member is working on, what they plan to work on next, and any obstacles they are facing. The goal is to keep the team in sync and to identify and address any issues that may be slowing down progress."
                    ),
                    MarkdownBlock(
                        text = "They must be done standing up. \uD83E\uDDCD \uD83E\uDDCD \uD83E\uDDCD"
                    )
                )
            )
        ),
        2 to mutableListOf(
            Section(
                id = 1,
                title = "CS 341",
                desc = "Divide and Conquer",
            ),
            Article(
                id = 2,
                title = "I LOVE MERGE SORT",
                desc = "... and the other ones too",
            ),
        ),
        3 to mutableListOf(
            Section(
                id = 1,
                title = "CS 370",
                desc = "Course Outline, Course Schedule, Grading Policy, etc.",
            ),
            Section(
                id = 2,
                title = "Floating point numbers",
                desc = "All you need is to add a decimal",
            ),
        )
    )

    init {
        println("DEBUG: noteList, $noteDict")
    }

    fun addSection(section: Section, boardId: Int) {
        noteDict[boardId]?.addNote(section)
        notifySubscribers()
    }
    fun addArticle(article: Article, boardId: Int) {
        noteDict[boardId]?.addNote(article)
        notifySubscribers()
    }

    fun del(note: Note, boardId: Int) {
        noteDict[boardId]?.removeNote(note)
        notifySubscribers()
    }

    fun addBlankBoard(boardId: Int) {
        noteDict[boardId] = mutableListOf()
        notifySubscribers()
    }

    fun removeBoard(boardId: Int) {
        noteDict.remove(boardId)
        notifySubscribers()
    }

}
