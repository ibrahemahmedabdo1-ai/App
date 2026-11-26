package com.seniorcare.app

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Vibrator
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("senior_care_store", MODE_PRIVATE) }
    private val handler = Handler(Looper.getMainLooper())
    private var english = false
    private var dark = false
    private var fontScale = 1.0f
    private lateinit var root: LinearLayout

    private fun tr(ar: String, en: String) = if (english) en else ar
    private fun arr(key: String): JSONArray = try { JSONArray(prefs.getString(key, "[]")) } catch (_: Exception) { JSONArray() }
    private fun saveArr(key: String, value: JSONArray) { prefs.edit().putString(key, value.toString()).apply() }
    private fun profile() = try { JSONObject(prefs.getString("profile", "{}")) } catch (_: Exception) { JSONObject() }
    private fun saveProfile(p: JSONObject) { prefs.edit().putString("profile", p.toString()).apply() }
    private fun nowText(): String = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        english = prefs.getBoolean("english", false)
        createChannel()
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 41)
        }
        showHome()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel("medication_reminders", tr("تذكير الأدوية", "Medication reminders"), NotificationManager.IMPORTANCE_HIGH)
            channel.description = tr("تنبيهات مواعيد الأدوية", "Medicine schedule alerts")
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun shape(color: Int, radius: Float, stroke: Int = Color.TRANSPARENT): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = radius
            if (stroke != Color.TRANSPARENT) setStroke(1, stroke)
        }

    private fun basePage(title: String): LinearLayout {
        dark = prefs.getBoolean("dark", false)
        fontScale = prefs.getFloat("font_scale", 1.0f)
        val bg = if (dark) Color.rgb(16, 27, 39) else Color.rgb(244, 248, 250)
        val fg = if (dark) Color.WHITE else Color.rgb(24, 48, 62)
        val scroll = ScrollView(this).apply { clipToPadding = false; setFillViewport(true) }
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 22, 20, 32)
            setBackgroundColor(bg)
            layoutDirection = View.LAYOUT_DIRECTION_LOCALE
        }
        scroll.addView(root)
        setContentView(scroll)
        text(root, tr("رفيق  •  رعاية واطمئنان", "RAFIQ  •  CARE & WELLNESS"), 13f, true,
            if (dark) Color.rgb(111, 211, 196) else Color.rgb(13, 139, 128))
        text(root, title, 27f, true, fg)
        val divider = View(this).apply { setBackgroundColor(if (dark) Color.rgb(48, 69, 81) else Color.rgb(220, 231, 234)) }
        root.addView(divider, LinearLayout.LayoutParams(-1, 1).apply { setMargins(0, 4, 0, 14) })
        return root
    }

    private fun text(parent: LinearLayout, value: String, size: Float = 16f, bold: Boolean = false, color: Int? = null) {
        val c = color ?: if (dark) Color.rgb(232, 241, 244) else Color.rgb(55, 76, 88)
        parent.addView(TextView(this).apply {
            text = value
            textSize = size * fontScale
            setTextColor(c)
            if (bold) setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD))
            setLineSpacing(2f, 1.05f)
            setPadding(5, 8, 5, 8)
            gravity = Gravity.START or Gravity.CENTER_VERTICAL
        })
    }

    private fun button(parent: LinearLayout, label: String, color: Int? = null, action: () -> Unit): Button {
        val fill = color ?: if (dark) Color.rgb(25, 133, 126) else Color.rgb(17, 132, 123)
        val b = Button(this).apply {
            text = label
            textSize = 16f * fontScale
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            isAllCaps = false
            setTextColor(Color.WHITE)
            background = shape(fill, 22f)
            elevation = 3f
            minHeight = (58 * resources.displayMetrics.density).toInt()
            setPadding(18, 12, 18, 12)
            layoutDirection = View.LAYOUT_DIRECTION_LOCALE
            gravity = Gravity.CENTER
            setOnClickListener { action() }
        }
        parent.addView(b, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 7, 0, 9) })
        return b
    }

    private fun section(parent: LinearLayout, title: String) {
        val tv = TextView(this).apply {
            text = title
            textSize = 18f * fontScale
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(if (dark) Color.rgb(135, 220, 207) else Color.rgb(17, 111, 107))
            background = shape(if (dark) Color.rgb(27, 52, 62) else Color.rgb(226, 242, 239), 16f)
            setPadding(14, 11, 14, 11)
        }
        parent.addView(tv, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 12, 0, 6) })
    }

    private fun metric(parent: LinearLayout, value: String, label: String, accent: Int) {
        val tile = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = shape(if (dark) Color.rgb(29, 43, 55) else Color.WHITE, 18f,
                if (dark) Color.rgb(52, 73, 83) else Color.rgb(225, 235, 237))
            setPadding(8, 12, 8, 12)
        }
        val number = TextView(this).apply {
            text = value; textSize = 24f * fontScale
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            setTextColor(accent); gravity = Gravity.CENTER
        }
        val caption = TextView(this).apply {
            text = label; textSize = 12f * fontScale
            setTextColor(if (dark) Color.LTGRAY else Color.rgb(91, 111, 121))
            gravity = Gravity.CENTER
        }
        tile.addView(number); tile.addView(caption)
        parent.addView(tile, LinearLayout.LayoutParams(0, -2, 1f).apply { setMargins(3, 5, 3, 10) })
    }

    private fun showHome() {
        val p = profile()
        val page = basePage(tr("الرئيسية", "Home"))
        val name = p.optString("name", "")
        val greeting = if (name.isBlank()) tr("أهلًا بك في رفيق", "Welcome to Rafiq") else tr("أهلًا، $name", "Welcome, $name")
        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = shape(if (dark) Color.rgb(24, 73, 76) else Color.rgb(222, 243, 238), 24f)
            setPadding(18, 16, 18, 16)
        }
        val welcome = TextView(this).apply {
            text = greeting; textSize = 22f * fontScale
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(if (dark) Color.WHITE else Color.rgb(20, 73, 70))
        }
        hero.addView(welcome)
        val sub = TextView(this).apply {
            text = tr("رفيقك اليومي للأدوية والصحة والاطمئنان", "Your daily companion for medication, health and reassurance")
            textSize = 14f * fontScale
            setTextColor(if (dark) Color.rgb(213, 236, 232) else Color.rgb(58, 104, 99))
            setPadding(0, 6, 0, 0)
        }
        hero.addView(sub)
        page.addView(hero, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 5, 0, 14) })

        val meds = arr("medications")
        val logs = arr("dose_logs")
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        var taken = 0
        var missed = 0
        var unconfirmed = 0
        for (i in 0 until logs.length()) {
            val item = logs.optJSONObject(i) ?: continue
            if (item.optString("at").startsWith(today)) {
                when (item.optString("status")) {
                    "taken" -> taken++
                    "not_taken" -> missed++
                    else -> unconfirmed++
                }
            }
        }
        section(page, tr("ملخص اليوم", "Today's overview"))
        val metrics = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        metric(metrics, meds.length().toString(), tr("الأدوية", "Medicines"), if (dark) Color.rgb(111, 211, 196) else Color.rgb(17, 132, 123))
        metric(metrics, taken.toString(), tr("تم التأكيد", "Confirmed"), Color.rgb(43, 153, 111))
        metric(metrics, missed.toString(), tr("لم تؤخذ", "Not taken"), Color.rgb(205, 119, 68))
        page.addView(metrics)
        text(page, tr("الجرعات غير المؤكدة: $unconfirmed. عدم تسجيل الجرعة لا يعني بالضرورة عدم تناولها.", "Unconfirmed doses: $unconfirmed. No response does not necessarily mean the dose was missed."), 13f)

        section(page, tr("الرعاية اليومية", "Daily care"))
        button(page, tr("💊  الأدوية والتذكيرات", "💊  Medicines & reminders")) { showMedicines() }
        button(page, tr("📋  السجل والمواعيد", "📋  History & appointments")) { showHistory() }
        button(page, tr("♥  القياسات الصحية", "♥  Health measurements")) { showHealth() }
        button(page, tr("🪪  الملف الطبي وبطاقة الطوارئ", "🪪  Medical profile & emergency card")) { showProfile() }
        button(page, tr("⚙  إعدادات العرض واللغة", "⚙  Display & language settings")) { showSettings() }

        section(page, tr("الطوارئ والاطمئنان", "Emergency & reassurance"))
        val sos = button(page, tr("🆘  اضغط مطولًا لطلب المساعدة", "🆘  Press and hold for help"), Color.rgb(190, 54, 62)) {}
        val runnable = Runnable { sos.isPressed = false; showSosConfirm() }
        sos.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> { v.isPressed = true; handler.postDelayed(runnable, 1400); true }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> { v.isPressed = false; handler.removeCallbacks(runnable); true }
                else -> true
            }
        }
        button(page, tr("❤️  أنا بخير", "❤️  I'm okay"), if (dark) Color.rgb(55, 105, 113) else Color.rgb(76, 130, 137)) {
            val msg = tr("أنا بخير الآن.", "I'm okay now.")
            val phone = p.optString("contact", "")
            if (phone.isNotBlank()) openSms(phone, msg) else showInfo(tr("لا توجد جهة اتصال مسجلة. يمكنك إضافة رقم في الملف الطبي.", "No emergency contact saved. Add a number in your medical profile."))
        }
        text(page, tr("التذكيرات تعمل محليًا على هذا الهاتف، وقد تتأثر بإعدادات توفير البطارية وصلاحيات الإشعارات.", "Reminders run locally on this phone and may be affected by battery optimization and notification permissions."), 12f)
    }

    private fun showMedicines() {
        val page = basePage(tr("الأدوية والتذكيرات", "Medicines & reminders"))
        text(page, tr("أضف الأدوية ومواعيدها لتصلك تنبيهات واضحة في الوقت المحدد.", "Add medicines and schedules to receive clear reminders on time."), 14f)
        button(page, tr("+ إضافة دواء جديد", "+ Add a new medicine")) { addMedicine() }
        val meds = arr("medications")
        if (meds.length() == 0) {
            val empty = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                background = shape(if (dark) Color.rgb(29, 43, 55) else Color.WHITE, 20f,
                    if (dark) Color.rgb(52, 73, 83) else Color.rgb(225, 235, 237))
                setPadding(20, 22, 20, 22)
            }
            val title = TextView(this).apply {
                text = tr("لا توجد أدوية مسجلة", "No medicines yet")
                textSize = 19f * fontScale
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(if (dark) Color.WHITE else Color.rgb(35, 65, 76))
                gravity = Gravity.CENTER
            }
            val hint = TextView(this).apply {
                text = tr("ابدأ بإضافة أول دواء ومواعيده.", "Start by adding your first medicine and its schedule.")
                textSize = 14f * fontScale
                setTextColor(if (dark) Color.LTGRAY else Color.GRAY)
                gravity = Gravity.CENTER
                setPadding(0, 8, 0, 0)
            }
            empty.addView(title); empty.addView(hint)
            page.addView(empty, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 10, 0, 14) })
        }
        for (i in 0 until meds.length()) {
            val m = meds.optJSONObject(i) ?: continue
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                background = shape(if (dark) Color.rgb(29, 43, 55) else Color.WHITE, 20f,
                    if (dark) Color.rgb(52, 73, 83) else Color.rgb(225, 235, 237))
                setPadding(15, 12, 15, 12)
            }
            val name = TextView(this).apply {
                text = "💊  " + m.optString("name")
                textSize = 20f * fontScale
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setTextColor(if (dark) Color.WHITE else Color.rgb(26, 61, 73))
            }
            card.addView(name)
            val details = TextView(this).apply {
                text = tr("الموعد: ${m.optString("time")}  •  ${m.optString("note")}", "Time: ${m.optString("time")}  •  ${m.optString("note")}")
                textSize = 14f * fontScale
                setTextColor(if (dark) Color.rgb(190, 207, 214) else Color.rgb(91, 111, 121))
                setPadding(0, 6, 0, 7)
            }
            card.addView(details)
            button(card, tr("✓  أخذت الجرعة", "✓  I took this dose"), Color.rgb(43, 145, 105)) { logDose(m.optString("name"), "taken"); showMedicines() }
            button(card, tr("لم آخذها بعد", "Not taken yet"), Color.rgb(190, 119, 65)) { logDose(m.optString("name"), "not_taken"); showMedicines() }
            button(card, tr("حذف الدواء", "Delete medicine"), Color.rgb(133, 76, 82)) { deleteMedicine(m.optString("id")); showMedicines() }
            page.addView(card, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 9, 0, 10) })
        }
        button(page, tr("رجوع للرئيسية", "Back to home"), if (dark) Color.rgb(55, 80, 92) else Color.rgb(91, 116, 126)) { showHome() }
    }

    private fun addMedicine() {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(24, 8, 24, 4) }
        val name = EditText(this).apply { hint = tr("اسم الدواء", "Medicine name") }
        val time = EditText(this).apply { hint = "08:00"; setText("08:00") }
        val note = EditText(this).apply { hint = tr("ملاحظات الجرعة (حسب وصف الطبيب)", "Dose notes (as prescribed)") }
        box.addView(name); box.addView(time); box.addView(note)
        AlertDialog.Builder(this).setTitle(tr("إضافة دواء", "Add medicine")).setView(box)
            .setNegativeButton(tr("إلغاء", "Cancel"), null)
            .setPositiveButton(tr("حفظ وتفعيل التذكير", "Save & enable reminder")) { _, _ ->
                val n = name.text.toString().trim()
                val t = time.text.toString().trim()
                if (n.isBlank() || !t.matches(Regex("[0-9]{2}:[0-9]{2}"))) { showInfo(tr("اكتب اسم الدواء والموعد بصيغة 08:00.", "Enter a medicine name and time in 08:00 format.")); return@setPositiveButton }
                val m = JSONObject().put("id", System.currentTimeMillis().toString()).put("name", n).put("time", t).put("note", note.text.toString())
                val a = arr("medications"); a.put(m); saveArr("medications", a)
                AlarmTools.schedule(this, m)
                showInfo(tr("تم حفظ الدواء وجدولة التذكير على هذا الهاتف.", "Medicine saved and reminder scheduled on this phone."))
                showMedicines()
            }.show()
    }

    private fun deleteMedicine(id: String) {
        val old = arr("medications"); val fresh = JSONArray()
        for (i in 0 until old.length()) { val m = old.optJSONObject(i) ?: continue; if (m.optString("id") != id) fresh.put(m) }
        saveArr("medications", fresh)
    }

    private fun logDose(name: String, status: String) {
        val a = arr("dose_logs")
        a.put(JSONObject().put("medicine", name).put("status", status).put("at", nowText()))
        saveArr("dose_logs", a)
        AlarmTools.cancelRepeat(this, name)
        showInfo(if (status == "taken") tr("تم تسجيل تأكيد الجرعة.", "Dose confirmation saved.") else tr("تم تسجيل أنك لم تأخذها بعد.", "Saved as not taken yet."))
    }

    private fun showHistory() {
        val page = basePage(tr("السجل والمواعيد", "History & appointments"))
        button(page, tr("+ إضافة موعد طبي", "+ Add medical appointment")) { addAppointment() }
        button(page, tr("عرض سجل الجرعات", "View dose history")) {
            val p = basePage(tr("سجل الجرعات", "Dose history"))
            val a = arr("dose_logs")
            for (i in a.length() - 1 downTo 0) {
                val x = a.optJSONObject(i) ?: continue
                val status = when (x.optString("status")) { "taken" -> tr("تم التأكيد", "Confirmed"); "not_taken" -> tr("لم تؤخذ", "Not taken"); else -> tr("غير مؤكد", "Unconfirmed") }
                text(p, "${x.optString("at")}  •  ${x.optString("medicine")}  •  $status")
            }
            button(p, tr("رجوع", "Back")) { showHistory() }
        }
        section(page, tr("المواعيد الطبية", "Medical appointments"))
        val ap = arr("appointments")
        for (i in ap.length() - 1 downTo 0) {
            val x = ap.optJSONObject(i) ?: continue
            text(page, "${x.optString("date")}  •  ${x.optString("title")}")
            button(page, tr("تمت الزيارة", "Visit completed")) {
                x.put("done", true); ap.put(i, x); saveArr("appointments", ap); showHistory()
            }
        }
        button(page, tr("رجوع للرئيسية", "Back to home")) { showHome() }
    }

    private fun addAppointment() {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(24, 8, 24, 4) }
        val title = EditText(this).apply { hint = tr("اسم الطبيب أو الفحص", "Doctor or test") }
        val date = EditText(this).apply { hint = tr("التاريخ والوقت", "Date and time"); setText(nowText()) }
        box.addView(title); box.addView(date)
        AlertDialog.Builder(this).setTitle(tr("موعد طبي جديد", "New appointment")).setView(box)
            .setNegativeButton(tr("إلغاء", "Cancel"), null).setPositiveButton(tr("حفظ", "Save")) { _, _ ->
                if (title.text.isNotBlank()) { val a = arr("appointments"); a.put(JSONObject().put("title", title.text.toString()).put("date", date.text.toString()).put("done", false)); saveArr("appointments", a); showHistory() }
            }.show()
    }

    private fun showHealth() {
        val page = basePage(tr("القياسات الصحية", "Health measurements"))
        button(page, tr("+ تسجيل قياس", "+ Add measurement")) {
            val types = arrayOf(tr("ضغط الدم", "Blood pressure"), tr("السكر", "Blood sugar"), tr("الوزن", "Weight"), tr("درجة الحرارة", "Temperature"), tr("أعراض / ملاحظات", "Symptoms / notes"))
            AlertDialog.Builder(this).setTitle(tr("اختر نوع القياس", "Choose measurement")).setItems(types) { _, which -> addMeasurement(types[which]) }.show()
        }
        val a = arr("measurements")
        for (i in a.length() - 1 downTo 0) {
            val x = a.optJSONObject(i) ?: continue
            text(page, "${x.optString("at")}  •  ${x.optString("type")}: ${x.optString("value")}", 15f)
        }
        button(page, tr("رجوع للرئيسية", "Back to home")) { showHome() }
    }

    private fun addMeasurement(type: String) {
        val input = EditText(this).apply { hint = tr("القيمة أو الملاحظة", "Value or note") }
        AlertDialog.Builder(this).setTitle(type).setView(input).setNegativeButton(tr("إلغاء", "Cancel"), null)
            .setPositiveButton(tr("حفظ", "Save")) { _, _ ->
                if (input.text.isNotBlank()) { val a = arr("measurements"); a.put(JSONObject().put("type", type).put("value", input.text.toString()).put("at", nowText())); saveArr("measurements", a); showHealth() }
            }.show()
    }

    private fun showProfile() {
        val p = profile()
        val page = basePage(tr("الملف الطبي وبطاقة الطوارئ", "Medical profile & emergency card"))
        val fields = listOf(
            "name" to tr("الاسم", "Name"),
            "age" to tr("العمر", "Age"),
            "chronic" to tr("الأمراض المزمنة", "Chronic conditions"),
            "allergies" to tr("الحساسية", "Allergies"),
            "avoid" to tr("أدوية يجب تجنبها", "Medicines to avoid"),
            "contactName" to tr("اسم جهة اتصال الطوارئ", "Emergency contact name"),
            "contact" to tr("رقم جهة اتصال الطوارئ", "Emergency contact phone")
        )
        for ((key, label) in fields) {
            text(page, "$label: ${p.optString(key, tr("غير مسجل", "Not set"))}")
            button(page, tr("تعديل", "Edit") + " - " + label) { editProfileField(key, label) }
        }
        section(page, tr("بطاقة طبية للطوارئ", "Emergency medical card"))
        text(page, tr("تتضمن الاسم والعمر والحالات المزمنة والحساسية والأدوية التي يجب تجنبها وجهة اتصال الطوارئ.", "Includes name, age, chronic conditions, allergies, medicines to avoid and emergency contact."))
        button(page, tr("مشاركة البطاقة الطبية", "Share medical card")) { shareMedicalCard() }
        button(page, tr("اتصال بجهة الطوارئ", "Call emergency contact")) {
            val phone = p.optString("contact", "")
            if (phone.isNotBlank()) startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))) else showInfo(tr("أضف رقم جهة اتصال أولًا.", "Add an emergency contact number first."))
        }
        button(page, tr("رجوع للرئيسية", "Back to home")) { showHome() }
    }

    private fun editProfileField(key: String, label: String) {
        val input = EditText(this).apply { setText(profile().optString(key, "")); hint = label }
        AlertDialog.Builder(this).setTitle(label).setView(input).setNegativeButton(tr("إلغاء", "Cancel"), null)
            .setPositiveButton(tr("حفظ", "Save")) { _, _ -> val p = profile(); p.put(key, input.text.toString()); saveProfile(p); showProfile() }.show()
    }

    private fun shareMedicalCard() {
        val p = profile()
        val body = buildString {
            append(tr("بطاقة طبية للطوارئ\n", "Emergency medical card\n"))
            append(tr("الاسم: ", "Name: ")).append(p.optString("name", "-")).append("\n")
            append(tr("العمر: ", "Age: ")).append(p.optString("age", "-")).append("\n")
            append(tr("أمراض مزمنة: ", "Chronic conditions: ")).append(p.optString("chronic", "-")).append("\n")
            append(tr("حساسية: ", "Allergies: ")).append(p.optString("allergies", "-")).append("\n")
            append(tr("أدوية يجب تجنبها: ", "Medicines to avoid: ")).append(p.optString("avoid", "-")).append("\n")
            append(tr("جهة الاتصال: ", "Emergency contact: ")).append(p.optString("contactName", "")).append(" ").append(p.optString("contact", "-"))
        }
        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, body), tr("مشاركة البطاقة الطبية", "Share medical card")))
    }

    private fun showSosConfirm() {
        val p = profile()
        (getSystemService(VIBRATOR_SERVICE) as? Vibrator)?.vibrate(250)
        AlertDialog.Builder(this).setTitle(tr("طلب مساعدة طارئة؟", "Request emergency help?"))
            .setMessage(tr("سيتم فتح تطبيق الرسائل برسالة جاهزة لجهة اتصال الطوارئ. راجع الرسالة واضغط إرسال بنفسك.", "Your messaging app will open with a prepared emergency message. Review it and press Send yourself."))
            .setNegativeButton(tr("إلغاء", "Cancel"), null)
            .setPositiveButton(tr("متابعة", "Continue")) { _, _ ->
                val phone = p.optString("contact", "")
                val person = p.optString("name", tr("المستخدم", "User"))
                val msg = tr("طوارئ! $person يحتاج إلى المساعدة. يرجى التواصل معه فورًا.", "EMERGENCY! $person needs help. Please contact them immediately.")
                if (phone.isNotBlank()) openSms(phone, msg) else showInfo(tr("لا توجد جهة اتصال طوارئ. أضف رقمًا في الملف الطبي.", "No emergency contact. Add a number in the medical profile."))
            }.show()
    }

    private fun openSms(phone: String, message: String) {
        try {
            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$phone")).putExtra("sms_body", message)
            startActivity(intent)
        } catch (_: Exception) { showInfo(tr("تعذر فتح تطبيق الرسائل على هذا الهاتف.", "Could not open the messaging app.")) }
    }

    private fun showSettings() {
        val page = basePage(tr("إعدادات العرض واللغة", "Display & language settings"))
        button(page, tr("تكبير الخط والأزرار", "Increase text and buttons")) {
            val next = (prefs.getFloat("font_scale", 1f) + 0.15f).coerceAtMost(1.75f)
            prefs.edit().putFloat("font_scale", next).apply(); showSettings()
        }
        button(page, tr("تصغير الخط والأزرار", "Decrease text and buttons")) {
            val next = (prefs.getFloat("font_scale", 1f) - 0.15f).coerceAtLeast(0.85f)
            prefs.edit().putFloat("font_scale", next).apply(); showSettings()
        }
        button(page, tr("تبديل الوضع الليلي / النهاري", "Toggle dark / light mode")) { prefs.edit().putBoolean("dark", !prefs.getBoolean("dark", false)).apply(); showSettings() }
        button(page, tr("التبديل إلى العربية / English", "Switch Arabic / English")) { prefs.edit().putBoolean("english", !prefs.getBoolean("english", false)).apply(); english = prefs.getBoolean("english", false); showSettings() }
        text(page, tr("يمكن اختيار حجم مريح للخط والأزرار. الوضع الليلي اختياري.", "Choose a comfortable text and button size. Dark mode is optional."))
        button(page, tr("رجوع للرئيسية", "Back to home")) { showHome() }
    }

    private fun showInfo(message: String) { AlertDialog.Builder(this).setMessage(message).setPositiveButton(tr("حسنًا", "OK"), null).show() }
}

object AlarmTools {
    fun schedule(context: Context, med: JSONObject) {
        val time = med.optString("time", "08:00").split(":")
        if (time.size != 2) return
        val hour = time[0].toIntOrNull() ?: return
        val minute = time[1].toIntOrNull() ?: return
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, minute); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }
        val intent = Intent(context, MedicationAlarmReceiver::class.java)
            .putExtra("medicine", med.optString("name")).putExtra("time", med.optString("time"))
        val id = med.optString("id").hashCode()
        val pi = PendingIntent.getBroadcast(context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        try {
            if (Build.VERSION.SDK_INT >= 23) {
                if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
                else am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
            } else am.setExact(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
        } catch (_: SecurityException) { am.set(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi) }
    }

    fun scheduleRepeat(context: Context, name: String, time: String) {
        val intent = Intent(context, MedicationAlarmReceiver::class.java)
            .putExtra("medicine", name).putExtra("time", time).putExtra("repeat", true)
        val pi = PendingIntent.getBroadcast(context, name.hashCode() xor 0x41A5, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val at = System.currentTimeMillis() + 10 * 60 * 1000L
        try {
            if (Build.VERSION.SDK_INT >= 23) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            else am.setExact(AlarmManager.RTC_WAKEUP, at, pi)
        } catch (_: SecurityException) { am.set(AlarmManager.RTC_WAKEUP, at, pi) }
    }

    fun cancelRepeat(context: Context, name: String) {
        val intent = Intent(context, MedicationAlarmReceiver::class.java)
        val pi = PendingIntent.getBroadcast(context, name.hashCode() xor 0x41A5, intent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
        if (pi != null) {
            (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(pi)
            pi.cancel()
        }
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(name.hashCode())
    }
}

class MedicationAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val name = intent.getStringExtra("medicine") ?: "Medicine"
        val time = intent.getStringExtra("time") ?: ""
        val repeat = intent.getBooleanExtra("repeat", false)
        val prefs = context.getSharedPreferences("senior_care_store", Context.MODE_PRIVATE)
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        val logs = try { JSONArray(prefs.getString("dose_logs", "[]")) } catch (_: Exception) { JSONArray() }
        var responded = false
        for (i in 0 until logs.length()) {
            val item = logs.optJSONObject(i) ?: continue
            if (item.optString("medicine") == name && item.optString("at").startsWith(today) &&
                (item.optString("status") == "taken" || item.optString("status") == "not_taken")) {
                responded = true
                break
            }
        }
        if (responded) {
            AlarmTools.cancelRepeat(context, name)
            return
        }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26 && manager.getNotificationChannel("medication_reminders") == null) {
            manager.createNotificationChannel(NotificationChannel("medication_reminders", "Medication reminders", NotificationManager.IMPORTANCE_HIGH))
        }
        val notification = if (Build.VERSION.SDK_INT >= 26) Notification.Builder(context, "medication_reminders") else Notification.Builder(context)
        val pending = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        notification.setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("موعد الدواء الآن")
            .setContentText("$name  •  $time")
            .setAutoCancel(true).setContentIntent(pending).setPriority(Notification.PRIORITY_HIGH)
        manager.notify(name.hashCode(), notification.build())
        AlarmTools.scheduleRepeat(context, name, time)

        if (!repeat) {
            val meds = try { JSONArray(prefs.getString("medications", "[]")) } catch (_: Exception) { JSONArray() }
            for (i in 0 until meds.length()) {
                val med = meds.optJSONObject(i) ?: continue
                if (med.optString("name") == name && med.optString("time") == time) {
                    AlarmTools.schedule(context, JSONObject(med.toString()))
                    break
                }
            }
        }
    }
}
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val prefs = context.getSharedPreferences("senior_care_store", Context.MODE_PRIVATE)
        val meds = try { JSONArray(prefs.getString("medications", "[]")) } catch (_: Exception) { JSONArray() }
        for (i in 0 until meds.length()) meds.optJSONObject(i)?.let { AlarmTools.schedule(context, it) }
    }
}
