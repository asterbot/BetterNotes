package shared


import article.model.ArticleModel
import article.view.ArticleViewModel
import individual_board.view.ViewModel as IndividualBoardViewModel
import boards.view.BoardViewModel
import individual_board.model.IndvBoardModel as IndividualBoardModel
import boards.model.BoardModel

val boardModel = BoardModel()
val boardViewModel = BoardViewModel(boardModel)

val individualBoardModel = IndividualBoardModel()
val individualBoardViewModel = IndividualBoardViewModel(individualBoardModel)

val articleModel = ArticleModel()
val articleViewModel = ArticleViewModel(articleModel)
