package app.schoolo

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val Bg = Color(0xFF0B0C12)
private val Panel = Color(0xFF151722)
private val Panel2 = Color(0xFF1D2030)
private val Purple = Color(0xFF9B87FF)
private val TextMain = Color(0xFFF5F5FA)
private val TextMuted = Color(0xFF9B9DB0)
private val DayNames = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
private val DayFull = listOf("Понедельник", "Вторник", "Среда", "Четверг", "Пятница", "Суббота", "Воскресенье")

data class Lesson(
    val subject: String,
    val room: String,
    val start: String,
    val end: String,
    val teacher: String = ""
)

class MainActivity : ComponentActivity() {
    private val askNotifications = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createNotificationChannel()
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent { SchooloApp(this) }
    }
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("schoolo_reminders", "Напоминания SCHOOLO", NotificationManager.IMPORTANCE_DEFAULT)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
}

@Composable
fun SchooloApp(context: Context) {
    val prefs = remember { context.getSharedPreferences("schoolo", Context.MODE_PRIVATE) }
    val schedule = remember {
        mutableStateListOf<SnapshotStateList<Lesson>>().apply {
            val raw = prefs.getString("schedule", null)
            if (raw != null) {
                try {
                    val arr = JSONArray(raw)
                    for (d in 0 until 7) {
                        val day = mutableStateListOf<Lesson>()
                        val items = arr.optJSONArray(d) ?: JSONArray()
                        for (i in 0 until items.length()) {
                            val o = items.getJSONObject(i)
                            day.add(Lesson(o.optString("subject"), o.optString("room"), o.optString("start"), o.optString("end"), o.optString("teacher")))
                        }
                        add(day)
                    }
                } catch (_: Exception) { clear() }
            }
            if (isEmpty()) {
                repeat(7) { add(mutableStateListOf<Lesson>()) }
                this[0].addAll(listOf(
                    Lesson("Математика", "214", "08:30", "09:15"),
                    Lesson("Русский язык", "105", "09:25", "10:10"),
                    Lesson("Физика", "312", "10:25", "11:10"),
                    Lesson("История", "207", "11:20", "12:05"),
                    Lesson("Информатика", "301", "12:25", "13:10")
                ))
            }
        }
    }
    var page by remember { mutableStateOf("home") }
    var selectedDay by remember { mutableIntStateOf(todayIndex()) }
    var showEditor by remember { mutableStateOf(false) }
    var editIndex by remember { mutableIntStateOf(-1) }
    var editDay by remember { mutableIntStateOf(selectedDay) }
    var subject by remember { mutableStateOf("") }
    var room by remember { mutableStateOf("") }
    var start by remember { mutableStateOf("08:30") }
    var end by remember { mutableStateOf("09:15") }
    var teacher by remember { mutableStateOf("") }
    var userName by remember { mutableStateOf(prefs.getString("name", "") ?: "") }
    var travel by remember { mutableStateOf(prefs.getInt("travel", 25).toString()) }
    var getReady by remember { mutableStateOf(prefs.getInt("ready", 35).toString()) }
    var breakfast by remember { mutableStateOf(prefs.getInt("breakfast", 15).toString()) }
    var reminder by remember { mutableStateOf(prefs.getInt("reminder", 10).toString()) }
    var notifyEnabled by remember { mutableStateOf(prefs.getBoolean("notify", true)) }
    var taskText by remember { mutableStateOf("") }
    val tasks = remember {
        mutableStateListOf<String>().apply {
            val a = prefs.getString("tasks", null)
            if (a != null) try { val j=JSONArray(a); for(i in 0 until j.length()) add(j.getString(i)) } catch (_: Exception) {}
        }
    }
    fun persist() {
        val all = JSONArray()
        schedule.forEach { day ->
            val arr = JSONArray()
            day.sortedBy { it.start }.forEach { l ->
                arr.put(JSONObject().put("subject",l.subject).put("room",l.room).put("start",l.start).put("end",l.end).put("teacher",l.teacher))
            }
            all.put(arr)
        }
        prefs.edit().putString("schedule", all.toString()).putString("name", userName)
            .putInt("travel", travel.toIntOrNull() ?: 25).putInt("ready", getReady.toIntOrNull() ?: 35)
            .putInt("breakfast", breakfast.toIntOrNull() ?: 15).putInt("reminder", reminder.toIntOrNull() ?: 10)
            .putBoolean("notify", notifyEnabled).putString("tasks", JSONArray(tasks).toString()).apply()
    }
    val lessonsToday = schedule[todayIndex()].sortedBy { it.start }
    val now = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
    val current = lessonsToday.firstOrNull { now >= it.start && now < it.end }
    val next = lessonsToday.firstOrNull { it.start > now }
    val featured = current ?: next
    val doneCount = lessonsToday.count { it.end <= now }
    val firstLesson = lessonsToday.firstOrNull()
    val wakeTime = firstLesson?.let {
        val t = toMinutes(it.start) - (travel.toIntOrNull() ?: 25) - (getReady.toIntOrNull() ?: 35) - (breakfast.toIntOrNull() ?: 15)
        fromMinutes(t)
    } ?: "—:—"
    val greeting = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        in 5..11 -> "Доброе утро"
        in 12..17 -> "Хорошего дня"
        else -> "Добрый вечер"
    }

    MaterialTheme(colorScheme = darkColorScheme(
        background = Bg, surface = Panel, primary = Purple, onPrimary = Bg,
        onBackground = TextMain, onSurface = TextMain, secondary = Color(0xFF69DDB6)
    )) {
        Scaffold(
            containerColor = Bg,
            bottomBar = {
                NavigationBar(containerColor = Panel, contentColor = TextMuted) {
                    NavigationBarItem(selected = page=="home", onClick={page="home"}, icon={ Icon(Icons.Default.Home, null) }, label={ Text("Главная") })
                    NavigationBarItem(selected = page=="schedule", onClick={page="schedule"}, icon={ Icon(Icons.Default.CalendarMonth, null) }, label={ Text("Расписание") })
                    NavigationBarItem(selected = page=="tasks", onClick={page="tasks"}, icon={ Icon(Icons.Default.CheckCircle, null) }, label={ Text("Задания") })
                    NavigationBarItem(selected = page=="settings", onClick={page="settings"}, icon={ Icon(Icons.Default.Settings, null) }, label={ Text("Настройки") })
                }
            },
            floatingActionButton = {
                if (page=="schedule") FloatingActionButton(onClick={
                    editDay=selectedDay; editIndex=-1; subject=""; room=""; start="08:30"; end="09:15"; teacher=""; showEditor=true
                }, containerColor=Purple, contentColor=Bg) { Icon(Icons.Default.Add, "Добавить урок") }
            }
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).padding(horizontal=18.dp)) {
                Row(Modifier.fillMaxWidth().padding(top=18.dp, bottom=18.dp), verticalAlignment=Alignment.CenterVertically) {
                    Box(Modifier.size(42.dp).background(Purple, RoundedCornerShape(14.dp)), contentAlignment=Alignment.Center) {
                        Icon(Icons.Default.School, null, tint=Bg, modifier=Modifier.size(25.dp))
                    }
                    Spacer(Modifier.width(11.dp))
                    Column(Modifier.weight(1f)) {
                        Text("SCHOOLO", fontWeight=FontWeight.Black, letterSpacing=2.sp, fontSize=18.sp)
                        Text("ТВОЙ УЧЕБНЫЙ ЦЕНТР", color=TextMuted, fontSize=9.sp, letterSpacing=1.5.sp)
                    }
                    Column(horizontalAlignment=Alignment.End) {
                        Text(now, fontSize=20.sp, fontWeight=FontWeight.Bold)
                        Text(SimpleDateFormat("d MMM", Locale("ru")).format(Date()), color=TextMuted, fontSize=11.sp)
                    }
                }
                when(page) {
                    "home" -> HomeScreen(greeting, userName, featured, current, lessonsToday, doneCount, wakeTime, travel, onSchedule={page="schedule"}, onSettings={page="settings"})
                    "schedule" -> ScheduleScreen(schedule, selectedDay, {selectedDay=it}, { day, idx, lesson ->
                        editDay=day; editIndex=idx; subject=lesson?.subject ?: ""; room=lesson?.room ?: ""; start=lesson?.start ?: "08:30"; end=lesson?.end ?: "09:15"; teacher=lesson?.teacher ?: ""; showEditor=true
                    }, { day, idx -> schedule[day].removeAt(idx); persist() })
                    "tasks" -> TasksScreen(tasks, taskText, {taskText=it}, { if(taskText.isNotBlank()){tasks.add(taskText.trim());taskText="";persist()} }, {tasks.removeAt(it);persist()})
                    else -> SettingsScreen(userName,{userName=it},travel,{travel=it},getReady,{getReady=it},breakfast,{breakfast=it},reminder,{reminder=it},notifyEnabled,{notifyEnabled=it}, {persist()})
                }
            }
        }
        if(showEditor) {
            AlertDialog(
                onDismissRequest={showEditor=false},
                title={Text(if(editIndex<0) "Новый урок" else "Изменить урок")},
                text={
                    Column(verticalArrangement=Arrangement.spacedBy(9.dp)) {
                        OutlinedTextField(subject,{subject=it},label={Text("Предмет")},singleLine=true)
                        OutlinedTextField(room,{room=it},label={Text("Кабинет")},singleLine=true)
                        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(start,{start=it},label={Text("Начало, ЧЧ:ММ")},singleLine=true,modifier=Modifier.weight(1f))
                            OutlinedTextField(end,{end=it},label={Text("Конец, ЧЧ:ММ")},singleLine=true,modifier=Modifier.weight(1f))
                        }
                        OutlinedTextField(teacher,{teacher=it},label={Text("Учитель (необязательно)")},singleLine=true)
                        Text("День: ${DayFull[editDay]}", color=TextMuted, fontSize=12.sp)
                    }
                },
                confirmButton={ TextButton(onClick={
                    if(subject.isNotBlank() && validTime(start) && validTime(end) && toMinutes(end)>toMinutes(start)) {
                        val l=Lesson(subject.trim(),room.trim(),start,end,teacher.trim())
                        if(editIndex<0) schedule[editDay].add(l) else schedule[editDay][editIndex]=l
                        persist(); showEditor=false
                    }
                }){Text("Сохранить")} },
                dismissButton={ Row { if(editIndex>=0) TextButton(onClick={schedule[editDay].removeAt(editIndex);persist();showEditor=false}){Text("Удалить",color=Color(0xFFFF8585))}; TextButton(onClick={showEditor=false}){Text("Отмена")} } }
            )
        }
    }
}

@Composable
private fun HomeScreen(
    greeting:String, name:String, featured:Lesson?, current:Lesson?, lessons:List<Lesson>, done:Int,
    wake:String, travel:String, onSchedule:()->Unit, onSettings:()->Unit
) {
    LazyColumn(verticalArrangement=Arrangement.spacedBy(14.dp), contentPadding=PaddingValues(bottom=18.dp)) {
        item {
            Spacer(Modifier.height(4.dp))
            Text("$greeting${if(name.isNotBlank()) ", $name" else ""} ✨", fontSize=26.sp, fontWeight=FontWeight.Bold)
            Text(SimpleDateFormat("EEEE, d MMMM", Locale("ru")).format(Date()).replaceFirstChar { it.uppercase() }, color=TextMuted)
        }
        item {
            Card(colors=CardDefaults.cardColors(containerColor=Panel), shape=RoundedCornerShape(24.dp), modifier=Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp)) {
                    Row(verticalAlignment=Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).background(Color(0xFF69DDB6), CircleShape))
                        Spacer(Modifier.width(8.dp))
                        Text(if(current!=null) "СЕЙЧАС ИДЁТ УРОК" else "СЛЕДУЮЩИЙ УРОК", color=TextMuted, fontSize=10.sp, letterSpacing=1.4.sp)
                    }
                    Spacer(Modifier.height(17.dp))
                    if(featured!=null) {
                        Text(featured.subject, fontSize=27.sp, fontWeight=FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text("◷  ${featured.start} — ${featured.end}", color=Purple, fontSize=15.sp)
                        Text("⌂  Кабинет ${featured.room.ifBlank { "не указан" }}", color=TextMuted, modifier=Modifier.padding(top=5.dp))
                        if(featured.teacher.isNotBlank()) Text("Учитель: ${featured.teacher}", color=TextMuted, modifier=Modifier.padding(top=4.dp))
                    } else {
                        Text("На сегодня всё!", fontSize=26.sp, fontWeight=FontWeight.Bold)
                        Text("Можно выдохнуть. Отдых тоже важен.", color=TextMuted, modifier=Modifier.padding(top=6.dp))
                    }
                    Spacer(Modifier.height(17.dp))
                    HorizontalDivider(color=Panel2)
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment=Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("${lessons.size} уроков сегодня", fontWeight=FontWeight.SemiBold)
                            Text("Пройдено: $done · Осталось: ${(lessons.size-done).coerceAtLeast(0)}", color=TextMuted, fontSize=12.sp)
                        }
                        TextButton(onClick=onSchedule) { Text("Расписание →") }
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                SmallInfoCard("ВРЕМЯ ПОДЪЁМА", wake, "Настраивается под тебя", Modifier.weight(1f), Purple)
                SmallInfoCard("УРОКОВ СЕГОДНЯ", lessons.size.toString(), "По расписанию", Modifier.weight(1f), Color(0xFF69DDB6))
            }
        }
        item {
            Card(colors=CardDefaults.cardColors(containerColor=Panel), shape=RoundedCornerShape(22.dp), modifier=Modifier.fillMaxWidth().clickable(onClick=onSettings)) {
                Row(Modifier.padding(17.dp), verticalAlignment=Alignment.CenterVertically) {
                    Icon(Icons.Default.Notifications, null, tint=Purple, modifier=Modifier.size(26.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Не забывай о важном", fontWeight=FontWeight.Bold)
                        Text("Настрой время сборов и напоминания", color=TextMuted, fontSize=12.sp)
                    }
                    Text("→", color=Purple, fontSize=20.sp)
                }
            }
        }
        item { Text("УРОКИ СЕГОДНЯ", color=TextMuted, fontSize=11.sp, letterSpacing=1.5.sp, modifier=Modifier.padding(top=4.dp)) }
        itemsIndexed(lessons) { index, l ->
            Row(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(16.dp)).padding(14.dp), verticalAlignment=Alignment.CenterVertically) {
                Column(Modifier.width(66.dp)) { Text(l.start, fontWeight=FontWeight.Bold); Text(l.end, color=TextMuted, fontSize=11.sp) }
                Box(Modifier.width(3.dp).height(39.dp).background(if(l.end<=SimpleDateFormat("HH:mm",Locale.getDefault()).format(Date())) Color(0xFF69DDB6) else Purple, CircleShape))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) { Text(l.subject, fontWeight=FontWeight.SemiBold); Text("Кабинет ${l.room.ifBlank{"—"}}", color=TextMuted, fontSize=12.sp) }
                if(l.end<=SimpleDateFormat("HH:mm",Locale.getDefault()).format(Date())) Icon(Icons.Default.CheckCircle,null,tint=Color(0xFF69DDB6),modifier=Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun SmallInfoCard(title:String, value:String, caption:String, modifier:Modifier, accent:Color) {
    Card(modifier=modifier, colors=CardDefaults.cardColors(containerColor=Panel), shape=RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(15.dp)) {
            Text(title, color=TextMuted, fontSize=9.sp, letterSpacing=1.sp)
            Text(value, fontSize=25.sp, fontWeight=FontWeight.Bold, color=accent, modifier=Modifier.padding(top=8.dp))
            Text(caption, color=TextMuted, fontSize=10.sp, modifier=Modifier.padding(top=4.dp))
        }
    }
}

@Composable
private fun ScheduleScreen(
    schedule:List<MutableList<Lesson>>, selectedDay:Int, onDay:(Int)->Unit,
    onEdit:(Int,Int,Lesson?)->Unit, onDelete:(Int,Int)->Unit
) {
    Column(Modifier.fillMaxSize()) {
        Text("Расписание", fontSize=27.sp, fontWeight=FontWeight.Bold)
        Text("Вся учебная неделя в одном месте", color=TextMuted, modifier=Modifier.padding(top=4.dp,bottom=16.dp))
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            DayNames.forEachIndexed { i, day ->
                val chosen=selectedDay==i
                Surface(onClick={onDay(i)}, color=if(chosen) Purple else Panel, shape=RoundedCornerShape(14.dp)) {
                    Column(Modifier.width(47.dp).padding(vertical=11.dp), horizontalAlignment=Alignment.CenterHorizontally) {
                        Text(day, color=if(chosen) Bg else TextMuted, fontWeight=FontWeight.Bold)
                        Text(schedule[i].size.toString(), color=if(chosen) Bg else TextMuted, fontSize=10.sp)
                    }
                }
            }
        }
        Text(DayFull[selectedDay], fontSize=20.sp, fontWeight=FontWeight.Bold, modifier=Modifier.padding(top=20.dp,bottom=10.dp))
        val lessons=schedule[selectedDay].sortedBy { it.start }
        if(lessons.isEmpty()) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment=Alignment.Center) {
                Column(horizontalAlignment=Alignment.CenterHorizontally) {
                    Text("Пока пусто", fontSize=22.sp, fontWeight=FontWeight.Bold)
                    Text("Нажми +, чтобы добавить первый урок", color=TextMuted, modifier=Modifier.padding(top=7.dp))
                }
            }
        } else {
            LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp), contentPadding=PaddingValues(bottom=90.dp)) {
                itemsIndexed(lessons) { _, lesson ->
                    val actualIndex=schedule[selectedDay].indexOf(lesson)
                    Card(colors=CardDefaults.cardColors(containerColor=Panel), shape=RoundedCornerShape(18.dp), modifier=Modifier.fillMaxWidth().clickable { onEdit(selectedDay,actualIndex,lesson) }) {
                        Row(Modifier.padding(15.dp), verticalAlignment=Alignment.CenterVertically) {
                            Column(Modifier.width(64.dp)) { Text(lesson.start, fontWeight=FontWeight.Bold); Text(lesson.end, color=TextMuted, fontSize=11.sp) }
                            Box(Modifier.width(3.dp).height(42.dp).background(Purple,RoundedCornerShape(4.dp)))
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(lesson.subject, fontWeight=FontWeight.Bold, fontSize=16.sp)
                                Text("Кабинет ${lesson.room.ifBlank{"—"}}", color=TextMuted, fontSize=12.sp)
                                if(lesson.teacher.isNotBlank()) Text(lesson.teacher,color=TextMuted,fontSize=11.sp)
                            }
                            Text("✎", color=Purple, fontSize=20.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TasksScreen(tasks:List<String>, taskText:String, onText:(String)->Unit, onAdd:()->Unit, onDelete:(Int)->Unit) {
    Column(Modifier.fillMaxSize()) {
        Text("Домашние задания", fontSize=27.sp, fontWeight=FontWeight.Bold)
        Text("Записывай, чтобы ничего не забыть", color=TextMuted, modifier=Modifier.padding(top=4.dp,bottom=18.dp))
        Row(verticalAlignment=Alignment.CenterVertically) {
            OutlinedTextField(taskText,onText,modifier=Modifier.weight(1f),label={Text("Что нужно сделать?")},singleLine=true)
            Spacer(Modifier.width(8.dp))
            FilledIconButton(onClick=onAdd, enabled=taskText.isNotBlank(), colors=IconButtonDefaults.filledIconButtonColors(containerColor=Purple,contentColor=Bg)) { Icon(Icons.Default.Add,null) }
        }
        Spacer(Modifier.height(16.dp))
        if(tasks.isEmpty()) Text("Здесь пока пусто. Добавь первое задание выше.",color=TextMuted)
        LazyColumn(verticalArrangement=Arrangement.spacedBy(9.dp), contentPadding=PaddingValues(bottom=20.dp)) {
            itemsIndexed(tasks) { i, task ->
                Row(Modifier.fillMaxWidth().background(Panel,RoundedCornerShape(15.dp)).padding(14.dp), verticalAlignment=Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle,null,tint=Purple)
                    Spacer(Modifier.width(10.dp))
                    Text(task,Modifier.weight(1f))
                    TextButton(onClick={onDelete(i)}) { Text("Готово",color=Color(0xFF69DDB6)) }
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    name:String,onName:(String)->Unit,travel:String,onTravel:(String)->Unit,
    ready:String,onReady:(String)->Unit,breakfast:String,onBreakfast:(String)->Unit,
    reminder:String,onReminder:(String)->Unit,notify:Boolean,onNotify:(Boolean)->Unit,onSave:()->Unit
) {
    LazyColumn(verticalArrangement=Arrangement.spacedBy(14.dp),contentPadding=PaddingValues(bottom=22.dp)) {
        item { Text("Настройки",fontSize=27.sp,fontWeight=FontWeight.Bold); Text("Подстрой SCHOOLO под себя",color=TextMuted,modifier=Modifier.padding(top=4.dp)) }
        item {
            SettingsCard("ТВОЙ ПРОФИЛЬ") {
                OutlinedTextField(name,onName,label={Text("Как тебя зовут?")},singleLine=true,modifier=Modifier.fillMaxWidth())
            }
        }
        item {
            SettingsCard("УТРЕННИЙ РАСЧЁТ") {
                Text("Время подъёма рассчитывается от первого урока и твоих сборов.",color=TextMuted,fontSize=12.sp)
                NumberSetting("Дорога до школы (мин)",travel,onTravel)
                NumberSetting("Сборы (мин)",ready,onReady)
                NumberSetting("Завтрак (мин)",breakfast,onBreakfast)
            }
        }
        item {
            SettingsCard("НАПОМИНАНИЯ") {
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Text("Уведомления"); Text("Разрешение Android требуется отдельно",color=TextMuted,fontSize=11.sp) }
                    Switch(checked=notify,onCheckedChange=onNotify)
                }
                NumberSetting("Напоминать за (мин)",reminder,onReminder)
                Text("Настройка сохраняется, но автоматические уведомления о каждом уроке требуют включённого разрешения Android.",color=TextMuted,fontSize=11.sp)
            }
        }
        item { Button(onClick=onSave,modifier=Modifier.fillMaxWidth(),colors=ButtonDefaults.buttonColors(containerColor=Purple,contentColor=Bg),shape=RoundedCornerShape(14.dp)) { Text("Сохранить настройки",fontWeight=FontWeight.Bold) } }
        item { Text("SCHOOLO 1.0.0 · Все данные хранятся локально на устройстве.",color=TextMuted,fontSize=11.sp) }
    }
}

@Composable
private fun SettingsCard(title:String,content:@Composable ColumnScope.()->Unit) {
    Card(colors=CardDefaults.cardColors(containerColor=Panel),shape=RoundedCornerShape(20.dp),modifier=Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Text(title,color=Purple,fontSize=10.sp,letterSpacing=1.5.sp,fontWeight=FontWeight.Bold)
            content()
        }
    }
}
@Composable
private fun NumberSetting(label:String,value:String,onValue:(String)->Unit) {
    OutlinedTextField(value,onValue,label={Text(label)},singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),modifier=Modifier.fillMaxWidth())
}

private fun todayIndex():Int = (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)+5)%7
private fun toMinutes(value:String):Int = try { val p=value.split(":");p[0].toInt()*60+p[1].toInt() } catch (_:Exception){0}
private fun fromMinutes(value:Int):String { val n=(value%1440+1440)%1440;return "%02d:%02d".format(n/60,n%60) }
private fun validTime(value:String):Boolean = Regex("^([01]?[0-9]|2[0-3]):[0-5][0-9]$").matches(value)
