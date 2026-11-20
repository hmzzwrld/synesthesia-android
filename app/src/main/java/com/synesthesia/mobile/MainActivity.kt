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
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
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
import androidx.compose.ui.res.painterResource
import com.synesthesia.mobile.ui.theme.SynesthesiaAtmosphere
import com.synesthesia.mobile.ui.theme.glassSurface
import com.synesthesia.mobile.util.ColorExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.foundation.border

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
    var ambientPalette by remember { mutableStateOf(Triple(Color(0xFFFF3D8B), Color(0xFFFF9F40), Color(0xFFA855F7))) }
    LaunchedEffect(selected?.artwork) {
        ambientPalette = ColorExtractor.extractPaletteFromUri(
            context, selected?.artwork,
            Color(0xFFFF3D8B), Color(0xFFFF9F40), Color(0xFFA855F7)
        )
    }
    MaterialTheme(colorScheme = darkColorScheme(primary=ambientPalette.first, background=Ink, surface=Color(0xFF12121E), onSurface=White)) {
        SynesthesiaAtmosphere(ambA=ambientPalette.first, ambB=ambientPalette.second, ambC=ambientPalette.third) {
            Box(Modifier.fillMaxSize()) {
                // Website reference: fixed background image, adaptive colour wash, ambient glow and fine mesh.
                Image(
                    painter = painterResource(id = R.drawable.synesthesia_background),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().blur(1.2.dp),
                    alpha = 0.66f
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
                // Website layout: independent floating island and bottom navigation, over the scroll layer.
                Column(
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().padding(horizontal=14.dp).padding(bottom=8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    AnimatedVisibility(selected!=null, enter=fadeIn()+slideInVertically { it/3 }, exit=fadeOut()+slideOutVertically { it/3 }) {
                        MiniPlayer(selected, playing, { if(player.isPlaying) player.pause() else player.play() }, {expandedPlayer=true}, { liquidQueueOpen=true })
                    }
                    Navigation(tab,{tab=it})
                }
                if(expandedPlayer && selected!=null) PlayerSheet(selected!!, playing, { if(player.isPlaying) player.pause() else player.play() }, {player.seekToPreviousMediaItem()}, {player.seekToNextMediaItem()}, {expandedPlayer=false}, player, tracks)
                AnimatedVisibility(visible=liquidQueueOpen,enter=fadeIn(tween(160))+slideInVertically(spring(dampingRatio=.78f,stiffness=360f)){it/5},exit=fadeOut(tween(130))+slideOutVertically(tween(180)){it/3}) {
                    LiquidQueueSheet(player, tracks, ambientPalette.first, { liquidQueueOpen=false })
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
@Composable private fun MiniPlayer(track:LocalTrack?,playing:Boolean,toggle:()->Unit,expand:()->Unit,openQueue:()->Unit){
    if(track==null)return
    Row(
        Modifier.widthIn(max=320.dp).fillMaxWidth().animateContentSize(spring(dampingRatio=.78f, stiffness=520f))
            .shadow(18.dp, RoundedCornerShape(30.dp), ambientColor=Color.Black.copy(.42f), spotColor=Color.Black.copy(.55f))
            .clip(RoundedCornerShape(30.dp))
            .background(Brush.linearGradient(listOf(Color.White.copy(.22f),Color(0xFF28233B).copy(.82f),Color(0xFF11101C).copy(.92f))))
            .background(Brush.radialGradient(listOf(Color.White.copy(.19f),Color.Transparent), radius=300f))
            .border(1.dp,Brush.verticalGradient(listOf(Color.White.copy(.48f),Color.White.copy(.12f),Color.White.copy(.06f))),RoundedCornerShape(30.dp))
            .combinedClickable(onClick=expand,onLongClick=openQueue)
            .padding(start=9.dp,end=7.dp,top=7.dp,bottom=7.dp),
        verticalAlignment=Alignment.CenterVertically
    ) {
        Box(Modifier.size(38.dp).clip(RoundedCornerShape(13.dp)).border(1.dp,Color.White.copy(.25f),RoundedCornerShape(13.dp))) { Artwork(track.artwork,38) }
        Spacer(Modifier.width(9.dp))
        Column(Modifier.weight(1f).padding(vertical=2.dp)) {
            Text(track.title,color=White,fontSize=12.sp,fontWeight=FontWeight.SemiBold,maxLines=2,overflow=TextOverflow.Ellipsis,lineHeight=14.sp)
            Text(track.artist,color=Muted,fontSize=10.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
        }
        IconButton(onClick=openQueue,modifier=Modifier.size(34.dp)) { Icon(Icons.Rounded.QueueMusic,"Open Liquid Queue",tint=Lavender,modifier=Modifier.size(19.dp)) }
        IconButton(toggle,modifier=Modifier.size(36.dp)) { Icon(if(playing)Icons.Rounded.Pause else Icons.Rounded.PlayArrow,"Play or pause",tint=White,modifier=Modifier.size(23.dp)) }
    }
}

@Composable private fun LiquidQueueSheet(player:ExoPlayer, library:List<LocalTrack>, accent:Color, close:()->Unit) {
    var queueRevision by remember { mutableIntStateOf(0) }
    val items = remember(player, library, queueRevision) {
        (0 until player.mediaItemCount).mapNotNull { i ->
            val id=player.getMediaItemAt(i).mediaId.toLongOrNull()
            library.firstOrNull { it.id==id }
        }
    }
    Box(Modifier.fillMaxSize().background(Color.Black.copy(.38f)).clickable(onClick=close),contentAlignment=Alignment.BottomCenter) {
        Column(
            Modifier.fillMaxWidth().fillMaxHeight(.58f).padding(horizontal=12.dp).padding(bottom=8.dp)
                .shadow(28.dp,RoundedCornerShape(topStart=38.dp,topEnd=38.dp,bottomStart=30.dp,bottomEnd=30.dp))
                .clip(RoundedCornerShape(topStart=38.dp,topEnd=38.dp,bottomStart=30.dp,bottomEnd=30.dp))
                .background(Brush.radialGradient(listOf(accent.copy(.22f),Color.Transparent),radius=650f))
                .background(Brush.linearGradient(listOf(Color.White.copy(.19f),Color(0xFF211C31).copy(.94f),Color(0xFF100F1B).copy(.98f))))
                .border(1.dp,Brush.verticalGradient(listOf(Color.White.copy(.42f),Color.White.copy(.12f))),RoundedCornerShape(topStart=38.dp,topEnd=38.dp,bottomStart=30.dp,bottomEnd=30.dp))
                .clickable(onClick={}).padding(horizontal=15.dp,vertical=14.dp),
            verticalArrangement=Arrangement.spacedBy(8.dp)
        ) {
            Box(Modifier.align(Alignment.CenterHorizontally).width(38.dp).height(4.dp).clip(CircleShape).background(Color.White.copy(.38f)))
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {
                Column {
                    Text("LIQUID QUEUE",color=accent,fontSize=9.sp,fontWeight=FontWeight.Bold,letterSpacing=2.sp)
                    Text("Up Next",color=White,fontSize=23.sp,fontWeight=FontWeight.Bold)
                }
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Text("${items.size} tracks",color=Muted,fontSize=11.sp)
                    IconButton(onClick=close) { Icon(Icons.Rounded.Close,"Close queue",tint=White) }
                }
            }
            Row(Modifier.fillMaxWidth().padding(bottom=2.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                Text("NOW PLAYING & UP NEXT",color=Muted,fontSize=9.sp,letterSpacing=1.4.sp)
                TextButton(onClick={ for(i in player.mediaItemCount-1 downTo player.currentMediaItemIndex+1) player.removeMediaItem(i); queueRevision++ },enabled=player.mediaItemCount>player.currentMediaItemIndex+1) { Text("Clear upcoming",color=accent,fontSize=11.sp) }
            }
            LazyColumn(Modifier.weight(1f).fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(5.dp),contentPadding=PaddingValues(bottom=8.dp)) {
                items(items.size,key={index -> items[index].id}) { index ->
                    val item=items[index]
                    val current=index==player.currentMediaItemIndex
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))
                        .background(if(current) Brush.horizontalGradient(listOf(accent.copy(.28f),Color.White.copy(.055f))) else Brush.horizontalGradient(listOf(Color.White.copy(.055f),Color.Transparent)))
                        .border(if(current)1.dp else .5.dp,if(current)Color.White.copy(.22f) else Color.White.copy(.055f),RoundedCornerShape(22.dp))
                        .clickable { player.seekToDefaultPosition(index); player.play() }
                        .padding(horizontal=9.dp,vertical=8.dp),verticalAlignment=Alignment.CenterVertically) {
                        Box(Modifier.size(44.dp).clip(CircleShape).border(1.dp,Color.White.copy(.2f),CircleShape)) { Artwork(item.artwork,44) }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(item.title,color=if(current) White else White.copy(.92f),fontSize=13.sp,fontWeight=FontWeight.SemiBold,maxLines=2,overflow=TextOverflow.Ellipsis)
                            Text(item.artist,color=Muted,fontSize=10.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
                        }
                        if(current) Icon(Icons.Rounded.GraphicEq,null,tint=accent,modifier=Modifier.padding(horizontal=5.dp))
                        else Column {
                            IconButton(onClick={if(index>player.currentMediaItemIndex+1) { player.moveMediaItem(index,index-1); queueRevision++ }},modifier=Modifier.size(26.dp),enabled=index>player.currentMediaItemIndex+1) { Icon(Icons.Rounded.KeyboardArrowUp,"Move up",tint=Muted,modifier=Modifier.size(18.dp)) }
                            IconButton(onClick={if(index<player.mediaItemCount-1) { player.moveMediaItem(index,index+1); queueRevision++ }},modifier=Modifier.size(26.dp),enabled=index<player.mediaItemCount-1) { Icon(Icons.Rounded.KeyboardArrowDown,"Move down",tint=Muted,modifier=Modifier.size(18.dp)) }
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color.White.copy(.055f)).padding(horizontal=12.dp,vertical=9.dp),verticalAlignment=Alignment.CenterVertically) {
                Icon(Icons.Rounded.Info,null,tint=Muted,modifier=Modifier.size(15.dp)); Spacer(Modifier.width(8.dp)); Text("Long-press the mini-player or tap the queue icon to return here.",color=Muted,fontSize=10.sp,maxLines=2)
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
    queue: List<LocalTrack>
) {
    var progress by remember { mutableFloatStateOf(0f) }
    var showQueue by remember { mutableStateOf(false) }
    var shuffle by remember { mutableStateOf(player.shuffleModeEnabled) }
    var repeat by remember { mutableIntStateOf(player.repeatMode) }
    LaunchedEffect(player) {
        while (true) {
            val duration = player.duration
            if (duration > 0) progress = (player.currentPosition.toFloat() / duration).coerceIn(0f, 1f)
            kotlinx.coroutines.delay(400)
        }
    }
    Box(Modifier.fillMaxSize().background(Color(0xFF05050D))) {
        if (track.artwork != null) {
            AsyncImage(
                model=track.artwork,
                contentDescription=null,
                contentScale=ContentScale.Crop,
                modifier=Modifier.fillMaxSize().blur(28.dp),
                alpha=0.72f
            )
        }
        Box(Modifier.fillMaxSize().background(Color(0xFF05050D).copy(alpha=.64f)))
        Box(Modifier.fillMaxSize().clickable(onClick=close).padding(12.dp), contentAlignment=Alignment.Center) {
        Column(
            Modifier.fillMaxWidth().fillMaxHeight(.94f)
                .glassSurface(RoundedCornerShape(34.dp), elevation=24.dp, isDarkTheme=true)
                .clickable {}.padding(horizontal=20.dp, vertical=16.dp),
            horizontalAlignment=Alignment.CenterHorizontally,
            verticalArrangement=Arrangement.spacedBy(10.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceBetween, verticalAlignment=Alignment.CenterVertically) {
                IconButton(onClick=close) { Icon(Icons.Rounded.KeyboardArrowDown, "Close player", tint=White) }
                Column(horizontalAlignment=Alignment.CenterHorizontally) {
                    Text("NOW PLAYING", color=Color(0xFFFF3D8B), letterSpacing=2.sp, fontSize=10.sp, fontWeight=FontWeight.Bold)
                    Text(if (showQueue) "LIQUID QUEUE" else "YOUR SOUND. YOUR WORLD.", color=Muted, fontSize=9.sp, letterSpacing=1.sp)
                }
                IconButton(onClick={ showQueue = !showQueue }) { Icon(if(showQueue) Icons.Rounded.Album else Icons.Rounded.QueueMusic, "Toggle queue", tint=White) }
            }
            if (showQueue) {
                Row(Modifier.fillMaxWidth(), verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.SpaceBetween) {
                    Text("Up Next", color=White, fontSize=22.sp, fontWeight=FontWeight.Bold)
                    Text("${queue.size} tracks", color=Muted, fontSize=11.sp)
                }
                LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding=PaddingValues(bottom=10.dp), verticalArrangement=Arrangement.spacedBy(3.dp)) {
                    items(queue) { item ->
                        val index = queue.indexOf(item)
                        val active = index == player.currentMediaItemIndex
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                                .background(if(active) Color.White.copy(.10f) else Color.Transparent)
                                .clickable { player.seekToDefaultPosition(index); player.play() }
                                .padding(horizontal=8.dp, vertical=7.dp),
                            verticalAlignment=Alignment.CenterVertically
                        ) {
                            Artwork(item.artwork, 42)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(item.title, color=if(active) Color(0xFFFF3D8B) else White, fontSize=12.sp, fontWeight=FontWeight.SemiBold, maxLines=1, overflow=TextOverflow.Ellipsis)
                                Text(item.artist, color=Muted, fontSize=10.sp, maxLines=1, overflow=TextOverflow.Ellipsis)
                            }
                            if(active) Icon(Icons.Rounded.GraphicEq, null, tint=Color(0xFFFF3D8B))
                            else Text(formatDuration(item.duration), color=Muted, fontSize=10.sp)
                        }
                    }
                }
            } else {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment=Alignment.Center) {
                    ArtworkLarge(track.artwork)
                }
                Column(Modifier.fillMaxWidth(), horizontalAlignment=Alignment.Start) {
                    Text(track.title, color=White, fontSize=21.sp, fontWeight=FontWeight.Bold, maxLines=1, overflow=TextOverflow.Ellipsis)
                    Text(track.artist, color=Muted, fontSize=14.sp, maxLines=1, overflow=TextOverflow.Ellipsis)
                    Text(track.album, color=Color(0xFFFF3D8B).copy(.85f), fontSize=11.sp, maxLines=1, overflow=TextOverflow.Ellipsis)
                }
                Slider(
                    value=progress,
                    onValueChange={ progress=it; if(player.duration>0) player.seekTo((player.duration*it).toLong()) },
                    colors=SliderDefaults.colors(thumbColor=Color(0xFFFF3D8B), activeTrackColor=Color(0xFFFF3D8B), inactiveTrackColor=Color.White.copy(.16f))
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceBetween) {
                    Text(formatDuration(player.currentPosition.coerceAtLeast(0)), color=Muted, fontSize=10.sp)
                    Text(if(player.duration>0) "-${formatDuration((player.duration-player.currentPosition).coerceAtLeast(0))}" else "0:00", color=Muted, fontSize=10.sp)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceEvenly, verticalAlignment=Alignment.CenterVertically) {
                    IconButton(onClick={ shuffle=!shuffle; player.shuffleModeEnabled=shuffle }) { Icon(Icons.Rounded.Shuffle, "Shuffle", tint=if(shuffle) Color(0xFFFF3D8B) else Muted) }
                    IconButton(onClick=previous) { Icon(Icons.Rounded.SkipPrevious, "Previous", tint=White, modifier=Modifier.size(34.dp)) }
                    FilledIconButton(toggle, colors=IconButtonDefaults.filledIconButtonColors(containerColor=Color(0xFFFF3D8B), contentColor=Color(0xFF05050D)), modifier=Modifier.size(66.dp)) {
                        Icon(if(playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, "Play or pause", modifier=Modifier.size(34.dp))
                    }
                    IconButton(onClick=next) { Icon(Icons.Rounded.SkipNext, "Next", tint=White, modifier=Modifier.size(34.dp)) }
                    IconButton(onClick={ repeat=(repeat+1)%3; player.repeatMode=repeat }) { Icon(Icons.Rounded.Repeat, "Repeat", tint=if(repeat!=Player.REPEAT_MODE_OFF) Color(0xFFFF3D8B) else Muted) }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.Center) {
                    Text("UP NEXT  ·  ${queue.size} TRACKS", color=Muted, fontSize=9.sp, letterSpacing=1.6.sp)
                }
            }
        }
        }
    }
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = (ms / 1000L).coerceAtLeast(0L)
    return "${totalSeconds / 60}:${(totalSeconds % 60).toString().padStart(2, '0')}"
}

@Composable private fun ArtworkLarge(uri:Uri?){ Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(24.dp)).background(Color(0xFF292039)),contentAlignment=Alignment.Center){if(uri!=null)AsyncImage(uri,null,Modifier.fillMaxSize())else Icon(Icons.Rounded.Album,null,tint=Lavender,modifier=Modifier.size(100.dp))} }
@Composable private fun Navigation(tab:Tab,select:(Tab)->Unit){
    val index=Tab.values().indexOf(tab)
    BoxWithConstraints(
        Modifier.widthIn(max=360.dp).fillMaxWidth().height(62.dp)
            .shadow(13.dp,RoundedCornerShape(999.dp),ambientColor=Color.Black.copy(.30f),spotColor=Color.Black.copy(.36f))
            .clip(RoundedCornerShape(999.dp))
            .background(Color(0xD91B1928))
            .border(1.dp,Brush.verticalGradient(listOf(Color.White.copy(.27f),Color.White.copy(.09f))),RoundedCornerShape(999.dp))
            .padding(horizontal=7.dp,vertical=6.dp)
    ) {
        val itemWidth=maxWidth/3
        val indicatorX by animateDpAsState(targetValue=itemWidth*index,animationSpec=spring(dampingRatio=.72f,stiffness=420f),label="nav-liquid-position")
        Box(Modifier.offset(x=indicatorX).width(itemWidth).fillMaxHeight().padding(horizontal=3.dp).clip(RoundedCornerShape(999.dp))
            .background(Brush.verticalGradient(listOf(Color.White.copy(.18f),Color(0xFFFF3D8B).copy(.10f),Color.White.copy(.045f))))
            .border(1.dp,Color.White.copy(.13f),RoundedCornerShape(999.dp)))
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            Tab.values().forEach { t ->
                val active=tab==t
                Column(Modifier.weight(1f).clip(RoundedCornerShape(999.dp)).clickable { select(t) }.padding(vertical=8.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                    Icon(when(t){Tab.HOME->Icons.Rounded.Home;Tab.SEARCH->Icons.Rounded.Search;Tab.LIBRARY->Icons.Rounded.LibraryMusic},null,tint=if(active)Color(0xFFFF6BA7) else Color(0xFFAAA3BD))
                    Text(t.label,color=if(active)White else Color(0xFFAAA3BD),fontSize=10.sp,fontWeight=if(active)FontWeight.Bold else FontWeight.Medium)
                }
            }
        }
    }
}
@Composable private fun SectionHeader(kicker:String,title:String){Row(Modifier.fillMaxWidth().padding(top=3.dp,bottom=7.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.Bottom){Text(kicker,color=Muted,fontSize=9.sp,letterSpacing=1.5.sp);Text(title,color=Lavender,fontSize=10.sp,fontWeight=FontWeight.Bold,letterSpacing=1.sp)}}
@Composable private fun EmptyCard(message:String){GlassCard(Modifier.fillMaxWidth().padding(bottom=5.dp)){Text(message,color=Muted,fontSize=12.sp,modifier=Modifier.padding(16.dp))}}
@Composable private fun GlassCard(modifier:Modifier=Modifier,content:@Composable ColumnScope.()->Unit){Column(modifier.glassSurface(RoundedCornerShape(22.dp),isDarkTheme=true).padding(14.dp),content=content)}
@Composable private fun PermissionPrompt(request:()->Unit){Box(Modifier.fillMaxSize().background(Ink.copy(.96f)).padding(28.dp),contentAlignment=Alignment.Center){Column(horizontalAlignment=Alignment.CenterHorizontally){Icon(Icons.Rounded.LibraryMusic,null,tint=Lavender,modifier=Modifier.size(52.dp));Spacer(Modifier.height(14.dp));Text("Let your music in",color=White,fontSize=22.sp,fontWeight=FontWeight.Bold);Text("SYNESTHESIA needs audio access to find and play music stored on your device. Your files stay on your phone.",color=Muted,fontSize=13.sp);Spacer(Modifier.height(18.dp));Button(request,colors=ButtonDefaults.buttonColors(containerColor=Violet)){Text("Allow music access")}}} }
