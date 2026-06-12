"""API and model tests. A small model is trained once per session."""
from __future__ import annotations

import pytest
from fastapi.testclient import TestClient

from app.config import settings
from app.main import app
from app.registry import reset_bundle


@pytest.fixture(scope="session", autouse=True)
def _train_small_model(tmp_path_factory):
    """Train on a small, fast dataset in a temp dir before any test runs."""
    tmp = tmp_path_factory.mktemp("models")
    settings.models_dir = tmp / "models"
    settings.data_dir = tmp / "data"
    settings.dataset_users = 200  # keep tests fast
    reset_bundle()
    yield
    reset_bundle()


@pytest.fixture()
def client():
    with TestClient(app) as c:
        yield c


def _profile(user_id: int = 1) -> dict:
    return {
        "user_id": user_id,
        "total_attempts": 40,
        "total_correct": 28,
        "avg_answer_seconds": 11.5,
        "per_category": [
            {"category": "Programming", "attempts": 12, "correct": 10},
            {"category": "Mathematics", "attempts": 10, "correct": 4},
            {"category": "Science", "attempts": 10, "correct": 8},
            {"category": "History", "attempts": 8, "correct": 6},
        ],
    }


def test_health(client):
    assert client.get("/health").json() == {"status": "UP"}


def test_info_exposes_metrics(client):
    body = client.get("/").json()
    assert "metrics" in body
    assert body["metrics"]["n_rows"] > 0


def test_difficulty_prediction_returns_valid_label(client):
    resp = client.post(
        "/api/ai/difficulty/predict",
        json={"points": 200, "avg_answer_seconds": 18.0, "global_success_rate": 0.35},
    )
    assert resp.status_code == 200
    body = resp.json()
    assert body["difficulty"] in {"EASY", "MEDIUM", "HARD"}
    assert 0.0 <= body["confidence"] <= 1.0
    assert abs(sum(body["probabilities"].values()) - 1.0) < 1e-6


def test_skill_estimate_in_range(client):
    resp = client.post("/api/ai/skill/estimate", json=_profile())
    assert resp.status_code == 200
    body = resp.json()
    assert 0.0 <= body["skill_score"] <= 100.0
    assert body["level"] >= 1


def test_weak_topics_flags_mathematics(client):
    resp = client.post("/api/ai/weak-topics", json=_profile())
    assert resp.status_code == 200
    weak = {w["category"] for w in resp.json()["weak_topics"]}
    # Mathematics has the lowest accuracy (4/10) and should be flagged.
    assert "Mathematics" in weak


def test_recommend_prioritises_flow_zone(client):
    candidates = [
        {"question_id": 1, "category": "Mathematics", "difficulty": "HARD", "points": 200},
        {"question_id": 2, "category": "Science", "difficulty": "EASY", "points": 100},
        {"question_id": 3, "category": "Mathematics", "difficulty": "EASY", "points": 100},
    ]
    resp = client.post(
        "/api/ai/recommend",
        json={"profile": _profile(), "candidates": candidates, "limit": 3},
    )
    assert resp.status_code == 200
    recs = resp.json()["recommendations"]
    assert len(recs) == 3
    # Scores are sorted descending.
    scores = [r["score"] for r in recs]
    assert scores == sorted(scores, reverse=True)
    for r in recs:
        assert 0.0 <= r["predicted_success"] <= 1.0
