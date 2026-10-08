# Изменения ванильных механик (текущее состояние)

Составлено 2026-10-08 по факту кода на `wip/stage-d-deletions`. Обновлено после сужения
(`ArmorStandMixin` удалён, `AbstractArrowMixin` без `@Overwrite`, геймрулы сокращены,
`LivingEntityMixin` сужен). Цель документа — честный список всего, чем мод трогает ванильную игру.

## Коротко: чего мод НЕ делает с ванилью

- `assets/minecraft/**` **удалён целиком** — перекраски GUI, меню, сплэшей, рождественского кота нет.
- `data/minecraft/**` содержит **только** `tags/blocks/mineable/axe.json` (аддитивно добавляет
  модовые блоки в ванильный тег топора; ванильные записи не трогает).
- Датапак генерации мира **удалён** — генерация, биомы, руда, пещеры, каньоны ванильные.
- Форс `disableElytraMovementCheck`, форс `allcheats`, форс времени суток, форс режима игры при
  загрузке — **убраны**.
- Туман войны **удалён целиком**.
- Огонь/спавнеры/скульк/блейз/листья/паника/удушье/заморозка/эвокер/трезубец/зелья/арбалет —
  ванильные.

## 1. Общие (common) миксины — 21, из них 4 — чистые accessor/invoker

`EntitySelectorAccessor`, `LivingEntityAccessor`, `ObjectiveCriteriaAccessor`,
`StructureBlockEntityAccessor` поведение не меняют.

Меняющие поведение (вне мода почти ничего не задето):

| Миксин | Цель | Что делает | Ваниль вне мода |
|---|---|---|---|
| `EntityMixin` | `Entity` | Дальность отрисовки предметов ×4 в орто; отмена тушения/звука при `souls_aflame`; подсветка охотничьих животных при вкл. подсветке. | Только орто / модовый эффект / вкл. подсветка. |
| `LivingEntityMixin` | `LivingEntity` | Партиклы левитации **только юнитам**; редирект входящего урона для атак `AttackerUnit` (ванильная ветка для остальных); пропуск ванильной брони для атак юнита; блок прерывающих эффектов для `uninterruptable` юнитов; урон от `intense_heat`. Пустая инъекция `onChangedBlock` и мёртвый `FrostWalkerOnEntityMoved` удалены. | Гейт по `AttackerUnit`/`Unit`/модовым эффектам. |
| `LivingEntityRendererMixin` | `LivingEntityRenderer` | Дрожь при `attack_slowdown` на `wraith_snow_layer`. | Модовый эффект/блок. |
| `MobMixin` (root) | `Mob` | Уклонение для **юнитов**; `setTarget` — пустой (мёртвый) инжект. | Только цель `Unit`. |
| `goals/MobMixin` | `Mob` | Отмена `setTarget` для мобов из `attackSuppressedNonUnits`. | Только мобы из списка. |
| `goals/RandomStrollGoalMixin` | `RandomStrollGoal` | Отмена `canContinueToUse` для мобов из `moveSuppressedNonUnits`. | Только мобы из списка. |
| `PathNavigationMixin` | `PathNavigation` | Отмена ванильного рекомпа пути и вертикальный гейт для **юнитов** при `rtsPathfinding`. | Гейт `mob instanceof Unit` + геймрул. |
| `fire/WalkNodeEvaluatorMixin` | `WalkNodeEvaluator` | Проходимость огня/магмы и непроходимость листвы для **юнитов**. | Только `Unit`. |
| `PowderSnowBlockMixin` | `PowderSnowBlock` | Рыхлый снег → снежный блок при входе **юнита**. | Только `Unit`. |
| `LevelChunkMixin` | `LevelChunk` | Пометка чанка для кэша проходимости при `rtsPathfinding`. | Гейт геймрулом. |
| `HoglinMixin` / `IronGolemMixin` | `Hoglin` / `IronGolem` | Уклонение при атаке **юнита**. | Только цель `Unit`. |
| `VexMixin` | `Vex` | Пустой инжект `tick`. | Ничего. |
| `WitchMixin` | `Witch` | Пересоздаёт цели, если они `null`. | В ванили no-op. |
| `AbstractArrowMixin` | `AbstractArrow` | **Без `@Overwrite`**: инъекция в `canHitEntity` только обнуляет результат для стрел юнита; `onHitEntity` перехватывается и копия ванильной логики применяется **только стрелам юнита**; угол nophysics-стрел — только юнитам; укороченная жизнь и игнор строения — только юнитам. | Стрелы игрока/ванильных мобов идут ванильным путём. |
| `UnitInventoryMobMixin` | `Mob` | Даёт **каждому** мобу 6-слотовый контейнер; NBT-тег только при непустом; выброс при смерти. **Сохранён как фреймворк** (план §1): любой моб может быть юнитом, иного общего предка у юнитов нет. | Инертно, пока инвентарь не используется. |
| `StructureBlockEntityMixin` | `StructureBlockEntity` | Варианты для `RTSStructureBlockEntity`. | Только модовый блок. |
| `DataCommandsMixin` | `DataCommands` | Добавляет провайдер `/data … building`. **Оставлен по решению владельца.** | Да — расширяет команды. |
| `ExecuteCommandMixin` | `ExecuteCommand` | Добавляет подкоманды `/execute … building`. **Оставлен по решению владельца.** | Да — расширяет команды. |

**Удалён:** `ArmorStandMixin` — стойки в строениях не используются, ванильная неуязвимость стоек к
моб-атакам больше не отменяется.

## 2. Клиентские (client) миксины — 11, почти все только в орторежиме

`OrthoViewMixin`, `CameraMixin`, `FrustumMixin`, `RenderChunkRegionMixin`, `MixinMinecraft`,
`ChatComponentMixin`, `CycleButtonMixin`, `ResourceLoadStateTrackerMixin` — активны только в
орторежиме / модовом контексте. `ClientLevelMixin`/`ClientPacketMixin` — звук только в орто;
`tickTime`/`getSkyColor` инертны (контент удалён); `setServerVerifiedBlockState`/
`addDestroyBlockEffect` — мёртвый код (`if(false)`).

## 3. Не-миксиновые изменения

- **Геймрулы — 12** (было 20). Оставлены только те, что реально читаются:
  `doLogFalling`, `neutralAggro`, `maxPopulation`, `slantedBuilding`, `lockAlliances`,
  `doNetherConversion`, `buildingsOutsideBorder`, `reignofnetherRtsMap`, `rtsPathfinding`,
  `pathfindingThreads`, `pathfindingChunkBuildsPerTick`, `animalSpawnYDiff`.
  Ванильные геймрулы мод **не переопределяет**.
  **Удалены** (не имели читателей): `doPlayerGriefing`, `groundYLevel`, `flyingMaxYLevel`,
  `allowBeacons`, `pvpModesOnly`, `beaconWinMinutes`, `allowedHeroes`, `randomItemDrops`
  (и enum `RandomItemDropRule`). Потолок высоты для летающих юнитов теперь `Unit.getFlyingMaxY()`
  (по умолчанию 320, юнит может переопределить).
- **Команды**: `/rtsapi`, `/execute … building …`, `/data … building` — расширения командного
  дерева (последние два — осознанно оставлены).
- **Время суток** сервер не форсит.

## 4. Осознанно оставленные интрузии

1. `DataCommandsMixin`/`ExecuteCommandMixin` — расширяют ванильное дерево `/data` и `/execute`
   (только модовые цели `building`; решение владельца — оставить).
2. `UnitInventoryMobMixin` — контейнер у всех мобов; сужение невозможно без потери фреймворка.
3. `LivingEntityMixin` — редиректы на `LivingEntity.hurt`/`actuallyHurt` (иначе не перехватить урон
   юнитов); поведение гейтится `AttackerUnit`/`Unit`.
4. `AbstractArrowMixin` — перехват `onHitEntity` копией ванильной логики (только для стрел юнита).
