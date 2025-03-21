package article.entities

import dev.snipme.highlights.Highlights

fun highlightCode(codeText: String) {
    Highlights.default().apply {
        setCode(codeText)
        getCodeStructure()
        getHighlights()
    }

}