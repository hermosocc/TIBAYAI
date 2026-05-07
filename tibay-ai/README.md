# TIBAY AI — Technical Identity & Build-up Analytics

AI-assisted technical assessment + hiring workflow MVP for Filipino skilled workers (welders / industrial laborers).

This prototype is explicitly **not** an official verification or certification system. It produces **AI-assisted screening outputs** intended for hackathon demos and early product validation.

## Tech Stack

- Backend: Spring Boot (MVC), Spring Security, Spring Data JPA
- Frontend: Thymeleaf + Bootstrap 5
- DB: MySQL (recommended) or H2 (default profile for fast demo)
- AI/NLP: OpenRouter (optional, via API key)
- OCR: OCR.Space (optional, via API key)
- CV: Roboflow hosted models (optional, via API key + model URLs)
- Storage: local filesystem (`./uploads`)

## Quick Start (H2 default)

```bash
cd tibay-ai
./mvnw spring-boot:run
```

Open:

- http://localhost:8080

Demo accounts (auto-seeded on first run if database is empty):

- Client: `client@demo.com` / `demo1234`
- Worker: `worker@demo.com` / `demo1234`

## MySQL Run

1. Create a database (example: `tibay_ai`)
2. Run with profile:

```bash
export MYSQL_HOST=localhost
export MYSQL_PORT=3306
export MYSQL_DB=tibay_ai
export MYSQL_USER=root
export MYSQL_PASSWORD=your_password

./mvnw spring-boot:run -Dspring-boot.run.profiles=mysql
```

## AI Integrations (Optional)

The MVP works without keys (it falls back to deterministic, demo-friendly outputs). If you add keys, outputs become more believable.

### OpenRouter (portfolio + evaluation text)

```bash
export OPENROUTER_API_KEY=...
export OPENROUTER_MODEL=openai/gpt-4o-mini
```

### OCR.Space (ID OCR)

```bash
export OCR_SPACE_API_KEY=...
```

### Roboflow (PPE + face detection)

```bash
export ROBOFLOW_API_KEY=...
export ROBOFLOW_FACE_MODEL_URL=https://detect.roboflow.com/<face-model>/<version>
export ROBOFLOW_PPE_MODEL_URL=https://detect.roboflow.com/<ppe-model>/<version>
```

Roboflow hosted endpoints normally accept a multipart form upload field named `file` and return JSON `predictions[]`.

## Demo Flow

1. Login as Worker → `Verify ID` → upload Government ID + Selfie
2. Upload welding image/video → AI-assisted assessment + authenticity screening
3. Generate portfolio → paste Tagalog/Taglish worker notes → ATS English output
4. Login as Client → post job → review applicants → Hire → match created
5. Open chat → send messages → notifications appear

