# API Documentation: Travel Planner

## Overview
This document describes the internal APIs for the Travel Planner application. The system follows a layered architecture with Model-View-Controller (MVC) pattern, separating data management, business logic, and UI components.

## Architecture Summary
```
┌─────────────────┐
│   UI Layer      │  (Views, Controllers)
├─────────────────┤
│ Service Layer   │  (Business Logic)
├─────────────────┤
│  Model Layer    │  (Domain Objects)
├─────────────────┤
│ Storage Layer   │  (JSON Parser, File I/O)
└─────────────────┘
```

---

## 1. Model Layer APIs

### 1.1 Trip Class

**Purpose:** Represents a single trip with destinations and activities.

#### `Trip(String tripId, String name, LocalDate startDate, LocalDate endDate)`
**Description:** Constructor to create a new trip.

**Parameters:**
- `tripId` (String): Unique identifier for the trip
- `name` (String): Trip name/title
- `startDate` (LocalDate): Trip start date
- `endDate` (LocalDate): Trip end date

**Returns:** Trip object

**Example:**
```java
Trip trip = new Trip("T001", "Europe Summer 2024",
    LocalDate.of(2024, 6, 1),
    LocalDate.of(2024, 6, 30));
```

---

#### `addActivity(Activity activity)`
**Description:** Adds an activity to the trip's activity list.

**Parameters:**
- `activity` (Activity): The activity object to add

**Returns:** `boolean` - true if added successfully, false if conflict detected

**Throws:** `ActivityConflictException` if time overlap detected

**Example:**
```java
Activity sightseeing = new Activity("A001", "Eiffel Tower Visit", ...);
boolean success = trip.addActivity(sightseeing);
```

---

#### `removeActivity(String activityId)`
**Description:** Removes an activity from the trip by its ID.

**Parameters:**
- `activityId` (String): Unique identifier of the activity to remove

**Returns:** `boolean` - true if removed, false if not found

**Example:**
```java
boolean removed = trip.removeActivity("A001");
```

---

#### `getActivitiesByCategory(ActivityCategory category)`
**Description:** Filters and returns activities matching a specific category.

**Parameters:**
- `category` (ActivityCategory): Enum value (FOOD, TRANSPORT, SIGHTSEEING, ACCOMMODATION, OTHER)

**Returns:** `List<Activity>` - List of matching activities

**Example:**
```java
List<Activity> foodActivities = trip.getActivitiesByCategory(ActivityCategory.FOOD);
```

---

#### `getTotalCost()`
**Description:** Calculates the total estimated cost of all activities in the trip.

**Parameters:** None

**Returns:** `double` - Sum of all activity costs

**Example:**
```java
double totalCost = trip.getTotalCost();
```

---

#### `getActivitiesInRange(LocalDateTime start, LocalDateTime end)`
**Description:** Retrieves all activities within a specified time range.

**Parameters:**
- `start` (LocalDateTime): Start of time range
- `end` (LocalDateTime): End of time range

**Returns:** `List<Activity>` - Activities within the range

**Example:**
```java
List<Activity> todayActivities = trip.getActivitiesInRange(
    LocalDateTime.of(2024, 6, 15, 0, 0),
    LocalDateTime.of(2024, 6, 15, 23, 59)
);
```

---

### 1.2 Activity Class

**Purpose:** Represents a single activity within a trip.

#### `Activity(String activityId, String name, LocalDateTime startTime, LocalDateTime endTime, ActivityCategory category, double cost, String location, String notes)`
**Description:** Constructor to create a new activity.

**Parameters:**
- `activityId` (String): Unique identifier
- `name` (String): Activity name
- `startTime` (LocalDateTime): Activity start time
- `endTime` (LocalDateTime): Activity end time
- `category` (ActivityCategory): Category enum
- `cost` (double): Estimated cost
- `location` (String): Activity location
- `notes` (String): Additional notes (optional)

**Returns:** Activity object

**Example:**
```java
Activity lunch = new Activity(
    "A002",
    "Lunch at Le Bistro",
    LocalDateTime.of(2024, 6, 15, 12, 0),
    LocalDateTime.of(2024, 6, 15, 13, 30),
    ActivityCategory.FOOD,
    45.50,
    "123 Rue de Paris",
    "Reservation under Smith"
);
```

---

#### `overlapsWith(Activity other)`
**Description:** Checks if this activity overlaps with another activity.

**Parameters:**
- `other` (Activity): Another activity to check against

**Returns:** `boolean` - true if there's a time conflict

**Example:**
```java
boolean hasConflict = activity1.overlapsWith(activity2);
```

---

#### `updateCost(double newCost)`
**Description:** Updates the cost of the activity.

**Parameters:**
- `newCost` (double): New cost value

**Returns:** `void`

**Example:**
```java
lunch.updateCost(52.00);
```

---

#### `getDuration()`
**Description:** Calculates the duration of the activity in minutes.

**Parameters:** None

**Returns:** `long` - Duration in minutes

**Example:**
```java
long durationMinutes = lunch.getDuration();
```

---

### 1.3 ActivityCategory Enum

**Purpose:** Defines predefined categories for activities.

**Values:**
- `FOOD` - Dining and meal activities
- `TRANSPORT` - Flights, trains, taxis, etc.
- `SIGHTSEEING` - Tourist attractions and tours
- `ACCOMMODATION` - Hotel check-ins, stays
- `OTHER` - Miscellaneous activities

---

## 2. Service Layer APIs

### 2.1 TripService Class

**Purpose:** Business logic for managing trips and activities.

#### `createTrip(String name, LocalDate startDate, LocalDate endDate)`
**Description:** Creates a new trip and persists it to storage.

**Parameters:**
- `name` (String): Trip name
- `startDate` (LocalDate): Start date
- `endDate` (LocalDate): End date

**Returns:** `Trip` - The created trip object

**Throws:** `InvalidDateRangeException` if endDate is before startDate

**Example:**
```java
TripService service = new TripService();
Trip newTrip = service.createTrip("Asia Adventure",
    LocalDate.of(2024, 9, 1),
    LocalDate.of(2024, 9, 15));
```

---

#### `getAllTrips()`
**Description:** Retrieves all trips from storage.

**Parameters:** None

**Returns:** `List<Trip>` - List of all trips

**Example:**
```java
List<Trip> allTrips = service.getAllTrips();
```

---

#### `getTripById(String tripId)`
**Description:** Retrieves a specific trip by ID.

**Parameters:**
- `tripId` (String): Trip identifier

**Returns:** `Trip` - The trip object, or null if not found

**Example:**
```java
Trip trip = service.getTripById("T001");
```

---

#### `updateTrip(Trip trip)`
**Description:** Updates an existing trip in storage.

**Parameters:**
- `trip` (Trip): Trip object with updated data

**Returns:** `boolean` - true if successful

**Throws:** `TripNotFoundException` if trip doesn't exist

**Example:**
```java
trip.setName("Europe Summer 2024 - Updated");
boolean updated = service.updateTrip(trip);
```

---

#### `deleteTrip(String tripId)`
**Description:** Deletes a trip and all its activities.

**Parameters:**
- `tripId` (String): Trip identifier

**Returns:** `boolean` - true if deleted successfully

**Example:**
```java
boolean deleted = service.deleteTrip("T001");
```

---

### 2.2 ActivityService Class

**Purpose:** Business logic for managing activities within trips.

#### `addActivityToTrip(String tripId, Activity activity)`
**Description:** Adds an activity to a specific trip with conflict validation.

**Parameters:**
- `tripId` (String): Target trip identifier
- `activity` (Activity): Activity to add

**Returns:** `boolean` - true if added successfully

**Throws:**
- `TripNotFoundException` if trip doesn't exist
- `ActivityConflictException` if time overlap detected

**Example:**
```java
ActivityService activityService = new ActivityService();
Activity museum = new Activity(...);
boolean added = activityService.addActivityToTrip("T001", museum);
```

---

#### `updateActivity(String tripId, Activity activity)`
**Description:** Updates an existing activity and re-validates conflicts.

**Parameters:**
- `tripId` (String): Trip identifier
- `activity` (Activity): Activity with updated data

**Returns:** `boolean` - true if updated successfully

**Throws:** `ActivityConflictException` if new time creates conflict

**Example:**
```java
museum.updateCost(25.00);
boolean updated = activityService.updateActivity("T001", museum);
```

---

#### `deleteActivity(String tripId, String activityId)`
**Description:** Removes an activity from a trip.

**Parameters:**
- `tripId` (String): Trip identifier
- `activityId` (String): Activity identifier

**Returns:** `boolean` - true if deleted

**Example:**
```java
boolean removed = activityService.deleteActivity("T001", "A001");
```

---

#### `detectConflicts(String tripId, Activity newActivity)`
**Description:** Checks for time conflicts before adding/updating an activity.

**Parameters:**
- `tripId` (String): Trip identifier
- `newActivity` (Activity): Activity to validate

**Returns:** `List<Activity>` - List of conflicting activities (empty if none)

**Example:**
```java
List<Activity> conflicts = activityService.detectConflicts("T001", newActivity);
if (!conflicts.isEmpty()) {
    System.out.println("Warning: Time conflicts detected!");
}
```

---

#### `getActivitiesByCategory(String tripId, ActivityCategory category)`
**Description:** Retrieves filtered activities for a trip.

**Parameters:**
- `tripId` (String): Trip identifier
- `category` (ActivityCategory): Filter category

**Returns:** `List<Activity>` - Filtered activities

**Example:**
```java
List<Activity> transport = activityService.getActivitiesByCategory(
    "T001",
    ActivityCategory.TRANSPORT
);
```

---

#### `calculateTripCost(String tripId)`
**Description:** Calculates total cost for a specific trip.

**Parameters:**
- `tripId` (String): Trip identifier

**Returns:** `double` - Total estimated cost

**Example:**
```java
double totalCost = activityService.calculateTripCost("T001");
```

---

## 3. Storage Layer APIs

### 3.1 JsonStorageManager Class

**Purpose:** Handles all file I/O operations and JSON parsing.

#### `JsonStorageManager(String dataDirectory)`
**Description:** Constructor that initializes the storage manager with a data directory path.

**Parameters:**
- `dataDirectory` (String): Path to data folder (default: "data/")

**Returns:** JsonStorageManager object

**Example:**
```java
JsonStorageManager storage = new JsonStorageManager("data/");
```

---

#### `saveTrips(List<Trip> trips)`
**Description:** Serializes and saves all trips to JSON file.

**Parameters:**
- `trips` (List<Trip>): List of trips to save

**Returns:** `boolean` - true if saved successfully

**Throws:** `IOException` if file write fails

**Example:**
```java
boolean saved = storage.saveTrips(allTrips);
```

---

#### `loadTrips()`
**Description:** Reads and deserializes trips from JSON file.

**Parameters:** None

**Returns:** `List<Trip>` - List of loaded trips (empty list if file doesn't exist)

**Throws:** `IOException`, `JsonParseException` if file read/parse fails

**Example:**
```java
List<Trip> trips = storage.loadTrips();
```

---

#### `exportTripToJson(Trip trip, String outputPath)`
**Description:** Exports a single trip to a separate JSON file.

**Parameters:**
- `trip` (Trip): Trip to export
- `outputPath` (String): Destination file path

**Returns:** `boolean` - true if exported successfully

**Example:**
```java
storage.exportTripToJson(trip, "exports/europe_trip.json");
```

---

#### `importTripFromJson(String inputPath)`
**Description:** Imports a trip from an external JSON file.

**Parameters:**
- `inputPath` (String): Source file path

**Returns:** `Trip` - Imported trip object

**Throws:** `IOException`, `JsonParseException` if import fails

**Example:**
```java
Trip importedTrip = storage.importTripFromJson("imports/backup.json");
```

---

### 3.2 JsonParser Class

**Purpose:** Low-level JSON serialization/deserialization utilities.

#### `serializeTrip(Trip trip)`
**Description:** Converts a Trip object to JSON string.

**Parameters:**
- `trip` (Trip): Trip object to serialize

**Returns:** `String` - JSON representation

**Example:**
```java
String json = JsonParser.serializeTrip(trip);
```

---

#### `deserializeTrip(String json)`
**Description:** Converts JSON string to Trip object.

**Parameters:**
- `json` (String): JSON string

**Returns:** `Trip` - Deserialized trip object

**Throws:** `JsonParseException` if JSON is malformed

**Example:**
```java
Trip trip = JsonParser.deserializeTrip(jsonString);
```

---

#### `serializeTripList(List<Trip> trips)`
**Description:** Converts a list of trips to JSON array string.

**Parameters:**
- `trips` (List<Trip>): List of trips

**Returns:** `String` - JSON array representation

**Example:**
```java
String jsonArray = JsonParser.serializeTripList(allTrips);
```

---

#### `deserializeTripList(String json)`
**Description:** Converts JSON array string to list of Trip objects.

**Parameters:**
- `json` (String): JSON array string

**Returns:** `List<Trip>` - List of deserialized trips

**Throws:** `JsonParseException` if JSON is malformed

**Example:**
```java
List<Trip> trips = JsonParser.deserializeTripList(jsonArrayString);
```

---

## 4. Exception Classes

### 4.1 ActivityConflictException
**Purpose:** Thrown when activity time ranges overlap.

**Constructor:**
```java
ActivityConflictException(String message, List<Activity> conflictingActivities)
```

**Methods:**
- `getConflictingActivities()` - Returns list of overlapping activities

---

### 4.2 TripNotFoundException
**Purpose:** Thrown when a trip ID doesn't exist in storage.

**Constructor:**
```java
TripNotFoundException(String tripId)
```

---

### 4.3 InvalidDateRangeException
**Purpose:** Thrown when trip end date is before start date.

**Constructor:**
```java
InvalidDateRangeException(String message)
```

---

### 4.4 JsonParseException
**Purpose:** Thrown when JSON file is corrupted or malformed.

**Constructor:**
```java
JsonParseException(String message, Throwable cause)
```

---

## 5. Utility Classes

### 5.1 DateTimeUtil Class

**Purpose:** Helper methods for date/time operations.

#### `formatDateTime(LocalDateTime dateTime, String pattern)`
**Description:** Formats a LocalDateTime to string.

**Parameters:**
- `dateTime` (LocalDateTime): Date-time to format
- `pattern` (String): Format pattern (e.g., "yyyy-MM-dd HH:mm")

**Returns:** `String` - Formatted date-time string

**Example:**
```java
String formatted = DateTimeUtil.formatDateTime(
    LocalDateTime.now(),
    "yyyy-MM-dd HH:mm"
);
```

---

#### `parseDateTime(String dateTimeString, String pattern)`
**Description:** Parses a string to LocalDateTime.

**Parameters:**
- `dateTimeString` (String): Date-time string
- `pattern` (String): Expected format pattern

**Returns:** `LocalDateTime` - Parsed date-time

**Throws:** `DateTimeParseException` if parsing fails

**Example:**
```java
LocalDateTime dt = DateTimeUtil.parseDateTime(
    "2024-06-15 14:30",
    "yyyy-MM-dd HH:mm"
);
```

---

#### `isWithinRange(LocalDateTime target, LocalDateTime start, LocalDateTime end)`
**Description:** Checks if a date-time falls within a range.

**Parameters:**
- `target` (LocalDateTime): Date-time to check
- `start` (LocalDateTime): Range start
- `end` (LocalDateTime): Range end

**Returns:** `boolean` - true if within range

**Example:**
```java
boolean isInRange = DateTimeUtil.isWithinRange(activityTime, tripStart, tripEnd);
```

---

### 5.2 IdGenerator Class

**Purpose:** Generates unique identifiers for trips and activities.

#### `generateTripId()`
**Description:** Creates a unique trip ID.

**Parameters:** None

**Returns:** `String` - Unique trip ID (format: "T" + timestamp + random)

**Example:**
```java
String newTripId = IdGenerator.generateTripId(); // "T1717584000123"
```

---

#### `generateActivityId()`
**Description:** Creates a unique activity ID.

**Parameters:** None

**Returns:** `String` - Unique activity ID (format: "A" + timestamp + random)

**Example:**
```java
String newActivityId = IdGenerator.generateActivityId(); // "A1717584000456"
```

---

## 6. Usage Flow Example

```java
// Initialize services
TripService tripService = new TripService();
ActivityService activityService = new ActivityService();

// Create a new trip
Trip europeTrip = tripService.createTrip(
    "Europe Summer 2024",
    LocalDate.of(2024, 6, 1),
    LocalDate.of(2024, 6, 30)
);

// Create an activity
Activity flight = new Activity(
    IdGenerator.generateActivityId(),
    "Flight to Paris",
    LocalDateTime.of(2024, 6, 1, 10, 0),
    LocalDateTime.of(2024, 6, 1, 14, 0),
    ActivityCategory.TRANSPORT,
    450.00,
    "CDG Airport",
    "Air France AF123"
);

// Add activity to trip (with conflict detection)
try {
    activityService.addActivityToTrip(europeTrip.getTripId(), flight);
} catch (ActivityConflictException e) {
    System.out.println("Conflict detected: " + e.getMessage());
    List<Activity> conflicts = e.getConflictingActivities();
    // Handle conflicts
}

// Calculate total cost
double totalCost = activityService.calculateTripCost(europeTrip.getTripId());
System.out.println("Total trip cost: $" + totalCost);

// Filter activities by category
List<Activity> transportActivities = activityService.getActivitiesByCategory(
    europeTrip.getTripId(),
    ActivityCategory.TRANSPORT
);

// Delete an activity
activityService.deleteActivity(europeTrip.getTripId(), flight.getActivityId());
```

---

## 7. Data Storage Format

### 7.1 JSON Structure for trips.json

```json
[
  {
    "tripId": "T1717584000123",
    "name": "Europe Summer 2024",
    "startDate": "2024-06-01",
    "endDate": "2024-06-30",
    "activities": [
      {
        "activityId": "A1717584001234",
        "name": "Flight to Paris",
        "startTime": "2024-06-01T10:00:00",
        "endTime": "2024-06-01T14:00:00",
        "category": "TRANSPORT",
        "cost": 450.00,
        "location": "CDG Airport",
        "notes": "Air France AF123"
      },
      {
        "activityId": "A1717584002345",
        "name": "Eiffel Tower Visit",
        "startTime": "2024-06-02T09:00:00",
        "endTime": "2024-06-02T12:00:00",
        "category": "SIGHTSEEING",
        "cost": 25.00,
        "location": "Champ de Mars",
        "notes": "Book tickets online"
      }
    ]
  }
]
```

---

## Performance Considerations

**Timeline Rendering Requirement:** The system must render a 30-day itinerary in under 1 second.

**Implementation Notes:**
- All trip data is loaded into memory on application startup
- Activity lists are maintained in sorted order by start time
- Conflict detection uses efficient O(n) linear scan algorithm
- JSON parsing is performed once at startup and on save operations only

---

## Version History

- **v1.0** - Initial API documentation for Travel Planner application
