package com.github.yournamehere

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ClipboardManager
import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.*
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.entities.Plugin
import com.aliucord.patcher.after
import com.lytefast.flexinput.fragment.FlexInputFragment
import kotlin.concurrent.thread
import kotlin.random.Random

@AliucordPlugin
class MyFirstKotlinPlugin : Plugin() {
    @Volatile private var isSpamming = false
    private var spamThread: Thread? = null
    private var customPrefix = ""
    private var isPrefixEnabled = false
    private var chatInputRef: EditText? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun start(context: Context) {
        patcher.after<FlexInputFragment>("onViewCreated", View::class.java) { param ->
            val view = param.args[0] as View
            val input = view.findViewById<EditText>(com.discord.R.id.chat_input_edit_text) ?: return@after
            val parent = input.parent as? ViewGroup ?: return@after
            chatInputRef = input

            val pasteBtn = ImageButton(context).apply {
                setImageResource(android.R.drawable.ic_menu_edit)
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                setPadding(15, 15, 15, 15)
                setOnClickListener {
                    val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    if (cb.hasPrimaryClip()) {
                        val text = cb.primaryClip?.getItemAt(0)?.text
                        if (text != null) {
                            val start = input.selectionStart.coerceAtLeast(0)
                            input.text.insert(start, text)
                        }
                    }
                }
            }
            parent.addView(pasteBtn, 1)

            input.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    if (isPrefixEnabled && customPrefix.isNotEmpty() && s != null && s.toString().trim().isEmpty() && !isSpamming) {
                        input.postDelayed({
                            if (input.text.toString().trim().isEmpty()) {
                                input.setText("$customPrefix ")
                                input.setSelection(input.text.length)
                            }
                        }, 150)
                    }
                }
            })
        }

        if (context is android.app.Activity) {
            val rootView = context.window.decorView as ViewGroup
            
            val spamBtn = ImageButton(context).apply {
                setImageResource(android.R.drawable.ic_menu_sort_by_size)
                setBackgroundColor(android.graphics.Color.parseColor("#55FF5555"))
                setPadding(25, 25, 25, 25)
            }
            
            val params = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT)
            params.gravity = Gravity.TOP or Gravity.START
            params.leftMargin = 200
            params.topMargin = 400
            
            rootView.addView(spamBtn, params)

            var initialX = 0
            var initialY = 0
            var initialTouchX = 0f
            var initialTouchY = 0f
            var isDragging = false

            spamBtn.setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.leftMargin
                        initialY = params.topMargin
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        isDragging = false
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (event.rawX - initialTouchX).toInt()
                        val dy = (event.rawY - initialTouchY).toInt()
                        if (Math.abs(dx) > 5 || Math.abs(dy) > 5) isDragging = true
                        params.leftMargin = initialX + dx
                        params.topMargin = initialY + dy
                        v.layoutParams = params
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (!isDragging) {
                            val input = chatInputRef
                            if (input != null) {
                                showSpamMenu(context, input)
                            }
                        }
                        true
                    }
                    else -> false
                }
            }
        }
    }

    override fun stop(context: Context) {
        isSpamming = false
        spamThread?.interrupt()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun showSpamMenu(context: Context, chatInput: EditText) {
        val dialog = AlertDialog.Builder(context).create()
        dialog.setTitle("Cấu hình Auto Spam")

        val rootLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(30, 30, 30, 30)
        }

        val dragBar = TextView(context).apply {
            text = "≡ Kéo để di chuyển ≡"
            gravity = Gravity.CENTER
            setBackgroundColor(android.graphics.Color.LTGRAY)
            setPadding(0, 25, 0, 25)
        }
        
        val scrollView = ScrollView(context)
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 20, 0, 0)
        }
        scrollView.addView(layout)

        val delayInput = EditText(context).apply {
            hint = "1. Delay (ms)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }
        layout.addView(delayInput)

        val tagLayout = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
        val tagCheck = CheckBox(context)
        val tagInput = EditText(context).apply { hint = "2. ID cần tag" }
        tagLayout.addView(tagCheck)
        tagLayout.addView(tagInput)
        layout.addView(tagLayout)

        layout.addView(TextView(context).apply { text = "2.5 Vị trí Tag:" })
        val tagPosGroup = RadioGroup(context).apply { orientation = RadioGroup.HORIZONTAL }
        val tagPosOptions = arrayOf("Đầu", "Giữa", "Cuối", "Random", "Cả 2", "Cả 3")
        for (i in tagPosOptions.indices) {
            val rb = RadioButton(context).apply { text = tagPosOptions[i]; setId(i + 100) }
            tagPosGroup.addView(rb)
        }
        tagPosGroup.check(100)
        layout.addView(tagPosGroup)

        layout.addView(TextView(context).apply { text = "3. Vị trí gửi chữ:" })
        val msgPosGroup = RadioGroup(context)
        val msgPosOptions = arrayOf("Đầu - Cuối", "Cuối - Đầu", "Random", "Full (CopyPaste)")
        for (i in msgPosOptions.indices) {
            val rb = RadioButton(context).apply { text = msgPosOptions[i]; setId(i + 200) }
            msgPosGroup.addView(rb)
        }
        msgPosGroup.check(200)
        layout.addView(msgPosGroup)

        layout.addView(TextView(context).apply { text = "5. Nguồn chữ:" })
        val msgSourceLayout = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
        val fileBtn = Button(context).apply { text = "Nhập file .txt" }
        val manualBtn = Button(context).apply { text = "Nhập tay" }
        msgSourceLayout.addView(fileBtn)
        msgSourceLayout.addView(manualBtn)
        layout.addView(msgSourceLayout)
        
        val msgList = mutableListOf<String>()
        
        fileBtn.setOnClickListener {
            val pathDialog = AlertDialog.Builder(context)
            val pathInput = EditText(context)
            pathInput.setText("/sdcard/spam.txt")
            pathDialog.setView(pathInput)
            pathDialog.setPositiveButton("Đọc") { _, _ ->
                try {
                    val file = java.io.File(pathInput.text.toString())
                    if (file.exists()) {
                        msgList.clear()
                        msgList.addAll(file.readLines().filter { it.isNotBlank() })
                        Toast.makeText(context, "Đã tải ${msgList.size} dòng", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {}
            }
            pathDialog.show()
        }
        
        manualBtn.setOnClickListener {
            val inputDialog = AlertDialog.Builder(context)
            val editText = EditText(context)
            editText.setMinLines(5)
            inputDialog.setView(editText)
            inputDialog.setPositiveButton("Lưu") { _, _ ->
                msgList.clear()
                msgList.addAll(editText.text.toString().split("\n").filter { it.isNotBlank() })
            }
            inputDialog.show()
        }

        val loopCheck = CheckBox(context).apply { text = "Loop (Lặp lại khi hết)" }
        layout.addView(loopCheck)

        val prefixLayout = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
        val prefixBtn = Button(context).apply { text = "Nhập Prefix" }
        val prefixCheck = CheckBox(context).apply { text = "Bật" }
        prefixLayout.addView(prefixBtn)
        prefixLayout.addView(prefixCheck)
        layout.addView(prefixLayout)

        prefixBtn.setOnClickListener {
            val inputDialog = AlertDialog.Builder(context)
            val editText = EditText(context)
            editText.setText(customPrefix)
            inputDialog.setView(editText)
            inputDialog.setPositiveButton("Lưu") { _, _ ->
                customPrefix = editText.text.toString()
            }
            inputDialog.show()
        }

        val btnLayout = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
        val startBtn = Button(context).apply { text = "START" }
        val stopBtn = Button(context).apply { text = "STOP" }
        btnLayout.addView(startBtn)
        btnLayout.addView(stopBtn)
        layout.addView(btnLayout)

        rootLayout.addView(dragBar)
        rootLayout.addView(scrollView)

        var initialX = 0; var initialY = 0; var initialTouchX = 0f; var initialTouchY = 0f
        dragBar.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = dialog.window?.attributes?.x ?: 0
                    initialY = dialog.window?.attributes?.y ?: 0
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val params = dialog.window?.attributes
                    params?.x = initialX + (event.rawX - initialTouchX).toInt()
                    params?.y = initialY + (event.rawY - initialTouchY).toInt()
                    dialog.window?.attributes = params
                    true
                }
                else -> false
            }
        }

        startBtn.setOnClickListener {
            if (isSpamming) return@setOnClickListener
            if (msgList.isEmpty()) return@setOnClickListener

            val delay = delayInput.text.toString().toLongOrNull() ?: 1000L
            val doTag = tagCheck.isChecked
            val tagId = tagInput.text.toString().trim()
            val tagPos = tagPosGroup.checkedRadioButtonId - 100
            val msgPos = msgPosGroup.checkedRadioButtonId - 200
            val isLoop = loopCheck.isChecked
            isPrefixEnabled = prefixCheck.isChecked

            isSpamming = true
            spamThread = thread {
                var index = 0
                while (isSpamming) {
                    var msg = if (isLoop) {
                        if (index >= msgList.size) index = 0
                        msgList[index++]
                    } else {
                        if (index >= msgList.size) break
                        msgList[index++]
                    }

                    msg = when (msgPos) {
                        0 -> "$msg\n"
                        1 -> "\n$msg"
                        2 -> if (Random.nextBoolean()) "$msg\n" else "\n$msg"
                        3 -> msg
                        else -> msg
                    }

                    if (doTag && tagId.isNotEmpty()) {
                        val mentionId = if (tagId.startsWith("<@")) tagId else "<@$tagId>"
                        val prefixesToCheck = arrayOf("> # ", "> ", "# ")
                        var prefixFound = ""
                        var contentWithoutPrefix = msg

                        for (p in prefixesToCheck) {
                            if (msg.startsWith(p)) {
                                prefixFound = p
                                contentWithoutPrefix = msg.substring(p.length)
                                break
                            }
                        }

                        msg = if (prefixFound.isNotEmpty()) {
                            "$prefixFound$mentionId $contentWithoutPrefix"
                        } else {
                            when (tagPos) {
                                0 -> "$mentionId $msg"
                                1 -> {
                                    val lines = msg.split("\n")
                                    if (lines.size > 1) {
                                        val mid = lines.size / 2
                                        lines.toMutableList().apply { add(mid, mentionId) }.joinToString("\n")
                                    } else { "$mentionId $msg" }
                                }
                                2 -> "$msg$mentionId"
                                3 -> if (Random.nextBoolean()) "$mentionId $msg" else "$msg$mentionId"
                                4 -> "$mentionId\n$msg\n$mentionId"
                                5 -> "$mentionId\n$mentionId\n$mentionId\n$msg"
                                else -> msg
                            }
                        }
                    }

                    val finalMsg = msg
                    (context as android.app.Activity).runOnUiThread {
                        chatInput.setText(finalMsg)
                        chatInput.onEditorAction(android.view.inputmethod.EditorInfo.IME_ACTION_SEND)
                    }
                    Thread.sleep(delay)
                }
                isSpamming = false
            }
        }

        stopBtn.setOnClickListener {
            isSpamming = false
            spamThread?.interrupt()
        }

        dialog.setView(rootLayout)
        dialog.show()
        dialog.window?.setLayout(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT)
    }
}
