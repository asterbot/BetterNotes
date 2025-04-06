package article.entities

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
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
    fun renderMarkdown() {
        println(rawString)
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

        when (node.type) {
            // For all headings (h1, h2, h3, ...)
            in headingSizes -> {
                Text(
                    text=t,
                    fontSize = headingSizes[node.type] ?: 0.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            MarkdownElementTypes.PARAGRAPH -> {
                // Ensure all styles are applied and a SINGLE Text composable is rendered
                val annotatedText = extractStyledText(node)
                Text(text = annotatedText)
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


    // So there's a thing called AnnotatedStrings which if you pass into a Text composable,
    //   the styles will apply where intended with the Text composable rendering as a single one
    //   instead of what we did before (where it rendered a new composable per style)
    // This function just returns an AnnotatedString which has the correct styles applied (after checking the AST)
    // Documentation: https://developer.android.com/reference/kotlin/androidx/compose/ui/text/SpanStyle

    private fun extractStyledText(node: ASTNode): AnnotatedString {
        fun AnnotatedString.Builder.appendStyledText(node: ASTNode, styles: List<SpanStyle>) {
            val text = extractRawText(node)
            if (text.isBlank()) return
            val start = this.length
            append(text)
            styles.forEach { style ->
                addStyle(style, start, start + text.length)
            }
        }
        return buildAnnotatedString {
            fun recurse(current: ASTNode, activeStyles: List<SpanStyle>) {
                // activeStyles keeps track of all the styles applied
                when (current.type) {
                    MarkdownTokenTypes.TEXT -> {
                        appendStyledText(current, activeStyles)
                    }

                    MarkdownTokenTypes.WHITE_SPACE -> {
                        append(" ")
                    }

                    MarkdownElementTypes.STRONG -> {
                        val newStyles = activeStyles + SpanStyle(fontWeight = FontWeight.Bold)
                        current.children.forEach { recurse(it, newStyles) }
                    }

                    MarkdownElementTypes.EMPH -> {
                        val newStyles = activeStyles + SpanStyle(fontStyle = FontStyle.Italic)
                        current.children.forEach { recurse(it, newStyles) }
                    }

                    MarkdownElementTypes.CODE_SPAN -> {
                        val newStyles = activeStyles + SpanStyle(fontFamily = FontFamily.Monospace)
                        current.children.forEach { recurse(it, newStyles) }
                    }

                    else -> {
                        current.children.forEach { recurse(it, activeStyles) }
                    }
                }
            }
            recurse(node, emptyList())
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
