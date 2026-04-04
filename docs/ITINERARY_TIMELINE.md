# Itinerary Timeline UI

## Overview
This document describes the itinerary timeline implemented on the Trip page.

The goal is to make trip schedules easier to scan by time, not only by list order.  
Each trip day now has a visual timeline where activities are placed by start/end time, with overlap activities highlighted.

## Where It Appears
- File: `src/main/resources/view/TripPage.fxml`
- Controller: `src/main/java/ui/TripPage.java`
- Screen: Trip details page (opened by double-clicking a trip from the home page)

## What Users See
- A new **Itinerary Timeline** section above the activity/expense lists.
- One timeline card per day from trip start date to trip end date.
- A horizontal 24-hour ruler (`00:00`, `06:00`, `12:00`, `18:00`, `24:00`).
- Activity blocks positioned according to their time window.
- Red blocks for activities that overlap with at least one other activity.
- Blue blocks for non-overlapping activities.
- Tooltip on each block with activity name and time range.
- Empty-day state message: `No activities planned`.

## Rendering Logic
1. The controller sorts activities by `startDateTime`.
2. Overlap detection checks every activity pair using `overlapsWith(...)`.
3. For each trip day, activities are clipped to that day window (`00:00` to `24:00`).
4. Clipped segments are converted into minute ranges (`startMinute`, `endMinute`).
5. Segment lanes are assigned so overlapping segments are drawn on different rows.
6. Segment position and width are calculated from minutes:
   - `x = startMinute / 1440 * DAY_TIMELINE_WIDTH`
   - `width = durationMinutes / 1440 * DAY_TIMELINE_WIDTH`

## Data and Persistence Impact
- No data model changes were required.
- Timeline is computed from existing `Trip` and `Activity` data only.
- Existing JSON persistence remains unchanged.

## Manual Test Checklist
1. Open a trip with multiple activities.
2. Confirm activity blocks appear in chronological order.
3. Add an activity in the morning and one in the evening; verify their block positions differ.
4. Add an activity that overlaps an existing one; verify both related blocks are red.
5. Add activity spanning midnight; verify it is split across two day cards.
6. Restart app and reopen trip; verify timeline still renders from saved data.

## Known Limitations
- Timeline width is fixed for now (`DAY_TIMELINE_WIDTH = 240.0`) to fit current window size.
- The timeline is currently view-only (drag-resize editing is not implemented).
- Hour labels are coarse (every 6 hours) to keep the UI compact.
