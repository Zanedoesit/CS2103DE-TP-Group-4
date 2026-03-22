# CS2103DE-TP-Group-4

Travel Planner is a Java desktop application for managing trips, activities, and expenses locally.

## What’s Implemented So Far (V1.2 Core Domain)

The following core domain logic is implemented:
- `Trip`, `Activity`, and `Expense` entities with time intervals and cost tracking.
- Overlap detection for trips and activities.
- CRUD-style lookups and deletes (by id or name).
- Aggregated cost calculation by currency.
- `TripManager` for managing a collection of trips.
- Exceptions organized under `exceptions`.

## Step-by-Step Demo Explanation

1. Create a Trip
```java
Trip trip = new Trip(1, "Osaka Weekend",
        LocalDateTime.of(2026, 4, 3, 9, 0),
        LocalDateTime.of(2026, 4, 5, 18, 0));
```
This creates a trip from April 3 to April 5. The trip itself is time-bounded, and it will be used to hold activities.

2. Create an Activity
```java
Activity museum = new Activity(10, "Museum",
        LocalDateTime.of(2026, 4, 4, 10, 0),
        LocalDateTime.of(2026, 4, 4, 12, 0));
```
This activity is on April 4, 10am–12pm.

3. Add an Expense to the Activity
```java
museum.addExpense(new Expense(100, "Ticket", 25.0f,
        Expense.Currency.USD, Expense.Type.ENTERTAINMENT));
```
This adds a $25 USD expense to the activity.

4. Add the Activity to the Trip
```java
trip.addActivity(museum);
```
This goes through the trip’s conflict-checking logic and accepts the activity.

5. Create an Overlapping Activity
```java
Activity overlap = new Activity(11, "Brunch",
        LocalDateTime.of(2026, 4, 4, 11, 30),
        LocalDateTime.of(2026, 4, 4, 12, 30));
```
This overlaps with the museum activity (11:30–12:00 overlap).

6. Try to Add the Overlap
```java
trip.addActivity(overlap);
```
This triggers `TimeIntervalConflictException`, and the code prints:
`Overlap detected`

7. Print Total Cost
```java
System.out.println("Total USD: " + trip.getTotalCost(Expense.Currency.USD));
```
This totals all expenses in USD (including activity expenses), so output is:
`Total USD: 25.0`

Expected output:
1. `Overlap detected`
2. `Total USD: 25.0`
