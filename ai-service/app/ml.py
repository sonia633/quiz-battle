"""Model training, persistence and inference.

Three supervised models back the API:

* **Correctness** — ``LogisticRegression`` estimating P(a player answers a
  question correctly). Drives recommendations.
* **Difficulty** — ``RandomForestClassifier`` predicting a question's
  difficulty label from its aggregate behaviour.
* **Skill** — gradient-boosted regressor (``XGBoost`` if installed, else
  scikit-learn ``GradientBoostingRegressor``) estimating a 0–100 ability score
  from a player's behavioural features.
"""
from __future__ import annotations

import dataclasses
from pathlib import Path
from typing import Any

import joblib
import numpy as np
import pandas as pd
from sklearn.compose import ColumnTransformer
from sklearn.ensemble import GradientBoostingRegressor, RandomForestClassifier
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import accuracy_score, r2_score, roc_auc_score
from sklearn.model_selection import train_test_split
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import OneHotEncoder, StandardScaler

from .config import CATEGORIES, DIFFICULTIES, DIFFICULTY_ORDINAL
from .dataset import load_dataset

try:  # XGBoost is optional; gracefully degrade if no wheel is available.
    from xgboost import XGBRegressor

    _HAS_XGB = True
except Exception:  # pragma: no cover - depends on environment
    _HAS_XGB = False

CORRECTNESS_FEATURES = ["category", "difficulty_ord", "points", "time_taken_sec", "prior_accuracy"]
DIFFICULTY_FEATURES = ["points", "time_taken_sec", "global_success_rate"]
SKILL_FEATURES = ["overall_accuracy", "avg_time", "total_attempts"] + [f"acc_{c}" for c in CATEGORIES]

MODEL_FILE = "model_bundle.joblib"


@dataclasses.dataclass
class ModelBundle:
    correctness: Pipeline
    difficulty: Pipeline
    skill: Any
    skill_backend: str
    metrics: dict[str, float]

    def save(self, models_dir: Path) -> Path:
        models_dir.mkdir(parents=True, exist_ok=True)
        path = models_dir / MODEL_FILE
        joblib.dump(self, path)
        return path

    @staticmethod
    def load(models_dir: Path) -> "ModelBundle":
        return joblib.load(models_dir / MODEL_FILE)


# --------------------------------------------------------------------------- #
# Feature engineering
# --------------------------------------------------------------------------- #
def _add_difficulty_ordinal(df: pd.DataFrame) -> pd.DataFrame:
    out = df.copy()
    out["difficulty_ord"] = out["difficulty"].map(DIFFICULTY_ORDINAL).astype(int)
    return out


def aggregate_user_features(df: pd.DataFrame) -> pd.DataFrame:
    """One row per user: overall accuracy, timing, volume and per-category accuracy."""
    grouped = df.groupby("user_id")
    base = grouped.agg(
        overall_accuracy=("is_correct", "mean"),
        avg_time=("time_taken_sec", "mean"),
        total_attempts=("is_correct", "size"),
    )

    # Per-category accuracy, pivoted to columns (missing categories -> 0).
    cat_acc = (
        df.groupby(["user_id", "category"])["is_correct"].mean().unstack("category")
    )
    cat_acc = cat_acc.reindex(columns=CATEGORIES).fillna(0.0)
    cat_acc.columns = [f"acc_{c}" for c in cat_acc.columns]

    features = base.join(cat_acc)
    if "user_latent_skill" in df.columns:
        features = features.join(grouped["user_latent_skill"].first().rename("latent"))
    return features.reset_index()


# --------------------------------------------------------------------------- #
# Training
# --------------------------------------------------------------------------- #
def _train_correctness(df: pd.DataFrame) -> tuple[Pipeline, float]:
    data = _add_difficulty_ordinal(df)
    X = data[CORRECTNESS_FEATURES]
    y = data["is_correct"]
    X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42, stratify=y)

    preprocessor = ColumnTransformer(
        transformers=[
            ("cat", OneHotEncoder(handle_unknown="ignore"), ["category"]),
            ("num", StandardScaler(), ["difficulty_ord", "points", "time_taken_sec", "prior_accuracy"]),
        ]
    )
    pipe = Pipeline(
        [("pre", preprocessor), ("clf", LogisticRegression(max_iter=1000))]
    )
    pipe.fit(X_train, y_train)
    auc = float(roc_auc_score(y_test, pipe.predict_proba(X_test)[:, 1]))
    return pipe, auc


def _train_difficulty(df: pd.DataFrame) -> tuple[Pipeline, float]:
    X = df[DIFFICULTY_FEATURES]
    y = df["difficulty"]
    X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42, stratify=y)

    pipe = Pipeline(
        [
            ("scale", StandardScaler()),
            ("clf", RandomForestClassifier(n_estimators=200, max_depth=12, random_state=42, n_jobs=-1)),
        ]
    )
    pipe.fit(X_train, y_train)
    acc = float(accuracy_score(y_test, pipe.predict(X_test)))
    return pipe, acc


def _train_skill(df: pd.DataFrame) -> tuple[Any, str, float]:
    feats = aggregate_user_features(df)
    target_col = "latent" if "latent" in feats.columns else "overall_accuracy"

    # Map target to a 0-100 skill score.
    raw = feats[target_col].to_numpy(dtype=float)
    if target_col == "latent":
        skill = 100.0 / (1.0 + np.exp(-raw))  # logistic squash of latent ability
    else:
        skill = 100.0 * raw
    feats = feats.copy()
    feats["skill_target"] = skill

    X = feats[SKILL_FEATURES]
    y = feats["skill_target"]
    X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)

    if _HAS_XGB:
        model: Any = XGBRegressor(
            n_estimators=300, max_depth=4, learning_rate=0.08,
            subsample=0.9, colsample_bytree=0.9, random_state=42,
        )
        backend = "xgboost"
    else:
        model = GradientBoostingRegressor(n_estimators=300, max_depth=3, learning_rate=0.08, random_state=42)
        backend = "sklearn_gbr"

    model.fit(X_train, y_train)
    r2 = float(r2_score(y_test, model.predict(X_test)))
    return model, backend, r2


def train_models(data_dir: Path, models_dir: Path, n_users: int = 1200, seed: int = 42) -> ModelBundle:
    """Train all models on the dataset and persist a single bundle."""
    df = load_dataset(data_dir, n_users=n_users, seed=seed)

    correctness, auc = _train_correctness(df)
    difficulty, diff_acc = _train_difficulty(df)
    skill, backend, r2 = _train_skill(df)

    bundle = ModelBundle(
        correctness=correctness,
        difficulty=difficulty,
        skill=skill,
        skill_backend=backend,
        metrics={
            "correctness_auc": round(auc, 4),
            "difficulty_accuracy": round(diff_acc, 4),
            "skill_r2": round(r2, 4),
            "n_rows": int(len(df)),
        },
    )
    bundle.save(models_dir)
    return bundle


# --------------------------------------------------------------------------- #
# Inference helpers
# --------------------------------------------------------------------------- #
def predict_correctness(
    bundle: ModelBundle, category: str, difficulty: str, points: int,
    time_taken_sec: float, prior_accuracy: float,
) -> float:
    row = pd.DataFrame(
        [{
            "category": category,
            "difficulty_ord": DIFFICULTY_ORDINAL.get(difficulty, 1),
            "points": points,
            "time_taken_sec": time_taken_sec,
            "prior_accuracy": prior_accuracy,
        }]
    )
    return float(bundle.correctness.predict_proba(row)[0, 1])


def predict_difficulty(
    bundle: ModelBundle, points: int, avg_answer_seconds: float, global_success_rate: float,
) -> tuple[str, dict[str, float]]:
    row = pd.DataFrame(
        [{"points": points, "time_taken_sec": avg_answer_seconds, "global_success_rate": global_success_rate}]
    )
    proba = bundle.difficulty.predict_proba(row)[0]
    classes = list(bundle.difficulty.named_steps["clf"].classes_)
    probs = {cls: float(p) for cls, p in zip(classes, proba)}
    best = max(probs, key=probs.get)
    return best, probs


def estimate_skill(bundle: ModelBundle, feature_row: dict[str, float]) -> float:
    row = pd.DataFrame([feature_row])[SKILL_FEATURES]
    score = float(bundle.skill.predict(row)[0])
    return max(0.0, min(100.0, score))
