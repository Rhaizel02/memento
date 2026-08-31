package com.memento.app.ui.calendar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.memento.app.R
import com.memento.app.domain.calendar.CulturalCalendarDay
import com.memento.app.domain.calendar.CulturalCalendarEngine
import com.memento.app.domain.calendar.CulturalCalendarMonth
import com.memento.app.domain.calendar.CulturalCalendarMonthIntensity
import com.memento.app.domain.calendar.CulturalCalendarYear
import com.memento.app.domain.model.MediaType
import com.memento.app.ui.theme.MementoSpacing
import com.memento.app.ui.theme.mediaTypeColor
import com.memento.app.ui.timeline.TimelineEventRow
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun CulturalCalendarScreen(
    state: CulturalCalendarUiState,
    onBack: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onShowYear: () -> Unit,
    onShowMonth: (YearMonth) -> Unit,
    onSelectDay: (LocalDate) -> Unit,
    onToday: () -> Unit,
    onOpenMedia: (String) -> Unit,
    onRetry: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.cultural_calendar_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = onToday) {
                        Icon(Icons.Outlined.Today, contentDescription = stringResource(R.string.calendar_go_today))
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = MementoSpacing.huge),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item("content") {
                Column(
                    modifier = Modifier.widthIn(max = 760.dp).fillMaxWidth().padding(horizontal = MementoSpacing.normal),
                    verticalArrangement = Arrangement.spacedBy(MementoSpacing.normal),
                ) {
                    CalendarPeriodHeader(
                        state = state,
                        onPrevious = onPrevious,
                        onNext = onNext,
                        onShowYear = onShowYear,
                    )
                    if (state.isLoading) LinearProgressIndicator(Modifier.fillMaxWidth())
                    if (state.isError) {
                        InlineCalendarError(onRetry)
                    } else if (state.viewMode == CalendarViewMode.MONTH) {
                        MonthContent(state, onSelectDay, onOpenMedia)
                    } else {
                        YearContent(requireNotNull(state.year), onShowMonth)
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarPeriodHeader(
    state: CulturalCalendarUiState,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onShowYear: () -> Unit,
) {
    val locale = currentLocale()
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.Outlined.ChevronLeft, contentDescription = stringResource(R.string.calendar_previous_period))
        }
        Row(
            modifier = Modifier.weight(1f).clickable(role = Role.Button, onClick = onShowYear)
                .padding(horizontal = MementoSpacing.small, vertical = MementoSpacing.medium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                if (state.viewMode == CalendarViewMode.MONTH) monthTitle(state.selectedMonth, locale)
                else state.selectedMonth.year.toString(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
            if (state.viewMode == CalendarViewMode.MONTH) {
                Icon(Icons.Outlined.ArrowDropDown, contentDescription = stringResource(R.string.calendar_choose_month_year))
            }
        }
        IconButton(onClick = onNext) {
            Icon(Icons.Outlined.ChevronRight, contentDescription = stringResource(R.string.calendar_next_period))
        }
    }
}

@Composable
private fun MonthContent(
    state: CulturalCalendarUiState,
    onSelectDay: (LocalDate) -> Unit,
    onOpenMedia: (String) -> Unit,
) {
    MonthGrid(
        month = state.month,
        today = state.today,
        selectedDate = state.selectedDate,
        onSelectDay = onSelectDay,
    )
    when {
        state.selectedDate != null -> SelectedDay(
            date = state.selectedDate,
            events = state.selectedEvents,
            onOpenMedia = onOpenMedia,
        )
        !state.isLoading && state.month.eventCount == 0 -> Text(
            stringResource(R.string.calendar_empty_month),
            modifier = Modifier.fillMaxWidth().padding(vertical = MementoSpacing.large),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        else -> Text(
            pluralStringResource(
                R.plurals.calendar_month_memory_summary,
                state.month.activeDayCount,
                state.month.activeDayCount,
                state.month.eventCount,
            ),
            modifier = Modifier.fillMaxWidth().padding(vertical = MementoSpacing.normal),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun MonthGrid(
    month: CulturalCalendarMonth,
    today: LocalDate,
    selectedDate: LocalDate?,
    onSelectDay: (LocalDate) -> Unit,
) {
    val locale = currentLocale()
    Row(Modifier.fillMaxWidth()) {
        DayOfWeek.entries.forEach { dayOfWeek ->
            Text(
                dayOfWeek.getDisplayName(TextStyle.NARROW, locale).uppercase(locale),
                modifier = Modifier.weight(1f).padding(vertical = MementoSpacing.small)
                    .clearAndSetSemantics { contentDescription = dayOfWeek.getDisplayName(TextStyle.FULL, locale) },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
    CulturalCalendarEngine.calendarCells(month.month).chunked(7).forEach { week ->
        Row(Modifier.fillMaxWidth()) {
            week.forEach { date ->
                if (date == null) {
                    Box(Modifier.weight(1f).defaultMinSize(minHeight = 56.dp))
                } else {
                    CalendarDayCell(
                        date = date,
                        day = month.days[date],
                        isToday = date == today,
                        isSelected = date == selectedDate,
                        onClick = { onSelectDay(date) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    date: LocalDate,
    day: CulturalCalendarDay?,
    isToday: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val description = calendarDayDescription(date, day, isToday)
    val border = when {
        isSelected -> BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        isToday -> BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary)
        else -> null
    }
    Surface(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = 56.dp).padding(2.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = description
                role = Role.Button
                selected = isSelected
            },
        shape = MaterialTheme.shapes.medium,
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        border = border,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 2.dp, vertical = MementoSpacing.xSmall),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
            )
            if (day != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    day.indicatorTypes.forEach { type ->
                        Box(
                            Modifier.size(7.dp).background(MaterialTheme.mediaTypeColor(type), CircleShape)
                                .clearAndSetSemantics { },
                        )
                    }
                    if (day.hiddenEventCount > 0) {
                        Text(
                            "+${day.hiddenEventCount}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectedDay(
    date: LocalDate,
    events: List<com.memento.app.domain.model.CulturalTimelineEvent>,
    onOpenMedia: (String) -> Unit,
) {
    val locale = currentLocale()
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = MementoSpacing.medium),
        verticalArrangement = Arrangement.spacedBy(MementoSpacing.xSmall),
    ) {
        Text(
            date.format(DateTimeFormatter.ofPattern(stringResource(R.string.calendar_selected_date_pattern), locale))
                .replaceFirstChar { it.titlecase(locale) },
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        if (events.isEmpty()) {
            Text(
                stringResource(R.string.calendar_empty_day),
                modifier = Modifier.padding(vertical = MementoSpacing.normal),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            events.forEach { event ->
                TimelineEventRow(
                    event = event,
                    onClick = { onOpenMedia(event.mediaItemId) },
                    modifier = Modifier.padding(vertical = MementoSpacing.xSmall),
                    reflectionMaxLines = 3,
                )
            }
        }
    }
}

@Composable
private fun YearContent(year: CulturalCalendarYear, onShowMonth: (YearMonth) -> Unit) {
    val maximum = year.months.maxOfOrNull(CulturalCalendarMonthIntensity::eventCount)?.coerceAtLeast(1) ?: 1
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val columns = if (maxWidth >= 600.dp) 4 else 3
        Column(verticalArrangement = Arrangement.spacedBy(MementoSpacing.medium)) {
            year.months.chunked(columns).forEach { rowMonths ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MementoSpacing.medium)) {
                    rowMonths.forEach { month ->
                        YearMonthCell(month, maximum, { onShowMonth(month.month) }, Modifier.weight(1f))
                    }
                    repeat(columns - rowMonths.size) { Box(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun YearMonthCell(
    month: CulturalCalendarMonthIntensity,
    maximum: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val locale = currentLocale()
    Surface(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = 112.dp),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            Modifier.padding(MementoSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(MementoSpacing.small),
        ) {
            Text(
                month.month.month.getDisplayName(TextStyle.SHORT, locale).uppercase(locale),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            LinearProgressIndicator(
                progress = { month.eventCount.toFloat() / maximum },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                pluralStringResource(R.plurals.calendar_events, month.eventCount, month.eventCount),
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                pluralStringResource(R.plurals.calendar_active_days, month.activeDayCount, month.activeDayCount),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun InlineCalendarError(onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = MementoSpacing.huge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MementoSpacing.normal),
    ) {
        Text(stringResource(R.string.calendar_error), textAlign = TextAlign.Center)
        Button(onClick = onRetry) { Text(stringResource(R.string.retry)) }
    }
}

@Composable
private fun calendarDayDescription(date: LocalDate, day: CulturalCalendarDay?, isToday: Boolean): String {
    val locale = currentLocale()
    val formattedDate = date.format(
        DateTimeFormatter.ofPattern(stringResource(R.string.calendar_accessibility_date_pattern), locale),
    )
    val dateText = if (isToday) stringResource(R.string.calendar_today_description, formattedDate) else formattedDate
    if (day == null) return stringResource(R.string.calendar_day_without_events, dateText)
    val details = mutableListOf<String>()
    day.mediaCounts[MediaType.BOOK]?.let {
        details += pluralStringResource(R.plurals.calendar_books, it, it)
    }
    day.mediaCounts[MediaType.MOVIE]?.let {
        details += pluralStringResource(R.plurals.calendar_movies, it, it)
    }
    day.mediaCounts[MediaType.SERIES]?.let {
        details += pluralStringResource(R.plurals.calendar_series, it, it)
    }
    day.mediaCounts[MediaType.GAME]?.let {
        details += pluralStringResource(R.plurals.calendar_games, it, it)
    }
    if (day.reflectionCount > 0) {
        details += pluralStringResource(R.plurals.calendar_reflections, day.reflectionCount, day.reflectionCount)
    }
    return stringResource(
        R.string.calendar_day_with_events,
        dateText,
        pluralStringResource(R.plurals.calendar_events, day.eventCount, day.eventCount),
        details.joinToString(", "),
    )
}

@Composable
private fun currentLocale(): Locale = Locale.forLanguageTag(LocalLocale.current.toLanguageTag())

private fun monthTitle(month: YearMonth, locale: Locale): String = month.format(
    DateTimeFormatter.ofPattern("MMMM yyyy", locale),
).replaceFirstChar { it.titlecase(locale) }
