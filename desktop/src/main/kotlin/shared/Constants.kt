package shared
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object Colors {
    val lightTeal: Color = Color(0xffB1CCD3)
    val medTeal: Color = Color(0xff2C666E)
    val darkTeal: Color = Color(0xff07393C)
    val black: Color = Color(0xff0a090C)
    val white: Color = Color(0xffFFFCFD)
    val lightGrey: Color = Color(0xffF0EDEE)
    val darkGrey: Color = Color(0xff333333)
}

@Composable
fun textButtonColours(): ButtonColors {
    return ButtonDefaults.buttonColors(
        containerColor = Colors.medTeal,
        contentColor = Colors.white
    )
}

@Composable
fun iconButtonColours(): IconButtonColors {
    return IconButtonDefaults.iconButtonColors(
        containerColor = Colors.medTeal,
        contentColor = Colors.white,
        disabledContainerColor = Colors.medTeal.copy(alpha = .4f)
    )
}