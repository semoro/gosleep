package me.semoro.gosleep.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import me.semoro.gosleep.ui.theme.GoSleepTheme
import kotlin.random.Random

// Data class for code blocks in the cyberpunk puzzle
data class CodeBlock(
    val code: String,
    val isTarget: Boolean = false,
    val isSelected: Boolean = false,
    val index: Int = -1
)

// Function to generate random hexadecimal codes
private fun generateRandomHexCode(length: Int): String {
    val chars = "0123456789ABCDEF"
    return (1..length)
        .map { chars[Random.nextInt(chars.length)] }
        .joinToString("")
}

// Cyberpunk access puzzle component
@Composable
fun CyberpunkAccessPuzzle(
    gridSize: Int = 4,
    targetSequenceLength: Int = 3,
    onPuzzleSolved: () -> Unit
) {
    // Generate a grid of random codes
    val allCodes = remember {
        val codes = mutableListOf<CodeBlock>()
        repeat(gridSize * gridSize) { index ->
            codes.add(
                CodeBlock(
                    code = generateRandomHexCode(2),
                    index = index
                )
            )
        }
        codes
    }

    fun isValidSelection(index: Int, selectedIndices: List<Int>, isPuzzleSolved: Boolean): Boolean {
        if (isPuzzleSolved || selectedIndices.size >= targetSequenceLength) {
            return false
        }

        if (selectedIndices.isEmpty()) {
            // First selection must be from the top row
            return index < gridSize
        }

        val lastSelectedIndex = selectedIndices.last()
        val lastSelectedRow = lastSelectedIndex / gridSize
        val lastSelectedCol = lastSelectedIndex % gridSize

        // If we have an odd number of selections, we need to select from the column
        // where the last selection was made
        return if (selectedIndices.size % 2 == 1) {
            // Select from the same column as the last selection
            index % gridSize == lastSelectedCol && index != lastSelectedIndex
        } else {
            // Select from the same row as the last selection
            index / gridSize == lastSelectedRow && index != lastSelectedIndex
        }
    }

    // Generate a target sequence
    val targetSequence = remember {
        buildList {
            repeat(targetSequenceLength) {
                val code = allCodes.filter { isValidSelection(it.index, this, false) }.random()
                add(code.index)
            }
        }.map { index -> allCodes[index].code }
    }

    // Track selected codes
    val selectedCodes = remember { mutableStateListOf<String>() }

    // Track selected indices for visual feedback
    val selectedIndices = remember { mutableStateListOf<Int>() }

    // Track if puzzle is solved
    var isPuzzleSolved by remember { mutableStateOf(false) }

    // Track if last attempt was incorrect
    var lastAttemptIncorrect by remember { mutableStateOf(false) }






    // Check if puzzle is solved when selected codes change
    LaunchedEffect(selectedCodes.size) {
        if (selectedCodes.size == targetSequence.size) {
            val isCorrect = selectedCodes.zip(targetSequence).all { (selected, target) -> selected == target }
            if (isCorrect) {
                isPuzzleSolved = true
                delay(500)
                onPuzzleSolved()
            } else {
                // Mark attempt as incorrect
                lastAttemptIncorrect = true
                // Reset selection after a short delay to show feedback
                delay(500)
                selectedCodes.clear()
                selectedIndices.clear()
                lastAttemptIncorrect = false
            }
        }
    }

    Column (
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
//        horizontalAlignment = Alignment.CenterHorizontally,
//        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
//        Column {


//            // Display target sequence
            Text(
                text = "ACCESS SEQUENCE",
                color = MaterialTheme.colorScheme.primary,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )

            Row(
//                modifier = Modifier.fillMaxWidth(),
//                horizontalArrangement = Arrangement.Center
            ) {
                targetSequence.forEachIndexed { index, code ->
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .size(40.dp)
                            .border(
                                BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                                shape = MaterialTheme.shapes.small
                            )
                            .background(MaterialTheme.colorScheme.surface),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = code,
                            color = MaterialTheme.colorScheme.primary,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
//        }
//        Column() {


            // Display grid of codes
            LazyVerticalGrid(
                columns = GridCells.Fixed(gridSize),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(allCodes) { codeBlock ->
                    val isSelected = selectedIndices.contains(codeBlock.index)
                    val isValid = isValidSelection(codeBlock.index, selectedIndices, isPuzzleSolved)
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .aspectRatio(1f)
                            .size(40.dp)
                            .border(
                                BorderStroke(
                                    2.dp,
                                    when {
                                        lastAttemptIncorrect -> MaterialTheme.colorScheme.error
                                        isSelected -> MaterialTheme.colorScheme.secondary
                                        isValid || isPuzzleSolved -> Color(0xFF4CAF50) // Green color for valid cells
                                        else -> MaterialTheme.colorScheme.primary
                                    }
                                ),
                                shape = MaterialTheme.shapes.small
                            )
                            .background(
                                when {
                                    lastAttemptIncorrect -> MaterialTheme.colorScheme.errorContainer
                                    isSelected -> MaterialTheme.colorScheme.secondaryContainer
                                    isValid || isPuzzleSolved -> Color(0x1A4CAF50) // Light green background for valid cells
                                    else -> MaterialTheme.colorScheme.surface
                                }
                            )
                            .clickable {
                                if (!isPuzzleSolved && selectedCodes.size < targetSequenceLength && !isSelected && isValid) {
                                    selectedCodes.add(codeBlock.code)
                                    selectedIndices.add(codeBlock.index)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = codeBlock.code,
                            color = when {
                                lastAttemptIncorrect -> MaterialTheme.colorScheme.error
                                isSelected -> MaterialTheme.colorScheme.secondary
                                isValid || isPuzzleSolved -> Color(0xFF4CAF50) // Green color for valid cells
                                else -> MaterialTheme.colorScheme.primary
                            },
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
//        }
    }
}
