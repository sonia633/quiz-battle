"""CLI to (re)train the AI models and persist the bundle.

Usage:
    python train.py
"""
from __future__ import annotations

import json

from app.config import settings
from app.ml import train_models


def main() -> None:
    print(f"Training models (users={settings.dataset_users}, seed={settings.dataset_seed}) ...")
    bundle = train_models(
        settings.data_dir, settings.models_dir,
        n_users=settings.dataset_users, seed=settings.dataset_seed,
    )
    print(f"Saved bundle to {settings.models_dir / 'model_bundle.joblib'}")
    print(f"Skill backend: {bundle.skill_backend}")
    print("Metrics:")
    print(json.dumps(bundle.metrics, indent=2))


if __name__ == "__main__":
    main()
