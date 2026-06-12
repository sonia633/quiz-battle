"""Runtime configuration and shared domain constants."""
from __future__ import annotations

from pathlib import Path

from pydantic_settings import BaseSettings, SettingsConfigDict

# Categories and difficulties mirror the Java quiz-service domain.
CATEGORIES: list[str] = [
    "Programming",
    "Mathematics",
    "Science",
    "History",
    "Geography",
    "Sports",
    "General Knowledge",
]

DIFFICULTIES: list[str] = ["EASY", "MEDIUM", "HARD"]
DIFFICULTY_ORDINAL: dict[str, int] = {d: i for i, d in enumerate(DIFFICULTIES)}

# Sweet-spot success probability for recommendations (the "flow zone"): hard
# enough to learn from, easy enough to stay motivated.
TARGET_SUCCESS_PROBABILITY: float = 0.70

BASE_DIR = Path(__file__).resolve().parent.parent
MODELS_DIR = BASE_DIR / "models"
DATA_DIR = BASE_DIR / "data"


class Settings(BaseSettings):
    """Environment-overridable settings (prefix QUIZAI_)."""

    model_config = SettingsConfigDict(env_prefix="QUIZAI_", extra="ignore")

    app_name: str = "Quiz Battle AI Service"
    host: str = "0.0.0.0"
    port: int = 8000
    models_dir: Path = MODELS_DIR
    data_dir: Path = DATA_DIR
    # Dataset size used when training from the synthetic generator.
    dataset_users: int = 1200
    dataset_seed: int = 42


settings = Settings()
