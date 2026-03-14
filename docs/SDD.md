# Software Design Document (SDD): Travel Planner

## 1. System Overview
The Travel Planner is a Java-based desktop application designed for travelers to manage complex itineraries locally. The system is designed to be highly portable, distributed as a single `.jar` file, and stores all user data in a local `data/` folder in JSON format. It provides a robust interface for organizing destinations, tracking activity costs, and preventing schedule conflicts.

---

## 2. Architecture Design
The system follows a **Three-Tier Architecture** to ensure a clean separation of concerns and ease of maintenance:

* **Presentation Layer (UI):** Built using JavaFX/Swing, this layer handles user interactions and displays the itinerary timeline. It communicates exclusively with the Logic Layer.
* **Logic Layer (Domain):** The central hub of the application. It processes business logic, such as validating activity timeframes for overlaps and calculating budget totals across categories.
* **Storage Layer (Data):** Responsible for data persistence. It converts Java objects into JSON strings to be saved in the `data/` directory and reconstructs objects during the loading process.

---

## 3. Major System Components
* **Trip:** The primary entity representing a travel plan. It acts as a container for a collection of activities.
* **Activity:** Represents a specific event within a trip. It contains data such as `location`, `startTime`, `endTime`, `cost`, and `Category`.
* **Category:** An enumeration used to classify activities (e.g., FOOD, TRANSPORT) for filtering and budgeting purposes.
* **Storage:** A utility component that handles file I/O operations, ensuring that user data is persisted locally and remains accessible across sessions.

---

## 4. UML Diagrams

### 4.1 Class Diagram
The following diagram illustrates the static structure of the system, including class attributes, methods, visibility, and relationships.

![Class Diagram](docs/images/class_diagram.png)

---

## 5. Key Design Decisions

### 5.1 Composition Relationship
We decided to model the relationship between `Trip` and `Activity` as **Composition**. Since an `Activity` in this system is contextually tied to a specific journey, it cannot exist independently of a `Trip`. This ensures that deleting a trip correctly cleans up all associated activity data.

### 5.2 Local JSON Persistence
To fulfill the requirement of a standalone `.jar` file without an external SQL/NoSQL database, we chose **JSON** for data storage. This provides a human-readable format that is lightweight and easy to parse using standard Java libraries, ensuring the application remains portable.

### 5.3 Logic-Centric Validation
Validation logic for time overlaps is placed within the `Trip` class rather than the UI. This ensures that the core business rules are preserved even if the user interface is modified or replaced in the future.