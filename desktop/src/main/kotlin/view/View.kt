package view

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

@Composable
fun App(onNavigate: () -> Unit) {
    val courses = mutableListOf("CS 346", "CS 341", "CS 350", "CS 370", "CS 375")
    val courseDescriptions = mutableListOf("App Development", "Algorithms", "Operating Systems", "Numerical Computation", "Idk")
    val numCourses: Int = courses.size

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = "Boards", style = MaterialTheme.typography.h2)

        Box(
            Modifier.fillMaxSize()
                .padding(15.dp)
                .background(Color(0xFFF0EDEE))
        ) {
            val state = rememberLazyGridState()
            LazyVerticalGrid(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(10.dp),
                columns = GridCells.Fixed(3),
                state = state
            ) {
                items(numCourses) { index ->
                    Button(
                        modifier = Modifier.padding(15.dp),
                        colors = ButtonDefaults.buttonColors(Color(0xffB1CCD3)),
                        onClick = {
                            println("Clicked ${courses[index]}")
                            onNavigate() // Switch to View2
                        }
                    ) {
                        Column(
                            modifier = Modifier.padding(15.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("${courses[index]} \n", textAlign = TextAlign.Center)
                            Text(courseDescriptions[index], textAlign = TextAlign.Center)
                        }
                    }
                }
            }
            VerticalScrollbar(
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                adapter = rememberScrollbarAdapter(scrollState = state)
            )
        }
    }
}

@Composable
fun View2(onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("This is View2", style = MaterialTheme.typography.h2)
        Button(onClick = onBack) {
            Text("Back to Main View")
        }
    }
}

@Composable
fun MainView() {
    var currentView by remember { mutableStateOf("App") }

    when (currentView) {
        "App" -> App(onNavigate = { currentView = "View2" })
        "View2" -> View2(onBack = { currentView = "App" })
    }
}