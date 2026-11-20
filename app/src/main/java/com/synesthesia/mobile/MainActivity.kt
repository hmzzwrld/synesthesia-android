package com.synesthesia.mobile

import android.Manifest
import android.media.MediaMetadataRetriever
import android.content.ContentUris
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import coil.compose.AsyncImage
import dev.chrisbanes.haze.HazeInputScale
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import androidx.compose.ui.res.painterResource
import com.synesthesia.mobile.ui.theme.SynesthesiaAtmosphere
import com.synesthesia.mobile.ui.theme.glassSurface
import com.synesthesia.mobile.util.ColorExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val Ink = Color(0xFF0B0B13)
private val Violet = Color(0xFF7655A8)
private val Teal = Color(0xFF568D9A)
private val Lavender = Color(0xFFC4A8FF)
private val White = Color(0xFFF4F0FF)
private val Muted = Color(0xFFAAA2BE)

data class LocalTrack(val id: Long, val title: String, val artist: String, val album: String, val uri: Uri, val artwork: Uri?, val duration: Long, val genre: String, val path: String)
private enum class Tab(val label: String) { HOME("Home"), SEARCH("Search"), LIBRARY("Library") }
private enum class LibraryTab(val label: String) { SONGS("Songs"), ALBUMS("Albums"), ARTISTS("Artists"), GENRES("Genres"), FOLDERS("Folders") }

class MainActivity : ComponentActivity() {
    private var player: ExoPlayer? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightNavigationBars = false
        player = ExoPlayer.Builder(this).build()
        setContent { SynesthesiaApp(player!!) }
    }
    override fun onDestroy() { player?.release(); player = null; super.onDestroy() }
}

@Composable
private fun SynesthesiaApp(player: ExoPlayer) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var tracks by remember { mutableStateOf<List<LocalTrack>>(emptyList()) }
    var tab by remember { mutableStateOf(Tab.HOME) }
    var libraryTab by remember { mutableStateOf(LibraryTab.SONGS) }
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<LocalTrack?>(null) }
    var playing by remember { mutableStateOf(false) }
    var expandedPlayer by remember { mutableStateOf(false) }
    var liquidQueueOpen by remember { mutableStateOf(false) }
    var scanning by remember { mutableStateOf(false) }
    var scanMessage by remember { mutableStateOf("Scan your device to find music") }
    var playCounts by remember { mutableStateOf<Map<Long, Int>>(emptyMap()) }
    var recentIds by remember { mutableStateOf<List<Long>>(emptyList()) }
    var monthlyCounts by remember { mutableStateOf<Map<Long, Int>>(emptyMap()) }
    val prefs = remember { context.getSharedPreferences("synesthesia_stats", 0) }
    LaunchedEffect(Unit) {
        playCounts = prefs.all.filterKeys { it.startsWith("plays_") }.mapNotNull { (k,v) -> k.removePrefix("plays_").toLongOrNull()?.let { it to (v as? Int ?: 0) } }.toMap()
        recentIds = prefs.getString("recent", "")!!.split(',').mapNotNull { it.toLongOrNull() }
        val monthKey = java.text.SimpleDateFormat("yyyyMM", java.util.Locale.US).format(java.util.Date())
        monthlyCounts = prefs.all.filterKeys { it.startsWith("month_${monthKey}_") }.mapNotNull { (k,v) -> k.removePrefix("month_${monthKey}_").toLongOrNull()?.let { it to (v as? Int ?: 0) } }.toMap()
    }
    fun play(track: LocalTrack) {
        selected = track
        val index = tracks.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        player.setMediaItems(tracks.map { MediaItem.Builder().setUri(it.uri).setMediaId(it.id.toString()).build() }, index, 0L)
        player.prepare(); player.play(); playing = true
        val count = (playCounts[track.id] ?: 0) + 1
        playCounts = playCounts + (track.id to count)
        recentIds = (listOf(track.id) + recentIds.filterNot { it == track.id }).take(30)
        val monthKey = java.text.SimpleDateFormat("yyyyMM", java.util.Locale.US).format(java.util.Date())
        val monthCount = (monthlyCounts[track.id] ?: 0) + 1
        monthlyCounts = monthlyCounts + (track.id to monthCount)
        prefs.edit().putInt("plays_${track.id}", count).putInt("month_${monthKey}_${track.id}", monthCount).putString("recent", recentIds.joinToString(",")).apply()
    }
    DisposableEffect(player, tracks) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) { playing = isPlaying }
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val id = mediaItem?.mediaId?.toLongOrNull()
                if (id != null) selected = tracks.firstOrNull { it.id == id } ?: selected
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }
    suspend fun scan() {
        scanning = true; scanMessage = "Scanning local audio…"
        val result = withContext(Dispatchers.IO) {
            val list = mutableListOf<LocalTrack>()
            val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            val projection = arrayOf(MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.ALBUM, MediaStore.Audio.Media.DURATION, MediaStore.Audio.Media.DATA, MediaStore.Audio.Media.ALBUM_ID)
            val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
            context.contentResolver.query(collection, projection, selection, null, "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC")?.use { c ->
                val idC=c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID); val titleC=c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE); val artistC=c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST); val albumC=c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM); val durC=c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION); val dataC=c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA); val albumIdC=c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                while(c.moveToNext()) {
                    val id=c.getLong(idC); val albumId=c.getLong(albumIdC); val uri=ContentUris.withAppendedId(collection,id)
                    val art=ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"),albumId)
                    val genre = runCatching {
                        val retriever = MediaMetadataRetriever()
                        try { retriever.setDataSource(context, uri); retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE)?.takeIf { it.isNotBlank() } ?: "Unknown genre" }
                        finally { retriever.release() }
                    }.getOrDefault("Unknown genre")
                    list.add(LocalTrack(id,c.getString(titleC)?:"Unknown title",c.getString(artistC)?.takeIf{it!="<unknown>"}?:"Unknown artist",c.getString(albumC)?.takeIf{it!="<unknown>"}?:"Unknown album",uri,art,c.getLong(durC),genre,c.getString(dataC)?:""))
                }
            }; list
        }
        tracks = result; scanning = false; scanMessage = if(result.isEmpty()) "No music found. Add MP3, FLAC, M4A or other audio files and scan again." else "Found ${result.size} tracks on this device"
    }
    val permission = if(Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE
    var hasPermission by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> hasPermission=granted; if(granted) scope.launch { scan() } }
    LaunchedEffect(hasPermission) { if(hasPermission && tracks.isEmpty()) scan() }
    var ambientPalette by remember { mutableStateOf(Triple(Color(0xFFA855F7), Color(0xFFEC4899), Color(0xFF6366F1))) }
    var activeAtmosphere by remember { mutableStateOf("album") }
    fun chooseAtmosphere(name: String) {
        activeAtmosphere = name
        ambientPalette = when (name) {
            "purple" -> Triple(Color(0xFFA855F7), Color(0xFFEC4899), Color(0xFF6366F1))
            "electric" -> Triple(Color(0xFF38BDF8), Color(0xFF2563EB), Color(0xFF22D3EE))
            "emerald" -> Triple(Color(0xFF34D399), Color(0xFF059669), Color(0xFFA3E635))
            "warm" -> Triple(Color(0xFFFB923C), Color(0xFFFF3D8B), Color(0xFFFACC15))
            else -> ambientPalette
        }
    }
    LaunchedEffect(selected?.artwork) {
        ambientPalette = ColorExtractor.extractPaletteFromUri(
            context, selected?.artwork,
            Color(0xFFA855F7), Color(0xFFEC4899), Color(0xFF6366F1)
        )
        activeAtmosphere = "album"
    }
    val glassHaze = remember { HazeState() }
    // Animate the complete artwork palette once, then share it with the background and glass surfaces.
    val ambientA by androidx.compose.animation.animateColorAsState(ambientPalette.first, tween(1800), label="ambient-a")
    val ambientB by androidx.compose.animation.animateColorAsState(ambientPalette.second, tween(1800), label="ambient-b")
    val ambientC by androidx.compose.animation.animateColorAsState(ambientPalette.third, tween(1800), label="ambient-c")
    val displayedPalette = Triple(ambientA, ambientB, ambientC)
    MaterialTheme(colorScheme = darkColorScheme(primary=ambientPalette.first, background=Ink, surface=Color(0xFF12121E), onSurface=White)) {
        SynesthesiaAtmosphere(ambA=displayedPalette.first, ambB=displayedPalette.second, ambC=displayedPalette.third) {
            Box(Modifier.fillMaxSize()) {
                // Capture the page behind floating controls so their surfaces can use real backdrop blur.
                Box(Modifier.fillMaxSize().haze(state = glassHaze)) {
                // Website reference: fixed background image, adaptive colour wash, ambient glow and fine mesh.
                Image(
                    painter = painterResource(id = R.drawable.synesthesia_background),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().blur(1.2.dp),
                    alpha = 0.82f
                )
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.linearGradient(
                            0f to ambientPalette.third.copy(alpha = 0.20f),
                            0.48f to Color(0xFF05050D).copy(alpha = 0.18f),
                            1f to ambientPalette.first.copy(alpha = 0.12f)
                        )
                    )
                )
                // The background artwork is drawn after SynesthesiaAtmosphere's base layer,
                // so place the album-reactive colour wash ABOVE the artwork to keep it visible.
                Box(Modifier.fillMaxSize().background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF7C3AED).copy(alpha = 0.13f), Color.Transparent),
                        center = androidx.compose.ui.geometry.Offset(340f, 720f), radius = 1450f
                    )
                ))
                Box(Modifier.fillMaxSize().background(
                    Brush.radialGradient(
                        colors = listOf(displayedPalette.first.copy(alpha = 0.14f), Color.Transparent),
                        center = androidx.compose.ui.geometry.Offset(180f, 520f), radius = 1050f
                    )
                ))
                Box(Modifier.fillMaxSize().background(
                    Brush.radialGradient(
                        colors = listOf(displayedPalette.second.copy(alpha = 0.09f), Color.Transparent),
                        center = androidx.compose.ui.geometry.Offset(760f, 1180f), radius = 900f
                    )
                ))
                Box(Modifier.fillMaxSize().background(
                    Brush.radialGradient(
                        colors = listOf(displayedPalette.third.copy(alpha = 0.12f), Color.Transparent),
                        center = androidx.compose.ui.geometry.Offset(520f, 1680f), radius = 1000f
                    )
                ))
                // Fine dotted glass mesh from the website, intentionally subtle on mobile.
                androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                    val step = 24.dp.toPx()
                    val dot = Color.White.copy(alpha = 0.025f)
                    var y = 0f
                    while (y < size.height) {
                        var x = 0f
                        while (x < size.width) {
                            drawCircle(dot, radius = 0.65.dp.toPx(), center = androidx.compose.ui.geometry.Offset(x, y))
                            x += step
                        }
                        y += step
                    }
                }
                Column(Modifier.fillMaxSize().systemBarsPadding().padding(horizontal=16.dp)) {
                    Row(Modifier.fillMaxWidth().padding(top=12.dp,bottom=14.dp), verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.SpaceBetween) {
                        Column {
                            Text("SYNESTHESIA", color=White, fontSize=17.sp, fontWeight=FontWeight.Black, letterSpacing=3.sp)
                            Text("YOUR SOUND. YOUR WORLD.", color=Muted, fontSize=9.sp, letterSpacing=2.sp)
                        }
                        IconButton(onClick={ if(hasPermission) scope.launch { scan() } else launcher.launch(permission) }) {
                            Icon(Icons.Rounded.Refresh, "Rescan music", tint=ambientPalette.first)
                        }
                    }
                    AnimatedContent(
                        tab,
                        transitionSpec={ fadeIn(tween(240))+slideInVertically(spring(dampingRatio=.82f,stiffness=260f)) { it/14 } togetherWith fadeOut(tween(150))+slideOutVertically(tween(180)) { -it/20 } },
                        label="main-tabs",
                        modifier=Modifier.weight(1f).fillMaxWidth()
                    ) { destination ->
                        when(destination) {
                            Tab.HOME -> HomeScreen(tracks, recentIds, playCounts, monthlyCounts, selected, ::play, {tab=Tab.LIBRARY}, scanMessage)
                            Tab.SEARCH -> SearchScreen(tracks, query, {query=it}, ::play)
                            Tab.LIBRARY -> LibraryScreen(tracks, libraryTab, {libraryTab=it}, query, {query=it}, ::play)
                        }
                    }
                }
                } // End captured background/content layer.
                if(expandedPlayer && selected!=null) PlayerSheet(selected!!, playing, { if(player.isPlaying) player.pause() else player.play() }, {player.seekToPreviousMediaItem()}, {player.seekToNextMediaItem()}, {expandedPlayer=false}, player, tracks, { expandedPlayer=false; liquidQueueOpen=true })
                // Website layout: independent floating island and bottom navigation, over the scroll layer.
                Column(
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().padding(horizontal=14.dp).padding(bottom=8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    AnimatedVisibility(selected!=null, enter=fadeIn()+slideInVertically { it/3 }, exit=fadeOut()+slideOutVertically { it/3 }) {
                        MiniPlayer(selected, playing, { if(player.isPlaying) player.pause() else player.play() }, {expandedPlayer=true}, { liquidQueueOpen=true }, glassHaze, displayedPalette, activeAtmosphere, ::chooseAtmosphere)
                    }
                    Navigation(tab,{tab=it}, glassHaze)
                }
                AnimatedVisibility(visible=liquidQueueOpen,enter=fadeIn(tween(160))+slideInVertically(spring(dampingRatio=.78f,stiffness=360f)){it/5},exit=fadeOut(tween(130))+slideOutVertically(tween(180)){it/3}) {
                    LiquidQueueSheet(player, tracks, displayedPalette, glassHaze, { liquidQueueOpen=false })
                }
                if(!hasPermission) PermissionPrompt { launcher.launch(permission) }
                if(scanning) Box(Modifier.fillMaxSize().background(Color.Black.copy(.35f)), contentAlignment=Alignment.Center) { CircularProgressIndicator(color=ambientPalette.first) }
            }
        }
    }
}

@Composable private fun HomeScreen(tracks:List<LocalTrack>, recent:List<Long>, counts:Map<Long,Int>, monthly:Map<Long,Int>, selected:LocalTrack?, onPlay:(LocalTrack)->Unit, openLibrary:()->Unit, message:String) {
    val recentTracks=recent.mapNotNull { id -> tracks.find{it.id==id} }
    val most=tracks.sortedByDescending { counts[it.id]?:0 }.filter { (counts[it.id]?:0)>0 }.take(10)
    val monthTop=tracks.sortedByDescending { monthly[it.id]?:0 }.filter { (monthly[it.id]?:0)>0 }.take(10)
    LazyColumn(verticalArrangement=Arrangement.spacedBy(16.dp), contentPadding=PaddingValues(bottom=190.dp)) {
        item { Column { Text("Good music, good mood.", color=White,fontSize=25.sp,fontWeight=FontWeight.Bold); Text(message,color=Muted,fontSize=12.sp) } }
        item { SectionHeader("YOUR ROTATION", "MOST PLAYED"); if(most.isEmpty()) EmptyCard("Your most-played tracks will appear here as you listen.") else most.take(5).forEach { TrackRow(it,onPlay,counts[it.id]?:0) } }
        item { SectionHeader("PICK UP WHERE YOU LEFT", "RECENTLY PLAYED"); if(recentTracks.isEmpty()) EmptyCard("Your listening history will build automatically.") else recentTracks.take(6).forEach { TrackRow(it,onPlay) } }
        item { SectionHeader("MONTHLY REPLAY", "YOUR TOP 10"); if(monthTop.isEmpty()) EmptyCard("Your monthly ranking starts with your listening activity.") else monthTop.forEachIndexed { i,t -> TrackRow(t,onPlay,monthly[t.id]?:0,rank=i+1) } }
        item { SectionHeader("YOUR COLLECTION", "LIBRARY"); GlassCard(Modifier.fillMaxWidth().clickable(onClick=openLibrary).padding(18.dp)) { Row(verticalAlignment=Alignment.CenterVertically) { Icon(Icons.Rounded.LibraryMusic,null,tint=Lavender); Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)){Text("Explore your music",color=White,fontWeight=FontWeight.SemiBold);Text("Songs · Albums · Artists · Genres · Folders",color=Muted,fontSize=11.sp)}; Icon(Icons.Rounded.ChevronRight,null,tint=Muted) } } }
    }
}

@Composable private fun SearchScreen(tracks:List<LocalTrack>, query:String, change:(String)->Unit, play:(LocalTrack)->Unit) {
    Column { Text("Find your sound",color=White,fontSize=24.sp,fontWeight=FontWeight.Bold); Spacer(Modifier.height(12.dp)); OutlinedTextField(query,change,Modifier.fillMaxWidth(),placeholder={Text("Search songs, artists, albums…",color=Muted)},leadingIcon={Icon(Icons.Rounded.Search,null,tint=Lavender)},singleLine=true,shape=RoundedCornerShape(20.dp),colors=OutlinedTextFieldDefaults.colors(focusedBorderColor=Lavender,unfocusedBorderColor=Color.White.copy(.16f),focusedTextColor=White,unfocusedTextColor=White)); LazyColumn(contentPadding=PaddingValues(bottom=190.dp)) { items(tracks.filter{query.isBlank() || listOf(it.title,it.artist,it.album).any{s->s.contains(query,true)}}) { TrackRow(it,play) } } }
}

@Composable private fun LibraryScreen(tracks:List<LocalTrack>, selected:LibraryTab, choose:(LibraryTab)->Unit, query:String, change:(String)->Unit, play:(LocalTrack)->Unit) {
    Column { Text("Your Library",color=White,fontSize=25.sp,fontWeight=FontWeight.Bold); Spacer(Modifier.height(10.dp)); Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) { LibraryTab.values().forEach { item -> FilterChip(selected==item,{choose(item)},label={Text(item.label,fontSize=11.sp)},colors=FilterChipDefaults.filterChipColors(selectedContainerColor=Violet.copy(.35f),labelColor=Muted,selectedLabelColor=White)) } }; Spacer(Modifier.height(8.dp)); when(selected) {
        LibraryTab.SONGS -> LazyColumn(contentPadding=PaddingValues(bottom=190.dp)) { items(tracks.filter{query.isBlank()||it.title.contains(query,true)||it.artist.contains(query,true)}){TrackRow(it,play)} }
        LibraryTab.ALBUMS -> LazyColumn(contentPadding=PaddingValues(bottom=190.dp)) { items(tracks.groupBy{it.album}.toList().sortedBy{it.first}) { (name,songs) -> CollectionRow(name,songs.first().artist,"${songs.size} tracks",songs.first().artwork){songs.firstOrNull()?.let(play)} } }
        LibraryTab.ARTISTS -> LazyColumn(contentPadding=PaddingValues(bottom=190.dp)) { items(tracks.groupBy{it.artist}.toList().sortedBy{it.first}) { (name,songs) -> CollectionRow(name,"Artist","${songs.size} tracks",songs.first().artwork){songs.firstOrNull()?.let(play)} } }
        LibraryTab.GENRES -> LazyColumn(contentPadding=PaddingValues(bottom=190.dp)) { items(tracks.groupBy{it.genre}.toList().sortedBy{it.first}) { (name,songs) -> CollectionRow(name,"Genre","${songs.size} tracks",songs.first().artwork){songs.firstOrNull()?.let(play)} } }
        LibraryTab.FOLDERS -> LazyColumn(contentPadding=PaddingValues(bottom=190.dp)) { items(tracks.groupBy{it.path.substringBeforeLast('/',"/")}.toList().sortedBy{it.first}) { (name,songs) -> CollectionRow(name.substringAfterLast('/').ifBlank{"Music"},name,"${songs.size} tracks",songs.first().artwork){songs.firstOrNull()?.let(play)} } }
    } }
}

@Composable private fun TrackRow(track:LocalTrack,onPlay:(LocalTrack)->Unit,plays:Int?=null,rank:Int?=null) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(19.dp))
        .background(Brush.horizontalGradient(listOf(Color.White.copy(.075f),Color.White.copy(.025f),Color.Transparent)))
        .border(.6.dp,Color.White.copy(.065f),RoundedCornerShape(19.dp))
        .clickable{onPlay(track)}.padding(horizontal=9.dp,vertical=9.dp),verticalAlignment=Alignment.CenterVertically) {
        Box(Modifier.size(48.dp).clip(RoundedCornerShape(15.dp)).border(1.dp,Color.White.copy(.14f),RoundedCornerShape(15.dp))) { Artwork(track.artwork,48) }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text((rank?.let{"$it  ·  "}?:"")+track.title,color=White,fontWeight=FontWeight.SemiBold,fontSize=13.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
            Text(track.artist+" · "+track.album,color=Muted,fontSize=10.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
        }
        if(plays!=null) Text("${plays}×",color=Lavender,fontSize=10.sp)
        Box(Modifier.padding(start=8.dp).size(31.dp).clip(CircleShape).background(Color.White.copy(.075f)).border(1.dp,Color.White.copy(.12f),CircleShape),contentAlignment=Alignment.Center) {
            Icon(Icons.Rounded.PlayArrow,null,tint=Lavender,modifier=Modifier.size(19.dp))
        }
    }
}
@Composable private fun CollectionRow(title:String,subtitle:String,detail:String,art:Uri?,click:()->Unit){ Row(Modifier.fillMaxWidth().clickable(onClick=click).padding(vertical=8.dp),verticalAlignment=Alignment.CenterVertically){Artwork(art,50);Spacer(Modifier.width(12.dp));Column(Modifier.weight(1f)){Text(title,color=White,fontWeight=FontWeight.SemiBold);Text(subtitle,color=Muted,fontSize=11.sp)};Text(detail,color=Muted,fontSize=10.sp)} }
@Composable private fun Artwork(uri:Uri?,size:Int){ Box(Modifier.size(size.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF282139)),contentAlignment=Alignment.Center){ if(uri!=null) AsyncImage(uri,null,Modifier.fillMaxSize()) else Icon(Icons.Rounded.Album,null,tint=Lavender,modifier=Modifier.size((size/2).dp)) } }
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable private fun MiniPlayer(track:LocalTrack?,playing:Boolean,toggle:()->Unit,expand:()->Unit,openQueue:()->Unit,hazeState:HazeState,ambientPalette:Triple<Color,Color,Color>,activeAtmosphere:String,onAtmosphere:(String)->Unit){
    if(track==null)return
    val podShape = RoundedCornerShape(999.dp)
    // A short elastic width pulse mirrors the website's springy island expansion.
    var trackChanged by remember { mutableStateOf(false) }
    LaunchedEffect(track.id) {
        trackChanged = true
        kotlinx.coroutines.delay(180)
        trackChanged = false
    }
    val podScaleX by animateFloatAsState(
        targetValue = if(trackChanged) 1.035f else 1f,
        animationSpec = spring(dampingRatio=.58f, stiffness=420f),
        label = "pod-elastic-x"
    )
    val podScaleY by animateFloatAsState(
        targetValue = if(trackChanged) .965f else 1f,
        animationSpec = spring(dampingRatio=.58f, stiffness=420f),
        label = "pod-elastic-y"
    )
    val podA by androidx.compose.animation.animateColorAsState(targetValue=ambientPalette.first, animationSpec=tween(1800), label="pod-album-a")
    val podB by androidx.compose.animation.animateColorAsState(targetValue=ambientPalette.second, animationSpec=tween(1800), label="pod-album-b")
    val podC by androidx.compose.animation.animateColorAsState(targetValue=ambientPalette.third, animationSpec=tween(1800), label="pod-album-c")
    val targetMetaWidth = (track.title.length * 7.1f).coerceIn(100f,140f).dp
    val metaWidth by animateDpAsState(
        targetValue = targetMetaWidth,
        animationSpec = spring(dampingRatio=.62f, stiffness=340f),
        label = "pod-title-width"
    )
    Row(
        Modifier
            .widthIn(max=390.dp)
            .wrapContentWidth()
            .animateContentSize(spring(dampingRatio=.62f, stiffness=340f))
            .graphicsLayer { this.scaleX = podScaleX; this.scaleY = podScaleY }
            .shadow(20.dp, podShape, ambientColor=podA.copy(.22f), spotColor=podC.copy(.12f))
            // Clip BEFORE the backdrop effect and again afterwards: this prevents Haze's
            // rectangular capture layer from leaking beyond the pill's rounded silhouette.
            .clip(podShape)
            .hazeChild(state = hazeState) {
                blurRadius = 14.dp
                backgroundColor = Color.Transparent
                noiseFactor = 0.018f
                inputScale = HazeInputScale.Fixed(0.66f)
            }
            .clip(podShape)
            // Website material: dark translucent base, liquid fill, ambient tint and gloss.
            .background(Color(0xFF0E0E18).copy(.42f))
            .background(Brush.linearGradient(listOf(Color.White.copy(.14f),Color.White.copy(.04f),Color(0xFF141222).copy(.38f))))
            .background(Brush.radialGradient(listOf(podA.copy(.16f),Color.Transparent),radius=460f,center=androidx.compose.ui.geometry.Offset(70f,70f)))
            .background(Brush.radialGradient(listOf(podB.copy(.14f),Color.Transparent),radius=360f,center=androidx.compose.ui.geometry.Offset(330f,120f)))
            .background(Brush.radialGradient(listOf(podC.copy(.12f),Color.Transparent),radius=420f,center=androidx.compose.ui.geometry.Offset(220f,180f)))
            .background(Brush.radialGradient(listOf(Color.White.copy(.23f),Color.Transparent),radius=250f,center=androidx.compose.ui.geometry.Offset(100f,-35f)))
            .border(1.dp,Brush.verticalGradient(listOf(Color.White.copy(.42f),Color.White.copy(.17f),Color.White.copy(.07f))),podShape)
            .combinedClickable(onClick=expand,onLongClick=openQueue)
            .padding(start=13.dp,end=18.dp,top=13.dp,bottom=13.dp),
        verticalAlignment=Alignment.CenterVertically
    ) {
        Box(Modifier.size(46.dp).border(1.5.dp,Color.White.copy(.23f),CircleShape).padding(3.dp)
            .clip(CircleShape).background(Color.White.copy(.08f))) {
            Artwork(track.artwork,40)
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.width(metaWidth).padding(vertical=1.dp)) {
            Text(track.title,color=White,fontSize=12.sp,fontWeight=FontWeight.SemiBold,maxLines=1,overflow=TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            Text(track.artist,color=Muted,fontSize=10.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(14.dp))
        Box(Modifier.size(34.dp).shadow(5.dp,CircleShape).clip(CircleShape)
            .background(Brush.radialGradient(listOf(Color.White,Color(0xFFE5E0F0),Color(0xFFCFCBE0))))
            .clickable(onClick=toggle),contentAlignment=Alignment.Center) {
            Icon(if(playing)Icons.Rounded.Pause else Icons.Rounded.PlayArrow,"Play or pause",tint=Color(0xFF171522),modifier=Modifier.size(21.dp))
        }
        Spacer(Modifier.width(4.dp))
        Row(
            Modifier.padding(start=5.dp).height(22.dp),
            horizontalArrangement=Arrangement.spacedBy(5.dp),
            verticalAlignment=Alignment.CenterVertically
        ) {
            val options = listOf(
                "purple" to Color(0xFFA855F7),
                "electric" to Color(0xFF38BDF8),
                "emerald" to Color(0xFF34D399),
                "warm" to Color(0xFFFB923C)
            )
            options.forEach { (name, color) ->
                Box(
                    Modifier.size(if(activeAtmosphere==name) 13.dp else 11.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(if(activeAtmosphere==name) 1.5.dp else 0.7.dp, Color.White.copy(.8f), CircleShape)
                        .clickable { onAtmosphere(name) }
                )
            }
        }
    }
}


@Composable private fun LiquidQueueSheet(player:ExoPlayer, library:List<LocalTrack>, palette:Triple<Color,Color,Color>, hazeState:HazeState, close:()->Unit) {
    var queueRevision by remember { mutableIntStateOf(0) }
    var queueBookmarked by remember { mutableStateOf(false) }
    val items = remember(player, library, queueRevision) {
        (0 until player.mediaItemCount).mapNotNull { i ->
            val id=player.getMediaItemAt(i).mediaId.toLongOrNull()
            library.firstOrNull { it.id==id }
        }
    }
    val organicShape = RoundedCornerShape(
        topStart=56.dp, topEnd=56.dp, bottomStart=46.dp, bottomEnd=46.dp
    )
    val accent = palette.first
    Box(Modifier.fillMaxSize().background(Color.Black.copy(.18f)).clickable(onClick=close)) {
        // Match the website's floating 420px / 48dvh queue, with the player-connected droplets.
        Box(
            Modifier.align(Alignment.BottomCenter)
                .fillMaxWidth().padding(horizontal=12.dp).padding(bottom=260.dp)
                .widthIn(max=420.dp).fillMaxHeight(.50f)
        ) {
            Box(Modifier.align(Alignment.BottomCenter).offset(y=12.dp).size(16.dp).clip(CircleShape)
                .background(Brush.linearGradient(listOf(Color.White.copy(.22f),Color(0xFF1E1A30).copy(.72f))))
                .border(1.dp,Color.White.copy(.22f),CircleShape))
            Box(Modifier.align(Alignment.BottomCenter).offset(y=25.dp).size(8.dp).clip(CircleShape)
                .background(Color.White.copy(.14f)).border(1.dp,Color.White.copy(.2f),CircleShape))
            Box(
                Modifier.fillMaxSize()
                    .shadow(28.dp,organicShape,ambientColor=accent.copy(.20f),spotColor=Color.Black.copy(.56f))
                    .clip(organicShape)
                    .hazeChild(state=hazeState) {
                        // The web queue uses blur(30px) saturate(180%).
                        blurRadius=30.dp
                        backgroundColor=Color.Transparent
                        noiseFactor=0.018f
                        inputScale=HazeInputScale.Fixed(0.66f)
                    }
                    .clip(organicShape)
                    // Darker material keeps list content legible while retaining the blurred background.
                    .background(Color(0xFF0E0C1A).copy(.46f))
                    .background(Brush.linearGradient(listOf(Color.White.copy(.14f),Color.White.copy(.035f),Color(0xFF0E0C1A).copy(.30f))))
                    .background(Brush.radialGradient(listOf(palette.first.copy(.20f),Color.Transparent),radius=850f,center=androidx.compose.ui.geometry.Offset(500f,1200f)))
                    .background(Brush.radialGradient(listOf(palette.second.copy(.12f),Color.Transparent),radius=620f,center=androidx.compose.ui.geometry.Offset(0f,0f)))
                    .background(Brush.radialGradient(listOf(palette.third.copy(.10f),Color.Transparent),radius=500f,center=androidx.compose.ui.geometry.Offset(300f,-60f)))
                    .border(1.dp,Brush.verticalGradient(listOf(Color.White.copy(.42f),Color.White.copy(.17f),Color.White.copy(.07f))),organicShape)
            ) {
                // Website's lq-sheen: broad top reflection and a faint lower-right reflection.
                Box(Modifier.fillMaxSize().background(
                    Brush.radialGradient(listOf(Color.White.copy(.20f),Color.Transparent),radius=430f,center=androidx.compose.ui.geometry.Offset(210f,0f))
                ))
                Box(Modifier.fillMaxSize().background(
                    Brush.radialGradient(listOf(Color.White.copy(.07f),Color.Transparent),radius=300f,center=androidx.compose.ui.geometry.Offset(900f,1500f))
                ))
                Column(
                    Modifier.fillMaxSize().padding(start=12.dp,end=12.dp,top=22.dp,bottom=12.dp),
                    verticalArrangement=Arrangement.spacedBy(3.dp)
                ) {
                    // Header mirrors the website: title, count pill, save/clear tools, close orb.
                    Row(
                        Modifier.fillMaxWidth().padding(start=16.dp,end=16.dp,bottom=12.dp),
                        verticalAlignment=Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("LIQUID QUEUE",color=accent,fontSize=9.sp,fontWeight=FontWeight.Bold,letterSpacing=2.sp)
                            Text("Up Next",color=White,fontSize=21.sp,fontWeight=FontWeight.ExtraBold)
                        }
                        Box(
                            Modifier.clip(RoundedCornerShape(999.dp))
                                .background(Color.White.copy(.075f))
                                .border(1.dp,Color.White.copy(.13f),RoundedCornerShape(999.dp))
                                .padding(horizontal=11.dp,vertical=6.dp)
                        ) {
                            Text("${items.size} tracks",color=Muted,fontSize=11.sp)
                        }
                        Spacer(Modifier.width(7.dp))
                        Box(
                            Modifier.size(34.dp).clip(CircleShape)
                                .background(Color.White.copy(if(queueBookmarked) .16f else .075f))
                                .border(1.dp,Color.White.copy(.16f),CircleShape)
                                .clickable { queueBookmarked=!queueBookmarked },
                            contentAlignment=Alignment.Center
                        ) {
                            Icon(if(queueBookmarked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                                "Bookmark queue",tint=if(queueBookmarked) accent else White,modifier=Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(7.dp))
                        Box(
                            Modifier.size(34.dp).clip(CircleShape)
                                .background(Color.White.copy(.075f))
                                .border(1.dp,Color.White.copy(.16f),CircleShape)
                                .clickable(enabled=player.mediaItemCount>player.currentMediaItemIndex+1) {
                                    for(i in player.mediaItemCount-1 downTo player.currentMediaItemIndex+1) player.removeMediaItem(i)
                                    queueRevision++
                                },
                            contentAlignment=Alignment.Center
                        ) {
                            Icon(Icons.Rounded.DeleteOutline,"Clear added tracks",tint=White,modifier=Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(7.dp))
                        Box(
                            Modifier.size(42.dp).clip(CircleShape)
                                .background(Brush.radialGradient(listOf(Color.White.copy(.25f),Color.White.copy(.055f))))
                                .border(1.dp,Color.White.copy(.20f),CircleShape)
                                .clickable(onClick=close),
                            contentAlignment=Alignment.Center
                        ) { Icon(Icons.Rounded.Close,"Close queue",tint=White,modifier=Modifier.size(20.dp)) }
                    }
                    LazyColumn(
                        Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement=Arrangement.spacedBy(2.dp),
                        contentPadding=PaddingValues(start=4.dp,top=2.dp,end=4.dp,bottom=8.dp)
                    ) {
                        items(items.size,key={index -> items[index].id}) { index ->
                            val item=items[index]
                            val current=index==player.currentMediaItemIndex
                            var dragDistance by remember(index,queueRevision) { mutableFloatStateOf(0f) }
                            Row(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                                    .background(if(current)
                                        Brush.horizontalGradient(listOf(accent.copy(.20f),Color.White.copy(.045f)))
                                    else Brush.horizontalGradient(listOf(Color.White.copy(.018f),Color.Transparent)))
                                    .pointerInput(index,queueRevision) {
                                        detectDragGesturesAfterLongPress(
                                            onDragEnd={dragDistance=0f},
                                            onDragCancel={dragDistance=0f},
                                            onDrag={change, amount ->
                                                change.consume()
                                                dragDistance += amount.y
                                                val step=if(dragDistance>58f) 1 else if(dragDistance < -58f) -1 else 0
                                                val target=index+step
                                                if(step!=0 && target>player.currentMediaItemIndex && target in 0 until player.mediaItemCount) {
                                                    player.moveMediaItem(index,target)
                                                    queueRevision++
                                                    dragDistance=0f
                                                }
                                            }
                                        )
                                    }
                                    .clickable { player.seekToDefaultPosition(index); player.play() }
                                    .padding(start=7.dp,end=12.dp,top=7.dp,bottom=7.dp),
                                verticalAlignment=Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.DragHandle,null,tint=Muted.copy(.85f),modifier=Modifier.size(22.dp))
                                Spacer(Modifier.width(8.dp))
                                Box(Modifier.size(46.dp).clip(CircleShape)
                                    .border(1.dp,Color.White.copy(.15f),CircleShape)) { Artwork(item.artwork,46) }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(item.title,color=White,fontSize=14.sp,fontWeight=FontWeight.SemiBold,
                                        maxLines=2,overflow=TextOverflow.Ellipsis,lineHeight=17.sp)
                                    Text(item.artist,color=Muted,fontSize=12.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
                                }
                                Spacer(Modifier.width(8.dp))
                                Text(formatDuration(item.duration),color=Muted,fontSize=11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable private fun PlayerSheet(
    track: LocalTrack,
    playing: Boolean,
    toggle: () -> Unit,
    previous: () -> Unit,
    next: () -> Unit,
    close: () -> Unit,
    player: ExoPlayer,
    queue: List<LocalTrack>,
    openQueue: () -> Unit
) {
    var progress by remember { mutableFloatStateOf(0f) }
    var shuffle by remember { mutableStateOf(player.shuffleModeEnabled) }
    var repeat by remember { mutableIntStateOf(player.repeatMode) }
    LaunchedEffect(player) {
        while (true) {
            val duration = player.duration
            if (duration > 0) progress = (player.currentPosition.toFloat() / duration).coerceIn(0f, 1f)
            kotlinx.coroutines.delay(350)
        }
    }
    val nextTrack = queue.getOrNull(player.currentMediaItemIndex + 1)
    val panelShape = RoundedCornerShape(48.dp)
    val tetherPulse by androidx.compose.animation.core.rememberInfiniteTransition(label="player-pod-tether")
        .animateFloat(initialValue=.72f,targetValue=1f,animationSpec=androidx.compose.animation.core.infiniteRepeatable(
            animation=tween(1500),repeatMode=androidx.compose.animation.core.RepeatMode.Reverse
        ),label="player-pod-tether-pulse")
    Box(Modifier.fillMaxSize()) {
        // Artwork-derived atmosphere sits behind the same liquid surface used by the website.
        if (track.artwork != null) {
            AsyncImage(model=track.artwork, contentDescription=null, contentScale=ContentScale.Crop,
                modifier=Modifier.fillMaxSize().blur(64.dp), alpha=.48f)
        }
        Box(Modifier.fillMaxSize().background(Brush.radialGradient(
            listOf(Color(0xFF7655A8).copy(.22f), Color(0xFF080812).copy(.38f), Color.Transparent), radius=1100f
        )))
        // Keep the compact island and navigation visible. The expanded surface grows from
        // directly above the island instead of replacing it with an unrelated full-screen card.
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(.22f),Color.Transparent,Color.Transparent))).clickable(onClick=close))
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val expandedHeight = (maxHeight - 182.dp).coerceAtLeast(420.dp)
            Column(
                Modifier.align(Alignment.TopCenter).padding(top=12.dp).fillMaxWidth().height(expandedHeight).padding(horizontal=12.dp).widthIn(max=440.dp)
                    .shadow(26.dp,panelShape,ambientColor=Color(0xFF7655A8).copy(.18f),spotColor=Color.Black.copy(.42f))
                    .clip(panelShape)
                    .background(Color(0xFF171522).copy(.72f))
                    .background(Brush.linearGradient(listOf(Color.White.copy(.18f),Color.White.copy(.045f),Color(0xFF090914).copy(.34f))))
                    .border(1.dp,Brush.verticalGradient(listOf(Color.White.copy(.34f),Color.White.copy(.12f),Color.White.copy(.07f))),panelShape)
                    .clickable {}.padding(horizontal=22.dp, vertical=17.dp),
                horizontalAlignment=Alignment.CenterHorizontally,
                verticalArrangement=Arrangement.spacedBy(9.dp)
            ) {
                // Website header: close orb, centred Now Playing label and queue/menu action.
                Row(Modifier.fillMaxWidth().height(42.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                    Box(Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(.10f)).border(1.dp,Color.White.copy(.16f),CircleShape).clickable(onClick=close),contentAlignment=Alignment.Center) {
                        Icon(Icons.Rounded.KeyboardArrowDown,"Close player",tint=White,modifier=Modifier.size(25.dp))
                    }
                    Column(horizontalAlignment=Alignment.CenterHorizontally) {
                        Text("NOW PLAYING",color=Color(0xFFFF3D8B),letterSpacing=2.1.sp,fontSize=10.sp,fontWeight=FontWeight.Bold)
                        Text("YOUR SOUND. YOUR WORLD.",color=Muted.copy(.9f),fontSize=9.sp,letterSpacing=1.35.sp)
                    }
                    Box(Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(.10f)).border(1.dp,Color.White.copy(.16f),CircleShape).clickable(onClick=openQueue),contentAlignment=Alignment.Center) {
                        Icon(Icons.Rounded.QueueMusic,"Queue",tint=White,modifier=Modifier.size(21.dp))
                    }
                }
                // Flexible artwork stage follows the website's square album-art stage.
                Box(Modifier.weight(1f).fillMaxWidth(),contentAlignment=Alignment.Center) {
                    BoxWithConstraints(Modifier.fillMaxWidth(),contentAlignment=Alignment.Center) {
                        val artSide = minOf(maxWidth, maxHeight)
                        Box(Modifier.size(artSide).clip(RoundedCornerShape(34.dp)).background(Color(0xFF292039))
                            .shadow(24.dp,RoundedCornerShape(34.dp),ambientColor=Color(0xFFFF3D8B).copy(.16f))) {
                            if(track.artwork!=null) AsyncImage(model=track.artwork,contentDescription="Album artwork",contentScale=ContentScale.Crop,modifier=Modifier.fillMaxSize())
                            else Icon(Icons.Rounded.Album,null,tint=Lavender,modifier=Modifier.align(Alignment.Center).size(92.dp))
                            Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color.White.copy(.18f),Color.Transparent,Color.Black.copy(.12f)))))
                            Box(Modifier.fillMaxSize().border(1.dp,Color.White.copy(.20f),RoundedCornerShape(34.dp)))
                        }
                    }
                }
                // Track identity and album line mirror .np-meta and .np-album.
                Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(2.dp)) {
                    Text(track.title,color=White,fontSize=21.sp,fontWeight=FontWeight.Bold,maxLines=1,overflow=TextOverflow.Ellipsis)
                    Text(track.artist,color=Muted,fontSize=14.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
                    Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Rounded.Album,null,tint=Color(0xFFFF3D8B).copy(.85f),modifier=Modifier.size(12.dp))
                        Text(track.album,color=Color(0xFFFF3D8B).copy(.85f),fontSize=11.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
                    }
                }
                Slider(value=progress,onValueChange={progress=it;if(player.duration>0)player.seekTo((player.duration*it).toLong())},
                    colors=SliderDefaults.colors(thumbColor=Color(0xFFFF3D8B),activeTrackColor=Color(0xFFFF3D8B),inactiveTrackColor=Color.White.copy(.16f)),
                    modifier=Modifier.fillMaxWidth().height(25.dp))
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                    Text(formatDuration(player.currentPosition.coerceAtLeast(0)),color=Muted,fontSize=10.sp)
                    Text(if(player.duration>0) "-${formatDuration((player.duration-player.currentPosition).coerceAtLeast(0))}" else "0:00",color=Muted,fontSize=10.sp)
                }
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceEvenly,verticalAlignment=Alignment.CenterVertically) {
                    IconButton(onClick={shuffle=!shuffle;player.shuffleModeEnabled=shuffle},modifier=Modifier.size(42.dp)) { Icon(Icons.Rounded.Shuffle,"Shuffle",tint=if(shuffle)Color(0xFFFF3D8B)else Muted,modifier=Modifier.size(21.dp)) }
                    IconButton(onClick=previous,modifier=Modifier.size(42.dp)) { Icon(Icons.Rounded.SkipPrevious,"Previous",tint=White,modifier=Modifier.size(28.dp)) }
                    FilledIconButton(toggle,colors=IconButtonDefaults.filledIconButtonColors(containerColor=Color(0xFFFF3D8B),contentColor=Color(0xFF05050D)),modifier=Modifier.size(70.dp)) {
                        Icon(if(playing)Icons.Rounded.Pause else Icons.Rounded.PlayArrow,"Play or pause",modifier=Modifier.size(36.dp))
                    }
                    IconButton(onClick=next,modifier=Modifier.size(42.dp)) { Icon(Icons.Rounded.SkipNext,"Next",tint=White,modifier=Modifier.size(28.dp)) }
                    IconButton(onClick={repeat=(repeat+1)%3;player.repeatMode=repeat},modifier=Modifier.size(42.dp)) { Icon(Icons.Rounded.Repeat,"Repeat",tint=if(repeat!=Player.REPEAT_MODE_OFF)Color(0xFFFF3D8B)else Muted,modifier=Modifier.size(21.dp)) }
                }
                // Website's compact Up Next pill: a real preview of the next queued song.
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(999.dp))
                    .background(Color.White.copy(.055f)).border(1.dp,Color.White.copy(.12f),RoundedCornerShape(999.dp))
                    .clickable(onClick=openQueue)
                    .padding(horizontal=9.dp,vertical=7.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                    if(nextTrack?.artwork!=null) Artwork(nextTrack.artwork,38) else Box(Modifier.size(38.dp).clip(CircleShape).background(Color.White.copy(.08f)),contentAlignment=Alignment.Center){Icon(Icons.Rounded.Album,null,tint=Muted)}
                    Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(1.dp)) {
                        Text("UP NEXT",color=Muted,fontSize=8.sp,letterSpacing=1.5.sp)
                        Text(nextTrack?.title ?: "End of queue",color=White,fontSize=12.sp,fontWeight=FontWeight.SemiBold,maxLines=1,overflow=TextOverflow.Ellipsis)
                    }
                    Icon(Icons.Rounded.KeyboardArrowUp,null,tint=Muted,modifier=Modifier.size(20.dp))
                }
            }
            // A glossy liquid neck physically bridges the expanded panel to the mini-player.
            // Its lower end overlaps the island's top edge, so the two surfaces read as one object.
            Box(
                Modifier.align(Alignment.BottomCenter).padding(bottom=142.dp).width(34.dp).height(44.dp)
                    .graphicsLayer { alpha=tetherPulse }
                    .clip(RoundedCornerShape(topStart=15.dp,topEnd=15.dp,bottomStart=999.dp,bottomEnd=999.dp))
                    .background(Brush.verticalGradient(listOf(Color.White.copy(.26f),Color(0xFFB9A8FF).copy(.20f),Color(0xFF7655A8).copy(.12f),Color.Transparent)))
                    .border(1.dp,Brush.verticalGradient(listOf(Color.White.copy(.30f),Color.White.copy(.10f),Color.Transparent)),RoundedCornerShape(999.dp))
            )
            Box(Modifier.align(Alignment.BottomCenter).padding(bottom=151.dp).size(12.dp)
                .shadow(12.dp,CircleShape,ambientColor=Color(0xFFFF3D8B).copy(.34f))
                .clip(CircleShape).background(Brush.radialGradient(listOf(Color.White.copy(.72f),Color(0xFFFF3D8B).copy(.35f),Color.Transparent))))
        }
    }
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = (ms / 1000L).coerceAtLeast(0L)
    return "${totalSeconds / 60}:${(totalSeconds % 60).toString().padStart(2, '0')}"
}

@Composable private fun ArtworkLarge(uri:Uri?){ Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(24.dp)).background(Color(0xFF292039)),contentAlignment=Alignment.Center){if(uri!=null)AsyncImage(uri,null,Modifier.fillMaxSize())else Icon(Icons.Rounded.Album,null,tint=Lavender,modifier=Modifier.size(100.dp))} }
@Composable private fun Navigation(tab:Tab,select:(Tab)->Unit,hazeState:HazeState){
    val index=Tab.values().indexOf(tab)
    BoxWithConstraints(
        Modifier.widthIn(max=330.dp).fillMaxWidth().height(72.dp)
            .shadow(13.dp,RoundedCornerShape(999.dp),ambientColor=Color.Black.copy(.30f),spotColor=Color.Black.copy(.36f))
            // Website nav material: 10px backdrop blur and a 93% opaque dark capsule.
            // The player keeps the stronger 14px mobile blur and richer ambient glow.
            .clip(RoundedCornerShape(999.dp))
            .hazeChild(state = hazeState) {
                blurRadius = 10.dp
                backgroundColor = Color.Transparent
                noiseFactor = 0f
                inputScale = HazeInputScale.Fixed(0.66f)
            }
            .clip(RoundedCornerShape(999.dp))
            .background(Color(0xFF14141D).copy(.93f))
            .background(Brush.linearGradient(listOf(Color.White.copy(.08f),Color.Transparent,Color(0xFF14141D).copy(.10f))))
            .border(1.dp,Brush.verticalGradient(listOf(Color.White.copy(.24f),Color.White.copy(.09f))),RoundedCornerShape(999.dp))
            .padding(horizontal=6.dp,vertical=6.dp)
    ) {
        val itemWidth=maxWidth/3
        // Preserve the existing spring-driven moving indicator animation.
        val indicatorX by animateDpAsState(targetValue=itemWidth*index,animationSpec=spring(dampingRatio=.72f,stiffness=420f),label="nav-liquid-position")
        Box(Modifier.offset(x=indicatorX).width(itemWidth).fillMaxHeight().padding(horizontal=3.dp).clip(RoundedCornerShape(999.dp))
            .background(Brush.verticalGradient(listOf(Color.White.copy(.17f),Color(0xFFFF3D8B).copy(.10f),Color.White.copy(.045f))))
            .border(1.dp,Color.White.copy(.13f),RoundedCornerShape(999.dp)))
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            Tab.values().forEach { t ->
                val active=tab==t
                Column(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(999.dp)).clickable { select(t) }
                    .padding(vertical=5.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center) {
                    Icon(when(t){Tab.HOME->Icons.Rounded.Home;Tab.SEARCH->Icons.Rounded.Search;Tab.LIBRARY->Icons.Rounded.LibraryMusic},null,tint=if(active)Color(0xFFFF6BA7) else Color(0xFFAAA3BD),modifier=Modifier.size(21.dp))
                    Spacer(Modifier.height(3.dp))
                    Text(t.label,color=if(active)White else Color(0xFFAAA3BD),fontSize=10.sp,fontWeight=if(active)FontWeight.Bold else FontWeight.Medium,maxLines=1)
                }
            }
        }
    }
}

@Composable private fun SectionHeader(kicker:String,title:String){Row(Modifier.fillMaxWidth().padding(top=3.dp,bottom=7.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.Bottom){Text(kicker,color=Muted,fontSize=9.sp,letterSpacing=1.5.sp);Text(title,color=Lavender,fontSize=10.sp,fontWeight=FontWeight.Bold,letterSpacing=1.sp)}}
@Composable private fun EmptyCard(message:String){GlassCard(Modifier.fillMaxWidth().padding(bottom=5.dp)){Text(message,color=Muted,fontSize=12.sp,modifier=Modifier.padding(16.dp))}}
@Composable private fun GlassCard(modifier:Modifier=Modifier,content:@Composable ColumnScope.()->Unit){Column(modifier.glassSurface(RoundedCornerShape(22.dp),isDarkTheme=true).padding(14.dp),content=content)}
@Composable private fun PermissionPrompt(request:()->Unit){Box(Modifier.fillMaxSize().background(Ink.copy(.96f)).padding(28.dp),contentAlignment=Alignment.Center){Column(horizontalAlignment=Alignment.CenterHorizontally){Icon(Icons.Rounded.LibraryMusic,null,tint=Lavender,modifier=Modifier.size(52.dp));Spacer(Modifier.height(14.dp));Text("Let your music in",color=White,fontSize=22.sp,fontWeight=FontWeight.Bold);Text("SYNESTHESIA needs audio access to find and play music stored on your device. Your files stay on your phone.",color=Muted,fontSize=13.sp);Spacer(Modifier.height(18.dp));Button(request,colors=ButtonDefaults.buttonColors(containerColor=Violet)){Text("Allow music access")}}} }
