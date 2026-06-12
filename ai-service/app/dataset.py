"""Synthetic quiz-attempt dataset.

Mirrors the schema of public Kaggle quiz datasets (one row per answered
question, with user/question features and a correctness label) but is generated
deterministically so training is reproducible and requires no network access.
To train on a real Kaggle CSV instead, drop a file with the same columns at
``data/attempts.csv`` and ``load_dataset`` will use it.
"""
from __future__ import annotations

from pathlib import Path

import numpy as np
import pandas as pd

from .config import CATEGORIES, DIFFICULTIES, DIFFICULTY_ORDINAL

COLUMNS = [
    "user_id",
    "category",
    "difficulty",
    "points",
    "time_taken_sec",
    "prior_accuracy",
    "global_success_rate",
    "user_latent_skill",
    "is_correct",
]


def _sigmoid(x: np.ndarray) -> np.ndarray:
    return 1.0 / (1.0 + np.exp(-x))


def generate_dataset(n_users: int = 1200, seed: int = 42) -> pd.DataFrame:
    """Generate a labelled attempt dataset with realistic latent structure.

    Each user has a latent overall skill plus per-category aptitude; each
    question's difficulty raises the bar. Correctness is drawn from a logistic
    model of (skill + aptitude - difficulty - time_pressure), so the trained
    models recover genuine signal rather than noise.
    """
    rng = np.random.default_rng(seed)
    n_cat = len(CATEGORIES)

    user_skill = rng.normal(0.0, 1.0, size=n_users)
    user_aptitude = rng.normal(0.0, 0.6, size=(n_users, n_cat))

    rows: list[dict] = []
    for user_id in range(1, n_users + 1):
        skill = user_skill[user_id - 1]
        n_attempts = int(rng.integers(15, 60))
        running_correct = 0
        for attempt in range(1, n_attempts + 1):
            cat_idx = int(rng.integers(0, n_cat))
            category = CATEGORIES[cat_idx]
            diff_idx = int(rng.integers(0, len(DIFFICULTIES)))
            difficulty = DIFFICULTIES[diff_idx]

            # Points rise with difficulty but overlap substantially across
            # levels (a wide spread), so points alone cannot perfectly identify
            # difficulty — the classifier must combine it with timing and rate.
            points = int(np.clip(100 + diff_idx * 40 + rng.normal(0, 28), 10, 1000))
            # Higher difficulty and lower skill => slower answers, on average.
            base_time = 8 + diff_idx * 5
            time_taken = float(max(2.0, rng.normal(base_time - skill, 3.0)))

            prior_accuracy = running_correct / max(1, attempt - 1)

            logit = (
                1.4 * skill
                + 1.1 * user_aptitude[user_id - 1, cat_idx]
                - 1.3 * diff_idx
                - 0.05 * (time_taken - base_time)
                + 0.6  # baseline: questions are answerable more often than not
            )
            p_correct = float(_sigmoid(np.array([logit]))[0])
            is_correct = int(rng.random() < p_correct)
            running_correct += is_correct

            rows.append(
                {
                    "user_id": user_id,
                    "category": category,
                    "difficulty": difficulty,
                    "points": points,
                    "time_taken_sec": round(time_taken, 2),
                    "prior_accuracy": round(prior_accuracy, 4),
                    # Filled in after the loop from question-difficulty means.
                    "global_success_rate": np.nan,
                    # Latent ability used to generate labels; the skill model
                    # learns to recover it from behaviour alone (no leakage).
                    "user_latent_skill": round(float(skill), 4),
                    "is_correct": is_correct,
                }
            )

    df = pd.DataFrame(rows, columns=COLUMNS)

    # Each row's "global success rate" is the difficulty-level mean perturbed by
    # per-question noise. It correlates with difficulty (so it is a useful
    # feature) without being a deterministic function of it (which would leak the
    # label and make the difficulty classifier trivially perfect).
    diff_success = df.groupby("difficulty")["is_correct"].transform("mean").to_numpy()
    noise = rng.normal(0.0, 0.09, size=len(df))
    df["global_success_rate"] = np.clip(diff_success + noise, 0.02, 0.98).round(4)
    return df


def load_dataset(data_dir: Path, n_users: int = 1200, seed: int = 42) -> pd.DataFrame:
    """Load ``data/attempts.csv`` if present, otherwise generate and cache it."""
    data_dir.mkdir(parents=True, exist_ok=True)
    csv_path = data_dir / "attempts.csv"
    if csv_path.exists():
        return pd.read_csv(csv_path)

    df = generate_dataset(n_users=n_users, seed=seed)
    df.to_csv(csv_path, index=False)
    return df
