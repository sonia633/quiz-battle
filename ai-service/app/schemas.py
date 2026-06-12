"""Pydantic request/response models for the AI API."""
from __future__ import annotations

from pydantic import BaseModel, Field

from .config import CATEGORIES, DIFFICULTIES


# --------------------------------------------------------------------------- #
# Shared building blocks
# --------------------------------------------------------------------------- #
class CategoryPerformance(BaseModel):
    """A player's accuracy and volume in one category."""

    category: str = Field(..., examples=CATEGORIES)
    attempts: int = Field(..., ge=0)
    correct: int = Field(..., ge=0)

    @property
    def accuracy(self) -> float:
        return self.correct / self.attempts if self.attempts else 0.0


class PlayerProfile(BaseModel):
    """Aggregate signal about a player, supplied by the Java services."""

    user_id: int
    total_attempts: int = Field(0, ge=0)
    total_correct: int = Field(0, ge=0)
    avg_answer_seconds: float = Field(12.0, ge=0)
    per_category: list[CategoryPerformance] = Field(default_factory=list)


# --------------------------------------------------------------------------- #
# Difficulty prediction
# --------------------------------------------------------------------------- #
class DifficultyPredictRequest(BaseModel):
    points: int = Field(..., ge=10, le=1000, description="Base points for the question")
    avg_answer_seconds: float = Field(..., ge=0, description="Mean time players take")
    global_success_rate: float = Field(..., ge=0, le=1, description="Fraction answered correctly")


class DifficultyPredictResponse(BaseModel):
    difficulty: str = Field(..., examples=DIFFICULTIES)
    confidence: float
    probabilities: dict[str, float]


# --------------------------------------------------------------------------- #
# Skill estimation
# --------------------------------------------------------------------------- #
class SkillEstimateResponse(BaseModel):
    user_id: int
    skill_score: float = Field(..., ge=0, le=100)
    level: int
    overall_accuracy: float
    estimated_by: str


# --------------------------------------------------------------------------- #
# Weak-topic detection + learning suggestions
# --------------------------------------------------------------------------- #
class WeakTopic(BaseModel):
    category: str
    accuracy: float
    attempts: int
    gap_to_average: float


class LearningSuggestion(BaseModel):
    category: str
    recommended_difficulty: str
    message: str


class WeakTopicsResponse(BaseModel):
    user_id: int
    weak_topics: list[WeakTopic]
    suggestions: list[LearningSuggestion]


# --------------------------------------------------------------------------- #
# Recommendation
# --------------------------------------------------------------------------- #
class CandidateQuestion(BaseModel):
    question_id: int
    category: str
    difficulty: str
    points: int = 100


class RecommendRequest(BaseModel):
    profile: PlayerProfile
    candidates: list[CandidateQuestion]
    limit: int = Field(10, ge=1, le=100)


class RecommendedQuestion(BaseModel):
    question_id: int
    category: str
    difficulty: str
    predicted_success: float
    score: float
    reason: str


class RecommendResponse(BaseModel):
    user_id: int
    recommendations: list[RecommendedQuestion]
