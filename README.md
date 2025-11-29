# 📱 Digital Munshi
### The "Agentic" Financial Identity for India’s Informal Economy

![Kotlin](https://img.shields.io/badge/Kotlin-100%25-purple) ![AI](https://img.shields.io/badge/AI-Gemini%201.5%20Flash-blue) ![Platform](https://img.shields.io/badge/Platform-Android-green) ![License](https://img.shields.io/badge/License-MIT-yellow)

## 📖 Executive Summary
**Digital Munshi** is an offline-first, AI-powered Android application designed to solve the **"Credit Invisibility"** problem for India’s 500 million+ informal workers (gig workers, street vendors, freelancers).

Unlike traditional expense trackers, Digital Munshi acts as an autonomous **"Financial Agent"**. It listens to scattered financial signals (Bank SMS, UPI alerts), normalizes the data using NLP, enriches it with **Gemini 1.5 Flash**, and compiles it into a legally verifiable **"Kamayi Patra" (Proof of Livelihood)**.

> **Key Feature:** The generated reports are cryptographically signed and admissible under **Section 65B of the Indian Evidence Act**, enabling formal lenders to underwrite loans for thin-file borrowers.

---

## 🚩 The Problem Statement
* **The "Thin File" Issue:** Informal workers transact in cash or via personal UPI. They lack formal salary slips or ITRs (Income Tax Returns).
* **The Trust Deficit:** Self-reported income is frequently rejected by banks due to high fraud risk.
* **The Data Mess:** Bank SMS formats vary wildly, often use "Fancy Unicode Fonts" to evade spam filters, and lack clear merchant details.

---

## 🛠 Technical Architecture

| Component | Technology Used |
| :--- | :--- |
| **Language** | Kotlin (100%) |
| **UI Toolkit** | Jetpack Compose (Material 3) |
| **Architecture** | MVVM with Repository Pattern |
| **Database** | Room (SQLite) - *Fully Offline Capable* |
| **AI Model** | Gemini 1.5 Flash (via Google AI SDK) |
| **Background Tasks** | WorkManager & Kotlin Coroutines |
| **Security** | Android Hardware Keystore (Elliptic Curve Cryptography) |

---

## ⚙️ Key Technical Modules

### Module A: The "Agentic Listener" (Ingestion)
* **Entry Point:** `SmsReceiver` broadcast receiver.
* **Normalization Engine:** Solves the "Fancy Font" problem using `java.text.Normalizer` (NFKC Form) to convert symbols (e.g., `𝖽𝖾𝖻𝗂𝗍𝖾𝖽`) into standard ASCII (`debited`).
* **Regex Logic:** Robust extraction of Amount, Date, and Transaction Type (CREDIT/DEBIT) across varied bank formats (HDFC, SBI, PNB, etc.).
* **Privacy:** Raw SMS data is processed in RAM and discarded; only extracted metadata is stored.

### Module B: The "Intelligence Worker" (Categorization)
Uses a **De-coupled Asynchronous Pipeline**:
1.  **Trigger:** Transaction saved -> `CategorizationWorker` enqueued.
2.  **Context Injection:** Fetches User Persona (e.g., "Driver") from `UserPreferences`.
3.  **AI Inference:** Sends Data + Persona to **Gemini 1.5 Flash**.

```text
Input: "Paid Rs 500 to Shell" + Context: "Driver"
Output: Category: "Business Inventory" (Fuel), Confidence: 98%
Explainability: "Identified Shell as a fuel station relevant to driving job
```

### Module C: The "Risk Engine" (Analytics)
Located in `ReportViewModel`, this transforms raw rows into Bank-Grade Metrics.
* **Stability Score (CV):** Calculates the Coefficient of Variation (`σ / μ`) of monthly income to determine if income is Volatile, Stable, or Highly Stable.
* **Seasonality Waveform:** Aggregates income by month to visualize business cycles (Peaks & Troughs) on a custom Canvas-drawn Bar Chart.
* **Business Health:** Calculates Profit Margin % (`(Income - Expense) / Income`).

### Module D: The "Trust Layer" (Security & Legal)
* **Section 65B Certification:** The UI forces the user to legally certify that the device and data are theirs.
* **Cryptographic Signing:**
  * Generates a **Public/Private Key Pair** inside the Android Hardware Keystore (Non-exportable).
  * Signs the JSON payload of the financial report using `SHA256withECDSA`.
* **QR Code Interoperability:** Generates a QR code containing the Signed JSON payload. Lenders scan this to instantly ingest verified data.

---

## 🔄 Detailed Data Workflow

1.  **Event:** SMS Received (`"Acct XX123 Debited INR 500..."`).
2.  **Ingest:** `SmsReceiver` wakes up → Normalizes Text → Regex Parse → Insert into Room DB (Status: `Unverified`).
3.  **Enrich (Background):** `WorkManager` wakes up → Calls Gemini API → Update Room DB (Status: `AI_Verified`, Category: `Food`, Reason: `...`).
4.  **Analyze:** `ReportViewModel` queries DB → Calculates Risk Profile (Stability Score, Velocity).
5.  **Certify:** User clicks "Sign & Lock" → App generates Hash → Signs with Private Key → Updates UI to "Verified Seal".
6.  **Share:** User clicks Share → App generates QR Code with Payload → Lender Scans.

---

## 🌟 Unique Selling Points (USP)

* **Hybrid Intelligence:** Combines the speed/offline capability of Regex with the semantic understanding of LLMs (Gemini).
* **Context-Aware AI:** The AI behavior changes based on the user's occupation (Persona Injection), making it smarter than generic expense trackers.
* **Legal Admissibility:** It is the only hackathon project that generates a report compliant with **Section 65B of the Indian Evidence Act**.
* **Lender-Centric UI:** The dashboard speaks the language of banks ("Stability," "Velocity," "Margins") rather than just "Budgeting."

---

## 📂 Folder Structure

```plaintext
com.githarshking.the_digital_munshi
├── data
│   ├── MunshiDatabase.kt      // Room Database
│   ├── Transaction.kt         // Data Entity (Schema)
│   ├── TransactionDao.kt      // SQL Queries
│   └── ReportViewModel.kt     // Risk Engine & Logic
├── ui
│   ├── ReportScreen.kt        // 5-Zone Risk Dashboard
│   ├── OnboardingScreen.kt    // User Persona Setup
│   └── theme                  // Material 3 Theme
├── workers
│   ├── CategorizationWorker.kt // Background AI Task
│   └── ImportWorker.kt         // PDF Parsing Task
├── utils
│   ├── GeminiClassifier.kt    // AI API Client
│   ├── StatementParser.kt     // PDF AI Agent (Gemini 2.5)
│   ├── PdfUtils.kt            // iText7 Extractor
│   ├── SecurityUtils.kt       // Crypto & Keystore
│   ├── QrCodeUtils.kt         // JSON & QR Generation
│   └── UserPreferences.kt     // SharedPrefs Manager
├── MainActivity.kt            // Navigation & Entry Point
└── SmsReceiver.kt             // Broadcast Receiver (The Ear)

```


## 🚀 Getting Started

1. **Clone the repository**
   ```bash
   git clone [https://github.com/yourusername/digital-munshi.git](https://github.com/yourusername/digital-munshi.git)
   ```

2. **Add API Key**
   * Get your API key from [Google AI Studio](https://aistudio.google.com/).
   * Add it to your `local.properties` file:
     ```properties
     GEMINI_API_KEY="your_key_here"
     ```

3. **Build and Run
    *Open in Android Studio Ladybug (or newer).
    *Sync Gradle and Run on an Emulator or Physical Device.

## 🏁 Conclusion
This architecture ensures **scalability, reliability, and security**, making **Digital Munshi** a production-ready prototype for financial inclusion. By bridging the gap between informal earnings and formal credit, we aim to empower millions of Indian workers.

---


