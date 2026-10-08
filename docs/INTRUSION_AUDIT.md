# Аудит интрузивности: что мод меняет в обычном мире

Полная инвентаризация всего, что мод меняет за пределами собственного RTS-контента.
Источник — чтение кода на `737a53e2` (ветка `1.21.1-clean`). Документ статический: это
описание состояния, а не план правок.

> **Поправка на 2026-10-07 (`e29e088e`).** Снимок на `737a53e2`. Этапы A–C (деинтрузивность)
> закрыты: датапак генерации и подмена `assets/minecraft` сняты, туман войны удалён целиком,
> безусловные миксины возвращены ваниле. Миксинов осталось **48** на 31 классе (было 49
> серверных + 27 клиентских). Часть описанных ниже проблем уже исправлена — полная актуализация
> документа ещё не делалась.
>
> **Поправка на 2026-10-08 (`a422e885`).** Этапы C–H закрыты: режимы, кроме `CLASSIC`, убраны;
> контент, чары и исследования удалены; читы вырезаны; §14 перевёл юниты и строения на модель
> «сущность + способности». Счёт миксинов и категории 0 (права команд, форс ванильных геймрулов)
> по-прежнему актуальны для исторической части, но исполненные пункты закрыты `PLAN_RTS_ONLY.md`
> §3–§6. Актуальное состояние — [`STATUS.md`](STATUS.md).
>
> **Поправка на 2026-10-08 (актуальная, `e87b66db`).** Выполнено сужение: `ArmorStandMixin` удалён,
> `AbstractArrowMixin` больше без `@Overwrite` (ванильные стрелы не трогаются), `LivingEntityMixin`
> сужен (партиклы левитации только юнитам, мёртвый код убран), геймрулов осталось **12** (удалены
> `doPlayerGriefing`, `groundYLevel`, `flyingMaxYLevel`, `allowBeacons`, `pvpModesOnly`,
> `beaconWinMinutes`, `allowedHeroes`, `randomItemDrops`). Миксины: **21 common / 11 client**.
> Авторитетная текущая инвентаризация — [`VANILLA_CHANGES.md`](VANILLA_CHANGES.md).

Пути относительно `src/main/java/com/solegendary/reignofnether/` внутри `___temp/`, если не
указано иное.

**Как читать.** Категория 0 — то, что нужно чинить независимо от philosophical вопроса об
интрузивности (дыры в правах и защите). Категории 1–5 — по убыванию вреда. Категория 6 —
мёртвый код. Раздел «Что в порядке» — важный: список неполон без него.

---

## 0. Метод и границы аудита

Проверено:

* все **49** общих (серверных) миксинов из `reignofnether.mixins.json` → раздел 2;
* все **27** клиентских миксинов → раздел 3;
* весь датапак `src/main/resources/data/` → раздел 1;
* все ассеты в `src/main/resources/assets/`, включая namespace `minecraft` → раздел 3;
* все **23** геймрула и их потребители → раздел 5;
* все места мутации мира и состояния игрока вне RTS-контента → разделы 4 и 6.

Не проверено: содержимое миров сообщества, взаимодействие с конкретными сторонними модами,
поведение на многопользовательских серверах.

Обозначения вердикта:

* **RTS** — нужно для геймплея, в не-RTS мире инертно.
* **world-mutation** — меняет обычный ванильный геймплей.
* **optional-content** — фича мода, которую можно не включать.
* **bug** — не задумано как фича.

---

## 1. Категория 0 — дыры и поломки, не имеющие отношения к RTS

| Что | Где | Следствие |
|---|---|---|
| Ванильный геймрул `disableElytraMovementCheck` принудительно ставится в `true` при каждом старте сервера | `player/PlayerServerEvents.java:183` | Игроки **намертво** летают сквозь блоки на любом сервере с модом. Безусловно, молча, необратимо |
| `isSingleplayerOwner()` форсится в `true` в `handleMovePlayer` и `handleMoveVehicle` | `mixin/ServerGamePacketListenerImplMixin.java:11-25` | Ванильная проверка скорости передвижения и расстояния транспорта отключена для всех игроков. Античит и защита от разгона не работают |
| `/sendfood`, `/sendwood`, `/sendore`, `/sendemerald` — **нет `.requires(...)`** | `resources/ResourcesServerEvents.java:340-375` | Любой игрок выдаёт себе ресурсы |
| `/rts-fog enable\|disable` — **нет `.requires(...)`** | `fogofwar/FogOfWarServerEvents.java:623-635` | Любой игрок или командный блок переключает туман войны |
| `BuildingCommand` исполняет произвольные строки команд с правом 2 | `building/BuildingCommand.java:138-164` | Датапак-строение может выполнить что угодно по триггерам `ON_BUILD_COMPLETE`, `ON_DESTROY`, `ON_DAMAGE_TAKEN`, `ON_CAPTURE`, `OFF_COOLDOWN_IF_*`, `ON_SCENARIO_START` |
| Дублирующая отправка пакетов частиц | `mixin/ServerLevelMixin.java:16-30` | Ванилла уже отправила пакет, миксин отправляет его второй раз для получателей на 32–512 блоков. Каждый обычный эффект приходит дважды. Безусловно |
| `allcheats` захардкожен на ники `solegendary` / `altsolegendary` | `player/PlayerServerEvents.java:859-862` | Чит даёт ресурсы 99999, `maxPopulation = 99999`, все читы |
| Клиентский «замок» игрового режима не блокирует ванильный `/gamemode` | `gamemode/GameModeServerEvents.java:32-36`, `gamemode/GameModeClientboundPacket.java:50-68` | Ограничение только визуальное, сервер не проверяет |

---

## 2. Категория 1 — подмена генерации мира

Датапак-файлы **нельзя** выключить геймрулом или конфигом: они применяются к каждому миру,
где установлен мод. Единственный механизм opt-in — геймрул `reignofnetherRtsMap`, но он
управляет рантайм-поведением и на датапак не влияет.

### 2.1 Пещеры выключены

`src/main/resources/data/minecraft/worldgen/configured_carver/`:

| Файл | Ванилла | Стало |
|---|---|---|
| `cave.json` | `probability 0.02` | `probability: 0.00`, `yScale 0`, радиусы 0 |
| `cave_extra_underground.json` | как ванилла | то же |
| `canyon.json` | как ванилла | то же |
| `nether_cave.json` | `probability 0.02` | `probability: 0.01` |

Комментарий в файлах: `I've had an instance of a crash with probability = 0 carvers;
replaceable = air is an effective workaround`. То есть carver намеренно оставлен
зарегистрированным, но никогда не запускается.

Итог: **в Оверворлде нет пещер, дрипстоун-пещер и каньонов, Незер почти пуст.** Энд не тронут.

### 2.2 Форма Оверворлда заменена

`data/minecraft/worldgen/density_function/`:

* `overworld/depth.json` — ванильное двухчленное сложение получает третье слагаемое
  `y_clamped_gradient(0.0 @ y75 → -0.8 @ y105)`. Плотность занижена на 0.8 в диапазоне
  y75–105 → поверхности ниже и площе, горы подавлены.
* `overworld/sloped_cheese.json`, `overworld_amplified/…`, `overworld_large_biomes/…` —
  `add(depth, jaggedness)` заменено на `add(depth, mul(jaggedness, half_negative(noise(jagged, xz 1500))))`, весь результат обёрнут в `clamp(min -1000000, max 1.5)`.

Файлы общие для всех трёх ванильных пресетов Оверворлда, поэтому затрагиваются все.

Во всех файлах комментарий `Cave logic short-circuit, must not exceed 1.5625` — то есть их
редактировали, чтобы отключённый carver нельзя было шорт-циклить.

### 2.3 Руды вынесены на поверхность

**14 ванильных биомов заменены** (`data/minecraft/worldgen/biome/`): `plains`,
`sunflower_plains`, `meadow`, `river`, `lukewarm_ocean`, `taiga`, `savanna`,
`windswept_savanna`, `beach`, `windswept_hills`, `windswept_gravelly_hills`, `desert`,
`stony_shore` — во всех шаг «Lakes» перенаправлен на `minecraft:lake_lava_surface`, а в шаг
«Ores» добавлены surface-фичи. В `stony_shore` добавлены **все шесть**, включая
`ore_gold_surface`, `ore_diamond_surface`, `ore_emerald_surface`, которых ванилла на
поверхности не генерирует никогда.

`data/minecraft/worldgen/placed_feature/ore_*_surface` — заменены 6 файлов плюс один новый
`ore_coal_surface_small`:

| Файл | `count` |
|---|---|
| `ore_coal_surface`, `ore_copper_surface`, `ore_iron_surface`, `ore_gold_surface` | **30** на чанк |
| `ore_diamond_surface` | 10 |
| `ore_emerald_surface` | **20** |
| `ore_coal_surface_small` (новый) | 15 |

Все в высотном диапазоне «верхний слой» (`absolute 0 → below_top(0)`).

`data/minecraft/worldgen/configured_feature/ore_*_surface` — заменены 6 файлов: увеличен
`size` (уголь 16, медь 12, железо 8, золото/алмаз/изумруд по 4), добавлено второе правило
замены на `tag_match: minecraft:bamboo_plantable_on` и
`discard_chance_on_air_exposure: 0.0`, из-за чего жила не выбрасывается на воздухе.

### 2.4 Мёртвый датапак — ничего не делает

| Путь | Файлов | Почему мёртв |
|---|---|---|
| `data/flat_dimensions/**` | 7 | нет ни noise router, ни dimension type, ни ссылки в Java |
| `data/overworldify/**` | 6 | carver'ы Нижнего мира/Энда не перечислены ни в одном carver list |
| `data/tectonic/**` | 2 | дословные копии ванильных фрагментов |
| `data/minecraft/tags/functions/{load,tick}.json` | 2 | каталог `functions` вместо `function` (1.21+ требует единственное число) |
| `data/reignofnether/functions/{load,tick}.mcfunction` | 2 | то же |
| `data/minecraft/worldgen/density_function/{erosion,temperature,lava_tunnel/total}.json` | 3 | содержимое совпадает с ванильным |
| `data/minecraft/worldgen/structure_set/mineshafts.json` | 1 | совпадает с ванильным |

Теги функций не загружаются — и это хорошо: `load.mcfunction` выставил бы
`randomTickSpeed 12`, отключил погоду, бессонницу и естественный спавн мобов, плюс broadcast
в каждом ванильном мире.

---

## 3. Категория 2 — безусловные миксины (серверные)

19 из 49 общих миксинов меняют ванильное поведение безусловно, независимо от наличия
RTS-юнитов. Ниже — только безусловная часть; остальные 30 либо инфраструктурные (4
аксессора + `PlayerMixin`), либо закрыты `instanceof`-проверкой.

### 3.1 Огонь — самый большой кластер

`mixin/fire/FireBlockMixin.bootStrap` отменяет ванильную регистрацию горючести и ставит
свою таблицу (~130 блоков Оверворлда), причём перерегистрация выполняется на каждом тике
огня. Последствия:

| Ванилла | Reign of Nether |
|---|---|
| Огонь распространяется по горючести | не распространяется вообще |
| Горит до age 15 | гаснет на age 2; `setValue(AGE, …)` при этом no-op |
| Обсидиан не горючий | **объявлен горючим** (20/5), спасает только `tryCatchFire` вне зданий мода |
| Все записи Незера в таблице горючести | **выброшены**: стволы/гифы, нилиумы, шрумлайт, лозы, корни, спряуты, блок незерита, соул-песок, соул-земля, базальт |

Плюс ставки урона переписаны для всех:

| Миксин | Ванилла | Стало |
|---|---|---|
| `fire/BaseFireBlockMixin:41-50` | `hurt(inFire, 1)` за тик | 3 урона раз в 20 тиков, `inFire` форсится даже для frost-walker |
| `fire/CampfireBlockMixin:33-38` | 1/2 урона раз в 10 тиков | 1/2 раз в 20 тиков — вдвое реже |
| `fire/MagmaBlockMixin:29-41` | мгновенные 4, отменяется при `isSteppingCarefully()` | 3 урона раз в 20 тиков, **применяется и при осторожном шаге** |

### 3.2 Прочее безусловное

| Миксин | Что меняется |
|---|---|
| `mixin/BaseSpawnerMixin:50-57` | **задержка респауна каждого спавнера в мире** — фиксированные 600 тиков вместо случайных 10–40 |
| `mixin/SculkCatalystBlockEntityMixin:49-85` | ванильный скульк не bloom'ит **вообще**: отключены XP-сферы, ачивка `KILL_MOB_NEAR_SCULK_CATALYST`, распространение скулька |
| `mixin/EntityMixin:56-64` | все сущности невосприимчивы к удушью (`inWall`) |
| `mixin/EntityMixin:78-87` | заморозка у всех сущностей ограничена 0.5 |
| `mixin/WitherRoseMixin:25-27` | блок полностью инертен — ничего не увядает и не наносит урона |
| `mixin/LeavesBlockMixin:14-22` | цель `LeavesBlock`, то есть **вся** листва: дубовая, вишнёвая и прочая рядом с crimson/warped стволом больше не гниёт |
| `mixin/PanicGoalMixin:22-25` | скорость **всех** ванильных `PanicGoal` (пчёлы, ламы, хоглины, козы, эндермены, аллаи) зажата на 1.2 |
| `mixin/BlazeMixin` | `Blaze.aiStep` отменяется целиком — **все ванильные блейзы перестают стрелять файрболлами** |
| `mixin/EvokerFangsMixin` | хитбокс клыков расширен с `inflate(0.2,0,0.2)` до `inflate(0.5,0,0.5)`; поле `dealtDamage` затенено, но не присваивается |
| `mixin/ThrownTridentMixin` | для всех трезубцев обходится дата-управляемый конвейер эффектов чар 1.21; Impaling реализован вручную |
| `mixin/ThrownPotionMixin` (`makeAreaOfEffectCloud`) | каждое замедленное зелье в мире получает `AdjustableAreaEffectCloud` с радиусом 3.0 / −0.5 / 10 вместо ванильного |
| `mixin/CrossbowMixin` (`getChargeDuration`) | время зарядки арбалета детерминировано: 35 тиков, −5 за уровень Quick Charge, вместо `20 + random(0..20)` |
| `mixin/UnitInventoryMobMixin` | **каждый** моб в мире реализует `UnitInventory` и пишет в NBT тег `reignofnether:UnitItems` |
| `mixin/AbstractArrowMixin` | `canHitEntity` объявлен без аннотации — необъявленный `@Overwrite` для всех стрел; `onHitEntity` применяет урон повторно поверх ванильного |
| `mixin/ExecuteCommandMixin` | добавляет `/execute building` и `/execute unit` верхним уровнем — Brigadier молча падает на дубликате, другой мод с таким именем конфликтует |
| `mixin/DataCommandsMixin` | перенаправляет `DataCommands.<clinit>`; жёсткая структурная зависимость от числа provider'ов в ванилле |
| `mixin/LevelChunkMixin` (половина) | `ResourceIndex.onBlockChange` выполняется на каждом `setBlockState` в каждом загруженном чанке на обеих сторонах, без гейта |
| `mixin/ServerLevelMixin` | см. категорию 0 |

### 3.3 Прочее безусловное, но менее тяжёлое

* `neutralAggro` (дефолт `true`) — каждые 20 тиков любой `PathfinderMob` в радиусе 10 блоков
  от юнита принудительно нацеливается на него (`unit/NonUnitServerEvents.java:75-98`).
* `unit/NonUnitServerEvents.java:111-119` — ванильные мобы **никогда** не целятся в юнитов
  мода, по жёсткой карте фракций (гolem/illager/villager→VILLAGERS, piglin/hoglin/ghast/blaze/
  wither skeleton→PIGLINS, skeleton/zombie/creeper/spider/slime/warden→MONSTERS). Без гейта.
* `mixin/PathNavigationMixin:70-104` — `@ModifyConstant(doubleValue = 1.0)` без `require = 0`
  меняет **все** константы `1.0` в `followThePath`, а не только нужную.

### 3.4 Ошибочная документация

`mixin/fogofwar/WorldBorderMixin.java` — javadoc утверждает, что миксин блокирует изменение
границы мира («Prevent changing worldborder… that freezes the server»). Код этого не делает: три
инъекции в `TAIL`, все с `cancellable = true`, **ни одна не отменяется**. Это наблюдатель,
который только ставит `FogChunkSnapshot.shouldRecapture = true`. Ванильная граница мира
меняется как обычно.

---

## 4. Категория 3 — ассеты и клиентский рендер

### 4.1 Подмена ванильных ассетов

Мод поставляет namespace `assets/minecraft/` — 35 файлов. Ничто в `assets/reignofnether/`
не может перекрыть ваниллу, поэтому это весь объём подмены.

| Файл | Эффект |
|---|---|
| `textures/gui/widgets.png` | **все ванильные GUI-виджеты перекрашены** по всему игре: кнопки, тумблеры, чекбоксы, заголовки списков. Кода мода на файл нет — чистая подмена |
| `models/item/carved_pumpkin.json` | вырезанная тыква как предмет рендерится белым котом в рождественской шапке; блок не тронут |
| `texts/splashes.txt` | все ванильные сплэши заменены на 91 строку про RTS/Warcraft/StarCraft |
| `textures/gui/title/minecraft.png`, `edition.png` | логотип главного меню |
| `textures/item/trident.png`, `textures/entity/trident.png` | текстура трезубца |
| `textures/misc/pumpkinblur.png` | оверлей тыквы |
| `models/block/nether_portal_{ns,ew}.json` | портал 4 блока толщиной с `tintindex: 1` |

Проверено и чисто: `assets/minecraft/sounds.json` отсутствует, ни один звуковой ид мода не
в неймспейсе `minecraft`, `assets/minecraft/lang/**` отсутствует.

### 4.2 Перекрас за границей мира — единственный безусловный recolour

`FogTintingBlockColor`, `BiomeColorsMixin`, `LiquidBlockRendererMixin` содержат одну и ту же
ветку:

```java
if (WorldBorderClientEvents.isOutsideWorldBorder(pos)) tint = 0x252933;   // OUTSIDE_WORLD_BORDER_TINT
else if (FogOfWarClientEvents.isEnabled() && !isBlockVisible(pos)) tint = 0x7882A0; // FOG_TINT_RGB
```

Ветка тумана правильно опциональна: за `reignofnetherRtsMap` + `reignofnetherForceFog` +
`isRTSPlayer`.

**Ветка границы мира не гейтится ничем** и зависит только от
`level.getWorldBorder().isWithinBounds(pos)`. Любой админ, сузивший границу мира, получает
**все блоки, траву, листву и воду за ней, умноженные на `0x252933`** — тёмно-сине-серый.
Без геймрула, без тумана, в ванильном мире.

Сюда же:

* `FogOfWarClientEvents.onRenderLivingEvent` (`FogOfWarClientEvents.java:272-281`) отменяет
  отрисовку **любой** живой сущности за границей мира — без гейта на туман;
* `ItemEntityRendererMixin:72` скрывает выпавшие предметы за границей;
* `WorldBorderRenderMixin` отменяет `LevelRenderer.renderWorldBorder` — **ванильная стена
  границы не рисуется нигде и ни для кого**;
* `WorldBorderClientEvents.onClientTick` вызывает `resetFogChunks()` (`levelRenderer.allChanged()`)
  при каждом изменении размера или центра границы.

Дополнительно, на все блоки и модели, независимо от гейтов:

* `ClientModEvents.onBlockColourEvent` регистрирует `FogTintingBlockColor` **для каждого блока
  в `BuiltInRegistries.BLOCK`** (пропуская 15 biome-тайнтованных ванильных), и
  `ModelEvent.ModifyBakingResult` оборачивает **каждую испечённую модель в игре**, принудительно
  ставя `tintIndex 0` там, где был `-1`. В не-RTS мире визуально нейтрально, но через
  `BlockColor` проходит теперь каждый блок;
* `Blocks.NETHER_PORTAL` получает принудительный tint (`0x00FF00` / `0xFF0000` / `0x0000FF`).

### 4.3 Прочие безусловные клиентские изменения

| Миксин | Что видит игрок |
|---|---|
| `TitleScreenMixin` | главное меню перехватывается целиком: своя панорама (вращается между `villagers`/`piglins`/`monsters`), свой сплэш, свой логотип, три кнопки (Lilypad, Discord, «Get RTS maps!»), брендинг. Ванильный `render` отменяется и заменяется своей реализацией |
| `MusicManagerMixin` | музыка главного меню заменена (в игре ванилльная не тронута) |
| `ClientLevelMixin#tickTime` | `@Inject` с `cancellable` **без условия**: ванильное применение времени заменено всегда; дневной цикл идёт к цели шагом ≤1 тик, цель — серверное время либо жёстко `18000` (полночь) рядом с источником ночной дисторсии |
| `LevelRendererMixin#renderLevel` TAIL | оверлей разрушения блока рендерится с расширенной с 32 до 256 дистанцией — всегда |
| `ClientPacketMixin#handleSetTime` | **не** отменяет (в отличие от прежних версий), только запоминает серверное время и перенацеливает клиентские часы |

### 4.4 Условные клиентские изменения

* `ClientLevelMixin` — `playSeededSound`/`playSound` в орторежиме: все позиционные звуки
  переиздаются на позиции выбранной сущности с громкостью 0.5×;
* `CameraMixin` в орторежиме — камера принудительно в третьем лице и на 20 блоков дальше;
* `OrthoViewMixin` в орторежиме — перспективная проекция заменяется ортографической;
* `FrustumMixin` в орторежиме — фрустум перестаёт сжиматься к кубу камеры;
* `RenderChunkRegionMixin` — вся листва рендерится зелёным стеклом при
  `hideLeavesMethod != NONE` (дефолт `NONE`, то есть выключено);
* `ClientModEvents.getSkyColor` — небо становится плоским тёмно-красным `(0.25, 0, 0)` только
  во время кровавой луны;
* `ClientLevelMixin.setServerVerifiedBlockState` — при тумане клиент отбрасывает
  серверные обновления блоков в невидимых позициях, состояние может рассинхронизироваться.

---

## 5. Категория 4 — обычное выживание

| Что | Где | Гейт |
|---|---|---|
| **Ванильный лут животных полностью отменяется**, если убито рабочим-охотником; еда переносится во внутренний инвентарь юнита | `unit/UnitServerEvents.java:680-729` | безусловно при охоте |
| **Охотничьи животные спавнятся вокруг каждой столицы** | `building/BuildingPlacement.java:881-888, 954-964` | безусловно при постройке столицы |
| **Животные патфиндятся из 10-блоковой зоны** вокруг каждого нового здания | `building/BuildingServerEvents.java:617-626` | безусловно |
| **Рост культур подавляется** внутри любого здания | `resources/ResourcesServerEvents.java:261-265` | внутри здания |
| **Ломание блока игроком внутри здания отменяется и блок заменяется на AIR** | `resources/ResourcesServerEvents.java:277-283` | внутри здания |
| **Ломание бревна превращает соседние брёвна в модовые `falling_*_log`** | `resources/ResourcesServerEvents.java:285-338, 459-481` | `doLogFalling`, дефолт **`true`** |
| **Каждая смерть юнита рядом с модовым скульк-катализатором** превращает `DIRT_PATH`→`DIRT` и уничтожает растения в радиусе 3 блока | `unit/UnitServerEvents.java:497-537` | в радиусе катализатора |
| **Любая сущность, путешествующая между измерениями внутри здания, отменяется**; игрок телепортируется к порталу | `building/BuildingServerEvents.java:968-989` | внутри здания |
| **Рост зданий ставит `SCAFFOLDING` до 5 блоков** под каждой колонной фундамента | `building/BuildingServerEvents.java:536-560, 466-469` | при постройке |
| Огонь внутри footprint здания уничтожается при постройке | `building/BuildingPlacement.java:590-596` | при постройке |
| Под зданием `FARMLAND`/`DIRT_PATH` превращается в `DIRT`, `SOUL_SAND`→`SOUL_SOIL`, `MAGMA_BLOCK`→`COBBLESTONE` под водой | `building/BuildingPlacement.java:1072-1123` | при постройке |
| Картофель/морковь как предметные сущности отменяются внутри зданий | `resources/ResourcesServerEvents.java:267-274` | внутри здания |
| Предметы внутри ферм отменяются | `building/BuildingServerEvents.java:999-1007` | внутри ферм |
| Спавн любого моба внутри footprint `Dungeon`/`FlameSanctuary` отбрасывается | `building/BuildingServerEvents.java:781-791` | в здании |
| Скаффолдинг ставится **и** уничтожается вокруг зданий | `building/BuildingServerEvents.java:466-469, 536-560` | при постройке |

---

## 6. Категория 5 — геймрулы с неванильными дефолтами

| Геймрул | Дефолт | Что делает | Вердикт |
|---|---|---|---|
| `doNetherConversion` | **`true`** | каждое здание с `NetherConvertingAddon` спавнит `NetherZone`, который **переписывает террейн** в незер-блоки (`building/NetherZone.java:141-182`) и откатывает при сносе (`:104-138`). При выключении начинает восстанавливать существующие зоны | world-mutation |
| `buildingsOutsideBorder` | **`true`** | пропускает проверку границы мира при размещении (`BuildingValidators.java:190-194`); при `false` здания за границей помечаются нелегальными и **автосносятся** (`BuildingPlacement.java:1410-1418, 656-659`) | world-mutation |
| `neutralAggro` | **`true`** | см. §3.3 | world-mutation |
| `doUnitGriefing` | **`false`** | урон взрывов по блокам срезается до листвы и TNT (`BuildingServerEvents.java:958-964`), то есть **криперы, TNT и кровати не разрушают ничего** | world-mutation |
| `doPlayerGriefing` | `true` | при `false` `BlockEvent.BreakEvent` отменяется для любого блока вне ресурсов мода и вне зданий (`blocks/BlockServerEvents.java:68-79`) | world-mutation |
| `doLogFalling` | **`true`** | см. §5 | world-mutation |
| `allowBeacons` | `true` | **только клиентский гейт** кнопки постройки маяка, серверной проверки нет (`building/buildings/neutral/Beacon.java:99`) | optional-content |
| `neutralAggro`, `maxPopulation`, `groundYLevel`, `flyingMaxYLevel`, `beaconWinMinutes`, `slantedBuilding`, `allowedHeroes`, `lockAlliances`, `scenarioMode`, `coopMode`, `pvpModesOnly`, `animalSpawnYDiff`, `pathfindingThreads`, `pathfindingChunkBuilds` | — | неванильные имена занимают короткие имена в общем пространстве | переименовать |

Все 23 геймрула синхронизируются на клиент (`gamerules/GameruleServerEvents.java:134-176` при
входе, `:20-132` при использовании команды), а запись через пакет требует прав 4
(`GameruleServerboundPacket.java:131`). Неопыт не может их изменить.

**Управление временем** (отдельно, потому что это не геймрул):

| Место | Что делает | Гейт |
|---|---|---|
| `time/TimeServerEvents.java:98-103` | `setDayTime(serverStartTime)` **каждый тик** — жёсткая блокировка дня | `scenarioMode == true` и нет RTS-игроков |
| `player/PlayerServerEvents.java:543-551` | `setDayTime(MONSTER_START_TIME_OF_DAY = 500)` или рассвет | старт матча |
| `survival/SurvivalServerEvents.java:282-292, 112-135` | форсирует рассвет/закат/стартовое время | только survival |
| `survival/SurvivalServerEvents.java:183-195` | `/debug-next-night` → `setDayTime(12450)` | требует чит `thereisnospoon`, **но консоль и командные блоки проверку обходят** |
| `tutorial/TutorialServerEvents.java:121-133` | `setDayTime(1000)`/`setDayTime(13000)` по клиентскому пакету | только на карте туториала |

---

## 7. Категория 6 — мёртвое, вводящее в заблуждение, баги

### 7.1 Мёртвые фичи

| Что | Состояние |
|---|---|
| `randomItemDrops` (дефолт 1) | `RTSPlayer.itemDropQueue` пишется при старте матча и восстанавливается из NBT, но **нигде не читается**. Геймрул сейчас не роняет ничего |
| `groundYLevel` | только плоскость тумана на клиенте (`fogofwar/PlayerChunksClientEvents.java:55-56`) плюс слайдер в HUD. Заявленного «минимума Y для камеры» в коде нет |
| `pathfindingChunkBuildsPerTick` при преварме | синхронный цикл преварма обходит бюджет; влияет только на рантайм-прогрев |

### 7.2 Мёртвый код

* `mixin/PlayerMixin.java` — тело целиком закомментировано (`:25-50`), но класс остался в
  списке миксинов. При этом цель — `Entity`, а не `Player`.
* `mixin/ZoglinMixin.java` — лежит на диске, но **не зарегистрирован**: ванильные зоглины не
  уважают уклонение юнитов.
* `data/flat_dimensions/**`, `data/overworldify/**`, `data/tectonic/**` — 15 файлов без ссылок.
* `data/minecraft/tags/functions/**`, `data/reignofnether/functions/**` — 4 файла в
  неправильных каталогах, не загружаются.
* `assets/minecraft/textures/gui/title/{minecraft,edition}-old.png`,
  `assets/minecraft/textures/block/green_stained_glass2.png`,
  `assets/minecraft/textures/misc/forcefield.png` — на них ничего не ссылается.

### 7.3 Баги, найденные при аудите

| Баг | Где |
|---|---|
| Дублирующая отправка пакетов частиц | `mixin/ServerLevelMixin.java:16-30` |
| `BaseSpawnerMixin.serverTick` возвращает успех у `sendParticles`, из-за чего пакет дублируется | `mixin/ServerLevelMixin.java:16-30` |
| `PowderSnowBlockMixin` без `cancellable = true` — ванильный `entityInside` всё равно выполняется, юнит получает урон от рыхлого снега | `mixin/PowderSnowBlockMixin.java:22-27` |
| `PathNavigationMixin` использует `@ModifyConstant(doubleValue = 1.0)` без `require = 0` — меняются все константы `1.0` в методе | `mixin/PathNavigationMixin.java:70-104` |
| `AbstractArrowMixin.canHitEntity` — метод без аннотации инъекции, то есть необъявленный `@Overwrite`; `super.canHitEntity` резолвится в `Projectile`, а не в `AbstractArrow` | `mixin/AbstractArrowMixin.java` |
| `AbstractArrowMixin.onHitEntity` применяет урон повторно поверх ванильного | там же |
| `UnitServerEvents.onLivingDeath` (`:554-596`) конвертирует юнита **после** его смерти — зомбифицирует уже мёртвую сущность | `unit/UnitServerEvents.java:554-596` |
| `LivingEntityMixin` при пересчёте урона ≤0 возвращает `true` **без** вызова `CommonHooks.onEntityIncomingDamage`, то есть NeoForge-событие `EntityIncomingDamageEvent` для таких ударов не fires | `mixin/LivingEntityMixin.java:140-167` |
| `ResourceCost.Emeralds(int emeralds)` игнорирует аргумент и передаёт конструктору `0` | `resources/ResourceCost.java:58-60` |
| Слой `UnitItem` (геройские предметы, магазины, изумрудная валюта) выключен флагом | `items/UnitItem.java:48` — `ENABLED = false` |
| Три исследования недостижимы: зарегистрированы, имеют стоимость и локализацию, но ни одно строение их не производит | `RESEARCH_VINDICATOR_AXES`, `RESEARCH_PILLAGER_CROSSBOWS`, `RESEARCH_HEAVY_TRIDENTS` |
| В пяти случаях ключ реестра исследования не совпадает с ключом локализации, тост показывает сырой ключ | `research/ResearchClient.java:35-38` |

---

## 8. Что в порядке

Чтобы список не читался как приговор:

* **Ванильный спавн мобов не тронут.** `MobSpawnEvent`, `NaturalSpawner`, `SpawnPlacement` в
  `src/main/java` не обрабатываются вообще.
* **Туман войны не действует на не-RTS игроков.** Каждый серверный гейт замыкается через
  `FogOfWarServerEvents.isFogActiveFor` (`:112-119`), возвращающий `true` для всех, кроме
  RTS-игроков. Включается только через `/rts-fog enable` при
  `reignofnetherRtsMap` + `reignofnetherForceFog`.
* **Мод никогда не меняет размер границы мира.** Все обращения — чтение `isWithinBounds` /
  `getSize` / `getDistanceToBorder` либо реакция на чужое изменение.
* **Ванильные звуки не подменены** — ни `assets/minecraft/sounds.json`, ни пересечений
  неймспейсов.
* **Все 23 геймрула синхронизированы** на клиент и защищены проверкой прав.
* **4 аксессора и `PlayerMixin`** не меняют поведение вообще.
* **20 миксинов** закрыты `instanceof`-проверкой и не трогают ваниллу, пока в мире нет
  сущностей мода.
* **Пять `fogofwar/*` серверных миксинов** закрыты двойным гейтом (`reignofnetherRtsMap` +
  `isRTSPlayer`) и после этапа 1 деинтрузивности в чужом мире не работают.
* **`fire/WalkNodeEvaluatorMixin`, `StructureBlockEntityMixin`, `BeaconBlockEntityMixin`,
  `WitchMixin`** — инертны для ваниллы: уходят сразу по `instanceof`/`isClientSide`.

---

## 9. Предлагаемый порядок этапов

Порядок пересобран по вреду, а не по удобству. Прежний порядок из `CLEAN_FORK.md` начинался
с миксинов и пропускал генерацию мира и ассеты.

| Этап | Что | Почему так |
|---|---|---|
| **0** | Категория 0 этого документа: права команд, валидация движения, элитра, дублирование частиц, `BuildingCommand` | Это дыры, а не «интрузивность». Чинится за час, чинит чужой мир от реальных поломок |
| **1** | Уже сделано (`737a53e2`): opt-in RTS-режима через `reignofnetherRtsMap` | — |
| **2** | Категория 1: удалить `data/minecraft/**`, кроме `tags/blocks/mineable/axe.json`. Это возвращает ванильную генерацию буквально удалением файлов | Самое разрушительное для чужого мира: нет пещер, поверхность и руда переделаны |
| **3** | Категория 4 из §4: удалить `assets/minecraft/**`, кроме `textures/item/trident.png` | Один файл перекрашивает весь GUI игры |
| **4** | Перекраска за границей мира + невидимая стена границы + отсечение сущностей/предметов за ней | Единственный безусловный recolour, бьёт модпак-карты с суженной границей |
| **5** | Геймрулы из §6: `doNetherConversion`, `buildingsOutsideBorder`, `neutralAggro`, `doUnitGriefing`, `doLogFalling` → ванильные дефолты | Пять дефолтов, каждый меняет обычный геймплей |
| **6** | Категория 2 (§3): огонь, спавнеры, скульк, блейзы, удушье, паника, листва, клыки, трезубец, зелья, арбалет | Каждый миксин — отдельный флаг или отдельный коммит |
| **7** | Категория 4 (§5): отмена ванильного лута, спавн животных, подавление роста культур, `break → AIR` | Ломает обычное выживание; часть — за флагом `reignofnetherSurvivalEcology` |
| **8** | Разделение миксинов по конфиг-флагам, клиентские миксины под флаги | Требует §7 `CLEAN_FORK.md` (поведенческие флаги) |
| **9** | Переименование геймрулов в namespaced-имена | ⚠️ ломает существующие миры, нужен миграционный путь |
| **10** | Ленивое создание `SavedData` | Мелочь по сравнению с предыдущими |
| **11** | Тикеты чанков | Уже частично закрыто (`ChunkTicketUtil`, радиус 0) |
| **12** | Финальная проверка чистоты | — |

Мёртвый код из §7.2 удаляется отдельным коммитом в любой момент — он ничего не меняет.

---

## 10. Связанные документы

* `CLEAN_FORK.md` — план работ и инвентаризация миксинов/геймрулов. Не покрывал генерацию
  мира, ассеты и перекраску — это закрыто здесь.
* `STATUS.md` — хронология и методы диагностики.
* `_GUIDES/README.md` — гайд по контенту.
* `README.md` — индекс и правила работы с кодом.