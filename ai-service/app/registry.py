"""Lazy-loaded singleton holding the trained model bundle."""
from __future__ import annotations

import logging

from .config import settings
from .ml import MODEL_FILE, ModelBundle, train_models

logger = logging.getLogger("quizai.registry")

_bundle: ModelBundle | None = None


def get_bundle() -> ModelBundle:
    """Return the loaded bundle, training one on first use if none exists."""
    global _bundle
    if _bundle is not None:
        return _bundle

    model_path = settings.models_dir / MODEL_FILE
    if model_path.exists():
        logger.info("Loading model bundle from %s", model_path)
        _bundle = ModelBundle.load(settings.models_dir)
    else:
        logger.warning("No model bundle found; training a fresh one (first run)")
        _bundle = train_models(
            settings.data_dir, settings.models_dir,
            n_users=settings.dataset_users, seed=settings.dataset_seed,
        )
        logger.info("Trained models: %s", _bundle.metrics)
    return _bundle


def reset_bundle() -> None:
    """Drop the cached bundle so the next call reloads/retrains (used by tests)."""
    global _bundle
    _bundle = None
