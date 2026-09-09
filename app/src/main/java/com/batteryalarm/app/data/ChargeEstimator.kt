package com.batteryalarm.app.data

/**
 * Estima el tiempo restante hasta el 100 % usando el progreso real de la
 * batería durante la sesión (por ejemplo: subió del 73 % al 80 % en X minutos).
 *
 * La estimación se recalcula constantemente y nunca muestra precisión falsa:
 * si no hay suficientes datos devuelve `null` (la UI muestra "Calculando…").
 */
object ChargeEstimator {

    private const val MIN_SAMPLES = 2
    private const val MIN_PROGRESS_PCT = 3
    private const val MIN_ELAPSED_MS = 90_000L
    private const val MAX_ETA_MIN = 720

    fun estimateMinutes(points: List<ChargePoint>, currentLevel: Int, now: Long): Int? {
        if (points.size < MIN_SAMPLES) return null

        val first = points.first()
        val last = points.last()
        val elapsedMs = last.ts - first.ts
        if (elapsedMs < MIN_ELAPSED_MS) return null

        val gained = last.level - first.level
        if (gained < MIN_PROGRESS_PCT) return null

        val pctPerMin = gained.toDouble() * 60_000.0 / elapsedMs
        if (pctPerMin <= 0.0) return null

        val remaining = (100 - currentLevel).coerceAtLeast(0)
        val minutes = kotlin.math.ceil(remaining / pctPerMin).toInt()
        return minutes.coerceIn(0, MAX_ETA_MIN)
    }
}