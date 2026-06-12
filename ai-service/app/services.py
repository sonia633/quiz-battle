"""Higher-level AI features composed from the trained models."""
from __future__ import annotations

from .config import CATEGORIES, DIFFICULTIES, TARGET_SUCCESS_PROBABILITY
from .ml import ModelBundle, SKILL_FEATURES, estimate_skill, predict_correctness
from .schemas import (
    LearningSuggestion,
    PlayerProfile,
    RecommendedQuestion,
    RecommendRequest,
    SkillEstimateResponse,
    WeakTopic,
    WeakTopicsResponse,
)

# A weak topic must have at least this many attempts to be trustworthy.
MIN_ATTEMPTS_FOR_WEAKNESS = 3


def _overall_accuracy(profile: PlayerProfile) -> float:
    return profile.total_correct / profile.total_attempts if profile.total_attempts else 0.0


def _skill_feature_row(profile: PlayerProfile) -> dict[str, float]:
    """Build the skill model's feature vector from a player profile."""
    row: dict[str, float] = {
        "overall_accuracy": _overall_accuracy(profile),
        "avg_time": profile.avg_answer_seconds,
        "total_attempts": float(profile.total_attempts),
    }
    by_cat = {cp.category: cp.accuracy for cp in profile.per_category}
    for cat in CATEGORIES:
        row[f"acc_{cat}"] = by_cat.get(cat, 0.0)
    return row


def estimate_player_skill(bundle: ModelBundle, profile: PlayerProfile) -> SkillEstimateResponse:
    score = estimate_skill(bundle, _skill_feature_row(profile))
    return SkillEstimateResponse(
        user_id=profile.user_id,
        skill_score=round(score, 2),
        level=int(score // 20) + 1,  # five broad tiers
        overall_accuracy=round(_overall_accuracy(profile), 4),
        estimated_by=bundle.skill_backend,
    )


def _recommended_difficulty(skill_score: float) -> str:
    if skill_score < 40:
        return "EASY"
    if skill_score < 70:
        return "MEDIUM"
    return "HARD"


def detect_weak_topics(bundle: ModelBundle, profile: PlayerProfile) -> WeakTopicsResponse:
    """Flag categories whose accuracy trails the player's own average.

    Using the player's own mean (rather than a fixed threshold) adapts to strong
    and weak players alike; only categories with enough attempts are considered.
    """
    eligible = [cp for cp in profile.per_category if cp.attempts >= MIN_ATTEMPTS_FOR_WEAKNESS]
    if not eligible:
        return WeakTopicsResponse(user_id=profile.user_id, weak_topics=[], suggestions=[])

    mean_acc = sum(cp.accuracy for cp in eligible) / len(eligible)
    skill = estimate_skill(bundle, _skill_feature_row(profile))

    weak: list[WeakTopic] = []
    suggestions: list[LearningSuggestion] = []
    for cp in sorted(eligible, key=lambda c: c.accuracy):
        # Weak if notably below the player's own mean, or simply below 60%.
        if cp.accuracy < mean_acc - 0.10 or cp.accuracy < 0.60:
            weak.append(
                WeakTopic(
                    category=cp.category,
                    accuracy=round(cp.accuracy, 4),
                    attempts=cp.attempts,
                    gap_to_average=round(mean_acc - cp.accuracy, 4),
                )
            )
            # Practise a notch below the player's global difficulty to rebuild.
            diff = _recommended_difficulty(max(0.0, skill - 15))
            suggestions.append(
                LearningSuggestion(
                    category=cp.category,
                    recommended_difficulty=diff,
                    message=(
                        f"Your {cp.category} accuracy is {cp.accuracy:.0%}. "
                        f"Try {diff.lower()} {cp.category} quizzes to close the gap."
                    ),
                )
            )

    return WeakTopicsResponse(user_id=profile.user_id, weak_topics=weak, suggestions=suggestions)


def recommend_questions(bundle: ModelBundle, request: RecommendRequest) -> list[RecommendedQuestion]:
    """Rank candidates by how close their predicted success is to the flow zone.

    Questions in weak categories get a small boost so practice is steered toward
    a player's gaps without abandoning the optimal-challenge principle.
    """
    profile = request.profile
    prior = _overall_accuracy(profile)
    weak_categories = {wt.category for wt in detect_weak_topics(bundle, profile).weak_topics}

    scored: list[RecommendedQuestion] = []
    for q in request.candidates:
        p_success = predict_correctness(
            bundle,
            category=q.category,
            difficulty=q.difficulty,
            points=q.points,
            time_taken_sec=profile.avg_answer_seconds,
            prior_accuracy=prior,
        )
        # Closeness to the target success probability (1.0 = perfect challenge).
        closeness = 1.0 - abs(p_success - TARGET_SUCCESS_PROBABILITY)
        boost = 0.12 if q.category in weak_categories else 0.0
        score = closeness + boost

        if q.category in weak_categories:
            reason = f"Targets a weak topic ({q.category}); predicted success {p_success:.0%}"
        else:
            reason = f"Well-matched challenge; predicted success {p_success:.0%}"

        scored.append(
            RecommendedQuestion(
                question_id=q.question_id,
                category=q.category,
                difficulty=q.difficulty,
                predicted_success=round(p_success, 4),
                score=round(score, 4),
                reason=reason,
            )
        )

    scored.sort(key=lambda r: r.score, reverse=True)
    return scored[: request.limit]
