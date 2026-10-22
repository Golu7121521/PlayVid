package com.playvid.presentation.reels

import android.content.ClipboardManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.playvid.data.reel.ReelHistoryItem
import com.playvid.download.DownloadRepository
import com.playvid.download.DownloadStatus
import com.playvid.presentation.AppViewModelProvider
import kotlinx.coroutines.delay

// ---------------------------------------------------------------- palette (same colours as i_s.html)

private class Pal(val dark: Boolean) {
    val bgTop = if (dark) Color(0xFF0F172A) else Color(0xFFF8FAFC)
    val bgBottom = if (dark) Color(0xFF020617) else Color(0xFFE2E8F0)
    val glass = if (dark) Color(0x990F172A) else Color(0xBFFFFFFF)
    val glassBorder = if (dark) Color(0x14FFFFFF) else Color(0x80FFFFFF)
    val text = if (dark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val muted = if (dark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val card = if (dark) Color(0xFF1E293B) else Color.White
    val dangerBg = if (dark) Color(0x26EF4444) else Color(0x1ADC2743)
    val dangerText = if (dark) Color(0xFFF87171) else Color(0xFFDC2743)
    val track = if (dark) Color(0x0DFFFFFF) else Color(0x0D000000)
    val videoBtn = if (dark) listOf(Color(0xFF38BDF8), Color(0xFF0284C7)) else listOf(Color(0xFF1E293B), Color(0xFF0F172A))
    val audioBtn = listOf(Color(0xFF7C3AED), Color(0xFF6D28D9))
}

private val IgColors = listOf(
    Color(0xFFF09433), Color(0xFFE6683C), Color(0xFFDC2743), Color(0xFFCC2366), Color(0xFFBC1888)
)

/** 45deg CSS gradient: bottom-left -> top-right. */
private val PrimaryGrad = Brush.linearGradient(
    colors = IgColors,
    start = Offset(0f, Float.POSITIVE_INFINITY),
    end = Offset(Float.POSITIVE_INFINITY, 0f)
)

private val ACTIVE = setOf(DownloadStatus.QUEUED, DownloadStatus.DOWNLOADING, DownloadStatus.MERGING, DownloadStatus.PAUSED)

/** Same as the web version: preview box is full width, max 260dp tall, video letterboxed (contain). */
private val PREVIEW_HEIGHT = 260.dp

// ---------------------------------------------------------------- small helpers

@Composable
private fun Txt(
    text: String,
    color: Color,
    size: Float,
    weight: FontWeight = FontWeight.Normal,
    modifier: Modifier = Modifier,
    align: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE
) {
    BasicText(
        text = text,
        modifier = modifier,
        style = TextStyle(color = color, fontSize = size.sp, fontWeight = weight, textAlign = align ?: TextAlign.Unspecified),
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis
    )
}

private fun Modifier.pressable(enabled: Boolean = true, down: Float = 0.97f, onClick: () -> Unit): Modifier = composed {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) down else 1f, tween(100), label = "press")
    this
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .clickable(interactionSource = src, indication = null, enabled = enabled, onClick = onClick)
}

private tailrec fun Context.findLifecycleOwner(): LifecycleOwner? = when (this) {
    is LifecycleOwner -> this
    is ContextWrapper -> baseContext.findLifecycleOwner()
    else -> null
}

@Composable
private fun IgLogo(size: androidx.compose.ui.unit.Dp) {
    Canvas(Modifier.size(size)) {
        val s = this.size.minDimension / 512f
        val brush = Brush.linearGradient(
            colors = IgColors,
            start = Offset(0f, this.size.height),
            end = Offset(this.size.width, 0f)
        )
        val sw = 45.4f * s
        drawRoundRect(
            brush = brush,
            topLeft = Offset(72.2f * s, 72.2f * s),
            size = Size(367.6f * s, 367.6f * s),
            cornerRadius = CornerRadius(105f * s),
            style = Stroke(sw)
        )
        drawCircle(brush = brush, radius = 108.9f * s, center = center, style = Stroke(sw))
        drawCircle(brush = brush, radius = 30.2f * s, center = Offset(390.5f * s, 121.5f * s))
    }
}

// ---------------------------------------------------------------- screen

@Composable
fun ReelScreen(vm: ReelViewModel = viewModel(factory = AppViewModelProvider.Factory)) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val jobs by DownloadRepository.jobs.collectAsStateWithLifecycle()
    val pal = remember(ui.dark) { Pal(ui.dark) }
    val context = LocalContext.current
    val focus: FocusManager = LocalFocusManager.current

    // toast (same pill as the web version)
    var toastText by remember { mutableStateOf("") }
    var toastVisible by remember { mutableStateOf(false) }
    var toastKey by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        vm.messages.collect {
            toastText = it
            toastVisible = true
            toastKey++
        }
    }
    LaunchedEffect(toastKey) {
        if (toastKey > 0) {
            delay(2500)
            toastVisible = false
        }
    }

    val paste = {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val text = cm.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(context)?.toString()?.trim()
        if (text.isNullOrBlank()) vm.message("Clipboard permission required.") else vm.onPaste(text)
    }
    val submit = {
        focus.clearFocus()
        vm.extract()
    }
    val shareLink: (String) -> Unit = { link ->
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, link)
        }
        context.startActivity(Intent.createChooser(send, "InstaSave Media"))
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(pal.bgTop, pal.bgBottom))),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            Modifier
                .widthIn(max = 452.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            // ---- header
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    IgLogo(26.dp)
                    Spacer(Modifier.size(8.dp))
                    BasicText(
                        "Insta Reel Downloader",
                        style = TextStyle(brush = PrimaryGrad, fontSize = 20.sp, fontWeight = FontWeight.Black),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(pal.glass)
                        .border(1.dp, pal.glassBorder, CircleShape)
                        .pressable(down = 0.9f) { vm.toggleTheme() },
                    contentAlignment = Alignment.Center
                ) { Txt(if (ui.dark) "☀️" else "🌙", pal.text, 17.6f) }
            }

            // ---- main card
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(pal.glass)
                    .border(1.dp, pal.glassBorder, RoundedCornerShape(28.dp))
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val bounce = rememberInfiniteTransition(label = "logo")
                val dy by bounce.animateFloat(
                    0f, -8f,
                    infiniteRepeatable(tween(1250, easing = LinearEasing), RepeatMode.Reverse), label = "dy"
                )
                Box(Modifier.graphicsLayer { translationY = dy.dp.toPx(); scaleX = 1f + dy / -8f * 0.03f; scaleY = 1f + dy / -8f * 0.03f }) {
                    IgLogo(65.dp)
                }
                Spacer(Modifier.height(10.dp))
                BasicText(
                    "Download Reel",
                    style = TextStyle(color = pal.text, fontSize = 28.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp)
                )
                Spacer(Modifier.height(6.dp))
                Txt("Media Extraction by \u200B◥◣GKV◢◤", pal.muted, 14.4f)
                Spacer(Modifier.height(24.dp))

                // ---- input group
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(pal.card)
                        .border(1.dp, pal.glassBorder, RoundedCornerShape(16.dp))
                        .padding(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicTextField(
                        value = ui.input,
                        onValueChange = vm::onInput,
                        singleLine = true,
                        textStyle = TextStyle(color = pal.text, fontSize = 15.2.sp, fontWeight = FontWeight.SemiBold),
                        cursorBrush = SolidColor(pal.text),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(onGo = { submit() }),
                        modifier = Modifier.weight(1f),
                        decorationBox = { inner ->
                            Box(Modifier.padding(16.dp), contentAlignment = Alignment.CenterStart) {
                                if (ui.input.isEmpty()) Txt("Paste Instagram link...", pal.muted, 15.2f, FontWeight.SemiBold)
                                inner()
                            }
                        }
                    )
                    Box(
                        Modifier.padding(horizontal = 10.dp).pressable(down = 0.9f) { paste() },
                        contentAlignment = Alignment.Center
                    ) { Txt("📋", pal.text, 17.6f) }
                    Box(
                        Modifier
                            .padding(end = 6.dp)
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(pal.dangerBg)
                            .pressable(down = 0.88f) { focus.clearFocus(); vm.clear() },
                        contentAlignment = Alignment.Center
                    ) {
                        val c = pal.dangerText
                        Canvas(Modifier.size(14.dp)) {
                            val w = 2.5.dp.toPx()
                            drawLine(c, Offset(0f, 0f), Offset(size.width, size.height), w, StrokeCap.Round)
                            drawLine(c, Offset(size.width, 0f), Offset(0f, size.height), w, StrokeCap.Round)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))

                // ---- primary button
                Box(
                    Modifier
                        .fillMaxWidth()
                        .alpha(if (ui.loading) 0.7f else 1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(PrimaryGrad)
                        .pressable(enabled = !ui.loading) { submit() }
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) { Txt("Extract Media", Color.White, 16f, FontWeight.Bold) }

                // ---- loader
                if (ui.loading) {
                    Spacer(Modifier.height(20.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Txt(ui.status, pal.text, 13.6f, FontWeight.Bold, Modifier.weight(1f))
                        Txt("${ui.progress}%", pal.text, 13.6f, FontWeight.Bold)
                    }
                    Spacer(Modifier.height(8.dp))
                    val frac by animateFloatAsState(ui.progress / 100f, tween(200), label = "progress")
                    Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(10.dp)).background(pal.track)) {
                        Box(Modifier.fillMaxWidth(frac).height(6.dp).clip(RoundedCornerShape(10.dp)).background(PrimaryGrad))
                    }
                }

                // ---- result
                ui.result?.let { res ->
                    Spacer(Modifier.height(24.dp))
                    var shown by remember(res) { mutableStateOf(false) }
                    LaunchedEffect(res) { shown = true }
                    AnimatedVisibility(
                        visible = shown,
                        enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { 20 }
                    ) {
                        Column {
                            ReelPreview(res.previewUrl, res.thumb)
                            Spacer(Modifier.height(16.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                res.links.forEach { l ->
                                    val key = ReelViewModel.buttonKey(l.link, l.isAudio)
                                    Pill(
                                        label = if (l.isAudio) "🎶 Download Audio MP3" else "🎬 Download Video MP4",
                                        busy = isBusy(ui.downloads[key], jobs),
                                        pal = pal,
                                        colors = if (l.isAudio) pal.audioBtn else pal.videoBtn
                                    ) { vm.download(l.link, l.isAudio) }
                                }
                                if (!res.hasAudio) {
                                    val key = ReelViewModel.buttonKey(res.previewUrl, true)
                                    Pill(
                                        label = "🎵 Download Audio Track (M4A/MP3)",
                                        busy = isBusy(ui.downloads[key], jobs),
                                        pal = pal,
                                        colors = pal.audioBtn
                                    ) { vm.download(res.previewUrl, true) }
                                }
                                Pill("🔗 Share File Link", busy = false, pal = pal, colors = null) { shareLink(res.previewUrl) }
                            }
                        }
                    }
                }
            }

            // ---- history
            if (ui.history.isNotEmpty()) {
                Spacer(Modifier.height(36.dp))
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(pal.glass)
                        .border(1.dp, pal.glassBorder, RoundedCornerShape(24.dp))
                        .padding(20.dp)
                ) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Txt("History", pal.text, 16f, FontWeight.Bold, Modifier.weight(1f))
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(pal.dangerBg)
                                .pressable(down = 0.93f) { vm.clearHistory() }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) { Txt("Clear All", pal.dangerText, 12f, FontWeight.Bold) }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (i in 0 until 4) {
                            val item = ui.history.getOrNull(i)
                            Box(Modifier.weight(1f)) {
                                if (item != null) {
                                    HistoryCell(item, pal, onOpen = { vm.openHistory(item) }, onDelete = { vm.removeHistory(i) })
                                }
                            }
                        }
                    }
                }
            }

            // ---- footer
            Spacer(Modifier.height(24.dp))
            val footerColor = if (ui.dark) Color(0xFFF8FAFC) else pal.text
            Column(Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                BasicText(
                    buildAnnotatedString {
                        append("We respect all content creators and do not support unauthorized reposting, copyright infringement, or misuse of downloaded media.")
                    },
                    style = TextStyle(color = footerColor, fontSize = 12.sp, lineHeight = 18.sp, textAlign = TextAlign.Center)
                )
                Spacer(Modifier.height(6.dp))
                BasicText(
                    buildAnnotatedString {
                        append("This tool is for ")
                        withStyle(SpanStyle(brush = PrimaryGrad, fontWeight = FontWeight.Bold)) { append("personal & fair-use purposes only") }
                        append(". All rights belong to their respective creators and owners.")
                    },
                    style = TextStyle(color = pal.muted, fontSize = 12.sp, lineHeight = 18.sp, textAlign = TextAlign.Center)
                )
            }
        }

        // ---- toast
        var lastToast by remember { mutableStateOf("") }
        if (toastText.isNotEmpty()) lastToast = toastText
        AnimatedVisibility(
            visible = toastVisible,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 30.dp),
            enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { it },
            exit = fadeOut(tween(300)) + slideOutVertically(tween(300)) { it }
        ) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF0F172A))
                    .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(24.dp))
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) { Txt(lastToast, Color.White, 13.6f, FontWeight.SemiBold, align = TextAlign.Center) }
        }
    }
}

private fun isBusy(jobId: Int?, jobs: List<com.playvid.download.DownloadJob>): Boolean =
    jobId != null && jobs.any { it.id == jobId && it.status in ACTIVE }

@Composable
private fun Pill(label: String, busy: Boolean, pal: Pal, colors: List<Color>?, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    val base = Modifier
        .fillMaxWidth()
        .alpha(if (busy) 0.7f else 1f)
        .clip(shape)
    val styled = if (colors != null) {
        base.background(Brush.linearGradient(colors, Offset.Zero, Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)))
    } else {
        base.background(pal.card).border(1.dp, pal.glassBorder, shape)
    }
    Box(
        styled.pressable(enabled = !busy) { onClick() }.padding(horizontal = 20.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Txt(
            if (busy) "⏳ Downloading..." else label,
            if (colors != null) Color.White else pal.text,
            15.2f, FontWeight.Bold, align = TextAlign.Center
        )
    }
}

@Composable
private fun HistoryCell(item: ReelHistoryItem, pal: Pal, onOpen: () -> Unit, onDelete: () -> Unit) {
    val bmp by rememberRemoteImage(item.thumb)
    val shape = RoundedCornerShape(12.dp)
    Box(Modifier.fillMaxWidth().aspectRatio(1f)) {
        Box(
            Modifier
                .fillMaxSize()
                .clip(shape)
                .background(pal.card)
                .border(1.dp, pal.glassBorder, shape)
                .pressable(down = 0.95f) { onOpen() }
        ) {
            bmp?.let { Image(it, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
        }
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .offset(x = 6.dp, y = (-6).dp)
                .size(22.dp)
                .clip(CircleShape)
                .background(Color(0xFFDC2743))
                .border(2.dp, pal.card, CircleShape)
                .pressable(down = 0.82f) { onDelete() },
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.size(9.dp)) {
                val w = 2.dp.toPx()
                drawLine(Color.White, Offset(0f, 0f), Offset(size.width, size.height), w, StrokeCap.Round)
                drawLine(Color.White, Offset(size.width, 0f), Offset(0f, size.height), w, StrokeCap.Round)
            }
        }
    }
}

/** In-app reel preview (ExoPlayer), thumbnail shown until the first frame is ready. */
@Composable
private fun ReelPreview(url: String, posterUrl: String?) {
    val context = LocalContext.current
    val poster by rememberRemoteImage(posterUrl)
    val player = remember(url) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(url))
            prepare()
            playWhenReady = false
        }
    }
    var rendered by remember(url) { mutableStateOf(false) }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onRenderedFirstFrame() { rendered = true }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }
    val owner = context.findLifecycleOwner()
    DisposableEffect(owner, player) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_STOP) player.pause() }
        owner?.lifecycle?.addObserver(obs)
        onDispose { owner?.lifecycle?.removeObserver(obs) }
    }

    Box(
        Modifier
            .fillMaxWidth()
            .height(PREVIEW_HEIGHT)
            .clip(RoundedCornerShape(18.dp))
            .background(Color.Black)
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = true
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    setBackgroundColor(android.graphics.Color.BLACK)
                    this.player = player
                }
            },
            update = { it.player = player }
        )
        val p = poster
        if (!rendered && p != null) {
            Image(p, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
        }
    }
}
