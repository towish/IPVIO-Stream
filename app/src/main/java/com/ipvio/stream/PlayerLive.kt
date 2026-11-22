package com.ipvio.stream

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import coil.compose.AsyncImage
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.RowScope

/** Channel info block: logo, number, name, running show with progress and "Next". */
@Composable
fun LiveInfoBlock(item: Item?, title: String, number: Int, epg: List<EpgEntry>, modifier: Modifier = Modifier) {
    val nowE = Epg.current(epg)
    val nextE = if (nowE != null) epg.firstOrNull { it.start >= nowE.end } else null
    val logo = item?.logo
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        Box(
            Modifier.size(72.dp).clip(RoundedCornerShape(DS.RadiusM)).background(Color(0x24FFFFFF)),
            contentAlignment = Alignment.Center
        ) {
            if (logo != null) AsyncImage(logo, null, Modifier.fillMaxSize().padding(8.dp), contentScale = ContentScale.Fit)
            else Text(raw = true, text = remember(title) { ChannelMenuLogic.monogram(title) }, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
        }
        Column(Modifier.widthIn(max = 760.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (number > 0) Text("$number", color = Accent2, fontSize = 22.sp, fontWeight = FontWeight.Bold, style = TNUM)
                Text(raw = true, text = title, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (nowE != null) {
                val frac = ((System.currentTimeMillis() / 1000 - nowE.start).toFloat() / (nowE.end - nowE.start).coerceAtLeast(1)).coerceIn(0f, 1f)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("${hhmm(nowE.start)}–${hhmm(nowE.end)}", color = Accent2, fontSize = 18.sp, fontWeight = FontWeight.Bold, style = TNUM, maxLines = 1)
                    Text(raw = true, text = nowE.title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(50)).background(DS.Overlay3)) {
                    Box(Modifier.fillMaxWidth(frac).fillMaxHeight().background(Accent1))
                }
                if (nextE != null) {
                    Text("Danach ${hhmm(nextE.start)}  ${nextE.title}", color = PT.T2, fontSize = 15.sp, style = TNUM, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            } else {
                val supported = item != null && Epg.supported(item)
                Text(
                    if (supported) "Kein Programm für diesen Sender" else "Kein Programm verfügbar",
                    color = Muted, fontSize = 15.sp, maxLines = 1
                )
            }
        }
    }
}

/**
 * Info bar when switching (1.2): across the full width at the bottom edge, soft dark gradient instead of a box.
 * Logo, LIVE, number, channel, quality, running show with time and progress, "Next" on the right.
 */
@Composable
fun LiveBanner(item: Item?, title: String, number: Int, epg: List<EpgEntry>, modifier: Modifier = Modifier) {
    val nowE = Epg.current(epg)
    val nextE = if (nowE != null) epg.firstOrNull { it.start >= nowE.end } else null
    val logo = item?.logo
    Box(modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xE6000000))))) {
        Row(
            Modifier.fillMaxWidth().padding(start = PT.MarginX, end = PT.MarginX, top = 56.dp, bottom = PT.MarginY),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            Box(
                Modifier.size(84.dp).clip(RoundedCornerShape(DS.RadiusM)).background(Color(0x33FFFFFF)),
                contentAlignment = Alignment.Center
            ) {
                if (logo != null) AsyncImage(logo, null, Modifier.fillMaxSize().padding(8.dp), contentScale = ContentScale.Fit)
                else Text(raw = true, text = remember(title) { ChannelMenuLogic.monogram(title) }, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("LIVE", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.clip(RoundedCornerShape(50)).background(Color(0xFFE5484D)).padding(horizontal = 12.dp, vertical = 3.dp))
                    if (number > 0) Text("$number", color = Accent2, fontSize = 22.sp, fontWeight = FontWeight.Bold, style = TNUM)
                    Text(raw = true, text = title, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    item?.quality?.takeIf { it.isNotBlank() }?.let {
                        Text(raw = true, text = it, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0x40FFFFFF)).padding(horizontal = 8.dp, vertical = 2.dp))
                    }
                }
                if (nowE != null) {
                    val frac = ((System.currentTimeMillis() / 1000 - nowE.start).toFloat() / (nowE.end - nowE.start).coerceAtLeast(1)).coerceIn(0f, 1f)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(raw = true, text = nowE.title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                        Text("${hhmm(nowE.start)}–${hhmm(nowE.end)}", color = PT.T2, fontSize = 17.sp, style = TNUM, maxLines = 1)
                    }
                    Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(50)).background(Color(0x40FFFFFF))) {
                        Box(Modifier.fillMaxWidth(frac).fillMaxHeight().background(Accent1))
                    }
                } else {
                    Text(if (item != null && Epg.supported(item)) "Kein Programm für diesen Sender" else "Kein Programm verfügbar", color = PT.T2, fontSize = 16.sp, maxLines = 1)
                }
            }
            if (nextE != null) {
                Column(Modifier.widthIn(max = 360.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Danach", color = PT.T2, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(raw = true, text = "${hhmm(nextE.start)}  ${nextE.title}", color = Color.White, fontSize = 17.sp, style = TNUM, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

/**
 * Channel list (Left in the live player): optional category column, channel column and on the right an info bar at the
 * bottom of the picture.
 *
 * T-027 (fluid side menu):
 *  - Categories: a focus step no longer switches the channel list. The list follows after a short rest
 *    ([ChannelMenuLogic.PREVIEW_DELAY_MS]) and never while Up/Down is held; OK or Right opens the category at once.
 *  - Same category order as the Live TV tab (provider order, Québec first, own order, hidden categories left out).
 *  - The category column opens scrolled to the selected category, so the focus always finds its row.
 *  - Rows have a fixed height, load the program off the main thread and show the channel number and a monogram
 *    when there is no logo. The focused channel lives in this composable: a step redraws only the rows and the info bar.
 *  - Lookups (by id, by category, channel numbers) are built once per channel list in the background.
 */
@Composable
fun ChannelOverlay(
    liveAll: List<Item>, curId: String, curItem: Item?,
    chanCat: String, onChanCat: (String) -> Unit,
    showCats: Boolean, onShowCats: (Boolean) -> Unit,
    focusedCh: Item?, onFocusedCh: (Item) -> Unit,
    inHeader: Boolean, onInHeader: (Boolean) -> Unit,
    chanFocus: FocusRequester, catsFocus: FocusRequester, headerFocus: FocusRequester,
    onClose: () -> Unit, onPick: (Item, List<Item>) -> Unit
) {
    val ctx = LocalContext.current
    CatOrder.init(ctx)
    HiddenCats.init(ctx)
    // Small lists are indexed at once, large ones in the background (the first opening shows "All channels" meanwhile)
    val ixState = produceState(MenuIndexCache.peek(liveAll) ?: if (liveAll.size <= 4000) MenuIndexCache.build(liveAll) else null, liveAll) {
        value = MenuIndexCache.peek(liveAll) ?: withContext(Dispatchers.Default) { MenuIndexCache.build(liveAll) }
    }
    val ix = ixState.value ?: EMPTY_INDEX
    // Remembered: the lists only change with their sources
    val favItems = remember(ix, Library.favs.size) { Library.favs.mapNotNull { ix.byId[it] } }
    val favCats = remember(ix, CatFavs.list.size, HiddenCats.list.size) { CatFavs.list.filter { it in ix.groups && it !in HiddenCats.list } }
    val qc = QcOrder.key()
    val provOrder = PlaylistStore.catOrder
    val catNames = remember(ix, favCats, CatOrder.version, HiddenCats.list.size, qc, provOrder) {
        ChannelMenuLogic.categories(
            ix.groups.keys, provOrder, favCats, HiddenCats.list.toList(),
            if (qc >= 0) GuideCollectionLogic::pinnedFirst else null
        ) { CatOrder.sorted(CatOrder.LIVE, it) }
    }
    // Own channel lists (created in the Live TV area with the menu key)
    val myLists = ChannelLists.lists.toList()
    val curList = ChannelLists.byKey(chanCat)
    val chanList = remember(chanCat, favItems, liveAll, curList, curList?.ids?.size, ix) {
        when {
            chanCat == FAV_CAT -> favItems
            chanCat == "ALL" -> liveAll
            curList != null -> ChannelListLogic.resolve(curList, ix.byId)
            else -> ix.groups[chanCat].orEmpty()
        }
    }
    val catRows = remember(ix, favItems.size, myLists, catNames, liveAll.size) {
        buildList {
            add(CatEntry(FAV_CAT, "Favoriten", favItems.size, AIcon.Star, raw = false, group = false))
            for (l in myLists) add(CatEntry(ChannelListLogic.key(l), l.name, ChannelListLogic.resolve(l, ix.byId).size, AIcon.Lists, raw = true, group = false))
            add(CatEntry("ALL", "Alle Sender", liveAll.size, null, raw = false, group = false))
            for (g in catNames) add(CatEntry(g, g, ix.groups[g]?.size ?: 0, null, raw = false, group = true))
        }
    }
    val headerItems = remember(myLists, favCats) { listOf("ALL", FAV_CAT) + myLists.map { ChannelListLogic.key(it) } + favCats }
    // The highlighted channel is kept here: only the info bar reads it, so a step in the list does not redraw the player
    val focus = remember { mutableStateOf(focusedCh ?: curItem) }
    val reportFocus by androidx.compose.runtime.rememberUpdatedState(onFocusedCh)
    LaunchedEffect(Unit) {
        androidx.compose.runtime.snapshotFlow { focus.value }.collectLatest { f -> if (f != null) { kotlinx.coroutines.delay(600); reportFocus(f) } }
    }
    val panelShape = RoundedCornerShape(topEnd = DS.RadiusXl, bottomEnd = DS.RadiusXl)

    Row(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxHeight().clip(panelShape)
                .background(Brush.horizontalGradient(listOf(Bg.copy(alpha = 0.98f), Bg.copy(alpha = 0.94f))))
        ) {
            if (showCats) {
                CategoryColumn(catRows, chanCat, catsFocus, onChanCat, onShowCats) { g -> CatFavs.toggle(ctx, g) }
                Box(Modifier.fillMaxHeight().padding(vertical = 24.dp).width(1.dp).background(DS.Hairline))
            }
            ChannelColumn(
                chanCat, curList, chanList, headerItems, ix.numOf, curId, showCats, inHeader, onInHeader, onShowCats, onClose,
                onChanCat, chanFocus, headerFocus, focus, onPick
            )
        }
        // Program info: flat bar at the bottom over the picture. Left out while the categories are open (no room, not needed).
        if (!showCats) InfoBar(focus, Modifier.weight(1f))
    }
}

/** Lookups of the channel menu for one channel list. */
private class MenuIndex(val list: List<Item>, val byId: Map<String, Item>, val groups: Map<String, List<Item>>, val numOf: Map<String, Int>)

private val EMPTY_INDEX = MenuIndex(emptyList(), emptyMap(), emptyMap(), emptyMap())

/** Built once per channel list: the menu is opened again and again while watching, with the same list. */
private object MenuIndexCache {
    @Volatile private var cur: MenuIndex? = null

    fun peek(l: List<Item>): MenuIndex? = cur?.takeIf { it.list === l }

    fun build(l: List<Item>): MenuIndex {
        peek(l)?.let { return it }
        val byId = HashMap<String, Item>(l.size * 2)
        val g = LinkedHashMap<String, MutableList<Item>>()
        for (it in l) { byId[it.id] = it; g.getOrPut(it.group) { ArrayList() }.add(it) }
        // Channel numbers: the same as Live TV and the guide (Bell numbers with the Québec order, T-029)
        return MenuIndex(l, byId, g, QcOrder.numbers(l)).also { cur = it }
    }
}

/** One row of the category column. [group] = provider category (can be a favorite category). */
private class CatEntry(val key: String, val label: String, val count: Int, val icon: AIcon?, val raw: Boolean, val group: Boolean)

/** Surfaces of the menu rows: focus = light glass with a frame in the highlight color, as in the Live TV tab. */
private val RowFocusFill = Color(0x2EFFFFFF)
private val RowSelectedFill = Color(0x12FFFFFF)

/**
 * Held Up/Down: limited steps per second and no scroll animation per step (as in Live TV). Also used by the TV guide.
 * T-030: the limit loosens the longer the key is held (about 9, then 16, then 28 steps per second, [ChannelMenuLogic.holdGapMs]).
 */
internal fun Modifier.liveHoldThrottle(): Modifier = onPreviewKeyEvent { e ->
    if (e.type == KeyEventType.KeyDown && (e.key == Key.DirectionDown || e.key == Key.DirectionUp) && e.nativeKeyEvent.repeatCount > 0) {
        val now = android.os.SystemClock.uptimeMillis()
        LiveHold.lastRepeat = now
        val gap = ChannelMenuLogic.holdGapMs(e.nativeKeyEvent.eventTime - e.nativeKeyEvent.downTime)
        if (now - LiveHold.lastStep < gap) true else { LiveHold.lastStep = now; false }
    } else false
}

@Composable
private fun CategoryColumn(
    rows: List<CatEntry>, chanCat: String, catsFocus: FocusRequester,
    onChanCat: (String) -> Unit, onShowCats: (Boolean) -> Unit, onFav: (String) -> Unit
) {
    val cur by androidx.compose.runtime.rememberUpdatedState(chanCat)
    // A hidden or vanished category is not in the column: the focus goes to "All channels" instead of nowhere
    val focusKey = if (rows.any { it.key == chanCat }) chanCat else "ALL"
    val cs = rememberLazyListState(ChannelMenuLogic.firstVisible(rows.indexOfFirst { it.key == focusKey }))
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    // Category under the focus: its channels are shown after a short rest, not on every step
    var hover by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(hover) {
        val h = hover ?: return@LaunchedEffect
        if (h == cur) return@LaunchedEffect
        kotlinx.coroutines.delay(ChannelMenuLogic.PREVIEW_DELAY_MS)
        while (LiveHold.active()) kotlinx.coroutines.delay(80)
        onChanCat(h)
    }
    Column(
        Modifier.fillMaxHeight().width(290.dp).padding(start = 20.dp, end = 12.dp, top = 24.dp, bottom = 16.dp)
            .onPreviewKeyEvent { e ->
                if (e.type != KeyEventType.KeyDown) false
                else if (e.key == Key.DirectionRight) {
                    // Right = to the channels of the focused category, also when its preview has not started yet
                    hover?.let { if (it != cur) onChanCat(it) }
                    hover = null
                    onShowCats(false); true
                }
                else e.key == Key.DirectionLeft
            },
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Kategorien", color = Tok.TextHi, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
        LazyColumn(Modifier.weight(1f).liveHoldThrottle(), state = cs, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            itemsIndexed(rows, contentType = { _, _ -> "cat" }) { i, r ->
                PCatRow(
                    r, r.key == chanCat, r.group && r.key in CatFavs.list,
                    if (r.key == focusKey) Modifier.focusRequester(catsFocus) else Modifier,
                    onFav = if (r.group) ({ onFav(r.key) }) else null,
                    onFocus = { hover = r.key; scope.launch { holdJump(cs, i) } }
                ) { hover = null; onChanCat(r.key); onShowCats(false) }
            }
        }
        Text("OK halten = Kategorie als Favorit", color = Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun ChannelColumn(
    chanCat: String, curList: ChList?, chanList: List<Item>, headerItems: List<String>, numOf: Map<String, Int>, curId: String,
    showCats: Boolean, inHeader: Boolean, onInHeader: (Boolean) -> Unit, onShowCats: (Boolean) -> Unit, onClose: () -> Unit,
    onChanCat: (String) -> Unit, chanFocus: FocusRequester, headerFocus: FocusRequester,
    focus: androidx.compose.runtime.MutableState<Item?>, onPick: (Item, List<Item>) -> Unit
) {
    val startIdx = remember(chanList, curId) { chanList.indexOfFirst { it.id == curId } }
    val ls = rememberLazyListState(ChannelMenuLogic.firstVisible(startIdx, 2))
    LaunchedEffect(chanCat) { ls.scrollToItem(ChannelMenuLogic.firstVisible(startIdx, 2)) }
    val jumpScope = androidx.compose.runtime.rememberCoroutineScope()
    Column(
        Modifier.fillMaxHeight().width(440.dp).padding(start = 20.dp, end = 16.dp, top = 24.dp, bottom = 16.dp)
            .onPreviewKeyEvent { e ->
                if (e.type != KeyEventType.KeyDown) false
                else if (e.key == Key.DirectionRight && !showCats && !inHeader) { onClose(); true }
                else if (e.key == Key.DirectionLeft && !showCats && !inHeader) { onShowCats(true); true }
                else false
            },
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            IconLabel(
                when { chanCat == FAV_CAT -> AIcon.Star; curList != null -> AIcon.Lists; else -> null },
                when { chanCat == FAV_CAT -> "Favoriten"; chanCat == "ALL" -> "Alle Sender"; curList != null -> curList.name; else -> chanCat },
                Tok.TextHi, 22.sp, FontWeight.ExtraBold, iconSize = 20.dp, iconTint = if (chanCat == FAV_CAT) PT.Gold else Tok.TextHi,
                raw = curList != null, modifier = Modifier.weight(1f, fill = false)
            )
            Text(
                raw = true, text = "${chanList.size}", color = Muted, fontSize = 13.sp, fontWeight = FontWeight.Bold, style = TNUM,
                modifier = Modifier.clip(RoundedCornerShape(50)).background(DS.Overlay1).padding(horizontal = 10.dp, vertical = 3.dp)
            )
        }
        LazyRow(Modifier.onFocusChanged { onInHeader(it.hasFocus) }, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(headerItems) { index, name ->
                val hl = ChannelLists.byKey(name)
                FilterPill(
                    if (name == "ALL") "Alle" else if (name == FAV_CAT) "Favoriten" else hl?.name ?: name, chanCat == name,
                    (if (name == chanCat) Modifier.focusRequester(headerFocus) else Modifier).onPreviewKeyEvent { ev ->
                        if (ev.type != KeyEventType.KeyDown) false
                        else if (ev.key == Key.DirectionDown) { runCatching { chanFocus.requestFocus() }; true }
                        else if (index == 0 && ev.key == Key.DirectionLeft) { onShowCats(true); true }
                        else if (index == headerItems.lastIndex && ev.key == Key.DirectionRight) { onClose(); true }
                        else false
                    },
                    aicon = if (name == FAV_CAT || name in CatFavs.list) AIcon.Star else if (hl != null) AIcon.Lists else null,
                    raw = hl != null
                ) { onChanCat(name) }
            }
        }
        if (chanList.isEmpty()) {
            // Empty selection (no favorites yet, list still loading): a focusable line, so the keys still go somewhere
            var f by remember { mutableStateOf(false) }
            val shape = RoundedCornerShape(DS.RadiusM)
            Box(
                Modifier.fillMaxWidth().height(64.dp).focusRequester(chanFocus).clip(shape)
                    .background(if (f) RowFocusFill else RowSelectedFill)
                    .then(if (f) Modifier.border(2.dp, Accent1, shape) else Modifier)
                    .onFocusChanged { f = it.isFocused }.clickable { }.padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) { Text("Keine Sender in dieser Auswahl", color = Muted, fontSize = 15.sp, maxLines = 1) }
            Spacer(Modifier.weight(1f))
        } else {
            LazyColumn(state = ls, modifier = Modifier.weight(1f).liveHoldThrottle(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                itemsIndexed(chanList, contentType = { _, _ -> "ch" }) { i, it ->
                    ChannelRow(
                        it, numOf[it.id] ?: -1, it.id == curId,
                        if (i == startIdx || (startIdx < 0 && i == 0)) Modifier.focusRequester(chanFocus) else Modifier,
                        onFocus = { focus.value = it; jumpScope.launch { holdJump(ls, i) } }
                    ) { onPick(it, chanList) }
                }
            }
        }
        Text("OK = ansehen  ·  halten = Favorit  ·  Links = Kategorien", color = Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Info bar of the highlighted channel: program now with progress and description. Only this part redraws on each step. */
@Composable
private fun InfoBar(focus: androidx.compose.runtime.State<Item?>, modifier: Modifier) {
    val fc = focus.value
    // Program of the highlighted channel: only after a short pause and not while Up/Down is held (before, every step started a request)
    val fe by produceState(emptyList<EpgEntry>(), fc?.id) {
        value = emptyList()
        if (fc == null) return@produceState
        EpgFull.peek(fc)?.let { value = it; return@produceState }
        kotlinx.coroutines.delay(250)
        while (LiveHold.active()) kotlinx.coroutines.delay(100)
        value = EpgFull.get(fc)
    }
    Box(modifier.fillMaxHeight().padding(horizontal = 28.dp, vertical = 28.dp), contentAlignment = Alignment.BottomCenter) {
        if (fc == null) return@Box
        val nowE = Epg.current(fe)
        val nextE = if (nowE != null) fe.firstOrNull { it.start >= nowE.end } else null
        val barShape = RoundedCornerShape(DS.RadiusL)
        Row(
            Modifier.fillMaxWidth().clip(barShape).background(Bg.copy(alpha = 0.92f)).border(1.dp, PT.PanelBorder, barShape)
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ChannelLogoBadge(fc.logo, fc.title, 56.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(raw = true, text = fc.title, color = Muted, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (nowE != null) {
                    val frac = ((System.currentTimeMillis() / 1000 - nowE.start).toFloat() / (nowE.end - nowE.start).coerceAtLeast(1)).coerceIn(0f, 1f)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(raw = true, text = nowE.title, color = Tok.TextHi, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                        Text(raw = true, text = "${hhmm(nowE.start)}–${hhmm(nowE.end)}", color = Accent2, fontSize = 14.sp, fontWeight = FontWeight.Bold, style = TNUM, maxLines = 1)
                    }
                    Box(Modifier.padding(vertical = 4.dp).fillMaxWidth().height(3.dp).clip(RoundedCornerShape(50)).background(DS.Overlay3)) {
                        Box(Modifier.fillMaxWidth(frac).fillMaxHeight().background(Accent1))
                    }
                    if (nowE.desc.isNotBlank()) {
                        Text(raw = true, text = nowE.desc, color = Tok.TextMid, fontSize = 15.sp, lineHeight = 20.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    }
                    if (nextE != null) {
                        Text("Danach ${hhmm(nextE.start)}  ${nextE.title}", color = PT.T2, fontSize = 14.sp, style = TNUM, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
                    }
                } else {
                    Text("Kein Programm für diesen Sender", color = Muted, fontSize = 15.sp, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun PCatRow(
    entry: CatEntry, selected: Boolean, fav: Boolean, modifier: Modifier,
    onFav: (() -> Unit)?, onFocus: () -> Unit, onClick: () -> Unit
) {
    var f by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(DS.RadiusS)
    Row(
        modifier.fillMaxWidth().height(46.dp).clip(shape)
            .background(if (f) RowFocusFill else if (selected) RowSelectedFill else Color.Transparent)
            .then(if (f) Modifier.border(2.dp, Accent1, shape) else Modifier)
            .onFocusChanged { f = it.isFocused; if (it.isFocused) onFocus() }
            .favKeys { onFav?.invoke() }
            .clickable(onClick = onClick)
            .padding(start = 8.dp, end = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically
    ) {
        // Selected category: a short bar in the highlight color
        Box(Modifier.width(3.dp).height(18.dp).clip(RoundedCornerShape(50)).background(if (selected) Accent1 else Color.Transparent))
        val ic = if (fav) AIcon.Star else entry.icon
        if (ic != null) AppIcon(ic, if (fav || entry.key == FAV_CAT) PT.Gold else Tok.TextMid, size = 16.dp)
        Text(
            entry.label, color = if (selected || f) Tok.TextHi else Tok.TextMid, fontSize = 15.sp,
            fontWeight = if (selected || f) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1, overflow = TextOverflow.Ellipsis, raw = entry.raw, modifier = Modifier.weight(1f)
        )
        Text(raw = true, text = "${entry.count}", color = Muted, fontSize = 12.sp, style = TNUM, maxLines = 1)
    }
}

/** Sizes of a channel row for the setting "Senderzeilen" (small, normal, large), like the rows of the Live TV tab. */
private class MenuRowDims(val height: Dp, val logo: Dp, val title: TextUnit, val sub: TextUnit)

private val MENU_ROW_DIMS = arrayOf(
    MenuRowDims(58.dp, 38.dp, 15.sp, 12.sp),
    MenuRowDims(66.dp, 44.dp, 16.sp, 13.sp),
    MenuRowDims(76.dp, 52.dp, 18.sp, 14.sp)
)

@Composable
private fun ChannelRow(item: Item, num: Int, current: Boolean, modifier: Modifier, onFocus: () -> Unit, onClick: () -> Unit) {
    val ctx = LocalContext.current
    var f by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(DS.RadiusM)
    // Cached program at once; otherwise wait briefly and not while Up/Down is held (rows that fly past load nothing).
    // The lookup runs off the main thread, as in the Live TV tab.
    val epg by produceState(Epg.peek(item) ?: emptyList(), item.id) {
        val cached = Epg.peek(item)
        value = cached ?: emptyList()
        if (cached != null) return@produceState
        kotlinx.coroutines.delay(150)
        while (LiveHold.active()) kotlinx.coroutines.delay(120)
        value = withContext(Dispatchers.Default) { Epg.now(item) }
    }
    // Menu key (if present): favorite and own channel lists. Holding OK = favorite immediately (user's wish).
    var menu by remember { mutableStateOf(false) }
    if (menu) Box { ChannelMenu(item, null, null) { menu = false } }
    val dims = MENU_ROW_DIMS[General.channelRowSize.coerceIn(0, 2)]
    Row(
        // Fixed height: the row does not grow when the program arrives, so the list never re-measures while scrolling
        modifier.fillMaxWidth().height(dims.height).clip(shape)
            .background(if (f) RowFocusFill else if (current) RowSelectedFill else Color.Transparent)
            .then(if (f) Modifier.border(2.dp, Accent1, shape) else Modifier)
            .onFocusChanged { f = it.isFocused; if (it.isFocused) onFocus() }
            .menuKeys(onMenu = { menu = true }, onLong = {
                // With own channel lists: choice (favorites or a list); otherwise favorite on/off immediately
                if (ChannelLists.lists.isNotEmpty()) { menu = true; return@menuKeys }
                val on = !Library.isFav(item.id)
                Library.toggleFav(ctx, item.id)
                android.widget.Toast.makeText(ctx, Lang.tr(if (on) "Zu Favoriten hinzugefügt" else "Aus Favoriten entfernt"), android.widget.Toast.LENGTH_SHORT).show()
            })
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ChannelRowContent(item, num, current, epg, dims)
    }
}

/** Content of a channel row. Does not read the focus state: a focus step redraws only the frame of the row. */
@Composable
private fun RowScope.ChannelRowContent(item: Item, num: Int, current: Boolean, epg: List<EpgEntry>, dims: MenuRowDims) {
    val nowE = Epg.current(epg)
    Text(
        raw = true, text = if (num > 0) "$num" else "", color = if (current) Accent1 else Muted, fontSize = 13.sp,
        fontWeight = FontWeight.Bold, style = TNUM, maxLines = 1, modifier = Modifier.width(44.dp)
    )
    ChannelLogoBadge(item.logo, item.title, dims.logo)
    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            raw = true, text = item.title, color = Tok.TextHi, fontSize = dims.title,
            fontWeight = if (current) FontWeight.ExtraBold else FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis
        )
        if (nowE != null) {
            val frac = ((System.currentTimeMillis() / 1000 - nowE.start).toFloat() / (nowE.end - nowE.start).coerceAtLeast(1)).coerceIn(0f, 1f)
            Text(raw = true, text = nowE.title, color = Tok.TextMid, fontSize = dims.sub, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Box(Modifier.fillMaxWidth().height(2.dp).clip(RoundedCornerShape(50)).background(DS.Overlay2)) {
                Box(Modifier.fillMaxWidth(frac).fillMaxHeight().background(Accent1))
            }
        } else Text(raw = true, text = item.group, color = Muted, fontSize = dims.sub, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
    if (current) AppIcon(AIcon.Play, Accent1, size = 16.dp)
    if (Catchup.version >= 0 && Catchup.has(item)) AppIcon(AIcon.Catchup, Accent2, size = 16.dp)
    // DeadLinks.count is read as a change signal (same pattern as Catchup.version): isDead itself is a plain map lookup
    if (DeadLinks.count >= 0 && DeadLinks.isDead(item.url)) AppIcon(AIcon.Warning, PT.Gold, size = 16.dp)
    if (Library.isFav(item.id)) AppIcon(AIcon.Star, PT.Gold, size = 16.dp)
}

/** Channel logo, or two or three letters of the name when there is none or it does not load. Also used by the TV guide. */
@Composable
internal fun ChannelLogoBadge(logo: String?, title: String, size: Dp) {
    var failed by remember(logo) { mutableStateOf(false) }
    Box(Modifier.size(size).clip(RoundedCornerShape(DS.RadiusS)).background(DS.Overlay2), contentAlignment = Alignment.Center) {
        if (logo != null && !failed) {
            AsyncImage(model = logo, contentDescription = null, modifier = Modifier.fillMaxSize().padding(5.dp), contentScale = ContentScale.Fit, onError = { failed = true })
        } else {
            Text(raw = true, text = remember(title) { ChannelMenuLogic.monogram(title) }, color = Tok.TextMid, fontSize = (size.value * 0.3f).sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
        }
    }
}

/**
 * Recently watched channels (wish from the Wishbox): a row in the live controls (OK while watching) to switch back quickly.
 * Uses the existing history ([History]); the current channel is left out. Resolved in one pass over the channels in the
 * background, so opening the controls stays fast with very large playlists.
 */
@Composable
fun RecentChannelsRow(liveAll: List<Item>, curId: String, onLabel: (String) -> Unit, onPick: (Item) -> Unit) {
    val recent by produceState(emptyList<Item>(), liveAll, curId, History.ids.size) {
        val want = History.ids.toList().filter { it != curId }.take(40)
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            val pos = HashMap<String, Int>(want.size * 2)
            want.forEachIndexed { i, id -> pos.putIfAbsent(id, i) }
            val out = arrayOfNulls<Item>(want.size)
            for (x in liveAll) { val p = pos[x.id] ?: continue; if (out[p] == null) out[p] = x }
            out.filterNotNull().take(10)
        }
    }
    if (recent.isEmpty()) return
    Column(Modifier.fillMaxWidth().padding(bottom = 14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Zuletzt gesehen", color = Muted, fontSize = 13.sp)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(recent, key = { it.id }) { ch -> RecentChannelChip(ch, onLabel) { onPick(ch) } }
        }
    }
}

@Composable
private fun RecentChannelChip(item: Item, onLabel: (String) -> Unit, onClick: () -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var f by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(DS.RadiusS)
    Row(
        Modifier.height(44.dp).widthIn(max = 220.dp).clip(shape)
            .background(if (f) Color.White else Color(0x24FFFFFF))
            .onFocusChanged { f = it.isFocused; if (it.isFocused) onLabel(item.title) }
            // W-009: long OK removes the channel from "Recently watched"
            .favKeys { removeRecent(ctx, item) }
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(Modifier.size(30.dp), contentAlignment = Alignment.Center) {
            val lg = item.logo
            if (lg != null) AsyncImage(lg, null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
            else Text(raw = true, text = remember(item.title) { ChannelMenuLogic.monogram(item.title) }, color = if (f) Color.Black else Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Text(raw = true, text = item.title, color = if (f) Color.Black else Color.White, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
