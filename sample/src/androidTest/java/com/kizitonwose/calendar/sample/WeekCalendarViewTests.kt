package com.kizitonwose.calendar.sample

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.kizitonwose.calendar.core.Week
import com.kizitonwose.calendar.core.WeekDay
import com.kizitonwose.calendar.sample.view.CalendarViewActivity
import com.kizitonwose.calendar.view.ViewContainer
import com.kizitonwose.calendar.view.WeekCalendarView
import com.kizitonwose.calendar.view.WeekDayBinder
import com.kizitonwose.calendar.view.WeekHeaderFooterBinder
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.lang.Thread.sleep
import java.time.YearMonth

/**
 * These are UI behaviour tests.
 * The core logic tests are in the data project.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class WeekCalendarViewTests {

    @get:Rule
    val homeScreenRule = ActivityScenarioRule(CalendarViewActivity::class.java)

    private val currentMonth = YearMonth.now()

    @Test
    fun dayBinderIsCalledOnDayChanged() {
        val calendarView = getWeekCalendarView()

        class DayViewContainer(view: View) : ViewContainer(view)

        var boundDay: WeekDay? = null

        val changedDate = currentMonth.atDay(4)

        homeScreenRule.scenario.onActivity {
            calendarView.scrollToDate(changedDate)
            calendarView.dayBinder = object : WeekDayBinder<DayViewContainer> {
                override fun create(view: View) = DayViewContainer(view)
                override fun bind(container: DayViewContainer, data: WeekDay) {
                    boundDay = data
                }
            }
        }

        // Allow the calendar to be rebuilt due to dayBinder change.
        sleep(2000)

        homeScreenRule.scenario.onActivity {
            calendarView.notifyDateChanged(changedDate)
        }

        // Allow time for date change event to be propagated.
        sleep(2000)

        assertEquals(changedDate, boundDay?.date)
    }

    @Test
    fun allBindersAreCalledOnWeekChanged() {
        val calendarView = getWeekCalendarView()

        val boundDays = mutableSetOf<WeekDay>()
        var boundHeaderWeek: Week? = null

        homeScreenRule.scenario.onActivity {
            calendarView.dayBinder = object : WeekDayBinder<DayViewContainer> {
                override fun create(view: View) = DayViewContainer(view)
                override fun bind(container: DayViewContainer, data: WeekDay) {
                    boundDays.add(data)
                }
            }
            calendarView.weekHeaderResource = R.layout.example_3_calendar_header
            calendarView.weekHeaderBinder = object : WeekHeaderFooterBinder<DayViewContainer> {
                override fun create(view: View) = DayViewContainer(view)
                override fun bind(container: DayViewContainer, data: Week) {
                    boundHeaderWeek = data
                }
            }
        }

        // Allow the calendar to be rebuilt due to binder change.
        sleep(2000)

        val firstDate = calendarView.findFirstVisibleDay()!!.date
        val lastDate = calendarView.findLastVisibleDay()!!.date

        homeScreenRule.scenario.onActivity {
            boundDays.clear()
            boundHeaderWeek = null
            calendarView.notifyWeekChanged(firstDate)
        }

        // Allow time for date change event to be propagated.
        sleep(2000)

        assertEquals(firstDate, boundHeaderWeek?.days?.first()?.date)
        assertEquals(lastDate, boundHeaderWeek?.days?.last()?.date)
        assertTrue(boundDays.map { it.date }.contains(firstDate))
        assertTrue(boundDays.map { it.date }.contains(lastDate))
    }

    @Test
    fun programmaticScrollToDateWorksAsExpected() {
        val calendarView = getWeekCalendarView()

        val dateInFourMonths = currentMonth.plusMonths(4).atDay(1)

        homeScreenRule.scenario.onActivity {
            calendarView.scrollToDate(dateInFourMonths)
        }

        sleep(2000)

        assertNotNull(calendarView.findViewWithTag(dateInFourMonths.hashCode()))
    }

    @Test
    fun programmaticScrollToWeekWorksAsExpected() {
        val calendarView = getWeekCalendarView()

        val dateInFourMonths = currentMonth.plusMonths(4).atDay(1)

        homeScreenRule.scenario.onActivity {
            calendarView.scrollToWeek(dateInFourMonths)
        }

        sleep(2000)

        assertNotNull(calendarView.findViewWithTag(dateInFourMonths.hashCode()))
    }

    @Test
    fun weekScrollListenerIsCalledWhenScrolled() {
        val calendarView = getWeekCalendarView()

        var targetWeek: Week? = null
        calendarView.weekScrollListener = { weekDays ->
            targetWeek = weekDays
        }

        val dateInTwoMonths = currentMonth.plusMonths(2).atDay(1)
        homeScreenRule.scenario.onActivity {
            calendarView.smoothScrollToWeek(dateInTwoMonths)
        }
        sleep(3000) // Enough time for smooth scrolling animation.
        assertTrue(targetWeek?.days.orEmpty().map { it.date }.contains(dateInTwoMonths))
    }

    @Test
    fun findVisibleWeekWorksAsExpected() {
        val calendarView = getWeekCalendarView()

        val dateInTwoMonths = currentMonth.plusMonths(2).atDay(1)
        homeScreenRule.scenario.onActivity {
            calendarView.scrollToDate(dateInTwoMonths)
        }

        sleep(2000)

        val firstVisibleWeek = calendarView.findFirstVisibleWeek()?.days.orEmpty().map { it.date }
        assertTrue(firstVisibleWeek.contains(dateInTwoMonths))
    }

    @Test
    fun weekDataUpdateRetainsPosition() {
        val calendarView = getWeekCalendarView()

        val dateInTwoMonths = currentMonth.plusMonths(2).atDay(1)
        val dateInNineMonths = currentMonth.plusMonths(9).atDay(1)

        var targetVisibleDay: WeekDay? = null
        calendarView.weekScrollListener = { week ->
            targetVisibleDay = week.days.first()
        }

        homeScreenRule.scenario.onActivity {
            calendarView.smoothScrollToWeek(dateInTwoMonths)
        }

        sleep(3000) // Enough time for smooth scrolling animation.

        homeScreenRule.scenario.onActivity {
            calendarView.updateWeekData(endDate = dateInNineMonths)
        }

        sleep(2000) // Enough time for UI adjustments.

        assertEquals(targetVisibleDay, calendarView.findFirstVisibleDay())
    }

    private class DayViewContainer(view: View) : ViewContainer(view)

    private fun getWeekCalendarView(): WeekCalendarView {
        onView(withId(R.id.examplesRecyclerview))
            .perform(actionOnItemAtPosition<RecyclerView.ViewHolder>(6, click()))

        lateinit var calendarView: WeekCalendarView
        homeScreenRule.scenario.onActivity { activity ->
            calendarView = activity.findViewById(R.id.exSevenCalendar)
        }
        sleep(1000)
        return calendarView
    }
}
