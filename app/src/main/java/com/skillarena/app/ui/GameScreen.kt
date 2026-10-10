package com.skillarena.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skillarena.core.cards.Card
import com.skillarena.core.match.MatchPhase
import com.skillarena.core.match.PlayError
import com.skillarena.core.match.MatchResult

@Composable
fun GameScreen(vm: GameViewModel, onExit: () -> Unit) {
    val match = vm.match
    vm.frame // read every frame so the HUD (timer, elixir, hand) recomposes
    val me = match.players[0]
    val selected = me.hand.firstOrNull { it.id == vm.selectedCardId }
    val textMeasurer = rememberTextMeasurer()
    var canvasSize by remember { mutableStateOf(Size.Zero) }

    Column(Modifier.fillMaxSize().background(Color(0xFF1B1B1B))) {
        TopBar(match.players[1].crowns, match.players[0].crowns, match.secondsRemaining, match.phase)

        Box(Modifier.weight(1f).fillMaxWidth()) {
            Canvas(
                Modifier
                    .fillMaxSize()
                    .onSizeChanged { canvasSize = Size(it.width.toFloat(), it.height.toFloat()) }
                    .pointerInput(Unit) {
                        detectTapGestures { offset -> vm.onArenaTap(ArenaTransform(canvasSize).toArena(offset)) }
                    },
            ) {
                vm.frame // read inside draw scope so every frame invalidates the canvas
                drawArena(match, ArenaTransform(size), selected, textMeasurer)
            }

            vm.lastError?.let { ErrorBanner(it, Modifier.align(Alignment.TopCenter)) }
            match.result?.let { ResultOverlay(it, vm.rating, onRematch = vm::startMatch, onExit = onExit) }
        }

        Hand(me.hand, me.nextCard, me.elixir, vm.selectedCardId, vm::onCardTap)
        ElixirBar(me.elixir)
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
        Modifier.fillMaxWidth().padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("👑 $enemyCrowns", color = Color(0xFFE53935), fontSize = 20.sp)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("%d:%02d".format(s / 60, s % 60), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            if (phaseLabel.isNotEmpty()) Text(phaseLabel, color = Color(0xFFE040FB), fontSize = 12.sp)
        }
        Text("👑 $myCrowns", color = Color(0xFF2979FF), fontSize = 20.sp)
    }
}

@Composable
private fun Hand(hand: List<Card>, next: Card, elixir: Float, selectedId: String?, onTap: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Volgende", color = Color.Gray, fontSize = 10.sp)
            CardView(next, affordable = false, selected = false, small = true) {}
        }
        Spacer(Modifier.width(4.dp))
        for (card in hand) {
            CardView(card, affordable = elixir >= card.cost, selected = card.id == selectedId, Modifier.weight(1f)) {
                onTap(card.id)
            }
        }
    }
}

@Composable
private fun CardView(
    card: Card,
    affordable: Boolean,
    selected: Boolean,
    modifier: Modifier = Modifier,
    small: Boolean = false,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier
            .then(if (small) Modifier.size(44.dp, 56.dp) else Modifier.height(84.dp))
            .background(if (affordable) Color(0xFF37474F) else Color(0xFF263238), shape)
            .border(if (selected) 3.dp else 1.dp, if (selected) Color.Yellow else Color.Gray, shape)
            .clickable(onClick = onClick)
            .padding(4.dp),
    ) {
        Text(
            card.name,
            color = if (affordable || small) Color.White else Color.Gray,
            fontSize = if (small) 9.sp else 12.sp,
            modifier = Modifier.align(Alignment.Center),
        )
        Text(
            "${card.cost}",
            color = Color(0xFFE040FB),
            fontWeight = FontWeight.Bold,
            fontSize = if (small) 10.sp else 14.sp,
            modifier = Modifier.align(Alignment.TopStart),
        )
    }
}

@Composable
private fun ElixirBar(elixir: Float) {
    Row(
        Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("${elixir.toInt()}", color = Color(0xFFE040FB), fontWeight = FontWeight.Bold, modifier = Modifier.width(24.dp))
        Box(Modifier.weight(1f).height(16.dp).background(Color(0xFF311B92), RoundedCornerShape(4.dp))) {
            Box(
                Modifier
                    .fillMaxWidth(elixir / 10f)
                    .height(16.dp)
                    .background(Color(0xFFD500F9), RoundedCornerShape(4.dp)),
            )
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
