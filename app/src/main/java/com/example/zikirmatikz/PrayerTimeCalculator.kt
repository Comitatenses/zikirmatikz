package com.example.zikirmatikz

import java.util.Calendar
import java.util.TimeZone
import kotlin.math.*

/**
 * Namaz vakti hesaplayıcı.
 * Türkiye Diyanet İşleri yöntemine göre:
 *   - Sabah (Fajr): 18°
 *   - Yatsı (Isha): 17°
 */
object PrayerTimeCalculator {

    private const val FAJR_ANGLE = 18.0
    private const val ISHA_ANGLE = 17.0

    data class PrayerTimes(
        val fajr: Long,
        val dhuhr: Long,
        val asr: Long,
        val maghrib: Long,
        val isha: Long
    )

    fun calculate(lat: Double, lng: Double, cal: Calendar): PrayerTimes {
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        val day = cal.get(Calendar.DAY_OF_MONTH)

        val jd = julianDay(year, month, day)
        val tz = TimeZone.getDefault().getOffset(cal.timeInMillis) / 3600000.0

        val dhuhr   = solarNoon(jd, lng, tz)
        val fajr    = angleTime(jd, lat, lng, tz, FAJR_ANGLE, before = true)
        val asr     = asrTime(jd, lat, lng, tz, shadowFactor = 1)
        val maghrib = angleTime(jd, lat, lng, tz, 0.833, before = false)
        val isha    = angleTime(jd, lat, lng, tz, ISHA_ANGLE, before = false)

        return PrayerTimes(
            fajr    = toMillis(fajr, cal),
            dhuhr   = toMillis(dhuhr, cal),
            asr     = toMillis(asr, cal),
            maghrib = toMillis(maghrib, cal),
            isha    = toMillis(isha, cal)
        )
    }

    // --- Astronomik hesaplamalar ---

    private fun julianDay(year: Int, month: Int, day: Int): Double {
        var y = year
        var m = month
        if (m <= 2) { y--; m += 12 }
        val a = floor(y / 100.0)
        val b = 2 - a + floor(a / 4.0)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.5
    }

    /** Güneş deklinasyonu (derece) ve denklem-i zamanı (saat) döndürür */
    private fun sunPosition(jd: Double): Pair<Double, Double> {
        val d = jd - 2451545.0
        val g = rad(fixAngle(357.529 + 0.98560028 * d))   // ortalama anomali
        val q = fixAngle(280.459 + 0.98564736 * d)         // ortalama boylam (derece)
        val l = rad(fixAngle(q + 1.915 * sin(g) + 0.020 * sin(2 * g)))
        val e = rad(23.439 - 0.00000036 * d)
        // Dik açı yükselişi: derece → saat (÷15)
        val ra    = deg(atan2(cos(e) * sin(l), cos(l))) / 15.0
        val decl  = deg(asin(sin(e) * sin(l)))
        // Denklem-i zaman saatte: q/15 − ra (normalize edilmiş)
        val eqt   = q / 15.0 - fixHour(ra)
        return Pair(decl, eqt)
    }

    private fun solarNoon(jd: Double, lng: Double, tz: Double): Double {
        val (_, eqt) = sunPosition(jd)
        return 12.0 + tz - lng / 15.0 - eqt
    }

    private fun hourAngle(jd: Double, lat: Double, angle: Double): Double {
        val (decl, _) = sunPosition(jd)
        val cosVal = (-sin(rad(angle)) - sin(rad(lat)) * sin(rad(decl))) /
                     (cos(rad(lat)) * cos(rad(decl)))
        if (cosVal < -1.0 || cosVal > 1.0) return 0.0
        return deg(acos(cosVal)) / 15.0   // derece → saat
    }

    private fun angleTime(
        jd: Double, lat: Double, lng: Double, tz: Double,
        angle: Double, before: Boolean
    ): Double {
        val noon = solarNoon(jd, lng, tz)
        val ha   = hourAngle(jd, lat, angle)
        return if (before) noon - ha else noon + ha
    }

    private fun asrTime(
        jd: Double, lat: Double, lng: Double, tz: Double,
        shadowFactor: Int
    ): Double {
        val noon   = solarNoon(jd, lng, tz)
        val (decl, _) = sunPosition(jd)
        val target = deg(atan(1.0 / (shadowFactor + tan(rad(abs(lat - decl))))))
        val ha     = hourAngle(jd, lat, -target)
        return noon + ha
    }

    private fun toMillis(hours: Double, ref: Calendar): Long {
        val c = Calendar.getInstance(TimeZone.getDefault())
        c.set(Calendar.YEAR,         ref.get(Calendar.YEAR))
        c.set(Calendar.MONTH,        ref.get(Calendar.MONTH))
        c.set(Calendar.DAY_OF_MONTH, ref.get(Calendar.DAY_OF_MONTH))
        c.set(Calendar.HOUR_OF_DAY,  hours.toInt())
        c.set(Calendar.MINUTE,       ((hours - hours.toInt()) * 60).toInt())
        c.set(Calendar.SECOND,       0)
        c.set(Calendar.MILLISECOND,  0)
        return c.timeInMillis
    }

    private fun fixAngle(a: Double) = a - 360.0 * floor(a / 360.0)
    private fun fixHour(h: Double)  = h - 24.0  * floor(h / 24.0)
    private fun rad(d: Double) = d * PI / 180.0
    private fun deg(r: Double) = r * 180.0 / PI
}
