# 🚗 Campus Ride Share

**Campus Ride Share** is an industry-level Android application built to simplify daily commutes for university students. It provides a secure and efficient platform for students to offer or join car and bike rides within their campus community.

## 🌟 Key Features

- **📍 Real-time Map Integration**: Seamless map experience using MapLibre SDK for picking locations and viewing routes.
- **🛣️ Intelligent Routing**: Automatic route generation and distance calculation via OSRM Routing.
- **🛰️ Live GPS Tracking**: Real-time location sharing using Android Foreground Services.
- **💰 Automated Cost Estimation**: Fair pricing based on distance (e.g., Rs. 20/km) and vehicle type.
- **🛡️ Multi-Role Support**: 
  - **User**: Post rides, request seats, track drivers, and rate experiences.
  - **Admin**: Full dashboard for moderating users, handling reports, and system overview.
- **🔔 Instant Notifications**: Status-based push notifications using Firebase Cloud Messaging (FCM).
- **🔒 Secure Profiles**: Firebase Authentication with detailed student profiles and verified vehicle info.

## 🛠️ Tech Stack

- **UI**: [Jetpack Compose](https://developer.android.com/jetpack/compose) (100% Declarative UI)
- **Architecture**: MVVM (Model-View-ViewModel) + Repository Pattern
- **Dependency Injection**: [Hilt](https://developer.android.com/training/dependency-injection/hilt-android)
- **Backend**: 
  - [Firebase Auth](https://firebase.google.com/docs/auth) (Email/Password & Google Sign-In)
  - [Firebase Realtime Database](https://firebase.google.com/docs/database) (NoSQL JSON Data)
  - [Firebase Cloud Messaging](https://firebase.google.com/docs/cloud-messaging) (Push Notifications)
- **Maps & Location**: MapLibre SDK, OSRM API, and Google Play Services Location.
- **Networking**: Retrofit & Coroutines for asynchronous API calls.

## 📸 Preview

*(Add your screenshots or a GIF demo here!)*

## ⚙️ Getting Started

### Prerequisites
- Android Studio Ladybug or newer
- JDK 17+
- A Firebase Project

### Setup
1. **Clone the repo**:
   ```bash
   git clone https://github.com/your-username/campus-ride-share.git
   ```
2. **Firebase Config**:
   - Create a project on the [Firebase Console](https://console.firebase.google.com/).
   - Add an Android App with the package name `com.atif.campusrideshare`.
   - Download `google-services.json` and place it in the `app/` directory.
   - Enable **Email/Password** in Auth and **Realtime Database**.
3. **Build & Run**:
   - Open the project in Android Studio.
   - Sync Gradle and hit **Run**.

## 🛡️ Security Rules
The project uses custom Firebase Security Rules for role-based access control. Ensure you apply the rules provided in the `firebase_rules` artifact to your Realtime Database.

---
**Made with ❤️ for the student community.**
