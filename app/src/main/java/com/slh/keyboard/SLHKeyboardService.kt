package com.slh.keyboard

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.inputmethodservice.InputMethodService
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.ScrollView
import android.widget.TextView

class SLHKeyboardService : InputMethodService() {

    private lateinit var prefs: PrefsManager
    private lateinit var kbRoot: LinearLayout
    private lateinit var keysContainer: LinearLayout
    private lateinit var suggestionsContainer: LinearLayout
    private lateinit var secondaryPanelScroll: ScrollView
    private lateinit var secondaryPanelContent: LinearLayout

    private var currentLang = "ar" // "ar" or "en"
    private var isShift = false
    private var isSymbols = false
    private var activePanel = "kb" // kb, emoji, symbols, clip

    private val handler = Handler(Looper.getMainLooper())
    private var isDeleting = false
    private val deleteRunnable = object : Runnable {
        override fun run() {
            if (isDeleting) {
                currentInputConnection?.deleteSurroundingText(1, 0)
                handler.postDelayed(this, 65)
            }
        }
    }

    private val arRows = listOf(
        listOf("ض","ص","ث","ق","ف","غ","ع","ه","خ","ح","ج"),
        listOf("ش","س","ي","ب","ل","ا","ت","ن","م","ك","ط"),
        listOf("ذ","ء","ؤ","ر","ى","ة","و","ز","ظ","د","BACK"),
        listOf("SYM","EMOJI","LANG","SPACE",".","ENTER")
    )

    private val enRows = listOf(
        listOf("q","w","e","r","t","y","u","i","o","p"),
        listOf("a","s","d","f","g","h","j","k","l"),
        listOf("SHIFT","z","x","c","v","b","n","m","BACK"),
        listOf("SYM","EMOJI","LANG","SPACE",".","ENTER")
    )

    private val symRows = listOf(
        listOf("1","2","3","4","5","6","7","8","9","0"),
        listOf("@","#","$","%","&","-","+","(",")","/"),
        listOf("*","\"","'",":",";","!","؟","_","=","BACK"),
        listOf("ABC","EMOJI","LANG","SPACE","،","ENTER")
    )

    private val numRow = listOf("1","2","3","4","5","6","7","8","9","0")

    private val longPressAlts = mapOf(
        "ض" to listOf("١","1"), "ص" to listOf("٢","2"), "ث" to listOf("٣","3"),
        "ق" to listOf("٤","4"), "ف" to listOf("٥","5"), "غ" to listOf("٦","6"),
        "ع" to listOf("٧","7"), "ه" to listOf("٨","8","ة"), "خ" to listOf("٩","9"),
        "ح" to listOf("٠","0"),
        "ا" to listOf("أ","إ","آ","ى","ء"),
        "ل" to listOf("لا","لأ","لإ","لآ"),
        "ي" to listOf("ئ","ى"),
        "ت" to listOf("ة","ث"),
        "و" to listOf("ؤ"),
        "." to listOf("،","؟","!","َ","ُ","ِ","ّ","ْ"),
        "q" to listOf("1"), "w" to listOf("2"), "e" to listOf("3"), "r" to listOf("4"),
        "t" to listOf("5"), "y" to listOf("6"), "u" to listOf("7"), "i" to listOf("8"),
        "o" to listOf("9"), "p" to listOf("0")
    )

    private val quickSuggestionsAr = listOf("السلام عليكم", "مرحبًا", "شكرًا", "بالتوفيق", "SLH ✦")
    private val quickSuggestionsEn = listOf("Hello", "Thanks", "Awesome", "Cyber", "SLH ✦")

    private val emojiList = listOf(
        "😀","😂","🥹","😍","😎","🤩","🥳","🤔","🫡","😭","😡","🤯","😴","😇",
        "👋","👏","🙌","🤝","👍","👌","✌️","💪","🙏","✍️","🧠","👑",
        "🔥","⚡","✨","🌟","🌙","☀️","🌹","❤️","🖤","💛","💎","✅","❌",
        "💻","📱","⌨️","🛡️","🔒","🔑","⚙️","🤖","🚀","💡","🇾🇪","🇸🇦","🇵🇸"
    )

    private val symbolList = listOf(
        "±","×","÷","≠","≈","≤","≥","∞","√","∑","π","θ","Ω","°",
        "■","▲","◆","●","★","☆","✦","✧","❖","◈","◉",
        "←","→","↑","↓","↔","⇐","⇒","➜","➤","↻",
        "$","€","£","₿","﷼","①","②","③","④","⑤","©","®","™",
        "꧁༺ SLH ༻꧂","『 SLH 』","✦ ───── ✦","❖ ═════ ❖"
    )

    override fun onCreateInputView(): View {
        prefs = PrefsManager(this)
        val root = layoutInflater.inflate(R.layout.keyboard_view, null)
        kbRoot = root.findViewById(R.id.kbRoot)
        keysContainer = root.findViewById(R.id.keysContainer)
        suggestionsContainer = root.findViewById(R.id.suggestionsContainer)
        secondaryPanelScroll = root.findViewById(R.id.secondaryPanelScroll)
        secondaryPanelContent = root.findViewById(R.id.secondaryPanelContent)

        setupToolbar(root)
        applyTheme()
        renderSuggestions()
        renderKeys()
        return root
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        if (::kbRoot.isInitialized) {
            applyTheme()
            renderSuggestions()
            if (activePanel == "kb") renderKeys()
        }
    }

    private fun setupToolbar(root: View) {
        root.findViewById<TextView>(R.id.btnToolKb).setOnClickListener { showPanel("kb") }
        root.findViewById<TextView>(R.id.btnToolEmoji).setOnClickListener { showPanel("emoji") }
        root.findViewById<TextView>(R.id.btnToolSymbols).setOnClickListener { showPanel("symbols") }
        root.findViewById<TextView>(R.id.btnToolClip).setOnClickListener { showPanel("clip") }
        root.findViewById<TextView>(R.id.btnToolSig).setOnClickListener {
            feedback()
            currentInputConnection?.commitText(prefs.signature, 1)
        }
        root.findViewById<TextView>(R.id.btnToolTheme).setOnClickListener {
            feedback()
            prefs.isLightMode = !prefs.isLightMode
            applyTheme()
            renderKeys()
            renderSuggestions()
        }
        root.findViewById<TextView>(R.id.btnToolSettings).setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }
    }

    private fun showPanel(panel: String) {
        feedback()
        activePanel = if (activePanel == panel && panel != "kb") "kb" else panel
        if (activePanel == "kb") {
            keysContainer.visibility = View.VISIBLE
            secondaryPanelScroll.visibility = View.GONE
            renderKeys()
        } else {
            keysContainer.visibility = View.GONE
            secondaryPanelScroll.visibility = View.VISIBLE
            when (activePanel) {
                "emoji" -> renderGridPanel(emojiList, 7, 22f)
                "symbols" -> renderGridPanel(symbolList, 6, 16f)
                "clip" -> renderClipboardPanel()
            }
        }
    }

    private fun applyTheme() {
        val bgColor = if (prefs.isLightMode) Color.parseColor("#E2E8F0") else Color.parseColor("#0D121E")
        kbRoot.setBackgroundColor(bgColor)
    }

    private fun renderSuggestions() {
        suggestionsContainer.removeAllViews()
        val list = if (currentLang == "ar") quickSuggestionsAr else quickSuggestionsEn
        val textColor = if (prefs.isLightMode) Color.parseColor("#0F172A") else Color.parseColor("#F3F6FC")
        val chipBg = if (prefs.isLightMode) Color.WHITE else Color.parseColor("#1A2234")

        for (word in list) {
            val tv = TextView(this).apply {
                text = word
                setTextColor(textColor)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                setPadding(dp(12), dp(4), dp(12), dp(4))
                background = GradientDrawable().apply {
                    setColor(chipBg)
                    cornerRadius = dp(16).toFloat()
                    setStroke(1, prefs.accentColor)
                }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(dp(4), 0, dp(4), 0) }
                setOnClickListener {
                    feedback()
                    currentInputConnection?.commitText("$word ", 1)
                }
            }
            suggestionsContainer.addView(tv)
        }
    }

    private fun renderKeys() {
        keysContainer.removeAllViews()
        val rows = mutableListOf<List<String>>()
        if (prefs.showNumRow && !isSymbols) rows.add(numRow)
        val baseRows = when {
            isSymbols -> symRows
            currentLang == "ar" -> arRows
            else -> enRows
        }
        rows.addAll(baseRows)

        val keyBg = if (prefs.isLightMode) Color.WHITE else Color.parseColor("#1A2234")
        val fnBg = if (prefs.isLightMode) Color.parseColor("#CBD5E1") else Color.parseColor("#242F47")
        val textColor = if (prefs.isLightMode) Color.parseColor("#0F172A") else Color.parseColor("#F3F6FC")
        val accent = prefs.accentColor

        for (row in rows) {
            val rowLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutDirection = View.LAYOUT_DIRECTION_LTR
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(48)
                ).apply { setMargins(0, dp(3), 0, dp(3)) }
            }
            if (!isSymbols && currentLang == "en" && row.size == 9 && row[0].equals("a", true)) {
                rowLayout.setPadding(dp(16), 0, dp(16), 0)
            }

            for (rawKey in row) {
                val label = when (rawKey) {
                    "SHIFT" -> "⇧"
                    "BACK" -> "⌫"
                    "ENTER" -> "↵"
                    "SYM" -> if (currentLang == "ar") "؟٣٢١" else "?123"
                    "ABC" -> if (currentLang == "ar") "أ ب" else "ABC"
                    "LANG" -> "🌐"
                    "EMOJI" -> "😀"
                    "SPACE" -> if (currentLang == "ar") "العربية • SLH" else "English • SLH"
                    else -> if (currentLang == "en" && isShift) rawKey.uppercase() else rawKey
                }

                val weight = when (rawKey) {
                    "SPACE" -> 4.2f
                    "ENTER" -> 1.6f
                    "BACK", "SHIFT", "SYM", "ABC" -> 1.45f
                    "LANG", "EMOJI" -> 1.1f
                    else -> 1f
                }

                val isFn = rawKey in listOf("SHIFT","BACK","SYM","ABC","LANG","EMOJI")
                val isEnter = rawKey == "ENTER"

                val keyView = TextView(this).apply {
                    text = label
                    gravity = Gravity.CENTER
                    setTextColor(if (isEnter) Color.parseColor("#080B12") else textColor)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, when {
                        rawKey == "SPACE" -> 13f
                        isFn -> 15f
                        else -> 19f
                    })
                    background = GradientDrawable().apply {
                        setColor(when {
                            isEnter -> accent
                            isFn -> fnBg
                            else -> keyBg
                        })
                        cornerRadius = dp(8).toFloat()
                        if (rawKey == "SHIFT" && isShift) setStroke(dp(2), accent)
                        else setStroke(1, Color.parseColor("#1FFFFFFF"))
                    }
                    layoutParams = LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        weight
                    ).apply { setMargins(dp(2), 0, dp(2), 0) }
                }

                if (rawKey == "BACK") {
                    keyView.setOnTouchListener { v, event ->
                        when (event.action) {
                            MotionEvent.ACTION_DOWN -> {
                                v.alpha = 0.65f
                                feedback()
                                currentInputConnection?.deleteSurroundingText(1, 0)
                                isDeleting = true
                                handler.postDelayed(deleteRunnable, 350)
                                true
                            }
                            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                v.alpha = 1f
                                isDeleting = false
                                handler.removeCallbacks(deleteRunnable)
                                true
                            }
                            else -> false
                        }
                    }
                } else {
                    keyView.setOnClickListener {
                        handleKeyClick(rawKey, label)
                    }
                    val alts = longPressAlts[rawKey]
                    if (alts != null) {
                        keyView.setOnLongClickListener { v ->
                            feedback()
                            showAlternatesPopup(v, alts)
                            true
                        }
                    }
                }
                rowLayout.addView(keyView)
            }
            keysContainer.addView(rowLayout)
        }
    }

    private fun handleKeyClick(rawKey: String, label: String) {
        feedback()
        val ic = currentInputConnection ?: return
        when (rawKey) {
            "SHIFT" -> { isShift = !isShift; renderKeys() }
            "SYM" -> { isSymbols = true; renderKeys() }
            "ABC" -> { isSymbols = false; renderKeys() }
            "LANG" -> {
                currentLang = if (currentLang == "ar") "en" else "ar"
                isShift = false; isSymbols = false
                renderKeys(); renderSuggestions()
            }
            "EMOJI" -> showPanel("emoji")
            "SPACE" -> ic.commitText(" ", 1)
            "ENTER" -> {
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
            }
            else -> {
                ic.commitText(label, 1)
                if (currentLang == "en" && isShift) {
                    isShift = false
                    renderKeys()
                }
            }
        }
    }

    private fun showAlternatesPopup(anchor: View, alts: List<String>) {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(6), dp(6), dp(6), dp(6))
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#111827"))
                cornerRadius = dp(10).toFloat()
                setStroke(dp(1), prefs.accentColor)
            }
        }
        val popup = PopupWindow(
            container,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            true
        )
        for (ch in alts) {
            val tv = TextView(this).apply {
                text = ch
                setTextColor(Color.WHITE)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
                gravity = Gravity.CENTER
                minWidth = dp(34)
                minHeight = dp(36)
                setPadding(dp(8), dp(4), dp(8), dp(4))
                setOnClickListener {
                    feedback()
                    currentInputConnection?.commitText(ch, 1)
                    popup.dismiss()
                }
            }
            container.addView(tv)
        }
        popup.showAsDropDown(anchor, -dp(20), -dp(95))
    }

    private fun renderGridPanel(items: List<String>, columns: Int, textSizeSp: Float) {
        secondaryPanelContent.removeAllViews()
        val grid = GridLayout(this).apply {
            columnCount = columns
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        val textColor = if (prefs.isLightMode) Color.parseColor("#0F172A") else Color.parseColor("#F3F6FC")
        val itemBg = if (prefs.isLightMode) Color.WHITE else Color.parseColor("#1A2234")

        for (item in items) {
            val tv = TextView(this).apply {
                text = item
                gravity = Gravity.CENTER
                setTextColor(textColor)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSp)
                setPadding(dp(6), dp(8), dp(6), dp(8))
                background = GradientDrawable().apply {
                    setColor(itemBg)
                    cornerRadius = dp(8).toFloat()
                }
                layoutParams = GridLayout.LayoutParams().apply {
                    width = 0
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    setMargins(dp(3), dp(3), dp(3), dp(3))
                }
                setOnClickListener {
                    feedback()
                    currentInputConnection?.commitText(item, 1)
                }
            }
            grid.addView(tv)
        }
        secondaryPanelContent.addView(grid)
    }

    private fun renderClipboardPanel() {
        secondaryPanelContent.removeAllViews()
        val textColor = if (prefs.isLightMode) Color.parseColor("#0F172A") else Color.parseColor("#F3F6FC")
        val cardBg = if (prefs.isLightMode) Color.WHITE else Color.parseColor("#1A2234")

        val items = mutableListOf<String>()
        if (!prefs.incognitoMode) {
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clipText = cm.primaryClip?.getItemAt(0)?.coerceToText(this)?.toString()
            if (!clipText.isNullOrBlank()) items.add(clipText)
        }
        items.add(prefs.signature)
        items.add("السلام عليكم ورحمة الله وبركاته")

        for (txt in items.distinct()) {
            val tv = TextView(this).apply {
                text = txt
                setTextColor(textColor)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                setPadding(dp(12), dp(10), dp(12), dp(10))
                background = GradientDrawable().apply {
                    setColor(cardBg)
                    cornerRadius = dp(10).toFloat()
                    setStroke(1, prefs.accentColor)
                }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, dp(4), 0, dp(4)) }
                setOnClickListener {
                    feedback()
                    currentInputConnection?.commitText(txt, 1)
                    showPanel("kb")
                }
            }
            secondaryPanelContent.addView(tv)
        }
    }

    private fun feedback() {
        if (prefs.soundEnabled) {
            val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            am.playSoundEffect(AudioManager.FX_KEY_CLICK, 0.5f)
        }
        if (prefs.vibrateEnabled) {
            val v = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(14, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(14)
            }
        }
    }

    private fun dp(value: Int): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), resources.displayMetrics).toInt()
}
