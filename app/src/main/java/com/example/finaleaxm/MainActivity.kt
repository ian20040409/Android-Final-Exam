package com.example.finaleaxm

import android.widget.NumberPicker
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import com.example.finaleaxm.ui.theme.FinaleaxmTheme
import java.time.LocalDate
import java.time.YearMonth

data class Memo(val date: LocalDate, val content: String)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FinaleaxmTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    CalendarApp(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun CalendarApp(modifier: Modifier = Modifier) {
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    val memos = remember { mutableStateListOf<Memo>() }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var showMemoDialog by remember { mutableStateOf(false) }

    // 用於計算拖曳方向 (-1表前一個月, 1表下一個月)
    var totalDragX by remember { mutableStateOf(0f) }
    var direction by remember { mutableStateOf(0) }

    // 顯示自訂 YearMonthPickerDialog
    var showCustomYearMonthPicker by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .pointerInput(Unit) {
                // 左右滑動切換月份 (如不需要可移除這段)
                detectHorizontalDragGestures(
                    onDragStart = { totalDragX = 0f },
                    onHorizontalDrag = { _, dragAmount ->
                        totalDragX += dragAmount
                    },
                    onDragEnd = {
                        if (totalDragX > 0) {
                            direction = -1
                            currentMonth = currentMonth.minusMonths(1)
                        } else if (totalDragX < 0) {
                            direction = 1
                            currentMonth = currentMonth.plusMonths(1)
                        }
                    }
                )
            }
    ) {
        // 年月顯示 (點擊後顯示自訂對話框)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = { showCustomYearMonthPicker = true }) {
                Text(
                    text = "${currentMonth.year}年${currentMonth.monthValue}月",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        TextButton(onClick = {
            // 返回今天
            direction = 0
            currentMonth = YearMonth.now()
            selectedDate = LocalDate.now()
        }) {
            Text("點選以返回今天", style = MaterialTheme.typography.bodyLarge)
        }

        // 星期標題列
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf("日", "一", "二", "三", "四", "五", "六").forEach { day ->
                Text(
                    text = day,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }
        }

        // 月曆顯示 (帶有翻頁動畫)
        AnimatedContent(
            targetState = currentMonth,
            transitionSpec = {
                val animDuration = 300
                if (direction >= 0) {
                    slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(animDuration)) togetherWith
                            slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(animDuration))
                } else {
                    slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(animDuration)) togetherWith
                            slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(animDuration))
                }
            }
        ) { animatedMonth ->
            LazyVerticalGrid(columns = GridCells.Fixed(7), modifier = Modifier.padding(vertical = 16.dp)) {
                val daysInMonth = animatedMonth.lengthOfMonth()
                val firstDayOfWeek = animatedMonth.atDay(1).dayOfWeek.value % 7

                // 空白填充
                items(firstDayOfWeek) {
                    Spacer(modifier = Modifier.size(72.dp))
                }

                // 日期格
                items(daysInMonth) { day ->
                    val date = animatedMonth.atDay(day + 1)
                    val memo = memos.firstOrNull { it.date == date }
                    val isToday = date == LocalDate.now()

                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .padding(4.dp)
                            .clickable {
                                selectedDate = date
                                showMemoDialog = true
                            }
                            .border(
                                width = if (isToday) 2.dp else 0.dp,
                                color = if (isToday) Color.Red else Color.Transparent,
                                shape = MaterialTheme.shapes.small
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${day + 1}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (date.dayOfWeek.value in 6..7) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground
                            )
                            if (memo != null) {
                                Text(
                                    text = if (memo.content.length > 5) memo.content.take(5) + "..." else memo.content,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // 顯示新增備忘錄對話框
        if (showMemoDialog) {
            AddMemoDialog(
                date = selectedDate,
                onDismiss = { showMemoDialog = false },
                onAddMemo = { content ->
                    selectedDate?.let { date ->
                        memos.removeIf { it.date == date }
                        memos.add(Memo(date, content))
                    }
                    showMemoDialog = false
                }
            )
        }

        // 顯示年、月客製化選擇對話框
        if (showCustomYearMonthPicker) {
            YearMonthPickerDialog(
                initialYear = currentMonth.year,
                initialMonth = currentMonth.monthValue,
                onDismiss = { showCustomYearMonthPicker = false },
                onConfirm = { year, month ->
                    currentMonth = YearMonth.of(year, month)
                    showCustomYearMonthPicker = false
                }
            )
        }

        // 底部功能列
        BottomAppBar(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                TextButton(onClick = { /*行程功能*/ }) {
                    Text("行程", style = MaterialTheme.typography.bodyLarge)
                }
                TextButton(onClick = { /*備忘錄功能*/ }) {
                    Text("備忘錄", style = MaterialTheme.typography.bodyLarge)
                }
                FloatingActionButton(onClick = { /*新增功能*/ }) {
                    Icon(Icons.Default.Add, contentDescription = "新增")
                }
                TextButton(onClick = { /*最新動態功能*/ }) {
                    Text("最新動態", style = MaterialTheme.typography.bodyLarge)
                }
                TextButton(onClick = { /*設定功能*/ }) {
                    Text("設定", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

@Composable
fun AddMemoDialog(date: LocalDate?, onDismiss: () -> Unit, onAddMemo: (String) -> Unit) {
    var memoContent by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = { Text("新增備忘錄: $date") },
        text = {
            Column {
                TextField(
                    value = memoContent,
                    onValueChange = { memoContent = it },
                    label = { Text("內容") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                IconButton(
                    onClick = { onAddMemo(memoContent) },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "新增")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onAddMemo(memoContent) }) {
                Text("確定")
            }
        },
        dismissButton = {
            TextButton(onClick = { onDismiss() }) {
                Text("取消")
            }
        }
    )
}

@Composable
fun YearMonthPickerDialog(
    initialYear: Int,
    initialMonth: Int,
    onDismiss: () -> Unit,
    onConfirm: (year: Int, month: Int) -> Unit
) {
    var selectedYear by remember { mutableStateOf(initialYear) }
    var selectedMonth by remember { mutableStateOf(initialMonth) }

    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = { Text("選擇年份與月份") },
        text = {
            Column {
                Text("年份", style = MaterialTheme.typography.bodyLarge)
                AndroidView(
                    factory = { context ->
                        NumberPicker(context).apply {
                            minValue = 1900
                            maxValue = 2100
                            value = selectedYear
                            setOnValueChangedListener { _, _, newVal ->
                                selectedYear = newVal
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                )

                Text("月份", style = MaterialTheme.typography.bodyLarge)
                AndroidView(
                    factory = { context ->
                        NumberPicker(context).apply {
                            minValue = 1
                            maxValue = 12
                            value = selectedMonth
                            setOnValueChangedListener { _, _, newVal ->
                                selectedMonth = newVal
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedYear, selectedMonth) }) {
                Text("確定")
            }
        },
        dismissButton = {
            TextButton(onClick = { onDismiss() }) {
                Text("取消")
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun CalendarAppPreview() {
    FinaleaxmTheme {
        CalendarApp()
    }
}
