# Nest: Personal Finance Manager

A native Android application, built with Kotlin, designed for simple and effective personal expense tracking. This project was developed as an academic exercise to demonstrate modern Android development practices, including a clean architecture and real-time data synchronization.

---

## Key Features

*   **User Authentication:** Secure sign-up and login functionality.
*   **Expense Tracking:** Easily add, view, and manage your daily expenses.
*   **Budget Management:** Set and monitor budgets to stay on top of your finances.
*   **Real-time Sync:** Powered by Firebase Realtime Database, all your data is instantly synchronized across any device you're logged into.

## Architecture & Tech Stack

This project is built following the **MVVM (Model-View-ViewModel)** design pattern. This architecture was chosen to create a clear separation of concerns, making the app more scalable, maintainable, and testable.

*   **View:** The UI layer (Activities/Fragments) is responsible for observing data from the ViewModel and displaying it. It knows nothing about the business logic.
*   **ViewModel:** Acts as a bridge between the View and the Model (Repository). It holds and processes UI-related data, surviving configuration changes.
*   **Model:** The data layer, managed by the Repository, which abstracts the data source (Firebase) from the rest of the app.

### Tech Stack

*   **Language:** **Kotlin**
*   **Architecture:** **MVVM (Model-View-ViewModel)**
*   **Backend & Database:** **Firebase Realtime Database** (for serverless storage and data sync)
*   **Authentication:** **Firebase Authentication**
*   **UI:** Android XML Layouts with Material Design Components
*   **Build System:** Gradle

## Project Structure

The project's structure is organized around the MVVM pattern to ensure a clean and logical codebase.

```plaintext
/nest/app/src/main/java/itson/appsmoviles/nest/
│
├── data/                  # MODEL layer
│   ├── model/             # Kotlin data classes (e.g., Expense.kt, User.kt)
│   ├── repository/        # Manages data operations (e.g., fetching from Firebase)
│   └── enum/              # Enumerations used across the app
│
├── ui/                    # VIEW and VIEWMODEL layers
│   ├── auth/              # Authentication screens (Login, Register)
│   ├── budget/            # Budget management feature
│   ├── expenses/          # Expense list and details
│   ├── home/              # Main dashboard/home screen
│   └── ... (and other UI feature packages)
│
└── NestApplication.kt     # Main Application class
```

Generated code
*   The **`data`** package contains all the data-handling logic, acting as the "Model" part of MVVM.
*   The **`ui`** package is organized by feature. Each feature sub-package (e.g., `expenses`) contains the corresponding `Activity` or `Fragment` (the View) and its `ViewModel`.

## Getting Started

To get a local copy up and running, follow these steps.

### Prerequisites

*   [Android Studio](https://developer.android.com/studio) (latest stable version recommended)
*   A Google account to create a Firebase project.

### Installation

1.  **Clone the repository**
    ```sh
    git clone https://github.com/jssroberto/nest.git
    ```
2.  **Open in Android Studio**
    *   Launch Android Studio and select `Open an Existing Project`.
    *   Navigate to the cloned `nest` directory and open it.

3.  **Set up Firebase**
    *   Go to the [Firebase Console](https://console.firebase.google.com/) and create a new project.
    *   Add an Android app to your Firebase project with the package name `itson.appsmoviles.nest` (or as defined in your `build.gradle.kts`).
    *   Download the `google-services.json` file provided by Firebase.
    *   Place the `google-services.json` file in the **`Nest/app/`** directory of your project.
    *   In the Firebase Console, enable **Authentication** (e.g., Email/Password) and the **Realtime Database**.

4.  **Build and Run**
    *   Sync the project with Gradle files in Android Studio.
    *   Select a device or emulator and click the "Run" button.

## Contributing

This project was developed for academic and portfolio purposes. Therefore, active development or acceptance of feature pull requests is not planned.

However, if you find a bug or have a suggestion, feel free to open an issue!

## License

Distributed under the MIT License. See `LICENSE` for more information.
