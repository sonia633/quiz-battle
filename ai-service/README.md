# AI Service (Python / FastAPI)

Machine-learning service powering personalised play for the Quiz Battle
Platform. Three supervised models are trained on a reproducible, Kaggle-style
quiz-attempt dataset:

| Model | Algorithm | Purpose |
|-------|-----------|---------|
| Correctness | `LogisticRegression` | P(player answers a question correctly) — drives recommendations |
| Difficulty | `RandomForestClassifier` | Predict a question's difficulty from its behaviour |
| Skill | `XGBoost` *(falls back to scikit-learn `GradientBoostingRegressor`)* | Estimate a 0–100 player ability score |

Higher-level features composed on top: **question recommendation**
(optimal-challenge + weak-topic steering), **weak-topic detection**, and
**personalised learning suggestions**.

## Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/health` | Liveness probe |
| GET | `/` | Service info + model metrics |
| POST | `/api/ai/difficulty/predict` | Predict question difficulty |
| POST | `/api/ai/skill/estimate` | Estimate a player's skill score |
| POST | `/api/ai/weak-topics` | Detect weak categories + suggestions |
| POST | `/api/ai/recommend` | Rank candidate questions for a player |
| POST | `/api/ai/retrain` | Retrain and hot-swap the model bundle |

Interactive docs (Swagger UI) at `/docs`, OpenAPI spec at `/openapi.json`.

## Run locally

```bash
python -m venv .venv
. .venv/Scripts/activate        # Windows  (Linux/macOS: source .venv/bin/activate)
pip install -r requirements.txt

python train.py                 # trains models -> models/model_bundle.joblib
uvicorn app.main:app --port 8000
```

If no model bundle exists, the service trains one automatically on first
startup. The dataset is generated deterministically into `data/attempts.csv`;
drop a real Kaggle CSV with the same columns there to train on real data.

## Test

```bash
pytest -q     # trains a small model in a temp dir, then exercises every endpoint
```

## Configuration (env vars, prefix `QUIZAI_`)

| Variable | Default | Notes |
|----------|---------|-------|
| `QUIZAI_PORT` | 8000 | HTTP port |
| `QUIZAI_DATASET_USERS` | 1200 | Synthetic users to generate |
| `QUIZAI_DATASET_SEED` | 42 | Reproducibility seed |

## Model metrics (seed 42)

`correctness_auc ≈ 0.81 · difficulty_accuracy ≈ 0.92 · skill_r² ≈ 0.84`
