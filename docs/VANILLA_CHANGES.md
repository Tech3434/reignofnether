# Изменения ванильных механик (текущее состояние)

Составлено 2026-10-08 по факту кода на `wip/stage-d-deletions` (поверх `67e020fd` + незакоммиченные
правки прогонов 2–5). Цель документа — честный список всего, чем мод трогает ванильную игру, и того,
что он **не** трогает.

## Коротко: чего мод НЕ делает с ванилью

- `assets/minecraft/**` **удалён целиком** — перекраски GUI, меню, сплэшей, рождественского кота нет.
- `data/minecraft/**` содержит **только** `tags/blocks/mineable/axe.json` (аддитивно добавляет
  модовые блоки в ванильный тег топора; ванильные записи не трогает).
- Датапак генерации мира (`data/minecraft/worldgen`, `flat_dimensions`, `overworldify`, `tectonic`)
  **удалён** — генерация, биомы, руда, пещеры, каньоны ванильные.
- Форс `disableElytraMovementCheck`, форс `allcheats`, форс времени суток, форс режима игры при
  загрузке — **убраны**.
- Туман войны **удалён целиком** (миксины и подсистема).
- Огонь/спавнеры/скульк/блейз/бочки/листья/паника/удушье/заморозка/эвокер/трезубец/зелья/арбалет —
  ванильные (соответствующие миксины удалены или инертны).

## 1. Общие (common) миксины, которые реально меняют ваниль

Всего 22 common-миксина; 4 из них — чистые accessor/invoker (не меняют поведение):
`EntitySelectorAccessor`, `LivingEntityAccessor`, `ObjectiveCriteriaAccessor`,
`StructureBlockEntityAccessor`.

Меняющие поведение:

| Миксин | Цель | Что делает | Затрагивает ли ваниль вне мода |
|---|---|---|---|
| `EntityMixin` | `Entity` | Дальность отрисовки предметов ×4 в орторежиме; отмена тушения и звука при эффекте `souls_aflame`; подсветка (`isCurrentlyGlowing`) охотничьих животных при вкл. подсветке на миникарте. | Да, но только: (а) клиент+орторежим, (б) сущности с модовым эффектом, (в) при включённой подсветке животных. |
| `LivingEntityMixin` | `LivingEntity` | Перезапись входящего урона для атак **юнитов** (ArmorUnit-путь), пропуск ванильной брони для атак юнита, отказ от прерывающих эффектов для `uninterruptable` юнитов, урон от `intense_heat`, партиклы левитации. | Гейт по `AttackerUnit`/`Unit`/модовым эффектам — ванильные существа не затронуты. |
| `LivingEntityRendererMixin` | `LivingEntityRenderer` | Принудительная дрожь при `attack_slowdown` на `wraith_snow_layer`. | Только модовый эффект/блок. |
| `MobMixin` (root) | `Mob` | `doHurtTarget` — шанс уклонения для **юнитов**; `setTarget` — пустой (мёртвый) инжект. | Только когда цель — `Unit`. |
| `goals/MobMixin` | `Mob` | Отмена `setTarget` для мобов из `attackSuppressedNonUnits` (управление ванильными мобами в RTS). | Только для мобов, добавленных модом в список. |
| `goals/RandomStrollGoalMixin` | `RandomStrollGoal` | Отмена `canContinueToUse` для мобов из `moveSuppressedNonUnits`. | Только для мобов из списка. |
| `PathNavigationMixin` | `PathNavigation` | Отмена ванильного рекомпа пути и расширение вертикального гейта для **юнитов** при вкл. `rtsPathfinding`. | Гейт `mob instanceof Unit` + геймрул. |
| `fire/WalkNodeEvaluatorMixin` | `WalkNodeEvaluator` | Проходимость огня/магмы и непроходимость листвы/сталактитов для **юнитов**. | Только `mob instanceof Unit`. |
| `PowderSnowBlockMixin` | `PowderSnowBlock` | Рыхлый снег превращается в снежный блок при входе **юнита**. | Только `Unit`. |
| `LevelChunkMixin` | `LevelChunk` | Пометка чанка «грязным» для кэша проходимости при `rtsPathfinding`. | Гейт геймрулом; без него инертно. |
| `HoglinMixin` / `IronGolemMixin` | `Hoglin` / `IronGolem` | Уклонение при атаке **юнита**. | Только когда цель — `Unit`. |
| `VexMixin` | `Vex` | Пустой инжект `tick`. | Ничего. |
| `WitchMixin` | `Witch` | Пересоздаёт цели лечения/атаки, если они `null`. | В ванили не `null` → no-op. |
| `ArmorStandMixin` | `ArmorStand` | Атака мобом по стойке **отменяется** (стойка не получает урон) и, если она в строении, наносит урон строению. | **Да**: любая стойка мира не получает урон от мобов. Кандидат на сужение до стоек внутри строений. |
| `AbstractArrowMixin` | `AbstractArrow` | `@Overwrite` `canHitEntity` и `onHitEntity` (полная копия ванильной логики + боеприпасы-прошивка юнитов), `tick` — угол nophysics-стрел, `tickDespawn` — укороченная жизнь стрел **юнитов**, `onHitBlock`/коллизия — игнор строения для гарнизонных юнитов. | **Да, но эквивалентно ванили** для не-юнитов; риск совместимости (полный overwrite). |
| `UnitInventoryMobMixin` | `Mob` | Даёт **каждому** мобу 6-слотовый инвентарь; NBT `reignofnether:UnitItems` пишется только при непустом инвентаре; выброс предметов при смерти. | Инертно, пока инвентарь не используется; тег появляется только у несущих. |
| `StructureBlockEntityMixin` | `StructureBlockEntity` | Варианты `updateBlockState`/`getRelatedCorners` для `RTSStructureBlockEntity`. | Только модовый блок. |
| `DataCommandsMixin` | `DataCommands` | Добавляет провайдер `/data … building` в ванильное дерево команд. | Да — в команды. |
| `ExecuteCommandMixin` | `ExecuteCommand` | Добавляет подкоманды `/execute …` (owner/…). | Да — в команды. |

## 2. Клиентские (client) миксины — 11

Почти все активны **только** в орторежиме (`OrthoviewClientEvents.isEnabled()`) или при модовом
контенте:

| Миксин | Цель | Что делает | Ваниль вне RTS |
|---|---|---|---|
| `OrthoViewMixin` | `GameRenderer` | Ортографическая матрица проекции. | Только орто. |
| `CameraMixin` | `Camera` | Отодвигает/наклоняет камеру. | Только орто. |
| `FrustumMixin` | `Frustum` | Заглушка `offsetToFullyIncludeCameraCube` (иначе залипание). | Только орто. |
| `RenderChunkRegionMixin` | `RenderChunkRegion` | Подмена листвы/снега на стекло/воздух (режимы листвы). | Только орто + включённый режим листвы. |
| `MixinMinecraft` | `Minecraft` | Возвращает обработку клавиш под `TopdownGui`. | Только при открытом TopdownGui. |
| `ChatComponentMixin` | `ChatComponent` | Сдвиг Y кликов по чату. | Только орто. |
| `ClientLevelMixin` | `ClientLevel` | Звук ортокак-будто-на-земле; `tickTime` — только при источнике ночи (контент удалён → no-op); `getSkyColor` — при кровавой луне (удалена → no-op); `setServerVerifiedBlockState`/`addDestroyBlockEffect` — dead-код (`if(false)`). | Фактически только орто/звук; остальное инертно. |
| `ClientPacketMixin` | `ClientPacketListener` | Учёт времени; отмены пакета нет. | Inertный. |
| `CycleButtonMixin` | `CycleButton` | Не показывает экран LOAD для RTS-структурного блока. | Только модовый блок. |
| `ResourceLoadStateTrackerMixin` | `ResourceLoadStateTracker` | Пересборка кнопок способностей после смены языка. | Не влияет на ваниль. |

## 3. Не-миксиновые изменения

- **Геймрулы.** Мод регистрирует 20 своих геймрулов (`doLogFalling`, `neutralAggro`,
  `doPlayerGriefing`, `groundYLevel`, `flyingMaxYLevel`, `allowBeacons`, `beaconWinMinutes`,
  `pvpModesOnly`, `slantedBuilding`, `allowedHeroes`, `lockAlliances`, `doNetherConversion`,
  `buildingsOutsideBorder`, `reignofnetherRtsMap`, `rtsPathfinding`, `pathfindingThreads`,
  `pathfindingChunkBuildsPerTick`, `animalSpawnYDiff`, `randomItemDrops`, `maxPopulation`).
  Ванильные геймрулы мод больше **не переопределяет**. `doPlayerGriefing` после правки прогона 4
  **ни на что не влияет** (оставлен зарегистрированным).
  Дефолты, меняющие «ванильное поведение при первом запуске», у всех `false`/безопасные.
- **`data/reignofnether/functions`** — нет. **`data/minecraft/functions`** — нет.
- **Время суток**: сервер его не форсит; клиент трогает только рядом с источником ночного искажения
  (контент удалён).
- **Союз/команды**: `/rtsapi`, `/execute rts-*`, `/data … building` — расширения команд.

## 4. Известные «глобальные» шероховатости (кандидаты на сужение)

1. `ArmorStandMixin` — моб-атака по любой стойке в мире не наносит урона (не только по стойкам
   внутри строений).
2. `AbstractArrowMixin` — `@Overwrite` `canHitEntity`/`onHitEntity` для всех стрел; для не-юнитов
   эквивалентно ванили, но ломается при обновлении vanilla-логики.
3. `DataCommandsMixin`/`ExecuteCommandMixin` — расширяют ванильное дерево команд.
4. `UnitInventoryMobMixin` — добавляет инвентарь каждому мобу (инертно, но тег возможен).
5. `LivingEntityMixin` — общий редирект урона на `LivingEntity` (гейт по `AttackerUnit`, но точка
   инъекции на всю иерархию).
