package ru.mobui.kanji.checker.shape

import ru.mobui.kanji.domain.practice.CompensationPipelineType

internal fun compensationPipeline(cfg: ShapeCheckConfig): CompensationPipeline =
    when (cfg.pipelineType) {
        CompensationPipelineType.Cascade -> ShapeCompensationPipeline(
            TranslationCompensator(cfg.maxTranslationPercent),
            SizeCompensator(cfg.maxSizeCompensationPercent),
            RotationCompensator(cfg.maxRotationDegrees),
            StretchCompensator(cfg.maxHorizontalStretchPercent, cfg.maxVerticalStretchPercent),
            JitterCompensator(cfg.jitterTolerancePercent),
            cfg.maxCompensationBudgetPercent,
            ShearCompensator(cfg.maxShearDegrees),
            cfg.enabledCompensations,
        )
        CompensationPipelineType.Procrustes -> ProcrustesShapeCompensationPipeline(
            jitter = JitterCompensator(cfg.jitterTolerancePercent),
            shear = ShearCompensator(cfg.maxShearDegrees),
            stretch = StretchCompensator(cfg.maxHorizontalStretchPercent, cfg.maxVerticalStretchPercent, preserveCenter = true),
            maxTranslationPercent = cfg.maxTranslationPercent,
            maxSizeCompensationPercent = cfg.maxSizeCompensationPercent,
            maxRotationDegrees = cfg.maxRotationDegrees,
            budgetLimitPercent = cfg.maxCompensationBudgetPercent,
            enabled = cfg.enabledCompensations,
        )
    }
