package com.example.zikirmatikz

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.example.zikirmatikz.ui.theme.ZikirmatikzTheme
import kotlinx.coroutines.delay
import java.util.Calendar

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ZikirmatikzTheme {
                ZikirmatikScreen()
            }
        }
    }
}

@Composable
fun ZikirmatikScreen() {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("zikirmatik", Context.MODE_PRIVATE)

    var count by remember { mutableIntStateOf(0) }
    var bildirimAktif by remember {
        mutableStateOf(prefs.getBoolean("bildirim_aktif", false))
    }
    var durumMesaji by remember { mutableStateOf("") }

    // Sonraki vakit geri sayımı
    var sonrakiVakit by remember { mutableStateOf("") }
    var kalanSure by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        while (true) {
            val lat = prefs.getFloat("lat", 0f).toDouble()
            val lng = prefs.getFloat("lng", 0f).toDouble()
            if (lat != 0.0 || lng != 0.0) {
                val now = System.currentTimeMillis()
                val cal = Calendar.getInstance()
                val times = PrayerTimeCalculator.calculate(lat, lng, cal)
                val prayerNames = listOf("Sabah", "Öğle", "İkindi", "Akşam", "Yatsı")
                val millis = listOf(times.fajr, times.dhuhr, times.asr, times.maghrib, times.isha)

                // Bugün kalan vakitler, yoksa yarının ilk vakti
                val next = millis.zip(prayerNames).firstOrNull { it.first > now }
                    ?: run {
                        val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
                        val t2 = PrayerTimeCalculator.calculate(lat, lng, tomorrow)
                        Pair(t2.fajr, "Sabah")
                    }

                val diff = next.first - now
                val totalMinutes = (diff + 59999) / 60000  // yukarı yuvarla
                val hours = totalMinutes / 60
                val minutes = totalMinutes % 60
                sonrakiVakit = next.second
                kalanSure = if (hours > 0) "${hours}s ${minutes}dk" else "${minutes}dk"
            }
            delay(10_000L)
        }
    }

    val hedef = 33
    val tamamlandi = count > 0 && count % hedef == 0

    // Konum + bildirim izni launcher
    val konumIzniLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { verildi ->
        if (verildi) {
            konumAlVePlanla(context) { mesaj ->
                durumMesaji = mesaj
            }
        } else {
            durumMesaji = "Konum izni gerekli"
            prefs.edit().putBoolean("bildirim_aktif", false).apply()
            bildirimAktif = false
        }
    }

    val bildirimIzniLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { verildi ->
        if (verildi) {
            // Bildirim izni alındı, şimdi konum iste
            konumIzniLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            durumMesaji = "Bildirim izni gerekli"
            prefs.edit().putBoolean("bildirim_aktif", false).apply()
            bildirimAktif = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1B5E20))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Başlık
        Text(
            text = "Zikirmatik",
            color = Color(0xFFFFD54F),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 32.dp)
        )

        // Sonraki vakit kartı
        if (sonrakiVakit.isNotEmpty()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF2E7D32))
                    .padding(vertical = 12.dp)
            ) {
                Text(
                    text = "Sonraki Vakit: $sonrakiVakit",
                    color = Color(0xFFA5D6A7),
                    fontSize = 14.sp
                )
                Text(
                    text = kalanSure,
                    color = Color(0xFFFFD54F),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Hedef bilgisi
        Text(
            text = "Hedef: $hedef | Tur: ${count / hedef}",
            color = Color(0xFFA5D6A7),
            fontSize = 16.sp
        )

        // Sayaç dairesi
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(200.dp)
                .clip(CircleShape)
                .background(if (tamamlandi) Color(0xFFFFD54F) else Color(0xFF2E7D32))
        ) {
            Text(
                text = (count % hedef).toString(),
                color = if (tamamlandi) Color(0xFF1B5E20) else Color.White,
                fontSize = 72.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }

        if (tamamlandi) {
            Text(
                text = "SubhanAllah! $hedef tamamlandı",
                color = Color(0xFFFFD54F),
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
        } else {
            Spacer(modifier = Modifier.height(32.dp))
        }

        // Zikir butonu
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(160.dp)
                .clip(CircleShape)
                .background(Color(0xFF4CAF50))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { count++ }
        ) {
            Text(
                text = "ZİKİR",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Ezan bildirimi toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF2E7D32))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Ezan Bildirimi",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                if (durumMesaji.isNotEmpty()) {
                    Text(
                        text = durumMesaji,
                        color = Color(0xFFA5D6A7),
                        fontSize = 12.sp
                    )
                } else {
                    Text(
                        text = "5 dk önce hatırlatma",
                        color = Color(0xFFA5D6A7),
                        fontSize = 12.sp
                    )
                }
            }
            Switch(
                checked = bildirimAktif,
                onCheckedChange = { yeniDeger ->
                    bildirimAktif = yeniDeger
                    prefs.edit().putBoolean("bildirim_aktif", yeniDeger).apply()
                    if (yeniDeger) {
                        // İzinleri iste ve planla
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            if (ContextCompat.checkSelfPermission(
                                    context, Manifest.permission.POST_NOTIFICATIONS
                                ) != PackageManager.PERMISSION_GRANTED
                            ) {
                                bildirimIzniLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                konumIzniLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                            }
                        } else {
                            konumIzniLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                        }
                    } else {
                        PrayerScheduler.cancel(context)
                        durumMesaji = ""
                    }
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFFFFD54F),
                    checkedTrackColor = Color(0xFF4CAF50)
                )
            )
        }

        // Sıfırla butonu
        TextButton(
            onClick = { count = 0 },
            modifier = Modifier.padding(bottom = 24.dp)
        ) {
            Text(
                text = "Sıfırla",
                color = Color(0xFFA5D6A7),
                fontSize = 18.sp
            )
        }
    }
}

private fun konumAlVePlanla(context: Context, onSonuc: (String) -> Unit) {
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    try {
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val prefs = context.getSharedPreferences("zikirmatik", Context.MODE_PRIVATE)
                prefs.edit()
                    .putFloat("lat", location.latitude.toFloat())
                    .putFloat("lng", location.longitude.toFloat())
                    .apply()
                PrayerScheduler.schedule(context)
                onSonuc("Aktif - vakitler planlandı")
            } else {
                onSonuc("Konum alınamadı, dışarı çıkıp tekrar dene")
            }
        }.addOnFailureListener {
            onSonuc("Konum hatası: ${it.message}")
        }
    } catch (e: SecurityException) {
        onSonuc("Konum izni verilmedi")
    }
}
