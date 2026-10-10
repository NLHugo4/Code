package com.skillarena.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.IntSize
import com.skillarena.app.ui.render.GameAssets
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skillarena.app.ui.render.ArenaTransform
import com.skillarena.app.ui.render.Palette
import com.skillarena.app.ui.render.drawBattle
import com.skillarena.app.ui.render.drawCardIcon
import com.skillarena.core.cards.BuildingCard
import com.skillarena.core.cards.Card
import com.skillarena.core.cards.SpellCard
import com.skillarena.core.match.MatchPhase
import com.skillarena.core.match.MatchResult
import com.skillarena.core.match.PlayError

@Composable
fun GameScreen(vm: GameViewModel, assets: GameAssets?, onExit: () -> Unit) {
    val match = vm.match
    vm.frame // read every frame so the HUD (timer, elixir, hand) recomposes
    val me = match.players[0]
    val selected = me.hand.firstOrNull { it.id == vm.selectedCardId }
    val textMeasurer = rememberTextMeasurer()
    var canvasSize by remember { mutableStateOf(Size.Zero) }

    // Game loop, synced to the display refresh rate.
    LaunchedEffect(match) {
        while (true) withFrameNanos(vm::onFrame)
    }

    Column(Modifier.fillMaxSize().background(Color(0xFF14161C))) {
        TopBar(match.players[1].crowns, match.players[0].crowns, match.secondsRemaining, match.phase)

        Box(Modifier.weight(1f).fillMaxWidth()) {
            Canvas(
                Modifier
                    .fillMaxSize()
                    .clipToBounds()
                    .onSizeChanged { canvasSize = Size(it.width.toFloat(), it.height.toFloat()) }
                    .pointerInput(Unit) {
                        detectTapGestures { offset -> vm.onArenaTap(ArenaTransform(canvasSize).toArena(offset)) }
                    },
            ) {
                vm.frame // invalidate the canvas every frame
                drawBattle(match, vm.renderAlpha, vm.now, selected, vm.effects, textMeasurer, assets)
            }

            vm.lastError?.let { ErrorBanner(it, Modifier.align(Alignment.TopCenter)) }
            match.result?.let { ResultOverlay(it, vm.rating, onRematch = vm::startMatch, onExit = onExit) }
        }

        Hand(me.hand, me.nextCard, me.elixir, vm.selectedCardId, vm.now, assets, vm::onCardTap)
        ElixirBar(me.elixir, assets)
    }
}

@Composable
private fun TopBar(enemyCrowns: Int, myCrowns: Int, secondsRemaining: Float, phase: MatchPhase) {
    val s = secondsRemaining.toInt()
    val phaseLabel = when (phase) {
        MatchPhase.DOUBLE_ELIXIR -> "2x elixir"
        MatchPhase.OVERTIME -> "Overtime"
        MatchPhase.TRIPLE_ELIXIR -> "Overtime 3x"
        else -> ""
    }
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Crowns(enemyCrowns, Palette.Red)
        Column(
            Modifier.background(Color(0xFF262A33), RoundedCornerShape(10.dp)).padding(horizontal = 14.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("%d:%02d".format(s / 60, s % 60), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            if (phaseLabel.isNotEmpty()) Text(phaseLabel, color = Palette.Elixir, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Crowns(myCrowns, Palette.Blue)
    }
}

@Composable
private fun Crowns(count: Int, color: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(3) { i ->
            Canvas(Modifier.size(22.dp)) {
                val w = size.width
                val h = size.height
                val crown = Path().apply {
                    moveTo(w * 0.1f, h * 0.8f)
                    lineTo(w * 0.1f, h * 0.3f)
                    lineTo(w * 0.32f, h * 0.55f)
                    lineTo(w * 0.5f, h * 0.15f)
                    lineTo(w * 0.68f, h * 0.55f)
                    lineTo(w * 0.9f, h * 0.3f)
                    lineTo(w * 0.9f, h * 0.8f)
                    close()
                }
                drawPath(crown, if (i < count) color else Color(0xFF3A3F4B))
            }
        }
    }
}

@Composable
private fun Hand(
    hand: List<Card>, next: Card, elixir: Float, selectedId: String?, now: Float, assets: GameAssets?, onTap: (String) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp).padding(top = 10.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Volgende", color = Color.Gray, fontSize = 10.sp)
            CardView(next, elixir = 10f, selected = false, now = now, assets = assets, small = true) {}
        }
        Spacer(Modifier.width(2.dp))
        for (card in hand) {
            CardView(card, elixir, selected = card.id == selectedId, now = now, assets = assets, modifier = Modifier.weight(1f)) {
                onTap(card.id)
            }
        }
    }
}

private fun cardBrush(card: Card) = when (card) {
    is SpellCard -> Brush.verticalGradient(listOf(Color(0xFF7B3FA0), Color(0xFF3E1F5C)))
    is BuildingCard -> Brush.verticalGradient(listOf(Color(0xFF9C6B3C), Color(0xFF4E3420)))
    else -> Brush.verticalGradient(listOf(Color(0xFF3F7BD6), Color(0xFF1C3A6E)))
}

@Composable
private fun CardView(
    card: Card,
    elixir: Float,
    selected: Boolean,
    now: Float,
    assets: GameAssets?,
    modifier: Modifier = Modifier,
    small: Boolean = false,
    onClick: () -> Unit,
) {
    val affordable = elixir >= card.cost
    val art = assets?.card(card.id)
    if (art != null) {
        // The card art (graphics/png/kaarten) already shows cost, name and count.
        val shape = RoundedCornerShape(8.dp)
        Box(
            modifier
                .offset(y = if (selected) (-10).dp else 0.dp)
                .then(if (small) Modifier.width(40.dp) else Modifier)
                .aspectRatio(art.width.toFloat() / art.height)
                .then(if (selected) Modifier.border(3.dp, Palette.Gold, shape) else Modifier)
                .clickable(onClick = onClick),
        ) {
            Image(art, contentDescription = card.name, modifier = Modifier.fillMaxSize())
            if (!affordable) {
                val missing = ((card.cost - elixir) / card.cost).coerceIn(0f, 1f)
                Box(Modifier.fillMaxWidth().fillMaxHeight(missing).background(Color.Black.copy(alpha = 0.55f), shape))
            }
        }
        return
    }
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier
            .offset(y = if (selected) (-10).dp else 0.dp)
            .then(if (small) Modifier.size(46.dp, 58.dp) else Modifier.height(92.dp))
            .background(cardBrush(card), shape)
            .border(if (selected) 3.dp else 1.5.dp, if (selected) Palette.Gold else Color(0xFF0D0F14), shape)
            .clickable(onClick = onClick),
    ) {
        Canvas(Modifier.fillMaxSize().padding(bottom = if (small) 0.dp else 16.dp)) { drawCardIcon(card.id, now) }
        if (!small) {
            Text(
                card.name,
                color = Color.White,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(bottom = 3.dp),
            )
        }
        // Not affordable yet: dark overlay that shrinks as elixir fills up
        if (!affordable) {
            val missing = ((card.cost - elixir) / card.cost).coerceIn(0f, 1f)
            Box(Modifier.fillMaxWidth().fillMaxHeight(missing).background(Color.Black.copy(alpha = 0.55f), shape))
        }
        Box(
            Modifier
                .padding(3.dp)
                .size(if (small) 16.dp else 20.dp)
                .background(Palette.Elixir, CircleShape)
                .border(1.5.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text("${card.cost}", color = Color.White, fontSize = if (small) 10.sp else 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ElixirBar(elixir: Float, assets: GameAssets?) {
    val empty = assets?.get("ui/elixerbalk_leeg.png")
    val fill = assets?.get("ui/elixerbalk_vulling.png")
    if (empty != null && fill != null) {
        val measurer = rememberTextMeasurer()
        Canvas(
            Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 8.dp, bottom = 10.dp)
                .aspectRatio(empty.width.toFloat() / empty.height),
        ) {
            val dst = IntSize(size.width.toInt(), size.height.toInt())
            drawImage(empty, dstSize = dst, filterQuality = FilterQuality.Medium)
            // Fill runs from x = 84 to 620 of 640 (graphics/LEESMIJ.md); clip it to the current elixir.
            val fillRight = size.width * (84f + 536f * (elixir / 10f).coerceIn(0f, 1f)) / 640f
            clipRect(right = fillRight) { drawImage(fill, dstSize = dst, filterQuality = FilterQuality.Medium) }
            // Number on the droplet at the left of the bar
            val label = measurer.measure(
                "${elixir.toInt()}",
                TextStyle(color = Color.White, fontSize = (size.height * 0.32f).toSp(), fontWeight = FontWeight.Bold),
            )
            drawText(label, topLeft = Offset(size.width * 0.053f - label.size.width / 2f, size.height * 0.6f - label.size.height / 2f))
        }
        return
    }
    Row(
        Modifier.fillMaxWidth().padding(start = 10.dp, end = 10.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(30.dp).background(Palette.Elixir, CircleShape).border(2.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text("${elixir.toInt()}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
        Spacer(Modifier.width(6.dp))
        Canvas(Modifier.weight(1f).height(18.dp)) {
            val gap = 3.dp.toPx()
            val seg = (size.width - gap * 9) / 10f
            for (i in 0 until 10) {
                val x = i * (seg + gap)
                val fill = (elixir - i).coerceIn(0f, 1f)
                drawRoundRect(Palette.ElixirDark, Offset(x, 0f), Size(seg, size.height), androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()))
                if (fill > 0f) {
                    drawRoundRect(
                        Brush.verticalGradient(listOf(Color(0xFFF08CFF), Palette.Elixir)),
                        Offset(x, 0f), Size(seg * fill, size.height), androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()),
                    )
                }
            }
        }
    }
}

@Composable
private fun ErrorBanner(error: PlayError, modifier: Modifier) {
    val msg = when (error) {
        PlayError.NOT_ENOUGH_ELIXIR -> "Niet genoeg elixir"
        PlayError.INVALID_POSITION -> "Hier kun je niet plaatsen"
        PlayError.NOT_IN_HAND -> "Kaart niet in je hand"
        PlayError.MATCH_OVER -> "De wedstrijd is voorbij"
    }
    Text(
        msg,
        color = Color.White,
        modifier = modifier.padding(8.dp).background(Color(0xCCB71C1C), RoundedCornerShape(6.dp)).padding(8.dp),
    )
}

@Composable
private fun ResultOverlay(result: MatchResult, rating: Int, onRematch: () -> Unit, onExit: () -> Unit) {
    val title = when (result.winner) {
        0 -> "Gewonnen!"
        null -> "Gelijkspel"
        else -> "Verloren"
    }
    Box(Modifier.fillMaxSize().background(Color(0xAA000000)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, color = Color.White, style = MaterialTheme.typography.displaySmall)
            Text("${result.crowns[0]} - ${result.crowns[1]}", color = Color.White, fontSize = 28.sp)
            Text("Nieuwe rating: $rating", color = Color.LightGray)
            Spacer(Modifier.height(16.dp))
            Button(onClick = onRematch) { Text("Opnieuw") }
            Spacer(Modifier.height(8.dp))
            Button(onClick = onExit) { Text("Menu") }
        }
    }
}
