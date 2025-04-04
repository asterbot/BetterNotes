package shared
import androidx.compose.material.TextFieldColors
import androidx.compose.material.TextFieldDefaults.outlinedTextFieldColors
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object Colors {
    val veryLightTeal: Color = Color(0xFFF6FBFC)
    val lightTeal: Color = Color(0xFFB1CCD3)
    val medTeal: Color = Color(0xFF2C666E)
    val darkTeal: Color = Color(0xFF07393C)
    val black: Color = Color(0xFF0A090C)
    val white: Color = Color(0xFFFFFCFD)
    val lightGrey: Color = Color(0xFFE0E0E0)
    val darkGrey: Color = Color(0xFF333333)
    val errorColor: Color = Color(0xFFCF0000)
}

val tagColors = listOf(
    Colors.veryLightTeal,
    Colors.lightTeal,
    Colors.medTeal,
    Colors.darkTeal,
    Colors.black,
    Colors.white,
    Colors.lightGrey,
    Colors.darkGrey,
    Colors.errorColor,
)

@Composable
fun textButtonColours(): ButtonColors {
    return ButtonDefaults.buttonColors(
        containerColor = Colors.medTeal,
        contentColor = Colors.white
    )
}

@Composable
fun transparentTextButtonColours(): ButtonColors {
    return ButtonDefaults.buttonColors(
        containerColor = Color.Transparent,
        contentColor = Colors.medTeal
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

@Composable
fun outlinedTextFieldColours(): TextFieldColors {
    return outlinedTextFieldColors(
        focusedBorderColor = Colors.medTeal,
        unfocusedBorderColor = Colors.darkGrey,
        focusedLabelColor = Colors.medTeal,
        unfocusedLabelColor = Colors.darkGrey,
        cursorColor = Colors.black
    )
}

@Composable
fun textFieldColours() : androidx.compose.material3.TextFieldColors {
    return TextFieldDefaults.colors(
        cursorColor = Color.Black,
        focusedLabelColor = Colors.darkGrey,
        focusedIndicatorColor = Colors.medTeal,
        unfocusedLabelColor = Colors.darkGrey,
        errorIndicatorColor = Colors.errorColor,
        errorLabelColor = Colors.errorColor,
        errorCursorColor = Colors.errorColor,
        unfocusedContainerColor = Colors.veryLightTeal,
        focusedContainerColor = Colors.veryLightTeal.times(0.9f)
    )
}

@Composable
fun checkboxColours(): CheckboxColors {
    return CheckboxDefaults.colors(
        checkmarkColor = Color.White,
        checkedColor = Colors.medTeal,
        uncheckedColor = Colors.darkTeal
    )
}

@Composable
fun switchColours(): SwitchColors {
    return SwitchDefaults.colors(
        checkedThumbColor = Colors.medTeal,
        checkedTrackColor = Colors.lightTeal,
        checkedBorderColor = Colors.medTeal,
        uncheckedThumbColor = Colors.lightGrey.times(0.7f),
        uncheckedTrackColor = Color.Transparent,
        uncheckedBorderColor = Colors.medTeal
    )
}


fun Color.times(factor: Float) = copy(
    red = (red * factor).coerceIn(0f, 1f),
    green = (green * factor).coerceIn(0f, 1f),
    blue = (blue * factor).coerceIn(0f, 1f),
)