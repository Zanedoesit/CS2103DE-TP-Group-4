# ✈️ Trip Planner — API Reference

> *Everything you need to build, manage, and traverse trips, activities, and expenses — all in one place.*

---

## 📚 Table of Contents

- [🚨 Exceptions](#-exceptions)
- [🔌 Interfaces](#-interfaces)
  - [TimeInterval](#timeinterval)
  - [ExpenseManagable](#expensemanagable)
  - [Copyable\<T\>](#copyablet)
- [📦 Classes](#-classes)
  - [Application](#application)
  - [BaseEntity](#baseentity)
  - [Expense](#expense)
  - [Location](#location)
  - [Activity](#activity)
  - [Trip](#trip)
  - [TripManager](#tripmanager)
  - [JsonStorage](#jsonstorage)

---

## 🚨 Exceptions

These checked exceptions are thrown throughout the API. Handle them gracefully — your users will thank you.

| Exception | When it's thrown |
|---|---|
| `TripNotFoundException` | A trip lookup by ID or name came up empty. |
| `ActivityNotFoundException` | An activity couldn't be found within a trip. |
| `ExpenseNotFoundException` | An expense lookup by ID or name failed. |
| `TimeIntervalConflictException` | A new trip or activity overlaps an existing time slot. |

---

## 🔌 Interfaces

### TimeInterval

> *Implemented by `Activity`, `Trip`*

Any entity that occupies a window of time. Provides start/end accessors, overlap detection, and duration helpers.

| Method | Returns | Description |
|---|---|---|
| `getStartDateTime()` | `LocalDateTime` | Returns the start of the interval. |
| `setStartDateTime()` | `void` | Sets the start of the interval. |
| `getEndDateTime()` | `LocalDateTime` | Returns the end of the interval. |
| `setEndDateTime()` | `void` | Sets the end of the interval. |
| `overlapsWith(TimeInterval other)` | `Boolean` | Returns `true` if this interval overlaps with `other`. |
| `getDuration()` | `Duration` | Time-based length of the interval. |
| `getPeriod()` | `Period` | Date-based length of the interval. |

---

### ExpenseManagable

> *Implemented by `Activity`, `Trip`*

Adds full CRUD expense management and cost aggregation to any entity. Whether it's a quick coffee or a transatlantic flight, this interface tracks it all.

| Method | Returns | Throws | Description |
|---|---|---|---|
| `addExpense(Expense expense)` | `void` | — | Adds an expense to this entity. |
| `deleteExpenseById(int id)` | `void` | `ExpenseNotFoundException` | Removes an expense by ID. |
| `deleteExpenseByName(String name)` | `void` | `ExpenseNotFoundException` | Removes an expense by name. |
| `getExpenseById(int id)` | `Expense` | `ExpenseNotFoundException` | Retrieves an expense by ID. |
| `getExpenseByName(String name)` | `Expense` | `ExpenseNotFoundException` | Retrieves an expense by name. |
| `getTotalCost(Expense.Currency currency)` | `float` | — | Sums all expenses in the given currency. |

---

### Copyable\<T\>

> *Implemented by `Expense`, `Activity`, `Trip`*

A tidy generic interface for deep-copying an object. One method, zero ambiguity.

| Method | Returns | Description |
|---|---|---|
| `copy()` | `T` | Returns a deep copy of the implementing object. |

---

## 📦 Classes

### Application

> *Top-level entry point*

The root of everything. Owns the UI, the trip collection, and the persistence layer.

| Field | Type | Description |
|---|---|---|
| `ui` | `UI` | The user-facing interface. |
| `tripManager` | `TripManager` | Manages the full collection of trips. |
| `jsonStorage` | `JsonStorage` | Handles reading and writing trip data. |

---

### BaseEntity

> *`abstract` — Extended by `Expense`, `Location`, `Activity`, `Trip`*

The shared DNA of every named entity in the system. All fields come with getters and setters.

| Field | Type | Description |
|---|---|---|
| `id` | `int` | Unique numeric identifier. |
| `name` | `String` | Display name. |
| `description` | `String` | Optional human-readable description. |
| `image` | `BufferedImage` | Optional image representation. |

---

### Expense

> *extends `BaseEntity` · implements `Copyable<Expense>`*

A single cost line item — categorised by type, denominated in a supported currency.

**`Expense.Type`**

| Value | Description |
|---|---|
| `FOOD` | Food and dining. |
| `ACCOMMODATION` | Lodging costs. |
| `TRANSPORTATION` | Travel and transport. |
| `ENTERTAINMENT` | Events and activities. |
| `OTHER` | Miscellaneous. |

**`Expense.Currency`**

`USD` · `EUR` · `GBP` · `JPY` · `CNY` · `...`

**Fields**

| Field | Type | Description |
|---|---|---|
| `cost` | `float` | The monetary amount of the expense. |

---

### Location

> *extends `BaseEntity`*

A physical place on the map. From a beachside café to a mountain summit — if it has coordinates, it belongs here.

**Fields**

| Field | Type | Description |
|---|---|---|
| `address` | `String` | Street address. |
| `city` | `String` | City name. |
| `country` | `String` | Country name. |
| `latitude` | `double` | Geographic latitude. |
| `longitude` | `double` | Geographic longitude. |

**Methods**

| Method | Returns | Description |
|---|---|---|
| `distanceTo(Location other)` | `double` | Calculates the distance to another location. |

---

### Activity

> *extends `BaseEntity` · implements `TimeInterval`, `ExpenseManagable`, `Copyable<Activity>`*

A single thing to do — a snorkelling excursion, a museum visit, a long lunch. Activities belong to trips and carry their own expenses and location.

**`Activity.Type`**

`SIGHTSEEING` · `ADVENTURE` · `RELAXATION` · `CULTURAL` · `OTHER` · `...`

**Fields**

| Field | Type | Description |
|---|---|---|
| `types` | `List<Type>` | One or more activity categories. |
| `expenses` | `List<Expense>` | Expenses tied to this activity. |
| `location` | `Location` | Where the activity takes place. |

> 📎 Inherits all methods from `TimeInterval`, `ExpenseManagable`, and `Copyable<Activity>`.

---

### Trip

> *extends `BaseEntity` · implements `TimeInterval`, `ExpenseManagable`, `Copyable<Trip>`*

The main event. A trip is a time-bounded journey with a destination, a set of activities, and its own expense ledger.

**Fields**

| Field | Type | Description |
|---|---|---|
| `activities` | `List<Activity>` | All activities on this trip. |
| `expenses` | `List<Expense>` | Trip-level expenses (flights, accommodation, etc.). |
| `location` | `Location` | Primary destination. |

**Methods**

| Method | Returns | Throws | Description |
|---|---|---|---|
| `addActivity(Activity activity)` | `void` | `TimeIntervalConflictException` | Adds an activity, checking for time conflicts. |
| `deleteActivityById(int id)` | `void` | `ActivityNotFoundException` | Removes an activity by ID. |
| `deleteActivityByName(String name)` | `void` | `ActivityNotFoundException` | Removes an activity by name. |
| `getOverlappingActivities()` | `List<Activity>` | — | Returns all activities with time conflicts. |
| `getOverlappingActivities(LocalDateTime begin, LocalDateTime end)` | `List<Activity>` | — | Returns activities overlapping a specific window. |

> 📎 Inherits all methods from `TimeInterval`, `ExpenseManagable`, and `Copyable<Trip>`.

---

### TripManager

> *The keeper of all trips*

A registry for every trip in the application. Add, remove, retrieve, and detect conflicts — your single source of truth for the trip catalogue.

**Fields**

| Field | Type | Description |
|---|---|---|
| `trips` | `List<Trip>` | All registered trips. |

**Methods**

| Method | Returns | Throws | Description |
|---|---|---|---|
| `addTrip(Trip trip)` | `void` | `TimeIntervalConflictException` | Adds a trip, checking for overlaps with existing trips. |
| `deleteTripById(int id)` | `void` | `TripNotFoundException` | Removes a trip by ID. |
| `deleteTripByName(String name)` | `void` | `TripNotFoundException` | Removes a trip by name. |
| `getTripById(int id)` | `Trip` | `TripNotFoundException` | Retrieves a trip by ID. |
| `getTripByName(String name)` | `Trip` | `TripNotFoundException` | Retrieves a trip by name. |
| `getOverlappingTrips()` | `List<Trip>` | — | Returns all trips with time conflicts. |
| `getOverlappingTrips(LocalDateTime begin, LocalDateTime end)` | `List<Trip>` | — | Returns trips overlapping a given window. |

---

### JsonStorage

> *Persistence layer — no database required*

Serialises and deserialises the trip catalogue to disk. Just a clean JSON file at a configurable path.

**Fields**

| Field | Type | Description |
|---|---|---|
| `filePath` | `String` | Path to the JSON storage file. |

**Methods**

| Method | Returns | Description |
|---|---|---|
| `saveTrips(List<Trip> trips)` | `void` | Serialises and writes all trips to disk. |
| `loadTrips()` | `List<Trip>` | Reads and deserialises trips from disk. |

---

*Generated from `API.java` · All fields carry getters & setters unless noted.*
