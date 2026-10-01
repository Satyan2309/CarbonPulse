# 🌿 CarbonPulse: AI-Powered Carbon Footprint & Sustainability Platform

![Java](https://img.shields.io/badge/java_21-%23ED8B00.svg?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/springboot_4-%236DB33F.svg?style=for-the-badge&logo=spring&logoColor=white)
![Angular](https://img.shields.io/badge/angular_18-%23DD0031.svg?style=for-the-badge&logo=angular&logoColor=white)
![TypeScript](https://img.shields.io/badge/typescript-%23007ACC.svg?style=for-the-badge&logo=typescript&logoColor=white)
![WebGL](https://img.shields.io/badge/WebGL-%23990000.svg?style=for-the-badge&logo=webgl&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/postgresql-%23316192.svg?style=for-the-badge&logo=postgresql&logoColor=white)
![OpenAI](https://img.shields.io/badge/OpenAI-%23412991.svg?style=for-the-badge&logo=openai&logoColor=white)

### 🌐 Live Deployment
| 🖥️ **Frontend (Web App)** | [Localhost:4200 (Dev)]() | Vercel (Coming Soon) |
| ⚙️ **Backend (API)** | [Localhost:8080 (Dev)]() | Render (Coming Soon) |

---

## 📖 What is CarbonPulse?
CarbonPulse is a comprehensive, full-stack sustainability management platform designed to help individuals track, analyze, and reduce their carbon footprint. By leveraging generative AI, the platform provides highly personalized lifestyle recommendations, tracks daily emissions across multiple categories, and encourages environmental action through community challenges and gamification.

The platform recently underwent a complete UI overhaul featuring a gorgeous **Bioluminescent Dark Theme** with immersive glowing effects and responsive animations.

### 🌍 Core Sustainability Modules
1. **Interactive 3D Dashboard:** A WebGL-powered responsive dashboard featuring a rotatable Earth and real-time Carbon KPIs.
2. **Carbon Tracking:** Log daily emissions across Transportation, Energy, Food, and Waste.
3. **Gamification & Rewards:** Earn XP, level up, and unlock dynamic rarity badges (Iron to Platinum) based on your eco-friendly actions.
4. **AI Recommendation System:** Receive predictive insights and personalized action plans via OpenAI.
5. **Community Challenges:** Join global sustainability challenges and track live progress side-by-side.

---

## 🤖 AI Insights Engine
CarbonPulse utilizes a dual-layered AI architecture to guarantee 100% uptime and dynamic personalization.

* **Primary Architecture — Generative AI (Spring AI + OpenAI):** Uses the `gpt-4o-mini` LLM. The system feeds the user's last 30 days of carbon logs, active goals, diet preferences, and commute modes into a highly engineered prompt. It returns a structured JSON response predicting monthly emissions and suggesting priority actions.
* **Explainability & Fallback — Rule-Based Heuristic Engine:** An in-house mathematical engine (`AIRuleServiceImpl`) that acts as a fail-safe. If the OpenAI API rate-limits or times out, this engine calculates percentage breakdowns to ensure the user always receives actionable insights.
* **Data Pipeline:** `CarbonEmissionRepository` + `UserRepository` ➡️ `GenerativeAIService` ➡️ Angular Frontend.

---

## 🛠️ Tech Stack & Libraries

### **Backend**
| Library | Version | Why Used |
| :--- | :--- | :--- |
| **Java & Spring Boot** | 21 / 4.x | Core REST API framework |
| **Spring Security & JWT** | - | Stateless authentication and role-based access |
| **Spring AI** | - | Seamless LLM integration and structured prompt engineering |
| **Hibernate / Spring Data JPA** | - | ORM and database management |

### **Frontend**
| Library | Version | Why Used |
| :--- | :--- | :--- |
| **Angular & TypeScript** | 18+ | Component-based SPA architecture |
| **JavaScript & RxJS** | ES6+ | Reactive programming for state and API handling |
| **Chart.js & Three.js** | - | Rendering dynamic emission breakdown charts and 3D globe |
| **Custom CSS**| - | Responsive, Bioluminescent dark-mode UI |

### **Database & Infrastructure**
| Library | Version | Why Used |
| :--- | :--- | :--- |
| **PostgreSQL (Neon DB)**| - | Primary relational database in the cloud |
| **Cloudinary** | - | Cloud storage for user profile pictures |
| **Google OAuth2** | - | Alternative social login flow |

---

## 📁 Folder Structure

```text
carbon-pulse/
├── backend/                # Spring Boot Java Application
│   ├── src/main/java/      # Controllers, Services, Entities, Repositories
│   └── src/main/resources/ # application.properties, static assets
├── frontend/               # Angular SPA Application
│   ├── src/app/core/       # Interceptors, Auth Guards, Base Services
│   └── src/app/features/   # AI, Dashboard, Challenges, Carbon Tracker
```

---

## 🚀 Getting Started (Local Setup)

### Prerequisites
* JDK 21
* Node.js (v20+) & npm (v10+)
* PostgreSQL (Local or Neon DB)
* OpenAI API Key

### Backend Setup
1. Navigate to the backend directory: `cd backend`
2. Create `application-local.properties` (or configure `.env` variables) with your database credentials and OpenAI key.
3. Run the Spring Boot application using Maven Wrapper:
   ```bash
   ./mvnw clean spring-boot:run
   ```

### Frontend Setup
1. Navigate to the frontend directory: `cd frontend`
2. Install dependencies:
   ```bash
   npm install --legacy-peer-deps
   ```
3. Start the Angular development server:
   ```bash
   npm start
   ```
4. Open your browser to `http://localhost:4200`

---

## 📄 License
MIT License — free to use for academic and research purposes.

## 👨‍💻 Author
* **Satyan Sinha**
* [GitHub Profile](https://github.com/Satyan2309)

---
