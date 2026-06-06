package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.data.db.FavoriteCity
import com.example.data.model.CurrentWeather
import com.example.data.model.GeocodingResult
import com.example.data.model.WeatherResponse
import com.example.ui.viewmodel.WeatherUiState
import com.example.ui.viewmodel.WeatherViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherDashboardScreen(
    viewModel: WeatherViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val favoriteCities by viewModel.favoriteCities.collectAsState()

    // Request permissions natively
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted) {
            viewModel.fetchWeatherFromGPS {
                Toast.makeText(context, "الرجاء تفعيل صلاحية الموقع الجغرافي بالهاتف.", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "لم يتم منح الصلاحية لتحديد الموقع الجغرافي آلياً.", Toast.LENGTH_SHORT).show()
        }
    }

    // Force RTL local layout direction for first class Arabic support
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        val currentCityName = when (val state = uiState) {
            is WeatherUiState.Success -> state.cityName
            else -> "جاري التحميل..."
        }

        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = Color(0xFF1A1C1E) // Premium Aesthetic Background (Vibrant Palette Theme)
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    Spacer(modifier = Modifier.height(16.dp))

                    // 1. Vibrant Top Bar / Design Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Location Indicator Badge: Rounded circle bg-[#D1E4FF] text-[#00315C]
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(Color(0xFFD1E4FF), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "📍",
                                    fontSize = 20.sp
                                )
                            }
                            Column {
                                Text(
                                    text = currentCityName,
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (-0.5).sp
                                )
                                Text(
                                    text = "تحديث: مباشر",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 12.sp
                                )
                            }
                        }

                        // GPS Device Location Button: bg-[#333538]
                        IconButton(
                            onClick = {
                                permissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .background(Color(0xFF333538), CircleShape),
                            colors = IconButtonDefaults.iconButtonColors(
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = "موقعي الحالي",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // 2. Search Section: bg-[#333538] with rounded borders
                    Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                        SearchSection(
                            query = searchQuery,
                            results = searchResults,
                            isSearching = isSearching,
                            onQueryChange = { viewModel.onSearchQueryChange(it) },
                            onCitySelect = { result ->
                                viewModel.fetchWeatherForLocation(
                                    latitude = result.latitude,
                                    longitude = result.longitude,
                                    cityName = result.name,
                                    country = result.country ?: result.admin1
                                )
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 3. Saved Cities Tags Ribbon
                    if (favoriteCities.isNotEmpty()) {
                        Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                            FavoritesRibbon(
                                favoriteCities = favoriteCities,
                                onFavoriteClick = { fav ->
                                    viewModel.fetchWeatherForLocation(
                                        latitude = fav.latitude,
                                        longitude = fav.longitude,
                                        cityName = fav.name,
                                        country = fav.country
                                    )
                                }
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Main App Content State Controller
                    when (val state = uiState) {
                        is WeatherUiState.Loading -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(350.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = Color(0xFFD1E4FF))
                            }
                        }

                        is WeatherUiState.Error -> {
                            Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                                ErrorCard(
                                    message = state.message,
                                    onRetry = {
                                        viewModel.fetchWeatherForLocation(30.0444, 31.2357, "القاهرة", "مصر")
                                    }
                                )
                            }
                        }

                        is WeatherUiState.Success -> {
                            SuccessContent(
                                state = state,
                                viewModel = viewModel
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchSection(
    query: String,
    results: List<GeocodingResult>,
    isSearching: Boolean,
    onQueryChange: (String) -> Unit,
    onCitySelect: (GeocodingResult) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF333538).copy(alpha = 0.6f)),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("ابحث عن مدينة عالمية...", color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "البحث", tint = Color.White, modifier = Modifier.size(20.dp)) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFD1E4FF),
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Color(0xFF232528),
                    unfocusedContainerColor = Color(0xFF232528)
                ),
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )

            if (isSearching) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color(0xFFD1E4FF), strokeWidth = 2.dp)
                }
            }

            if (results.isNotEmpty() && query.trim().length >= 2) {
                Spacer(modifier = Modifier.height(10.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 180.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    results.forEach { city ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onCitySelect(city) }
                                .padding(vertical = 10.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.LocationCity, contentDescription = null, tint = Color(0xFFD1E4FF), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = city.name,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = listOfNotNull(city.admin1, city.country).joinToString("، "),
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 11.sp
                                )
                            }
                        }
                        HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                    }
                }
            }
        }
    }
}

@Composable
fun FavoritesRibbon(
    favoriteCities: List<FavoriteCity>,
    onFavoriteClick: (FavoriteCity) -> Unit
) {
    Column {
        Text(
            text = "المدن المحفوظة:",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 2.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(favoriteCities) { city ->
                SuggestionChip(
                    onClick = { onFavoriteClick(city) },
                    label = { Text(city.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium) },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = Color(0xFF333538).copy(alpha = 0.5f)
                    ),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(12.dp),
                    icon = { Icon(Icons.Default.Favorite, contentDescription = null, tint = Color(0xFFFF5A5A), modifier = Modifier.size(12.dp)) }
                )
            }
        }
    }
}

@Composable
fun SuccessContent(
    state: WeatherUiState.Success,
    viewModel: WeatherViewModel
) {
    val current = state.weather.current
    val daily = state.weather.daily
    val hourly = state.weather.hourly

    if (current == null) {
        Box(modifier = Modifier.padding(20.dp)) {
            ErrorCard(message = "البيانات الحالية للطقس غير متوفرة لهذا الموقع.") {}
        }
        return
    }

    val arabicCondition = viewModel.translateWeatherCode(current.weatherCode)

    Spacer(modifier = Modifier.height(16.dp))

    // 1. Hero Weather Display
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        // Massive Backdrop Translucent Blurred Temperature
        Text(
            text = "${current.temperature2m.toInt()}",
            color = Color.White.copy(alpha = 0.04f),
            fontSize = 140.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.offset(y = (-30).dp)
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.zIndex(1f)
        ) {
            // Heart Fav & Edit Trigger at Top
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.clickable { viewModel.toggleFavorite() }
            ) {
                Text(
                    text = state.cityName,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = if (state.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "حفظ",
                    tint = if (state.isFavorite) Color(0xFFFF5A5A) else Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Large Temperature Display in light typography
            Text(
                text = "${current.temperature2m.toInt()}°",
                color = Color.White,
                fontSize = 80.sp,
                fontWeight = FontWeight.Light,
                textAlign = TextAlign.Center,
                lineHeight = 80.sp
            )

            // Weather Condition Highlighted in Light Blue `#D1E4FF`
            Text(
                text = arabicCondition,
                color = Color(0xFFD1E4FF),
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Weather Icon Component Graphic (Sun Orb + Blurred cloud glassmorphic overlay)
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .height(64.dp),
                contentAlignment = Alignment.Center
            ) {
                // Sun orb glowing backing
                Box(
                    modifier = Modifier
                        .size(45.dp)
                        .offset(x = (-12).dp, y = (-8).dp)
                        .background(Color(0xFFFFB300), CircleShape)
                )
                // Glassmorphic clouds
                Box(
                    modifier = Modifier
                        .size(width = 75.dp, height = 34.dp)
                        .offset(x = 10.dp, y = 10.dp)
                        .background(Color.White.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                        .border(0.5.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(20.dp))

    // 2. Horizontal Hourly Forecast
    if (hourly != null && hourly.time.isNotEmpty()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "توقعات الساعات القادمة",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 12.dp)
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val count = minOf(24, hourly.time.size)
                items((0 until count).toList()) { index ->
                    val timeStr = hourly.time[index]
                    val rawTemp = hourly.temperature2m[index]
                    val rCode = hourly.weatherCode[index]
                    val isFirst = index == 0
                    val hourLabel = if (isFirst) "الآن" else formatHour(timeStr)

                    // Active highlight `bg-[#D1E4FF] text-[#00315C]`, passive `bg-white/5`
                    val containerBg = if (isFirst) Color(0xFFD1E4FF) else Color.White.copy(alpha = 0.05f)
                    val mainColor = if (isFirst) Color(0xFF00315C) else Color.White
                    val subColor = if (isFirst) Color(0xFF00315C).copy(alpha = 0.7f) else Color.White.copy(alpha = 0.6f)

                    Column(
                        modifier = Modifier
                            .width(76.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(containerBg)
                            .border(1.dp, if (isFirst) Color.Transparent else Color.White.copy(alpha = 0.05f), RoundedCornerShape(24.dp))
                            .padding(vertical = 14.dp, horizontal = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = hourLabel,
                            color = subColor,
                            fontSize = 12.sp,
                            fontWeight = if (isFirst) FontWeight.Bold else FontWeight.Normal
                        )
                        // Weather condition Emoji
                        Text(
                            text = viewModel.translateWeatherCode(rCode).split(" ").last(),
                            fontSize = 24.sp
                        )
                        Text(
                            text = "${rawTemp.toInt()}°",
                            color = mainColor,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(28.dp))

    // 3. Immersive Bottom Panel (Color: `#2D2F33`, Rounded Corner Radius `40.dp`)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = Color(0xFF2D2F33),
                shape = RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp)
            )
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            
            // Details Grid structure
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                DetailItem(
                    icon = "💧",
                    label = "الرطوبة",
                    value = "${current.relativeHumidity2m}%",
                    modifier = Modifier.weight(1f)
                )
                DetailItem(
                    icon = "💨",
                    label = "الرياح",
                    value = "${current.windSpeed10m.toInt()} كم/س",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                DetailItem(
                    icon = "🌡️",
                    label = "المحسوسة",
                    value = "${current.apparentTemperature.toInt()}°مئوية",
                    modifier = Modifier.weight(1f)
                )
                DetailItem(
                    icon = "☁️",
                    label = "الغيوم",
                    value = "${current.cloudCover}%",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // AI Smart Advisor Integration Inside bottom pane
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.9f)),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "الذكاء الاصطناعي",
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "مستشار الطقس الذكي",
                                color = Color(0xFF38BDF8),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = { viewModel.triggerGeminiAdvice() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(text = "ولد نصيحة ✨", fontSize = 11.sp, color = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (state.isGeminiLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = Color(0xFF38BDF8), modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("جاري تدقيق البيانات الفضائية...", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                            }
                        }
                    } else if (!state.geminiAdvice.isNullOrEmpty()) {
                        Surface(
                            color = Color.White.copy(alpha = 0.04f),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = state.geminiAdvice,
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 13.sp,
                                lineHeight = 19.sp,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    } else {
                        Text(
                            text = "اطلب استشارة الذكاء الاصطناعي لتحليل الرياح، الرطوبة السائدة وتوجيه إرشادات صحية متميزة لليوم.",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            modifier = Modifier.padding(horizontal = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // 7 Days Daily Forecast list
            if (daily != null && daily.time.isNotEmpty()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "التوقعات للأيام المقبلة",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    daily.time.forEachIndexed { index, dateStr ->
                        val dCode = daily.weatherCode[index]
                        val maxT = daily.temperature2mMax[index]
                        val minT = daily.temperature2mMin[index]
                        val dayName = if (index == 0) "اليوم" else if (index == 1) "غداً" else getDayOfWeekArabic(dateStr)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = dayName,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )

                            // Weather code description + emoji
                            Text(
                                text = viewModel.translateWeatherCode(dCode),
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1.8f),
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 1
                            )

                            Row(
                                horizontalArrangement = Arrangement.End,
                                modifier = Modifier.weight(1.2f)
                            ) {
                                Text(
                                    text = "${maxT.toInt()}°",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "${minT.toInt()}°",
                                    color = Color.White.copy(alpha = 0.4f),
                                    fontSize = 12.sp
                                )
                            }
                        }

                        if (index < daily.time.size - 1) {
                            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Android Navigation Bar Gesture Indicator mockup rendering at very bottom
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .height(5.dp)
                    .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(2.5.dp))
                    .align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
fun DetailItem(
    icon: String,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(20.dp))
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(20.dp))
            .padding(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(Color.White.copy(alpha = 0.05f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = icon, fontSize = 20.sp)
            }
            Column {
                Text(
                    text = label,
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = value,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun ErrorCard(
    message: String,
    onRetry: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.ErrorOutline, contentDescription = "تنبيه", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(36.dp))
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onErrorContainer,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("إعادة المحاولة", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// Formats UTC API datetime String (e.g. "2026-06-06T15:00") into pleasant Arabic Hours
fun formatHour(timeStr: String): String {
    return try {
        // Sample: 2026-06-06T15:00
        val parts = timeStr.split("T")
        if (parts.size == 2) {
            val hourMin = parts[1] // "15:00"
            val hourInt = hourMin.split(":")[0].toIntOrNull() ?: 12
            when {
                hourInt == 0 -> "12 ص"
                hourInt == 12 -> "12 م"
                hourInt > 12 -> "${hourInt - 12} م"
                else -> "$hourInt ص"
            }
        } else {
            timeStr
        }
    } catch (e: Exception) {
        timeStr
    }
}

// Translate ISO dates (e.g. "2026-06-06") to Arabic friendly Day of Week Names
fun getDayOfWeekArabic(dateStr: String): String {
    return try {
        val formatInput = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
        val dateValue = formatInput.parse(dateStr)
        if (dateValue != null) {
            val formatOutput = SimpleDateFormat("EEEE", Locale("ar"))
            formatOutput.format(dateValue)
        } else {
            dateStr
        }
    } catch (e: Exception) {
        dateStr
    }
}
