# Graphics v2 – low-poly stijl

Alle graphics bestaan als **SVG** (bron, schaalbaar) én als **PNG op 2x** (map `png/`, direct bruikbaar in Android, bijv. in `res/drawable-nodpi`).
Bestandsnamen van kaarten en units zijn gelijk aan de kaart-id's in `CardCatalog.kt`.

## Arena (past precies op `Arena.kt`: 18×32 tegels, 40 px per tegel)
| Bestand | Formaat | Gebruik |
|---|---|---|
| `arena/arena_achtergrond` | 720×1280 | Complete arena (statisch, met bruggen) |
| `arena/arena_grond` | 720×1280 | Arena **zonder** bruggen en glinsters – gebruik deze als je het water animeert |
| `arena/water_basis` | 720×80 | Alleen de rivierstrook (tegels y 15–17) |
| `arena/water_glinster_a`, `_b` | 720×80, transparant | Glinster-lagen, naadloos herhaalbaar in x-richting (voor animatie) |
| `arena/brug` | 112×132 | Brug; midden op x = 3,5 en 14,5 tegels, y = 16 tegels |
| `arena/omgeving` | 900×1600 | Rand rond het speelveld (bomen); speelveld komt op (90,160) |

Rivier: pixel-y 600–680. Bruggen: midden x = 140 en 580. Torenplekken: koning (360,1160) en (360,120); wachttorens (140/580, 1020) en (140/580, 260).

## Torens (blauw = speler 0 onder, rood = speler 1 boven)
| Bestand | Formaat | Ankerpunt (= positie van de toren) | Advies tekenbreedte |
|---|---|---|---|
| `torens/wachttoren_{blauw,rood}` | 160×210 | (80, 180) | ±3,2 tegels |
| `torens/koningstoren_{blauw,rood}` | 240×260 | (120, 205) | ±4,8 tegels |
| `torens/puin` | 160×180 | (80, 150) | als toren kapot is |

## Units – `units/{blauw,rood}/<id>`
200×200, ankerpunt (100, 182) = voeten (bij `cannon`: (100, 150)). Schaduw zit erin.
Advies tekenbreedte: ongeveer `max(1.6, radius × 3.2)` tegels.
`drake` is een vliegende unit: de schaduw staat al onder hem, teken hem op dezelfde plek.
ids: knight, archers, giant, goblins, musketeer, drake, brute, skeletons, ram_rider, cannon

## Spreuken – `spreuken/<id>` (200×200)
fireball, arrows, zap – bruikbaar als projectiel/inslag-effect.

## Kaarten – `kaarten/<id>` (200×240)
Alle 13 kaarten met kosten, naam en aantal (x2/x3). Kleur van de rand: blauw = troep, oranje = gebouw, paars = spreuk.

## Elixer – `ui/`
- `elixerbalk_leeg` (640×96) + `elixerbalk_vulling` (640×96): teken eerst leeg, dan vulling geknipt (clip) op breedte. De vulbalk loopt van x = 84 tot x = 620 (10 vakjes van 53,6 px).
- `elixerbalk_voorbeeld`: zo ziet het eruit bij 6.
- `elixer_druppel` (48×64): voor kosten/teller. Het getal zelf teken je in code.

Lettertype op kaarten: Poppins Bold.

Zie `WATER_ANIMATIE.md` voor het bewegende water.
