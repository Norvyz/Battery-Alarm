package com.batteryalarm.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Un punto (nivel de batería en un momento) dentro de una sesión de carga. */
data class ChargePoint(
    val level: Int,
    val ts: Long,
    val elapsedSec: Long
)

/**
 * Sesión de carga completa. Se guarda localmente (nunca se envía a ningún
 * servidor) y deja preparados los datos mínimos para dibujar una gráfica
 * de porcentaje vs. tiempo en el futuro.
 */
data class ChargeSession(
    val startTime: Long,
    val startLevel: Int,
    val endTime: Long? = null,
    val endLevel: Int? = null,
    val reachedTarget: Boolean = false,
    val avgPowerW: Double? = null,
    val maxPowerW: Double? = null,
    val maxTempTenths: Int? = null,
    val points: List<ChargePoint> = emptyList()
)

/** Guarda localmente las sesiones de carga terminadas. */
object ChargeHistory {

    private const val PREFS = "charge_history"
    private const val KEY_SESSIONS = "sessions"

    private const val MAX_SESSIONS = 20
    private const val MAX_POINTS = 240

    fun save(context: Context, session: ChargeSession) {
        if (session.startTime <= 0L) return
        val cappedSession = if (session.points.size > MAX_POINTS) {
            session.copy(points = session.points.takeLast(MAX_POINTS))
        } else {
            session
        }
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val list = try {
            JSONArray(prefs.getString(KEY_SESSIONS, "[]") ?: "[]")
        } catch (_: Exception) {
            JSONArray()
        }
        list.put(cappedSession.toJson())
        while (list.length() > MAX_SESSIONS) list.remove(0)
        prefs.edit().putString(KEY_SESSIONS, list.toString()).apply()
    }

    fun sessions(context: Context): List<ChargeSession> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val list = try {
            JSONArray(prefs.getString(KEY_SESSIONS, "[]") ?: "[]")
        } catch (_: Exception) {
            return emptyList()
        }
        val result = ArrayList<ChargeSession>(list.length())
        for (i in 0 until list.length()) {
            try {
                (list.getJSONObject(i).toSession())?.let { result.add(it) }
            } catch (_: Exception) {
            }
        }
        return result
    }

    private fun ChargeSession.toJson(): JSONObject = JSONObject().apply {
        put("startTime", startTime)
        put("startLevel", startLevel)
        put("endTime", endTime ?: JSONObject.NULL)
        put("endLevel", endLevel ?: JSONObject.NULL)
        put("reachedTarget", reachedTarget)
        put("avgPowerW", avgPowerW ?: JSONObject.NULL)
        put("maxPowerW", maxPowerW ?: JSONObject.NULL)
        put("maxTempTenths", maxTempTenths ?: JSONObject.NULL)
        val arr = JSONArray()
        points.forEach { p ->
            arr.put(JSONObject().apply {
                put("l", p.level)
                put("t", p.ts)
                put("e", p.elapsedSec)
            })
        }
        put("points", arr)
    }

    private fun JSONObject.toSession(): ChargeSession? {
        val startTime = optLong("startTime").takeIf { it > 0L } ?: return null
        val arr = optJSONArray("points") ?: JSONArray()
        val points = ArrayList<ChargePoint>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            points.add(ChargePoint(o.optInt("l"), o.optLong("t"), o.optLong("e")))
        }
        return ChargeSession(
            startTime = startTime,
            startLevel = optInt("startLevel"),
            endTime = if (isNull("endTime")) null else optLong("endTime"),
            endLevel = if (isNull("endLevel")) null else optInt("endLevel"),
            reachedTarget = optBoolean("reachedTarget"),
            avgPowerW = if (isNull("avgPowerW")) null else optDouble("avgPowerW"),
            maxPowerW = if (isNull("maxPowerW")) null else optDouble("maxPowerW"),
            maxTempTenths = if (isNull("maxTempTenths")) null else optInt("maxTempTenths"),
            points = points
        )
    }
}