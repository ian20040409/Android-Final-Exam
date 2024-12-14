package com.example.finaleaxm

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.NumberPicker
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.LocalDate
import java.time.YearMonth

// Memo 資料類型
data class Memo(val date: LocalDate, val content: String)

// ViewModel，處理日曆與備忘錄邏輯
class CalendarViewModel : ViewModel() {
    var currentMonth by mutableStateOf(YearMonth.now())
    var selectedDate by mutableStateOf<LocalDate?>(null)
    var memos by mutableStateOf<List<Memo>>(emptyList())

    fun selectDate(date: LocalDate) {
        selectedDate = date
    }

    fun addMemo(memo: Memo) {
        memos = memos + memo
    }

    fun previousMonth() {
        currentMonth = currentMonth.minusMonths(1)
    }

    fun nextMonth() {
        currentMonth = currentMonth.plusMonths(1)
    }

    fun loadMemos(context: Context) {
        val prefs: SharedPreferences = context.getSharedPreferences("MemoPrefs", Context.MODE_PRIVATE)
        val memoString = prefs.getString("Memos", "")
        memos = memoString?.split(";")?.mapNotNull {
            val parts = it.split(":")
            try {
                val date = LocalDate.parse(parts[0])
                val content = parts[1]
                Memo(date, content)
            } catch (e: Exception) {
                null
            }
        } ?: emptyList()
    }

    fun saveMemos(context: Context) {
        val prefs: SharedPreferences = context.getSharedPreferences("MemoPrefs", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        val memoStrings = memos.joinToString(";") { "${it.date}:${it.content}" }
        editor.putString("Memos", memoStrings)
        editor.apply()
    }

    fun removeMemo(memo: Memo) {
        memos = memos.filter { it != memo }
    }
}

@Composable
fun CalendarGrid(
    currentMonth: YearMonth,
    memos: List<Memo>,
    selectedDate: LocalDate?,
    onDateClick: (LocalDate) -> Unit,
    isDialogVisible: Boolean
) {
    LazyVerticalGrid(columns = GridCells.Fixed(7), modifier = Modifier.padding(16.dp)) {
        val daysOfWeek = listOf("日", "一", "二", "三", "四", "五", "六")
        items(7) { index ->
            Text(
                text = daysOfWeek[index],
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        val daysInMonth = currentMonth.lengthOfMonth()
        val firstDayOfWeek = currentMonth.atDay(1).dayOfWeek.value % 7
        items(firstDayOfWeek) { Spacer(modifier = Modifier.size(80.dp)) }

        items(daysInMonth) { day ->
            val date = currentMonth.atDay(day + 1)
            val dateMemos = memos.filter { it.date == date }
            val isSelected = date == selectedDate
            val isToday = date == LocalDate.now() // 判斷是否為今天
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .padding(4.dp)
                    .background(
                        color = if (isToday) Color.LightGray else Color.Transparent
                    )
                    .border(
                        width = if (isSelected) 2.dp else 0.dp,
                        color = if (isSelected) Color.Red else Color.Transparent
                    )
                    .clickable(enabled = !isDialogVisible) { onDateClick(date) },
                contentAlignment = Alignment.TopCenter
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = date.dayOfMonth.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                    // 備忘錄顯示，每個最多顯示3個字
                    dateMemos.take(2).forEach {
                        Text(
                            text = if (it.content.length > 3) "${it.content.take(3)}..." else it.content,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun CalendarScreen(viewModel: CalendarViewModel = viewModel(), context: Context) {
    val currentMonth = viewModel.currentMonth
    val selectedDate = viewModel.selectedDate
    val memos = viewModel.memos
    var memoText by remember { mutableStateOf("") }
    var showDialog by remember { mutableStateOf(false) }

    var showYearMonthPicker by remember { mutableStateOf(false) }

    // 用於動畫方向判斷 (-1=前一月, 1=下一月, 0=無方向)
    var direction by remember { mutableStateOf(0) }

    val memosInDialog = remember { mutableStateListOf<String>() }

    LaunchedEffect(Unit) { viewModel.loadMemos(context) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (viewModel.selectedDate != null) {
                        val selectedDateMemos = viewModel.memos
                            .filter { it.date == viewModel.selectedDate }
                            .map { it.content }
                        memosInDialog.clear()
                        memosInDialog.addAll(selectedDateMemos)
                        showDialog = true
                    } else {
                        Toast.makeText(context, "請先選擇日期", Toast.LENGTH_SHORT).show()
                    }
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Memo")
            }
        },
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                var accumulatedDistance = 0f
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (accumulatedDistance > 100f) {
                            direction = -1
                            viewModel.previousMonth()
                        } else if (accumulatedDistance < -100f) {
                            direction = 1
                            viewModel.nextMonth()
                        }
                        accumulatedDistance = 0f
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        accumulatedDistance += dragAmount
                    }
                )
            }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 年月顯示 + 年月選擇按鈕
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    TextButton(onClick = { showYearMonthPicker = true }) {
                        Text("${currentMonth.year} 年 ${currentMonth.monthValue} 月")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = {
                        direction = -1
                        viewModel.previousMonth()
                    }) { Text("<") }

                    TextButton(onClick = {
                        // 返回今天
                        val now = YearMonth.now()
                        direction = if (now > viewModel.currentMonth) 1 else if (now < viewModel.currentMonth) -1 else 0
                        viewModel.currentMonth = now
                    }) { Text("返回今天") }

                    TextButton(onClick = {
                        direction = 1
                        viewModel.nextMonth()
                    }) { Text(">") }
                }

                // 月曆顯示，有滑動動畫
                AnimatedContent(
                    targetState = currentMonth,
                    transitionSpec = {
                        val animDuration = 300
                        if (direction >= 0) {
                            slideInHorizontally(
                                initialOffsetX = { it },
                                animationSpec = tween(animDuration)
                            ) togetherWith slideOutHorizontally(
                                targetOffsetX = { -it },
                                animationSpec = tween(animDuration)
                            )
                        } else {
                            slideInHorizontally(
                                initialOffsetX = { -it },
                                animationSpec = tween(animDuration)
                            ) togetherWith slideOutHorizontally(
                                targetOffsetX = { it },
                                animationSpec = tween(animDuration)
                            )
                        }
                    }
                ) { animatedMonth ->
                    CalendarGrid(
                        currentMonth = animatedMonth,
                        memos = memos,
                        selectedDate = selectedDate,
                        onDateClick = { viewModel.selectDate(it) },
                        isDialogVisible = showDialog
                    )
                }
            }

            // 備忘錄對話框
            if (showDialog) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .heightIn(min = 300.dp, max = 400.dp)
                            .background(Color.White, MaterialTheme.shapes.medium)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "備忘錄",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.align(Alignment.Center)
                            )
                            IconButton(
                                onClick = { showDialog = false },
                                modifier = Modifier.align(Alignment.CenterEnd)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close Dialog",
                                    tint = Color.Gray
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))

                        LazyVerticalGrid(
                            columns = GridCells.Fixed(1),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            items(memosInDialog) { memo ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = memo,
                                        modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    selectedDate?.let {
                                        Text(
                                            text = it.toString(),
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.padding(horizontal = 8.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            val matchingMemo = viewModel.memos.find { it.date == viewModel.selectedDate && it.content == memo }
                                            if (matchingMemo != null) {
                                                viewModel.removeMemo(matchingMemo)
                                                viewModel.saveMemos(context)
                                                memosInDialog.remove(memo)
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete Memo", tint = Color.Red)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextField(
                                value = memoText,
                                onValueChange = { memoText = it },
                                label = { Text("輸入備忘錄") },
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    if (memoText.isNotEmpty() && viewModel.selectedDate != null) {
                                        val newMemo = Memo(viewModel.selectedDate!!, memoText)
                                        viewModel.addMemo(newMemo)
                                        viewModel.saveMemos(context)
                                        memosInDialog.add(memoText)
                                        memoText = ""
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add Memo")
                            }

                        }
                    }
                }
            }

            // 顯示 YearMonthPickerDialog
            if (showYearMonthPicker) {
                YearMonthPickerDialog(
                    initialYear = currentMonth.year,
                    initialMonth = currentMonth.monthValue,
                    onDismiss = { showYearMonthPicker = false },
                    onConfirm = { year, month ->
                        val newMonth = YearMonth.of(year, month)
                        // 計算方向
                        direction = when {
                            newMonth > viewModel.currentMonth -> 1
                            newMonth < viewModel.currentMonth -> -1
                            else -> 0
                        }
                        viewModel.currentMonth = newMonth
                        showYearMonthPicker = false
                    }
                )
            }
        }
    }
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
fun PreviewCalendarScreen() {
    CalendarScreen(context = LocalContext.current)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                CalendarScreen(viewModel = viewModel(), context = this)
            }
        }
    }
}