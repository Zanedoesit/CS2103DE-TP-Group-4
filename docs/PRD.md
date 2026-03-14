# Product Requirements Document (PRD): Travel Planner

## 1. Product Overview
The **Travel Planner** is a standalone application designed to help travelers manage complex itineraries.

---

## 2. Problem Statement
Travelers, specifically exchange students and families, often struggle to synchronize transport, food, and sightseeing plans. This application will help them to plan their travel with ease.

## 3. Target Users / Stakeholders
**Exchange Students:** Students traveling on a budget who need to track spending across multiple cities.  

**Family Travelers:** Users managing high-density schedules who need to prevent overbooking and categorize activities for different family members.

---

## 4. User Stories
**Timeline Management:** As a traveler, I want to add a new destination and date to my trip so that I can see my full travel timeline in one place.  

**Itinerary Maintenance:** As a user, I want to delete a cancelled flight from my itinerary so that my schedule remains accurate.  

**Budget Monitoring:** As a budget traveler, I want to tag each activity with a cost so that I can see the total estimated spending for my trip. 

---

## 5. Functional Requirements
**Local Data Persistence:** The system shall store all travel plans in JSON format within a dedicated `data/` folder.  

**CRUD Operations:** The application shall provide the ability to Create, Read, Update, and Delete trips and activities.  

**Cost Aggregation:** The system shall automatically calculate the total estimated cost based on activity tags.  

**Conflict Detection Logic:** The system shall identify and alert the user if two activity timeframes overlap.  

**Data Filtering:** The system shall allow users to filter activities based on pre-defined categories (e.g., Food, Transport).

---

## 6. Non-Functional Requirements
**Portability:** The system must be delivered as a single executable `.jar` file. 

**No External Dependencies:** The system shall not require any SQL or NoSQL database engines to function. [cite: 627]

**Performance:** The UI must render the full timeline of a 30-day itinerary in under 1 second using local data. [cite: 68]