package article.entities

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.flavours.commonmark.CommonMarkFlavourDescriptor
import org.intellij.markdown.parser.MarkdownParser

class MarkdownHandler(private var rawString: String) {
    // Stores the parse tree for a given Markdown text
    private val flavour = CommonMarkFlavourDescriptor()

    // Parse the raw text and create tree
    private var rootNode: ASTNode = MarkdownParser(flavour).buildMarkdownTreeFromString(rawString)


    @Composable
    fun renderMarkdown(){
        // Renders markdown for ALL the nodes
        Column(
            modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
        ) {
            // renderMarkdownNode(rootNode)

            rootNode.children.forEach {node ->
                println("------------------------")
                printASTNode(node)
                renderMarkdownNode(node)
            }
        }

    }

    @Composable
    fun renderMarkdownNode(node: ASTNode){
        // Renders markdown for an individual ASTNode

        val headingSizes = mapOf(
            MarkdownElementTypes.ATX_1 to 36.sp,
            MarkdownElementTypes.ATX_2 to 30.sp,
            MarkdownElementTypes.ATX_3 to 24.sp,
            MarkdownElementTypes.ATX_4 to 20.sp,
            MarkdownElementTypes.ATX_5 to 18.sp,
            MarkdownElementTypes.ATX_6 to 16.sp
        )


        val t = extractStyledText(node)
        if (t == "") return // do this if there is nothing to render

        when (node.type) {
            // For all headings (h1, h2, h3, ...)
            in headingSizes -> {
                Text(
                    text=t,
                    fontSize = headingSizes[node.type] ?: 0.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            MarkdownElementTypes.PARAGRAPH->{
                Row{
                    node.children.forEach { renderMarkdownNode(it) }
                }

            }

            MarkdownElementTypes.STRONG -> {
                Text(
                    text = t.trim('*'),
                    fontWeight = FontWeight.Bold,
                )
            }

            MarkdownElementTypes.EMPH -> {
                Text(
                    text = t.trim('*'),
                    fontStyle = FontStyle.Italic
                )
            }

            MarkdownElementTypes.CODE_SPAN ->{
                Text(
                    text = t.trim('`'),
                    fontFamily = FontFamily.Monospace
                )
            }

            MarkdownTokenTypes.EOL -> {}

            // Normal text
            else ->{
                println("DEBUG TYPE: ${node.type}")
                Text(
                    text=t
                )
            }


        }


    }

    fun updateText(newString: String){
        rawString = newString
        rootNode = MarkdownParser(flavour).buildMarkdownTreeFromString(rawString)
    }

    private fun extractStyledText(node: ASTNode): String{
        if (node.type == MarkdownTokenTypes.TEXT || node.type == MarkdownElementTypes.CODE_SPAN) {
            return extractRawText(node)
        }
        else if (node.type == MarkdownTokenTypes.WHITE_SPACE) {
            return " "
        }
        else if (node.type == MarkdownTokenTypes.EMPH) {
            return "*"
        }
        else {
            return node.children.joinToString("") { extractStyledText(it) }.trim()
        }
    }

    private fun printASTNode(node: ASTNode){
        // For debugging
        print(node.type.toString() + " (text: [" + extractRawText(node) + "])\n")

        node.children.forEach {
            printASTNode(it)
        }
    }

    private fun extractRawText(node: ASTNode): String {
        return rawString.substring(node.startOffset, node.endOffset)
    }
}
