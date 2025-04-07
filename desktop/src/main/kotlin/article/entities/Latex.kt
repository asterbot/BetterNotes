
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.dp
import org.scilab.forge.jlatexmath.TeXConstants
import org.scilab.forge.jlatexmath.TeXFormula
import shared.Colors
import java.awt.Color
import java.awt.image.BufferedImage

@Composable
fun LatexRenderer(
    latex: String,
    modifier: Modifier = Modifier,
    fontSize: Float = 20f,
    foregroundColor: androidx.compose.ui.graphics.Color = Colors.black,
    backgroundColor: androidx.compose.ui.graphics.Color = Colors.lightTeal
) {
    var fgColor = foregroundColor.toAwtColor()
    var bgColor = backgroundColor.toAwtColor()
    val latexImage = remember(latex, fontSize, foregroundColor, backgroundColor) {
        renderLatex(latex, fontSize, fgColor, bgColor)
    }

    Box(modifier = modifier.padding(8.dp)) {
        Image(
            bitmap = latexImage,
            contentDescription = "LaTeX formula: $latex",
            modifier = Modifier.fillMaxWidth()
        )
    }
}


private fun renderLatex(
    latex: String,
    fontSize: Float,
    foregroundColor: Color,
    backgroundColor: Color
): ImageBitmap {
    // create a TeXFormula from latex string
    var formula: TeXFormula? = null
    try{
        formula = TeXFormula(latex)
    }
    catch (e: Exception){
        formula = TeXFormula("\\text{Invalid Syntax}")
    }

    // create a TeXIcon from the formula
    val icon = formula?.createTeXIcon(
        TeXConstants.STYLE_DISPLAY,
        fontSize
    )
    // foreground and background colors
    icon?.insets?.top = 5
    icon?.insets?.bottom = 5
    icon?.insets?.left = 5
    icon?.insets?.right = 5
    // create a BufferedImage to render the TeXIcon
    val image = BufferedImage(
        icon!!.iconWidth,
        icon.iconHeight,
        BufferedImage.TYPE_INT_ARGB
    )
    // Get the Graphics2D object and set the background color
    val g2 = image.createGraphics()
    g2.color = backgroundColor
    g2.fillRect(0, 0, icon.iconWidth, icon.iconHeight)
    // paint the TeXIcon on the BufferedImage
    icon.paintIcon(null, g2, 0, 0)
    g2.dispose()
    // convert BufferedImage to an ImageBitmap
    return image.toComposeImageBitmap()
}

fun androidx.compose.ui.graphics.Color.toAwtColor(): java.awt.Color {
    // function to convert android.compose.ui.graphics.Color to java.awt.Color
    return java.awt.Color(
        this.red,
        this.green,
        this.blue,
        this.alpha
    )
}