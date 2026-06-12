"""FastAPI application exposing the AI features."""
from __future__ import annotations

import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from . import __version__
from .config import settings
from .ml import predict_difficulty, train_models
from .registry import get_bundle, reset_bundle
from .schemas import (
    DifficultyPredictRequest,
    DifficultyPredictResponse,
    PlayerProfile,
    RecommendRequest,
    RecommendResponse,
    SkillEstimateResponse,
    WeakTopicsResponse,
)
from .services import detect_weak_topics, estimate_player_skill, recommend_questions

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(name)s %(message)s")
logger = logging.getLogger("quizai")


@asynccontextmanager
async def lifespan(app: FastAPI):
    # Warm up (load or train) the models before serving traffic.
    bundle = get_bundle()
    logger.info("AI service ready. Model metrics: %s", bundle.metrics)
    yield


app = FastAPI(
    title=settings.app_name,
    version=__version__,
    description="Recommendations, difficulty prediction, skill estimation and "
                "weak-topic detection for the Quiz Battle Platform.",
    lifespan=lifespan,
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.get("/health", tags=["System"])
def health() -> dict:
    return {"status": "UP"}


@app.get("/", tags=["System"])
def info() -> dict:
    bundle = get_bundle()
    return {
        "service": settings.app_name,
        "version": __version__,
        "skill_backend": bundle.skill_backend,
        "metrics": bundle.metrics,
    }


@app.post("/api/ai/difficulty/predict", response_model=DifficultyPredictResponse, tags=["AI"])
def difficulty_predict(req: DifficultyPredictRequest) -> DifficultyPredictResponse:
    bundle = get_bundle()
    label, probs = predict_difficulty(
        bundle, req.points, req.avg_answer_seconds, req.global_success_rate
    )
    return DifficultyPredictResponse(
        difficulty=label, confidence=round(probs[label], 4), probabilities=probs
    )


@app.post("/api/ai/skill/estimate", response_model=SkillEstimateResponse, tags=["AI"])
def skill_estimate(profile: PlayerProfile) -> SkillEstimateResponse:
    return estimate_player_skill(get_bundle(), profile)


@app.post("/api/ai/weak-topics", response_model=WeakTopicsResponse, tags=["AI"])
def weak_topics(profile: PlayerProfile) -> WeakTopicsResponse:
    return detect_weak_topics(get_bundle(), profile)


@app.post("/api/ai/recommend", response_model=RecommendResponse, tags=["AI"])
def recommend(req: RecommendRequest) -> RecommendResponse:
    recs = recommend_questions(get_bundle(), req)
    return RecommendResponse(user_id=req.profile.user_id, recommendations=recs)


@app.post("/api/ai/retrain", tags=["System"])
def retrain() -> dict:
    """Retrain all models from the dataset and hot-swap the bundle."""
    bundle = train_models(
        settings.data_dir, settings.models_dir,
        n_users=settings.dataset_users, seed=settings.dataset_seed,
    )
    reset_bundle()
    return {"status": "retrained", "metrics": bundle.metrics}
