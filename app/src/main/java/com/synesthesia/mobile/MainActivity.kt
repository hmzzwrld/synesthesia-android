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
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import coil.compose.AsyncImage
import com.synesthesia.mobile.ui.theme.SynesthesiaAtmosphere
import com.synesthesia.mobile.ui.theme.DefaultAmbA
import com.synesthesia.mobile.ui.theme.DefaultAmbB
import com.synesthesia.mobile.ui.theme.DefaultAmbC
import com.synesthesia.mobile.ui.theme.glassSurface
import com.synesthesia.mobile.util.ColorExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.collect

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
        if (Build.VERSION.SDK_INT >= 29) {
            window.isNavigationBarContrastEnforced = false
        }
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
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
    var scrollProgress by remember { mutableFloatStateOf(0f) }
    var scanning by remember { mutableStateOf(false) }
    var scanMessage by remember { mutableStateOf("Scan your device to find music") }
    var playCounts by remember { mutableStateOf<Map<Long, Int>>(emptyMap()) }
    var recentIds by remember { mutableStateOf<List<Long>>(emptyList()) }
    var monthlyCounts by remember { mutableStateOf<Map<Long, Int>>(emptyMap()) }
    var ambientPalette by remember {
        mutableStateOf(Triple(DefaultAmbA, DefaultAmbB, DefaultAmbC))
    }

    // Artwork colours now drive the persistent Aurora behind every screen.
    LaunchedEffect(selected?.id) {
        ambientPalette = ColorExtractor.extractPaletteFromUri(
            context = context,
            data = selected?.artwork,
            defaultA = DefaultAmbA,
            defaultB = DefaultAmbB,
            defaultC = DefaultAmbC
        )
    }
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
    DisposableEffect(player) {
        val listener = object : Player.Listener { override fun onIsPlayingChanged(isPlaying: Boolean) { playing = isPlaying } }
        player.addListener(listener); onDispose { player.removeListener(listener) }
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
    val text = White
    MaterialTheme(colorScheme = darkColorScheme(primary=Lavender, background=Ink, surface=Color(0xFF191625), onSurface=White)) {
        SynesthesiaAtmosphere(
            ambA = ambientPalette.first,
            ambB = ambientPalette.second,
            ambC = ambientPalette.third,
            scrollProgress = scrollProgress
        ) {
            Box(Modifier.fillMaxSize().systemBarsPadding()) {
                Column(Modifier.fillMaxSize().padding(horizontal=16.dp)) {
                    Row(Modifier.fillMaxWidth().padding(top=12.dp,bottom=14.dp), verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.SpaceBetween) {
                        Column { Text("SYNESTHESIA", color=White, fontSize=17.sp, fontWeight=FontWeight.Black, letterSpacing=3.sp); Text("YOUR SOUND. YOUR SPACE.", color=Muted, fontSize=9.sp, letterSpacing=2.sp) }
                        IconButton(onClick={ if(hasPermission) scope.launch { scan() } else launcher.launch(permission) }) { Icon(Icons.Rounded.Refresh, "Rescan music", tint=Lavender) }
                    }
                    AnimatedContent(tab, transitionSpec={ fadeIn(tween(220))+slideInVertically { it/18 } togetherWith fadeOut(tween(150))+slideOutVertically { -it/18 } }, label="main-tabs", modifier=Modifier.weight(1f)) { destination ->
                        when(destination) {
                            Tab.HOME -> HomeScreen(tracks, recentIds, playCounts, monthlyCounts, selected, ::play, {tab=Tab.LIBRARY; scrollProgress=0f}, scanMessage, {scrollProgress=it})
                            Tab.SEARCH -> SearchScreen(tracks, query, {query=it}, ::play, {scrollProgress=it})
                            Tab.LIBRARY -> LibraryScreen(tracks, libraryTab, {libraryTab=it}, query, {query=it}, ::play, {scrollProgress=it})
                        }
                    }
                    AnimatedVisibility(selected!=null, enter=fadeIn()+slideInVertically { it/2 }, exit=fadeOut()+slideOutVertically { it/2 }) {
                        MiniPlayer(selected, playing, { if(player.isPlaying) player.pause() else player.play() }, {expandedPlayer=true})
                    }
                    Navigation(tab,{tab=it; scrollProgress=0f})
                }
                if(expandedPlayer && selected!=null) PlayerSheet(selected!!, playing, { if(player.isPlaying) player.pause() else player.play() }, {player.seekToPreviousMediaItem()}, {player.seekToNextMediaItem()}, {expandedPlayer=false}, player)
                if(!hasPermission) PermissionPrompt { launcher.launch(permission) }
                if(scanning) Box(Modifier.fillMaxSize().background(Color.Black.copy(.35f)), contentAlignment=Alignment.Center) { CircularProgressIndicator(color=Lavender) }
            }
        }
    }
}

@Composable private fun HomeScreen(tracks:List<LocalTrack>, recent:List<Long>, counts:Map<Long,Int>, monthly:Map<Long,Int>, selected:LocalTrack?, onPlay:(LocalTrack)->Unit, openLibrary:()->Unit, message:String, onScroll:(Float)->Unit) {
    val listState = rememberLazyListState()
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex * 360f + listState.firstVisibleItemScrollOffset.toFloat() }
            .collect { offset -> onScroll((offset / 450f).toInt().coerceIn(0, 8) / 8f) }
    }
    val recentTracks=recent.mapNotNull { id -> tracks.find{it.id==id} }
    val most=tracks.sortedByDescending { counts[it.id]?:0 }.filter { (counts[it.id]?:0)>0 }.take(10)
    val monthTop=tracks.sortedByDescending { monthly[it.id]?:0 }.filter { (monthly[it.id]?:0)>0 }.take(10)
    LazyColumn(state=listState, verticalArrangement=Arrangement.spacedBy(16.dp), contentPadding=PaddingValues(bottom=18.dp)) {
        item { Column { Text("Good music, good mood.", color=White,fontSize=25.sp,fontWeight=FontWeight.Bold); Text(message,color=Muted,fontSize=12.sp) } }
        item { SectionHeader("YOUR ROTATION", "MOST PLAYED"); if(most.isEmpty()) EmptyCard("Your most-played tracks will appear here as you listen.") else most.take(5).forEach { TrackRow(it,onPlay,counts[it.id]?:0) } }
        item { SectionHeader("PICK UP WHERE YOU LEFT", "RECENTLY PLAYED"); if(recentTracks.isEmpty()) EmptyCard("Your listening history will build automatically.") else recentTracks.take(6).forEach { TrackRow(it,onPlay) } }
        item { SectionHeader("MONTHLY REPLAY", "YOUR TOP 10"); if(monthTop.isEmpty()) EmptyCard("Your monthly ranking starts with your listening activity.") else monthTop.forEachIndexed { i,t -> TrackRow(t,onPlay,monthly[t.id]?:0,rank=i+1) } }
        item { SectionHeader("YOUR COLLECTION", "LIBRARY"); GlassCard(Modifier.fillMaxWidth().clickable(onClick=openLibrary).padding(18.dp)) { Row(verticalAlignment=Alignment.CenterVertically) { Icon(Icons.Rounded.LibraryMusic,null,tint=Lavender); Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)){Text("Explore your music",color=White,fontWeight=FontWeight.SemiBold);Text("Songs · Albums · Artists · Genres · Folders",color=Muted,fontSize=11.sp)}; Icon(Icons.Rounded.ChevronRight,null,tint=Muted) } } }
    }
}

@Composable private fun SearchScreen(tracks:List<LocalTrack>, query:String, change:(String)->Unit, play:(LocalTrack)->Unit, onScroll:(Float)->Unit) {
    val listState = rememberLazyListState()
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex * 360f + listState.firstVisibleItemScrollOffset.toFloat() }
            .collect { offset -> onScroll((offset / 450f).toInt().coerceIn(0, 8) / 8f) }
    }
    Column { Text("Find your sound",color=White,fontSize=24.sp,fontWeight=FontWeight.Bold); Spacer(Modifier.height(12.dp)); OutlinedTextField(query,change,Modifier.fillMaxWidth(),placeholder={Text("Search songs, artists, albums…",color=Muted)},leadingIcon={Icon(Icons.Rounded.Search,null,tint=Lavender)},singleLine=true,shape=RoundedCornerShape(20.dp),colors=OutlinedTextFieldDefaults.colors(focusedBorderColor=Lavender,unfocusedBorderColor=Color.White.copy(.16f),focusedTextColor=White,unfocusedTextColor=White,focusedContainerColor=Color.Transparent,unfocusedContainerColor=Color.Transparent)); LazyColumn(state=listState) { items(tracks.filter{query.isBlank() || listOf(it.title,it.artist,it.album).any{s->s.contains(query,true)}}) { TrackRow(it,play) } } }
}

@Composable private fun LibraryScreen(tracks:List<LocalTrack>, selected:LibraryTab, choose:(LibraryTab)->Unit, query:String, change:(String)->Unit, play:(LocalTrack)->Unit, onScroll:(Float)->Unit) {
    val listState = rememberLazyListState()
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex * 360f + listState.firstVisibleItemScrollOffset.toFloat() }
            .collect { offset -> onScroll((offset / 450f).toInt().coerceIn(0, 8) / 8f) }
    }
    Column { Text("Your Library",color=White,fontSize=25.sp,fontWeight=FontWeight.Bold); Spacer(Modifier.height(10.dp)); Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) { LibraryTab.values().forEach { item -> FilterChip(selected==item,{choose(item)},label={Text(item.label,fontSize=11.sp)},colors=FilterChipDefaults.filterChipColors(selectedContainerColor=Violet.copy(.35f),labelColor=Muted,selectedLabelColor=White)) } }; Spacer(Modifier.height(8.dp)); when(selected) {
        LibraryTab.SONGS -> LazyColumn(state=listState) { items(tracks.filter{query.isBlank()||it.title.contains(query,true)||it.artist.contains(query,true)}){TrackRow(it,play)} }
        LibraryTab.ALBUMS -> LazyColumn(state=listState) { items(tracks.groupBy{it.album}.toList().sortedBy{it.first}) { (name,songs) -> CollectionRow(name,songs.first().artist,"${songs.size} tracks",songs.first().artwork){songs.firstOrNull()?.let(play)} } }
        LibraryTab.ARTISTS -> LazyColumn(state=listState) { items(tracks.groupBy{it.artist}.toList().sortedBy{it.first}) { (name,songs) -> CollectionRow(name,"Artist","${songs.size} tracks",songs.first().artwork){songs.firstOrNull()?.let(play)} } }
        LibraryTab.GENRES -> LazyColumn(state=listState) { items(tracks.groupBy{it.genre}.toList().sortedBy{it.first}) { (name,songs) -> CollectionRow(name,"Genre","${songs.size} tracks",songs.first().artwork){songs.firstOrNull()?.let(play)} } }
        LibraryTab.FOLDERS -> LazyColumn(state=listState) { items(tracks.groupBy{it.path.substringBeforeLast('/',"/")}.toList().sortedBy{it.first}) { (name,songs) -> CollectionRow(name.substringAfterLast('/').ifBlank{"Music"},name,"${songs.size} tracks",songs.first().artwork){songs.firstOrNull()?.let(play)} } }
    } }
}

@Composable private fun TrackRow(track:LocalTrack,onPlay:(LocalTrack)->Unit,plays:Int?=null,rank:Int?=null) { Row(Modifier.fillMaxWidth().clickable{onPlay(track)}.padding(vertical=7.dp),verticalAlignment=Alignment.CenterVertically) { Artwork(track.artwork,46); Spacer(Modifier.width(11.dp)); Column(Modifier.weight(1f)) { Text((rank?.let{"$it. "}?:"")+track.title,color=White,fontWeight=FontWeight.SemiBold,fontSize=13.sp,maxLines=1,overflow=TextOverflow.Ellipsis); Text(track.artist+" · "+track.album,color=Muted,fontSize=11.sp,maxLines=1,overflow=TextOverflow.Ellipsis) }; if(plays!=null) Text("${plays}×",color=Lavender,fontSize=11.sp); Icon(Icons.Rounded.PlayCircleOutline,null,tint=Lavender,modifier=Modifier.padding(start=8.dp)) } }
@Composable private fun CollectionRow(title:String,subtitle:String,detail:String,art:Uri?,click:()->Unit){ Row(Modifier.fillMaxWidth().clickable(onClick=click).padding(vertical=8.dp),verticalAlignment=Alignment.CenterVertically){Artwork(art,50);Spacer(Modifier.width(12.dp));Column(Modifier.weight(1f)){Text(title,color=White,fontWeight=FontWeight.SemiBold);Text(subtitle,color=Muted,fontSize=11.sp)};Text(detail,color=Muted,fontSize=10.sp)} }
@Composable private fun Artwork(uri:Uri?,size:Int){ Box(Modifier.size(size.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF282139)),contentAlignment=Alignment.Center){ if(uri!=null) AsyncImage(uri,null,Modifier.fillMaxSize()) else Icon(Icons.Rounded.Album,null,tint=Lavender,modifier=Modifier.size((size/2).dp)) } }
@Composable private fun MiniPlayer(track:LocalTrack?,playing:Boolean,toggle:()->Unit,expand:()->Unit){ if(track==null)return; Row(Modifier.fillMaxWidth().padding(horizontal=3.dp, vertical=8.dp).glassSurface(RoundedCornerShape(25.dp), elevation=16.dp, isDarkTheme=true).clickable(onClick=expand).padding(horizontal=12.dp, vertical=10.dp),verticalAlignment=Alignment.CenterVertically){Artwork(track.artwork,38);Spacer(Modifier.width(10.dp));Column(Modifier.weight(1f)){Text(track.title,color=White,fontSize=12.sp,fontWeight=FontWeight.SemiBold,maxLines=1);Text(track.artist,color=Muted,fontSize=10.sp,maxLines=1)};IconButton(toggle){Icon(if(playing)Icons.Rounded.Pause else Icons.Rounded.PlayArrow,null,tint=White)} } }
@Composable private fun PlayerSheet(track:LocalTrack,playing:Boolean,toggle:()->Unit,previous:()->Unit,next:()->Unit,close:()->Unit,player:ExoPlayer){ Box(Modifier.fillMaxSize().background(Color.Black.copy(.30f)).clickable(onClick=close).padding(14.dp),contentAlignment=Alignment.Center){ Column(Modifier.fillMaxWidth().fillMaxHeight(.88f).glassSurface(RoundedCornerShape(32.dp),isDarkTheme=true).clickable(enabled=false){}.padding(22.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.SpaceEvenly){ Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("NOW PLAYING",color=Muted,letterSpacing=2.sp,fontSize=11.sp);IconButton(close){Icon(Icons.Rounded.Close,null,tint=White)}};ArtworkLarge(track.artwork);Text(track.title,color=White,fontSize=22.sp,fontWeight=FontWeight.Bold,maxLines=1);Text(track.artist,color=Muted);var progress by remember{mutableFloatStateOf(0f)}; LaunchedEffect(player){while(true){if(player.duration>0)progress=(player.currentPosition.toFloat()/player.duration).coerceIn(0f,1f);kotlinx.coroutines.delay(500)}};Slider(progress,{progress=it; if(player.duration>0)player.seekTo((player.duration*it).toLong())},colors=SliderDefaults.colors(thumbColor=Lavender,activeTrackColor=Violet,inactiveTrackColor=Color.White.copy(.15f)));Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceEvenly,verticalAlignment=Alignment.CenterVertically){IconButton(previous){Icon(Icons.Rounded.SkipPrevious,null,tint=White,modifier=Modifier.size(34.dp))};FilledIconButton(toggle,colors=IconButtonDefaults.filledIconButtonColors(containerColor=Lavender,contentColor=Ink),modifier=Modifier.size(64.dp)){Icon(if(playing)Icons.Rounded.Pause else Icons.Rounded.PlayArrow,null,modifier=Modifier.size(34.dp))};IconButton(next){Icon(Icons.Rounded.SkipNext,null,tint=White,modifier=Modifier.size(34.dp))}} } } }
@Composable private fun ArtworkLarge(uri:Uri?){
    val shape = RoundedCornerShape(26.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .shadow(18.dp, shape)
            .clip(shape)
            .background(Color(0xFF292039))
            .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(.58f), Color.White.copy(.06f))), shape),
        contentAlignment = Alignment.Center
    ) {
        if (uri != null) AsyncImage(uri, null, Modifier.fillMaxSize())
        else Icon(Icons.Rounded.Album, null, tint = Lavender, modifier = Modifier.size(100.dp))
    }
}
@Composable private fun Navigation(tab:Tab,select:(Tab)->Unit){
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal=5.dp, vertical=8.dp)
            .glassSurface(RoundedCornerShape(30.dp), elevation=18.dp, isDarkTheme=true)
            .padding(horizontal=7.dp, vertical=7.dp),
        horizontalArrangement=Arrangement.SpaceEvenly
    ) {
        Tab.values().forEach { item ->
            val active = tab == item
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(23.dp))
                    .clickable { select(item) }
                    .then(
                        if (active) Modifier.background(
                            Brush.verticalGradient(
                                listOf(
                                    Lavender.copy(alpha = 0.23f),
                                    Violet.copy(alpha = 0.12f),
                                    Color.Transparent
                                )
                            )
                        ) else Modifier
                    )
                    .padding(vertical=9.dp),
                horizontalAlignment=Alignment.CenterHorizontally
            ) {
                Icon(
                    when(item) {
                        Tab.HOME -> Icons.Rounded.Home
                        Tab.SEARCH -> Icons.Rounded.Search
                        Tab.LIBRARY -> Icons.Rounded.LibraryMusic
                    },
                    contentDescription = item.label,
                    tint = if(active) Lavender else Muted
                )
                Text(
                    item.label,
                    color = if(active) White else Muted,
                    fontSize=10.sp,
                    fontWeight=if(active) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}
@Composable private fun SectionHeader(kicker:String,title:String){Row(Modifier.fillMaxWidth().padding(top=3.dp,bottom=7.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.Bottom){Text(kicker,color=Muted,fontSize=9.sp,letterSpacing=1.5.sp);Text(title,color=Lavender,fontSize=10.sp,fontWeight=FontWeight.Bold,letterSpacing=1.sp)}}
@Composable private fun EmptyCard(message:String){GlassCard(Modifier.fillMaxWidth().padding(bottom=5.dp)){Text(message,color=Muted,fontSize=12.sp,modifier=Modifier.padding(16.dp))}}
@Composable private fun GlassCard(modifier:Modifier=Modifier,content:@Composable ColumnScope.()->Unit){Column(modifier.glassSurface(RoundedCornerShape(22.dp),isDarkTheme=true).padding(14.dp),content=content)}
@Composable private fun PermissionPrompt(request:()->Unit){Box(Modifier.fillMaxSize().background(Ink.copy(.96f)).padding(28.dp),contentAlignment=Alignment.Center){Column(horizontalAlignment=Alignment.CenterHorizontally){Icon(Icons.Rounded.LibraryMusic,null,tint=Lavender,modifier=Modifier.size(52.dp));Spacer(Modifier.height(14.dp));Text("Let your music in",color=White,fontSize=22.sp,fontWeight=FontWeight.Bold);Text("SYNESTHESIA needs audio access to find and play music stored on your device. Your files stay on your phone.",color=Muted,fontSize=13.sp);Spacer(Modifier.height(18.dp));Button(request,colors=ButtonDefaults.buttonColors(containerColor=Violet)){Text("Allow music access")}}} }
