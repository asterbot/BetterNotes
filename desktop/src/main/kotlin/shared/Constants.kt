package shared
import androidx.compose.material.TextFieldColors
import androidx.compose.material.TextFieldDefaults.outlinedTextFieldColors
import androidx.compose.material3.*
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
    val errorColor: Color = Color(0xFFCF0000)
}

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
        errorCursorColor = Colors.errorColor
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