package com.example.finaleaxm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
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
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.*
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.util.concurrent.TimeUnit
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import java.time.Month

// Memo 資料類型
data class Memo(val date: LocalDate, val time: LocalTime?, val content: String)

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
            val parts = it.split("|")
            try {
                val date = LocalDate.parse(parts[0])
                val time = if (parts[1] != "null") LocalTime.parse(parts[1]) else null
                val content = parts[2]
                Memo(date, time, content)
            } catch (e: Exception) {
                null
            }
        } ?: emptyList()
    }

    fun saveMemos(context: Context) {
        val prefs: SharedPreferences = context.getSharedPreferences("MemoPrefs", Context.MODE_PRIVATE)
        val memoStrings = memos.joinToString(";") { "${it.date}|${it.time}|${it.content}" }
        prefs.edit().putString("Memos", memoStrings).apply()
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
    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        modifier = Modifier.padding(16.dp),
    ) {
        // 星期標題
        val daysOfWeek = listOf("日", "一", "二", "三", "四", "五", "六")
        items(7) { index ->
            Text(
                text = daysOfWeek[index],
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = if (index == 0 || index == 6) Color.Red else Color.Gray // 禮拜六、日為紅色
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp)
            )
        }

        val daysInMonth = currentMonth.lengthOfMonth()
        val firstDayOfWeek = currentMonth.atDay(1).dayOfWeek.value % 7
        items(firstDayOfWeek) { Spacer(modifier = Modifier.size(70.dp)) }

        items(daysInMonth) { day ->
            val date = currentMonth.atDay(day + 1)
            val dateMemos = memos.filter { it.date == date }
            val isSelected = date == selectedDate
            val isToday = date == LocalDate.now()
            Box(
                modifier = Modifier
                    .size(70.dp) // 格子變大
                    .padding(4.dp)
                    .background(
                        color = if (isToday) Color(0xFFBBDEFB) else Color.Transparent,
                        shape = MaterialTheme.shapes.small
                    )
                    .border(
                        width = if (isSelected) 3.dp else 1.dp, // 選中的邊框加粗
                        color = if (isSelected) Color(0xFF1976D2) else Color.LightGray,
                        shape = MaterialTheme.shapes.small
                    )
                    .clickable(enabled = !isDialogVisible) { onDateClick(date) },
                contentAlignment = Alignment.TopCenter
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = date.dayOfMonth.toString(),
                        style = MaterialTheme.typography.bodyLarge, // 日期字體稍微加大
                        textAlign = TextAlign.Center
                    )
                    if (dateMemos.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .size(24.dp) // 提醒數量的徽章變大
                                .background(
                                    color = Color.Red,
                                    shape = MaterialTheme.shapes.medium
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = dateMemos.size.toString(),
                                style = MaterialTheme.typography.bodySmall.copy(color = Color.White)
                            )
                        }
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
    var showTimePicker by remember { mutableStateOf(false) }
    var selectedTime by remember { mutableStateOf<LocalTime?>(null) }

    var showYearMonthPicker by remember { mutableStateOf(false) }

    // 用於動畫方向判斷 (-1=前一月, 1=下一月, 0=無方向)
    var direction by remember { mutableStateOf(0) }

    val memosInDialog = remember { mutableStateListOf<Memo>() }

    LaunchedEffect(Unit) { viewModel.loadMemos(context) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (viewModel.selectedDate != null) {
                        val selectedDateMemos = viewModel.memos
                            .filter { it.date == viewModel.selectedDate }
                        memosInDialog.clear()
                        memosInDialog.addAll(selectedDateMemos)
                        showDialog = true
                    } else {
                        Toast.makeText(context, "請先選擇日期", Toast.LENGTH_SHORT).show()
                    }
                },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
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
                            .heightIn(min = 300.dp, max = 500.dp)
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
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = memo.content,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        memo.time?.let {
                                            Text(
                                                text = it.toString(),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color.Gray
                                            )
                                        }
                                    }
                                    selectedDate?.let {
                                        Text(
                                            text = it.toString(),
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.padding(horizontal = 8.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            viewModel.removeMemo(memo)
                                            viewModel.saveMemos(context)
                                            memosInDialog.remove(memo)
                                        }
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete Memo", tint = Color.Red)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            TextField(
                                value = memoText,
                                onValueChange = { memoText = it },
                                label = { Text("輸入備忘錄") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                TextButton(onClick = { showTimePicker = true }) {
                                    Text(if (selectedTime != null) "選擇時間: ${selectedTime}" else "選擇提醒時間")
                                }
                                IconButton(
                                    onClick = {
                                        if (memoText.isNotEmpty() && viewModel.selectedDate != null) {
                                            val newMemo = Memo(
                                                viewModel.selectedDate!!,
                                                selectedTime,
                                                memoText
                                            )
                                            viewModel.addMemo(newMemo)
                                            viewModel.saveMemos(context)
                                            memosInDialog.add(newMemo)
                                            // 排程通知
                                            scheduleMemoReminder(context, newMemo)
                                            memoText = ""
                                            selectedTime = null
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Add Memo")
                                }
                            }
                        }
                    }
                }
            }

            // 時間選擇器對話框
            if (showTimePicker) {
                TimePickerDialog(
                    onDismiss = { showTimePicker = false },
                    onConfirm = { hour, minute ->
                        selectedTime = LocalTime.of(hour, minute)
                        showTimePicker = false
                    }
                )
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

@Composable
fun TimePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit
) {
    var selectedHour by remember { mutableStateOf(LocalTime.now().hour) }
    var selectedMinute by remember { mutableStateOf(LocalTime.now().minute) }

    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = { Text("選擇提醒時間") },
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // 時
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("時", style = MaterialTheme.typography.bodyMedium)
                    NumberPicker(
                        value = selectedHour,
                        onValueChange = { selectedHour = it },
                        range = 0..23
                    )
                }
                // 分
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("分", style = MaterialTheme.typography.bodyMedium)
                    NumberPicker(
                        value = selectedMinute,
                        onValueChange = { selectedMinute = it },
                        range = 0..59
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedHour, selectedMinute) }) {
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
fun NumberPicker(
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange
) {
    AndroidView(
        factory = { context ->
            NumberPicker(context).apply {
                minValue = range.first
                maxValue = range.last
                this.value = value
                setOnValueChangedListener { _, _, newVal ->
                    onValueChange(newVal)
                }
            }
        },
        modifier = Modifier
            .width(60.dp)
            .height(100.dp)
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewCalendarScreen() {
    CalendarScreen(context = LocalContext.current)
}



@Composable
fun SeasonalTheme(month: Month, content: @Composable () -> Unit) {
    val colors = when (month) {
        Month.MARCH, Month.APRIL, Month.MAY -> {
            lightColorScheme(
                primary = Color(0xFF388E3C),   // 深綠
                secondary = Color(0xFFD81B60), // 深粉
                background = Color(0xFFF0FFF0), // 帶淡綠調的白色背景
                primaryContainer = Color(0xFF81C784), // 比較淺的綠色容器
                onPrimaryContainer = Color.White
            )
        }
        Month.JUNE, Month.JULY, Month.AUGUST -> {
            lightColorScheme(
                primary = Color(0xFFD32F2F),   // 深紅
                secondary = Color(0xFFC2185B), // 深粉紅
                background = Color(0xFFFFF5F5), // 帶淡粉紅調的白色背景
                primaryContainer = Color(0xFFFFCDD2), // 淺粉紅容器
                onPrimaryContainer = Color.Black
            )
        }
        Month.SEPTEMBER, Month.OCTOBER, Month.NOVEMBER -> {
            lightColorScheme(
                primary = Color(0xFF5D4037),   // 深咖啡棕
                secondary = Color(0xFF6D4C41), // 稍深棕灰
                background = Color(0x19BF680C), // 淡米黃色背景
                primaryContainer = Color(0xFFD7CCC8), // 淺棕灰容器
                onPrimaryContainer = Color.Black
            )
        }
        else -> {
            lightColorScheme(
                primary = Color(0xFF1976D2),   // 深藍
                secondary = Color(0xFF455A64), // 深灰藍
                background = Color(0xFFF0F8FF), // 帶淡藍調的白色背景
                primaryContainer = Color(0xFFBBDEFB), // 淺藍容器
                onPrimaryContainer = Color.Black
            )
        }
    }

    MaterialTheme(
        colorScheme = colors,
        typography = MaterialTheme.typography,
        shapes = MaterialTheme.shapes,
        content = content
    )
}
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        createNotificationChannel()
        setContent {
            val viewModel: CalendarViewModel = viewModel()
            SeasonalTheme(month = viewModel.currentMonth.month) {
                CalendarScreen(viewModel = viewModel, context = this)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Memo Reminders"
            val descriptionText = "Channel for memo reminders"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel("memo_reminder_channel", name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}

// 排程提醒的函數和 WorkManager 的 Worker
fun scheduleMemoReminder(context: Context, memo: Memo) {
    memo.time?.let { time ->
        val now = java.time.LocalDateTime.now()
        val memoDateTime = memo.date.atTime(time)
        val delay = java.time.Duration.between(now, memoDateTime).toMillis()
        if (delay > 0) {
            val data = Data.Builder()
                .putString("content", memo.content)
                .build()

            val workRequest = OneTimeWorkRequestBuilder<MemoReminderWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(data)
                .build()

            WorkManager.getInstance(context).enqueue(workRequest)
        }
    }
}

class MemoReminderWorker(appContext: Context, workerParams: WorkerParameters) :
    Worker(appContext, workerParams) {

    override fun doWork(): Result {
        val content = inputData.getString("content") ?: return Result.failure()

        val builder = NotificationCompat.Builder(applicationContext, "memo_reminder_channel")
            .setSmallIcon(R.drawable.ic_notification) // 確保有此圖標資源
            .setContentTitle("備忘錄提醒")
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(applicationContext)) {
            notify(System.currentTimeMillis().toInt(), builder.build())
        }

        return Result.success()
    }
}
