package sdk.chat.demo.robot.activities

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.text.HtmlCompat
import com.bumptech.glide.Glide
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.MaterialCalendarView
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import sdk.chat.core.session.ChatSDK
import sdk.chat.demo.pre.R
import sdk.chat.demo.robot.api.model.TaskHistory
import sdk.chat.demo.robot.extensions.StoryHelper
import sdk.chat.demo.robot.handlers.DailyTaskHandler
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

class TaskCalendarActivity : BaseActivity(), View.OnClickListener {
    private lateinit var taskProcess: TaskHistory
//    private lateinit var taskPieAdapter: TaskPieAdapter
    private lateinit var vPre: ImageView
    private lateinit var vNext: ImageView
    private lateinit var tvStory: TextView
    private lateinit var tvStoryTitle: TextView
    private lateinit var calendarView: MaterialCalendarView
    private lateinit var selectedDates: List<CalendarDay>
    private var lastView: View? = null
    private var lastDate: CalendarDay? = null
    private var selectDrawable: Drawable? = null


    companion object {
        // 提供静态启动方法（推荐）
        fun start(context: Context) {
            context.startActivity(Intent(context, TaskCalendarActivity::class.java))
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(layout)
        selectDrawable =
            AppCompatResources.getDrawable(this@TaskCalendarActivity, R.drawable.ic_calendar_check)
        findViewById<View>(R.id.back).setOnClickListener(this)
        vPre = findViewById<ImageView>(R.id.pre_chapter)
        vNext = findViewById<ImageView>(R.id.next_chapter)
        calendarView = findViewById<MaterialCalendarView>(R.id.calendarView)
        tvStory = findViewById<TextView>(R.id.story)
        tvStoryTitle = findViewById<TextView>(R.id.title)
        vPre.setOnClickListener(this)
        vNext.setOnClickListener(this)
        tvStoryTitle.setOnClickListener(this)
        calendarView.setOnDateChangedListener { widget, date, selected ->
            setSelectCalendar(widget, date)
        }
        calendarView.setOnMonthChangedListener { widget, date ->
            setSelectCalendar(widget, lastDate)
        }


// 2. 等待视图加载完成
        calendarView.post(Runnable {
            // 3. 查找 topbar 视图
            val topbar = calendarView.findViewById<View?>(R.id.header) // 使用库中定义的ID
            if (topbar != null) {
                val headerParams: ViewGroup.LayoutParams = topbar.layoutParams
                headerParams.width = ViewGroup.LayoutParams.WRAP_CONTENT
                topbar.setLayoutParams(headerParams)


                // 2. 修改 month_name 宽度
                val monthName = findViewById<TextView?>(R.id.month_name)
                val textParams = monthName.layoutParams as LinearLayout.LayoutParams
                textParams.width = ViewGroup.LayoutParams.WRAP_CONTENT
                textParams.weight = 0f // 必须设为0才能使用 WRAP_CONTENT
                monthName.setLayoutParams(textParams)
                topbar.requestLayout()
            }
        })


        loadData()
    }

    override fun getLayout(): Int {
        return R.layout.activity_task_calendar
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onDestroy() {
        super.onDestroy()
        Glide.get(this).clearMemory()
    }

    override fun onClick(v: View?) {
        when (v?.id) {

            R.id.back -> {
                finish()
            }

            R.id.pre_chapter -> {
                if (lastDate != null) {
                    var index = selectedDates.indexOfFirst { it == lastDate } - 1
                    if (index >= 0) {
                        setSelectCalendar(calendarView, selectedDates[index], true)
                    }
                }
            }

            R.id.next_chapter -> {
                if (lastDate != null) {
                    var index = selectedDates.indexOfFirst { it == lastDate } + 1
                    if (index < selectedDates.size) {
                        setSelectCalendar(calendarView, selectedDates[index], true)
                    }
                }
            }
        }
    }

    override fun onError(e: Throwable) {
        if (ChatSDK.config().debug) {
            e.printStackTrace()
        }
        alert.onError(e)
    }

    private fun loadData() {
        dm.add(
            DailyTaskHandler.getTaskHistory()
                .subscribeOn(Schedulers.io()) // Specify database operations on IO thread
                .observeOn(AndroidSchedulers.mainThread()) // Results return to main thread
                .subscribe(
                    { data ->
                        if (data != null) {
//                            var fake = Chapter()
//                            fake.content = "fakestory"
//                            fake.date = "2025-05-07"
//                            fake.title = "faketitle"
//                            data.history.add(0, fake)
                            taskProcess = data
                            setCalendar()
                        } else {
                            throw IllegalArgumentException(getString(R.string.failed_and_retry))
                        }
                    },
                    this
                )
        )
    }

    private fun setCalendar() {
        selectedDates = taskProcess.history.map { c -> c.calendarDay }

        for (d in selectedDates) {
            calendarView.setDateSelected(d, true)
        }

        var lastTaskDate = selectedDates[selectedDates.size - 1]
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            var d = LocalDate.of(lastTaskDate.year, lastTaskDate.month, lastTaskDate.day)
                .with(TemporalAdjusters.lastDayOfMonth())
            lastTaskDate = CalendarDay.from(d.year, d.monthValue, d.dayOfMonth)
        }

        calendarView.state().edit()
            .setMinimumDate(CalendarDay.from(selectedDates[0].year, selectedDates[0].month, 1))
            .setMaximumDate(lastTaskDate)
            .commit()

        calendarView.postDelayed({
            setSelectCalendar(calendarView, selectedDates[selectedDates.size - 1])
        }, 500)
    }

    private fun setSelectCalendar(
        widget: MaterialCalendarView,
        date: CalendarDay?,
        force: Boolean = false
    ) {
        if (date == null) {
            return
        }
        var index = selectedDates.indexOfFirst { it == date }
        if (index < 0) {
            widget.setDateSelected(date, false)
            return
        }
        if (index == 0) {
            vPre.setImageResource(R.mipmap.ic_pre_chapter_gray)
        } else {
            vPre.setImageResource(R.mipmap.ic_pre_chapter)
        }
        if (index == selectedDates.size - 1) {
            vNext.setImageResource(R.mipmap.ic_next_chapter_gray)
        } else {
            vNext.setImageResource(R.mipmap.ic_next_chapter)
        }
        val outViews = ArrayList<View>()
        if (force) {
            widget.setCurrentDate(date)
        }
        widget.findViewsWithText(outViews, date.day.toString(), View.FIND_VIEWS_WITH_TEXT)
        for (v in outViews) {
            var clxName = v::class.simpleName
            if ("DayView" == clxName) {
                var d: CalendarDay? = StoryHelper.getCalendarDate(v)
                if (d == date) {
                    if (lastView != null) {
                        StoryHelper.setCustomBackground(lastView!!, null)
                        var od: CalendarDay? = StoryHelper.getCalendarDate(lastView!!)
                        widget.setDateSelected(od, true)
                    }
                    widget.setDateSelected(date, false)
                    lastView = v
                    lastDate = d
                    (v as TextView).setTextColor(getColor(R.color.white))
                    StoryHelper.setCustomBackground(v, selectDrawable)
                    setChapterByDate(d)
                    break
                }
            }
        }
    }


    private fun setChapterByDate(d: CalendarDay) {
        var chapter = taskProcess.history.firstOrNull { it.calendarDay == d }
        if (chapter != null) {
            tvStoryTitle.text = chapter.title
            tvStory.text = HtmlCompat.fromHtml(chapter.content, HtmlCompat.FROM_HTML_MODE_LEGACY);
        }
    }


}