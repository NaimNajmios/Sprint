package com.najmi.sprint.core.ai.classifier

/**
 * Session classifier — to be implemented in Phase 3.
 *
 * Pipeline:
 * 1. Rule-based pre-filter (cheap, deterministic)
 * 2. LLM actor classification (Groq/Gemini Flash)
 * 3. LLM critic validation
 * 4. Confidence-based routing (auto-commit / queue for review)
 */
// TODO: Phase 3 — Implement classification engine with actor-critic pattern
