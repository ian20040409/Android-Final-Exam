package com.example.finaleaxm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.finaleaxm.ui.theme.FinaleaxmTheme
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.*
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.with

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

    // 用於計算拖曳方向 (-1 表示往前一個月，1 表示往後一個月)
    var totalDragX by remember { mutableStateOf(0f) }
    var direction by remember { mutableStateOf(0) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { totalDragX = 0f },
                    onHorizontalDrag = { _, dragAmount ->
                        totalDragX += dragAmount
                    },
                    onDragEnd = {
                        if (totalDragX > 0) {
                            // 往右拖，顯示前一個月
                            direction = -1
                            currentMonth = currentMonth.minusMonths(1)
                        } else if (totalDragX < 0) {
                            // 往左拖，顯示下一個月
                            direction = 1
                            currentMonth = currentMonth.plusMonths(1)
                        }
                    }
                )
            }
    ) {
        // 標題與月份切換
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "${currentMonth.year}年${currentMonth.monthValue}月",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = {
                    direction = -1
                    currentMonth = currentMonth.minusMonths(1)
                }) {
                    Text("<", style = MaterialTheme.typography.bodyLarge)
                }
                TextButton(onClick = {
                    // 返回今天
                    direction = 0
                    currentMonth = YearMonth.now()
                    selectedDate = LocalDate.now()
                }) {
                    Text("點選以返回今天", style = MaterialTheme.typography.bodyLarge)
                }
                IconButton(onClick = {
                    direction = 1
                    currentMonth = currentMonth.plusMonths(1)
                }) {
                    Text(">", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }

        // 星期標題
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

        // 使用 AnimatedContent 包裹日期格子顯示區域
        AnimatedContent(
            targetState = currentMonth,
            transitionSpec = {
                // 根據 direction 決定動畫方向，預設使用 slideInHorizontally/slideOutHorizontally
                val animDuration = 300
                if (direction >= 0) {
                    // 往下個月 (向左滑入)
                    slideInHorizontally(
                        initialOffsetX = { it },
                        animationSpec = tween(animDuration)
                    ) with slideOutHorizontally(
                        targetOffsetX = { -it },
                        animationSpec = tween(animDuration)
                    )
                } else {
                    // 往上個月 (向右滑入)
                    slideInHorizontally(
                        initialOffsetX = { -it },
                        animationSpec = tween(animDuration)
                    ) with slideOutHorizontally(
                        targetOffsetX = { it },
                        animationSpec = tween(animDuration)
                    )
                }
            }
        ) { animatedMonth ->
            // 月曆格子
            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                modifier = Modifier.padding(vertical = 16.dp)
            ) {
                val daysInMonth = animatedMonth.lengthOfMonth()
                val firstDayOfWeek = animatedMonth.atDay(1).dayOfWeek.value % 7

                // 空白填充
                items(firstDayOfWeek) {
                    Spacer(modifier = Modifier.size(72.dp))
                }

                // 日期格子
                items(daysInMonth) { day ->
                    val date = animatedMonth.atDay(day + 1)
                    val memo = memos.firstOrNull { it.date == date }
                    val isToday = date == LocalDate.now()
                    val isSelected = date == selectedDate

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

        // 新增備忘錄對話框
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

        // 底部功能按鈕
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
@Preview(showBackground = true)
@Composable
fun CalendarAppPreview() {
    FinaleaxmTheme {
        CalendarApp()
    }
}
