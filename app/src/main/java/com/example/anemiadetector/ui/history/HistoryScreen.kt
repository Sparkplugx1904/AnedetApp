package com.example.anemiadetector.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.anemiadetector.R
import com.example.anemiadetector.data.local.entity.ExaminationEntity
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

/**
 * History screen showing examination records
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onNavigateBack: () -> Unit,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val examinations by viewModel.examinations.collectAsState()
    val filterState by viewModel.filterState.collectAsState()
    val sortState by viewModel.sortState.collectAsState()

    var showFilterMenu by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var examinationToDelete by remember { mutableStateOf<ExaminationEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.history_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Filter button
                    IconButton(onClick = { showFilterMenu = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filter")
                    }
                    
                    // Sort button
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(Icons.Default.Sort, contentDescription = "Sort")
                    }

                    // Filter dropdown
                    DropdownMenu(
                        expanded = showFilterMenu,
                        onDismissRequest = { showFilterMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.history_filter_all)) },
                            onClick = {
                                viewModel.setFilter(FilterType.ALL)
                                showFilterMenu = false
                            },
                            leadingIcon = {
                                if (filterState == FilterType.ALL) {
                                    Icon(Icons.Default.Check, contentDescription = null)
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.history_filter_anemia)) },
                            onClick = {
                                viewModel.setFilter(FilterType.ANEMIA)
                                showFilterMenu = false
                            },
                            leadingIcon = {
                                if (filterState == FilterType.ANEMIA) {
                                    Icon(Icons.Default.Check, contentDescription = null)
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.history_filter_non_anemia)) },
                            onClick = {
                                viewModel.setFilter(FilterType.NON_ANEMIA)
                                showFilterMenu = false
                            },
                            leadingIcon = {
                                if (filterState == FilterType.NON_ANEMIA) {
                                    Icon(Icons.Default.Check, contentDescription = null)
                                }
                            }
                        )
                    }

                    // Sort dropdown
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.history_sort_newest)) },
                            onClick = {
                                viewModel.setSort(SortType.NEWEST)
                                showSortMenu = false
                            },
                            leadingIcon = {
                                if (sortState == SortType.NEWEST) {
                                    Icon(Icons.Default.Check, contentDescription = null)
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.history_sort_oldest)) },
                            onClick = {
                                viewModel.setSort(SortType.OLDEST)
                                showSortMenu = false
                            },
                            leadingIcon = {
                                if (sortState == SortType.OLDEST) {
                                    Icon(Icons.Default.Check, contentDescription = null)
                                }
                            }
                        )
                    }
                }
            )
        }
    ) { padding ->
        if (examinations.isEmpty()) {
            // Empty state
            EmptyHistoryState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            )
        } else {
            // Examination list
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = examinations,
                    key = { it.id }
                ) { examination ->
                    ExaminationItem(
                        examination = examination,
                        onDelete = { examinationToDelete = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    // Delete confirmation dialog
    examinationToDelete?.let { examination ->
        AlertDialog(
            onDismissRequest = { examinationToDelete = null },
            title = { Text(stringResource(R.string.history_delete_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteExamination(examination)
                        examinationToDelete = null
                    }
                ) {
                    Text(stringResource(R.string.btn_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { examinationToDelete = null }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }
}

/**
 * Single examination item
 */
@Composable
private fun ExaminationItem(
    examination: ExaminationEntity,
    onDelete: (ExaminationEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (examination.predictedLabel == "Anemia") {
        Color(0xFFFF3B30).copy(alpha = 0.1f)
    } else {
        Color(0xFF34C759).copy(alpha = 0.1f)
    }

    val iconColor = if (examination.predictedLabel == "Anemia") {
        Color(0xFFFF3B30)
    } else {
        Color(0xFF34C759)
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail placeholder (80x80dp)
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(iconColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Info column
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (examination.predictedLabel == "Anemia") {
                            Icons.Default.Warning
                        } else {
                            Icons.Default.CheckCircle
                        },
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = examination.predictedLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = iconColor
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${(examination.confidence * 100).roundToInt()}%",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = formatTimestamp(examination.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Delete button
            IconButton(onClick = { onDelete(examination) }) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.btn_delete),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

/**
 * Empty state
 */
@Composable
private fun EmptyHistoryState(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.History,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.history_empty),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Format timestamp to readable date
 */
private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
