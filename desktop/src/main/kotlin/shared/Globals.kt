package globals


import individual_board.view.ViewModel as IndividualBoardViewModel
import boards.view.ViewModel as BoardViewModel
import individual_board.model.Model as IndividualBoardModel
import boards.model.Model as BoardModel

val boardModel = BoardModel()
val boardViewModel = BoardViewModel(boardModel)

val individualBoardModel = IndividualBoardModel()
val individualBoardViewModel = IndividualBoardViewModel(individualBoardModel)


