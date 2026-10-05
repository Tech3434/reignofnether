# План: оставить только РТС-каркас, контент вынести в документацию

Цель ветки: мод становится **чистым каркасом РТС-системы**. Всё, что является контентом
готовых фракций — юниты, здания, способности, исследования, предметы, ресурсы, волны —
удаляется из кода и сохраняется как документация. Остаётся механика: интерфейсы и цели,
производство, здания как система, атрибуты, эффекты, чары, камера, HUD, команды, читы.
Позже владелец ветки пишет на этом каркасе свою фракцию.

Обоснование: `FEATURES.md` (каталог), `INTRUSION_AUDIT.md` (инвентаризация вреда),
`HOWTO_FACTION.md` (как добавлять контент). Состояние кода при составлении: `5079004e`.

## Принятые решения

1. **Удаляются «дефолтные фракции» жителей / нежити / пиглинов** вместе со всем
   содержащимся контентом. `Faction` удаляется полностью, абстракцией не заменяется.
2. **Удалённый контент сохраняется как документация** — переносится в `docs/reference/`, а не
   остаётся в дереве. Код и так сохраняется в git поимённо.
3. **Каркас РТС сохраняется целиком**: интерфейсы юнитов и зданий, цели, производство,
   гарнизоны, ауры, атрибуты, эффекты, камера, HUD, команды, читы, кастомные строения.
   Чары уходят (решение 9).
4. **Режимы, кроме `CLASSIC`, удаляются.**
5. **Интрузивность убирается.**
6. **Туман войны удаляется целиком.** Совместимой переработки владелец не заказывал, а текущая
   конструкция совместимости не имеет: она сокрывает данные, а не рисует, поэтому конфликтует с
   любым модом рендера и протекает мимо фильтра пакетов. Разбор — `INTRUSION_AUDIT.md` §4.2.
7. **Остаются как рабочий шаблон:** `VillagerUnit` (строитель), `TownCentre` (ратуша),
   `Barracks` (производственное здание), `VindicatorUnit` (боевой юнит).
8. **Остаётся палитра спецблоков** — под будущие способности новых юнитов.
9. **Остаётся инвентарь юнита** (`UnitInventory` и его миксин) — чтобы юниты носили предметы
   из мира.
10. **Чары и способности — в документацию и удаляются** (решение владельца).

## ⚠ Последствие, о котором надо знать до начала

После удаления контента в моде останется **четыре юнита-шаблона и два здания** (решение 7).
Мод почти перестанет быть играбельным: гейты `compileJava`, `validateMixins`, `runData`,
`runServer`, `runClient` будут зелёными, но игровой функционал сведутся к двум зданиям.

Отсюда порядок этапов: сначала убрать интрузивность (этапы A–C), пока контент ещё есть и
этапы можно проверить в игре, и только потом удалять контент (этап D).

Практическая рекомендация: держать промежуточную точку. Пока каркас пустой, ветка почти
непригодна для игры — если нужен играбельный вариант, его стоит держать на отдельной ветке от
этапа C.

---

## 1. Что остаётся — каркас (не трогать ни в одном этапе)

| Подсистема | Файлы | Зачем |
|---|---|---|
| Интерфейсы юнитов | `unit/interfaces/{Unit,AttackerUnit,WorkerUnit,RangedAttackerUnit,HeroUnit,ConvertableUnit,ArmSwingingUnit,KeyframeAnimated}` | контракт юнита |
| Статика юнита | `Unit.createDefaultAttributes()`, `Unit.tick()`, `Unit.createCooldownMap()` | база атрибутов и тик |
| Цели и ИИ | `unit/goals/**` (34 файла) | вся навигация и поведение |
| Пути | `unit/pathfinding/**` (13 файлов) | гейт тройной, ваниллу не вредит |
| События юнитов | `unit/UnitServerEvents`, `UnitClientEvents`, `NonUnitServerEvents` | спавн, команды, управление ванильными мобами |
| Инвентарь юнита | `items/UnitInventory` | 6 слотов, добавляется миксином каждому мобу |
| Базовые классы способностей | `ability/{Ability,HeroAbility,Abilities}`, `AbilityButton`, `ability/UnitAction` | контракт способности |
| Здание как система | `building/{Building,BuildingPlacement,BuildingUtils,BuildingValidators,BuildingBlockData,BuildingPlaceButton}`, `building/buildings/placements/**` | ядро зданий |
| Производство | `building/production/{ProductionItem,ActiveProduction,ProductionItemList,ProdDupeRule,ProductionBuilding,ProductionPlacement}` | очередь, стоимость, `onComplete` |
| Аддоны | `building/addon/**` (5 контрактов) | гарнизон, аура, конвертация, магазин |
| Данные размещения | `building/data/{DataType,DataStorage}` | персистентность аддонов |
| Кастомные строения | `building/custombuilding/**` + `RTSStructureBlockEntity` | инструмент автора, полностью самодостаточен |
| Атрибуты | `registrars/AttributeRegistrar` | 20 атрибутов, работают на любой сущности |
| Эффекты | `registrars/MobEffectRegistrar` | 28 эффектов |
| Частицы | `registrars/ParticleRegistrar` | 6 типов |
| ~~Чары~~ | ~~`registrars/EnchantmentRegistrar`~~ | **удаляются по решению владельца**, см. §7 E.1 |
| Реестры | `api/ReignOfNetherRegistries` (`BUILDING`, `PRODUCTION_ITEM`, `DATA_TYPE`) | точка регистрации |
| Камера и UI | `orthoview/**`, `guiscreen/TopdownGui*`, `minimap/**`, `hud/**` | рабочий инструмент ГМа |
| Команды | `commands/**` | `/rtsapi`, `execute rts-related`, аргумент-селекторы |
| Читы | `player/PlayerServerEvents.onPlayerChat`, список читов | операторские переключатели |
| Союзы | `alliance/**` | командные отношения |
| Игроки и матч | `player/{RTSPlayer,RTSPlayerSaveData,PlayerServerEvents,PlayerClientEvents}` | вход, выход, счёт |
| Песочница-инструмент | `sandbox/{SandboxServer,SandboxClientEvents,SandboxActionButtons}` | спавн юнитов, якоря, переключатель отношений |
| Утилиты | `util/{MyRenderer,LevelRenderCompat,ArrayUtil,SavedDataCompat,ChunkTicketUtil,MiscUtil}` | отрисовка и мелкие помощники |
| Прочее | `keybinds/**`, `gamerules/**`, `time/TimeUtils`, `api/**`, `config/**` | инфраструктура |

## 2. Что уходит в документацию

| Подсистема | Файлов | Куда |
|---|---|---|
| Юниты | `unit/units/**` — 54 класса + `*Prod` | `docs/reference/units.md` |
| Здания | `building/buildings/**` — 51 + абстрактные базы | `docs/reference/buildings.md` |
| Способности | `ability/abilities/**`, `ability/heroAbilities/**` — 93 | `docs/reference/abilities.md` |
| Чары | `registrars/EnchantmentRegistrar`, `data/reignofnether/enchantment/*.json` — 7 | `docs/reference/enchantments.md` |
| Туман войны | `mixin/fogofwar/**` (19), `FogOfWar*`, `FogChunkSnapshot`, `FogTintingBlockColor`, геймрул `reignofnetherForceFog` | `docs/reference/fog-of-war.md` — почему сокрытие данных несовместимо с модами рендера и что делала визуальная альтернатива |
| Исследования | `research/researchItems/**` — 51 | `docs/reference/research.md` |
| Экономика | `resources/{ResourceSources,ResourceIndex,ResourceChunk,ResourcesServerEvents}` | `docs/reference/economy.md` |
| Предметы юнитов | `items/{UnitItem,UnitItemBuilder,UnitItems,unititems/**}` | `docs/reference/items.md` |
| Волны | `survival/**` — 15 | `docs/reference/waves.md` |
| Сценарий, обучение, старт | `scenario/**`, `tutorial/**`, `startpos/**`, `matchstart/**`, `rtsmap/**` | `docs/reference/match-setup.md` |
| Фракция | `faction/**` | `docs/reference/faction.md` |
| Ассеты контента | текстуры юнитов/зданий, `models/item/*` спавн-яиц, звуки юнитов, lang-ключи | `docs/reference/assets.md` |
| Структуры | `data/reignofnether/structures/*.nbt` — 67 | `docs/reference/buildings.md` |

Восстановление любого класса: `git show 5079004e:src/main/java/com/solegendary/reignofnether/<путь>`.

---

## 3. Этап A — закрыть реальные поломки

Пока контент есть, этап проверяем в игре.

☐ A.1 Убрать форс `disableElytraMovementCheck` — `player/PlayerServerEvents.java:183`.
☐ A.2 Вернуть ванильную валидацию движения — `mixin/ServerGamePacketListenerImplMixin.java:11-25`.
☐ A.3 Дублирование пакетов частиц — `mixin/ServerLevelMixin.java:16-30`.
☐ A.4 Права команд: `/sendfood`, `/sendwood`, `/sendore`, `/sendemerald`
(`resources/ResourcesServerEvents.java:340-375`), `/rts-fog`
(`fogofwar/FogOfWarServerEvents.java:623-635`). Минимум `hasPermission(2)`.
☐ A.5 Ограничить `BuildingCommand` (`building/BuildingCommand.java:138-164`) — сейчас
выполняет произвольные команды с правом 2.
☐ A.6 Убрать форс `allcheats` на два авторских ника (`player/PlayerServerEvents.java:859-862`).
☐ A.7 Вернуть ключи локализации 7 чар (`enchantment.reignofnether.*`).

**Проверка:** гейты 1–4 плюс ручная — разгон не рвёт соединение, частицы не двоятся,
`/sendfood` без прав отвечает отказом.

---

## 4. Этап B — убрать интрузивность

Порядок из `INTRUSION_AUDIT.md` §9. Каждый пункт отдельный коммит.

### B.1 Генерация мира
☐ B.1.1 Удалить `data/minecraft/**`, кроме `tags/blocks/mineable/axe.json`.
Возвращает пещеры и каньоны (`probability` 0.00), ванильную плотность рельефа, 14 ванильных
биомов без руды на поверхности.
☐ B.1.2 Удалить мёртвое: `data/flat_dimensions/**`, `data/overworldify/**`,
`data/tectonic/**`, `data/minecraft/tags/functions/**`, `data/reignofnether/functions/**`.

### B.2 Ванильные ассеты
☐ B.2.1 Удалить `assets/minecraft/`, кроме `textures/item/trident.png` и
`textures/entity/trident.png` (решение при удалении `BlazeMixin` — если он не нужен, удалить и их).
Убирает перекраску всех GUI, рождественского кота, подмену сплэшей и логотипа меню.

### B.3 Туман войны — удаляется целиком

Текущий туман сокрывает **данные**, а не рисует: `ChunkMapMixin` выкидывает затемнённых
игроков из получателей чанка, `TrackedEntityMixin` отменяет синхронизацию сущностей,
`ServerLevelParticleMixin` режет частицы, `ChunkMapInitialSendMixin` отдаёт снимок с диска,
`ClientChunkCacheMixin` заставляет клиент держать устаревший снимок, `ClientLevelMixin
.setServerVerifiedBlockState` отбрасывает серверные обновления блоков,
`ClientPacketListenerLightMixin` сохраняет клиентский свет. Из-за этого он несовместим с любым
модом рендера и любым модом, шлющим свои пакеты по чанку. Решение владельца — удалить.

☐ B.3.1 Удалить `mixin/fogofwar/` целиком (19 миксинов) и убрать их из
`reignofnether.mixins.json`.
☐ B.3.2 Удалить подсистему: `FogOfWarServerEvents`, `FogOfWarClientEvents`,
`FogChunkSnapshot`, `FogTintingBlockColor`, `FogOfWarServerboundPacket`,
`FogChunkServerboundPacket`, `FogChunkClientboundPacket`, `FogBuildingClientboundPacket`,
`FogOfWarServerEvents`-связанные пакеты в `registrars/PacketHandler`.
☐ B.3.3 Удалить `ClientModEvents.onBlockColourEvent` и `ModelEvent.ModifyBakingResult`
(обёртка каждого блока и каждой модели) вместе с `mixin/fogofwar/BlockColorsAccessor`.
☐ B.3.4 Удалить `FogOfWarClientEvents.onRenderLivingEvent` — отмену рендера живых сущностей
за границей мира.
☐ B.3.5 Удалить геймрул `reignofnetherForceFog` и его отображение в
`gamerules/GameruleClientboundPacket`, `GameruleServerboundPacket`, `GameruleAction`,
`GameruleClient`, локализации.
☐ B.3.6 **Свип зависимостей.** Триан удаляемых компонентов имеют читателей вне подсистемы:
`RangedAttackerUnit.getFogRevealDuration` (читает GhastUnit, который удаляется),
`NightUtils.isInRangeOfNightSource` (комментирует использование ауры ночи),
`BloodMoon` (читает `isBlockVisibleFor`, удаляется вместе с NecromancerUnit),
`MinimapClientEvents` (`DARK_TERRAIN_PARTITIONS_MAX` — слой затемнённого рельефа становится
мёртвым, миникарту оставляем как инструмент ГМа, слой удалить),
`UnitServerEvents`/`FogOfWarClientEvents` в HUD. Найти все и почистить.
☐ B.3.7 Проверить, что `Mausoleum`/`Stronghold` (`NightSourceAddon`) не ссылаются на туман.
Они удаляются, но `NightSourceAddon` остаётся как контракт — комментарии в нём обновить.

### B.4 Перекраска за границей мира — попадает в B.3

☐ B.5.1 Ветка `0x252933` в `FogTintingBlockColor`, `BiomeColorsMixin`,
`LiquidBlockRendererMixin` уходит вместе с B.3.
☐ B.5.2 Скрытие предметов за границей — `fogofwar/ItemEntityRendererMixin.java:72` уходит
вместе с миксином. Проверить, нет ли не-туманного назначения у этого миксина.
☐ B.5.3 **Стена границы мира рисуется** (решение владельца). Удалить
`mixin/fogofwar/WorldBorderRenderMixin.java`: он отменяет `LevelRenderer.renderWorldBorder`.
Удаление миксина предпочтительнее инверсии условия — это возвращает ровно ванильное
поведение.

### B.5 Дефолты геймрулов

**Решение владельца:** поведение должно совпадать с ванильным.

☐ B.5.1 `doNetherConversion` → `false` (сейчас порталы зданий переписывают террейн).
☐ B.5.2 `buildingsOutsideBorder` → `false`.
☐ B.5.3 `neutralAggro` → `false`.
☐ B.5.4 **`doPlayerGriefing` → `false`.** Это ванильный геймрул, и мод регистрирует его с
дефолтом `true`, то есть меняет ванильное поведение (в ванилле ломание блоков выключено по
умолчанию). Возврат `false` — это и есть «как в ванильной игре».
☐ B.5.5 `doLogFalling` → `false` (ломание бревна превращает соседние в модовые блоки).
☐ B.5.6 **`doUnitGriefing` — геймрула в ванилле нет.** Сейчас при `false`
`BuildingServerEvents.onExplosion:958-964` срезает урон взрывов по блокам до листвы и TNT,
то есть криперы и TNT в мире не разрушают ничего — это не ванильное поведение. Варианты:
(a) удалить геймрул и перехватывать только тот урон, что попадает по
`BuildingPlacement`, оставив всё остальное ванилле; (b) оставить геймрул, но дефолт `true`.
Рекомендация — (a): геймрул без ванильного аналога в общем пространстве имён только
конфликтует.

### B.6 Безусловные миксины
☐ B.6.1 `fire/FireBlockMixin` — вернуть ванильную таблицу горючести, горение до age 15, убрать
горючесть обсидиана и `tryCatchFire`.
☐ B.6.2 `fire/BaseFireBlockMixin`, `CampfireBlockMixin`, `MagmaBlockMixin` — вернуть ванильные
ставки урона.
☐ B.6.3 `BaseSpawnerMixin` — убрать форс 600 тиков на все спавнеры; роуковскую часть оставить
(со спавнерами мода уйдёт).
☐ B.6.4 `SculkCatalystBlockEntityMixin` — вернуть ванильный bloom.
☐ B.6.5 `BlazeMixin` — вернуть ванильный `aiStep`.
☐ B.6.6 `WitherRoseMixin`, `LeavesBlockMixin`, `PanicGoalMixin` — вернуть ваниллу.
☐ B.6.7 `EntityMixin` — вернуть урон от удушья и полную заморозку. **Внимание:** с отключённым
удушьем камера орторежима на высоте безопасна; если удушье вернуть — проверить, что камера не
застревает в блоках (этап G).
☐ B.6.8 `EvokerFangsMixin` — вернуть хитбокс.
☐ B.6.9 `ThrownTridentMixin`, `ThrownPotionMixin`, `CrossbowMixin.getChargeDuration` — вернуть
ванильные конвейеры.
☐ B.6.10 `UnitInventoryMobMixin` — писать `reignofnether:UnitItems` в NBT только при
непустом инвентаре, а не всем мобам.
☐ B.6.11 `AbstractArrowMixin` — объявить `canHitEntity` явно или убрать; убрать повторное
применение урона.
☐ B.6.12 `PathNavigationMixin` — ограничить `@ModifyConstant(doubleValue = 1.0)` через
`require = 0`.
☐ B.6.13 `UnitServerEvents` — не конвертировать юнита после смерти; починить
`ConvertableUnit`, который не удаляет исходную сущность.
☐ B.6.14 Удалить `PlayerMixin` из списка миксинов (тело закомментировано) и незарегистрированный
`ZoglinMixin`.

### B.7 Клиентские миксины без гейта
☐ B.7.1 `TitleScreenMixin` — за флагом, дефолт `false`. Сейчас главное меню перехватывается
целиком безусловно.
☐ B.7.2 `MusicManagerMixin` — за флагом.
☐ B.7.3 `ClientLevelMixin.tickTime` — снять безусловный `cancellable`.
☐ B.7.4 `LevelRendererMixin` (`renderLevel` TAIL) — оверлей разрушения блока с 32 до 256.

### B.8 Экология выживания
Всё за флагом `survivalEcology`, дефолт `false`. Часть пунктов уходит сама при удалении
контента — отмечать при удалении, не дублировать.
☐ B.8.1 Отмена ванильного лута животных при охоте — `unit/UnitServerEvents.java:680-729`.
☐ B.8.2 Спавн животных вокруг столицы — `building/BuildingPlacement.java:881-888`.
☐ B.8.3 Вытеснение животных вокруг нового здания — `building/BuildingServerEvents.java:617-626`.
☐ B.8.4 Подавление роста культур в здании — `resources/ResourcesServerEvents.java:261-265`.
☐ B.8.5 Ломание блока в здании → AIR — `resources/ResourcesServerEvents.java:277-283`.
☐ B.8.6 `DIRT_PATH`→`DIRT` и уничтожение растений при смерти юнита у скульк-катализатора —
`unit/UnitServerEvents.java:497-537`.
☐ B.8.7 Отмена перехода между измерениями в здании — `building/BuildingServerEvents.java:968-989`.
☐ B.8.8 `FIRE` в центре порталов каждый тик.
☐ B.8.9 Установка `Blocks.SCAFFOLDING` под фундаментом — оставить как часть зданий или снять.
☐ B.8.10 Превращение `FARMLAND`/`DIRT_PATH`/`SOUL_SAND`/`MAGMA_BLOCK` под зданием —
`BuildingPlacement.java:1072-1123`.

**Проверка B:** создать новый мир на ванильном профиле, сравнить `run/world` до/после;
ручная проверка огня, спавнеров, скулька, блейзов, руды, границы мира.

---

## 5. Этап C — режимы

☐ C.1 Удалить `scenario/**` (10 файлов) с `GameMode.SCENARIO`, `ScenarioRole`,
`ScenarioSaveData`, `ScenarioRoleSaveData`, пакетами и ветками меню.
☐ C.2 Удалить `survival/**` (15 файлов) с `GameMode.SURVIVAL`, `WaveDifficulty`,
`SurvivalSaveData`, пакетами, `/debug-next-night`, `/debug-end-wave`.
⚠ Таблицы юнитов и ИИ волн — в `docs/reference/waves.md` (этап D, до удаления).
☐ C.3 Удалить геймрул `coopMode` — он существует только чтобы отключить автопобеду.
☐ C.4 Удалить `tutorial/**`, `startpos/**`, `matchstart/**`, `rtsmap/**`.
☐ C.5 Оставить `GameMode.CLASSIC` как единственный; убрать ветки выбора режима из HUD
(`hud/HudClientEvents.java:1626-1660`) и из `ClientGameModeHelper`.
☐ C.6 Перевести песочницу с признака `Faction.NONE` в `RTSPlayer` на **проверку прав
оператора**. Песочница — инструмент ГМа, а не режим, и она остаётся.
☐ C.7 Убрать `PlayerServerboundPacket.startRTSScenario` и `publishScenarioMap`
(`player/PlayerServerEvents.java:1328-1385`).

---

## 6. Этап D — удалить контент

**Сначала** перенести в `docs/reference/`, **потом** удалять файлы.

☐ D.1 `docs/reference/units.md` — таблица ростера со статами и ролью каждого юнита; анатомия
класса юнита: `createAttributes`, `defineSynchedData`, `initialiseGoals`/`registerGoals`,
`tick`, NBT через `addUnitSaveData`, интерфейсы. Уже частично есть в `HOWTO_FACTION.md` §5 —
дополнить числами.
☐ D.2 `docs/reference/buildings.md` — таблица 51 здания по группам; анатомия класса здания;
контракты пяти аддонов; `BuildingPlacement` как система (HP по блокам, `minBlocksPercent`,
строительство, тикеты, разрушение).
☐ D.3 `docs/reference/abilities.md` — 93 способности с эффектом, кулдауном и гейтом по
исследованию; анатомия `Ability`; список 36 способностей, жёстко привязанных кастом к
классу юнита, — это и есть причина, по которой способность нельзя перенести без юнита.
☐ D.4 `docs/reference/production.md` — как `ProductionItem`, очередь и стоимость работают
вместе; чем заменять шов, если владелец захочет создавать юниты без зданий.
☐ D.5 `docs/reference/research.md` — как устроено гейтирование способностей по исследованиям
и почему гейт только клиентский.
☐ D.6 `docs/reference/economy.md` — `ResourceSources`, `ResourceIndex`, население, лимиты.
☐ D.7 `docs/reference/waves.md` — планировщик волн как готовый пример «спавнить волны по
таймеру»: таблицы T1–T6, `WaveEnemy`, `WaveDifficulty`, выбор точек.
☐ D.8 `docs/reference/items.md` — `UnitItem`/`UnitItemBuilder`, категории, инвентарь юнита;
отметить, что слой был выключен `ENABLED = false` и наполнен на 7 из 30 пунктов.
☐ D.9 `docs/reference/assets.md` — что нужно из ассетов для нового юнита/здания: модель,
текстура, `mobheads`, звуки, lang-ключи.

☐ D.10 Удалить `unit/units/**` (264 файла в пакете `unit`, из них контент — все кроме
`unit/interfaces`, `unit/goals`, `unit/pathfinding`, `unit/controls` и четырёх
`*ServerEvents`/`*ClientEvents`).
**Исключения по решению владельца (решение 7): `unit/units/villagers/VillagerUnit`,
`unit/units/villagers/VindicatorUnit` и их `*Prod` остаются как рабочий шаблон.**
☐ D.11 Удалить `building/buildings/**`.
**Исключения по решению владельца (решение 7): `building/buildings/villagers/TownCentre`
и `building/buildings/villagers/Barracks` остаются как рабочий шаблон.**
Абстрактные базы (`shared/AbstractFarm`, `AbstractStockpile`, `AbstractBridge`, `AbstractMarket`,
`piglins/AbstractPortal`) удаляются — они часть контента, а не каркаса.
☐ D.12 Удалить `ability/abilities/**`, `ability/heroAbilities/**` (решение владельца: в
документацию). В `ability/UnitAction` удалить константы способностей, оставив те, что нужны
механике (проверить по `UnitActionItem` и `Ability`).
☐ D.13 Удалить `research/researchItems/**`. Проверить `ResearchSaveData` — оставить как
инфраструктуру флагов.
☐ D.14 Удалить `resources/{ResourceSources,ResourceIndex,ResourceChunk,ResourcesSaveData,
ResourcesServerEvents}`. Решить судьбу `ResourceCost`/`ResourceCosts`: они нужны
`ProductionItem.getCost()`. Вариант — оставить один плоский `ResourceCost` без
привязки к фракциям.
☐ D.15 Удалить `items/UnitItem*`, `items/unititems/**`, `items/ItemUtil` (частично),
`items/ItemServerEvents`, `items/ItemClientEvents`. `items/UnitInventory` **остаётся** по
решению владельца (решение 9) — под перенос предметов из мира.
☐ D.16 Удалить `faction/**`.
☐ D.17 Удалить `survival/**`, `scenario/**`, `tutorial/**`, `startpos/**`, `matchstart/**`,
`rtsmap/**` (если не сделано в C).
☐ D.18 Удалить `data/reignofnether/structures/**` (67 NBT).
☐ D.19 Почистить ассеты: текстуры юнитов и зданий, `models/item/*` спавн-яиц, сплэши и
переводы контента. **Оставить** переводы каркаса: `hud.*`, `abilities.*`, `unitstats.*`,
`commands.*`, `creativetab.*`, `resources.*`, `server.*`, `unititemtype.*`,
`enchantment.reignofnether.*`.
☐ D.20 Почистить реестры. `EntityRegistrar` — 67 типов, остаются 4 юнита-шаблона.
`BlockRegistrar` — 47 блоков: **остаётся палитра** по решению владельца (решение 8) —
`walkable_magma_block`, `temporary_walkable_magma_block`, `unextinguishable_soul_fire`,
`spider_friendly_barrier`, `wraith_snow_layer_block`, `decayable_nether_wart_block`,
`decayable_warped_wart_block`, `rts_structure_block`, четыре маркера гарнизона и производства;
**удаляются** 16 `rts_start_block_*` (неразрушимые, лежат в ванильной вкладке), 8 блоков голов
и стеновых черепов, `horizontal_portal`. `ItemRegistrar`, `BlockEntityRegistrar`,
`MobEffectRegistrar`, `ParticleRegistrar`, `SoundRegistrar` — по спискам из решений ниже.

---

## 7. Этап E — дыры каркаса, найденные при удалении

Обнаруживаются только когда контента нет. Каждый пункт — либо починить, либо осознанно
оставить с записью.

☐ E.1 **Чары удаляются целиком** (решение владельца). Семь чар датапака вместе с
`registrars/EnchantmentRegistrar` и `data/reignofnether/enchantment/*.json` уходят в
`docs/reference/enchantments.md` и удаляются. Удалить и читателей в каркасе: `breaching`
(`unit/UnitServerEvents.java:1000`), `fortifying` (`unit/interfaces/Unit.java:476`,
`util/MiscUtil.getMaxAbsorptionAmount`). Снять локализацию `enchantment.reignofnether.*`
и `hud.enchant.reignofnether.*`.
☐ E.2 **Атрибуты без потребителей.** Шесть (`critical_hit_chance`, `explosive_hit_chance`,
`lifesteal`, `mana_on_hit`, `scale`, `building_damage_bonus`) имели геттеры без вызовов в пути
атаки. Частицы `floating_crit`, `floating_heart`, `mana` написаны — не хватает вызова. Либо
реализовать, либо удалить; при удалении контента `building_damage_bonus` точно не нужен.
☐ E.3 **Эффекты.** Часть из 28 использовалась только удаляемым контентом:
`zombie_infected`, `slime_infected`, `frost_damage`, `warm`, `villager_inspiration`,
`limited_lifespan`. Решить, что из этого — каркас, а что мусор.
☐ E.4 **Население без зданий.** `getTotalPopulationSupply` (`BuildingServerEvents.java:681-692`)
суммирует `cost.population` построенных зданий → без зданий лимит 0. Нужен плоский лимит
на игрока. Связано с решением по `ResourceCost` из D.14.
☐ E.5 **Ленивое создание `SavedData`.** 8 файлов пишутся в любой мир. Проверить, создаёт ли
`DataStorage.save()` записи, в которые ничего не клали; иначе — удалять пустые файлы после
сохранения.
☐ E.6 **`ProductionItems` пуст.** `ReignOfNetherRegistries.PRODUCTION_ITEM` останется пустым —
проверить, что `CustomBuilding.checkAndAddProductionItems` и
`CustomBuildingPlacement.getProductionItem` это переживают.
☐ E.7 **`UnitItem.ENABLED = false`** и 7 из 30 предметов в реестре — если слой удалён (D.15),
проверить, что на `ItemUtil` больше ничего не висит.
☐ E.8 **`neutralAggro` без юнитов** (`NonUnitServerEvents:75-98`) станет бессмысленным:
он нацеливает ванильных мобов на юнитов. `attackSuppressedNonUnits` и `nonUnitMoveTargets`
работают на ванильной навигации и остаются полезными.
☐ E.9 **Скрытая связность `EntityRegistrar.getEntityType`.** `ProductionItems.getProductionItem`
ищет юнит через `EntityRegistrar.getEntityType(prodItem.getItemName())`, поэтому строка
`itemName` в каждом `*Prod` обязана совпадать с `case` в этом свитче. Несовпадение не даёт
ошибки — кастомные строения просто молча не смогут обучить юнита. Это ловушка, на которую
попадёт владелец, добавляя своих юнитов. Либо починить (сопоставлять по
`EntityType<?>` напрямую, без строкового свитча), либо оставить как есть.

**Проверка:** гейты 1–4; `runClient` грузится без ошибок; в логе нет новых исключений при
входе в мир.

---

## 8. Этап F — `Faction`

Становится тривиальным после этапа D: при живых контентных классах enum ещё что-то значил.

☐ F.1 Удалить `faction/**` (уже в D.16).
☐ F.2 Убрать `Unit.getFaction()` (`unit/interfaces/Unit.java:172`) и
`Building.getFaction()` (`building/Building.java:100`) из контрактов. **Оба метода входят в
список «не трогать» §1 — здесь исключение осознанное**, их правка не ломает механику, а
убирает поле, которое после удаления контента не имеет носителей.
☐ F.3 Переписать `PlayerServerEvents.startRTS:463-474, 562-575` — свичи по фракции исчезают;
стартовый юнит и столица задаются параметром или конфигом.
☐ F.4 `building/custombuilding/CustomBuilding.java:93-95` — три флага
`buildableByVillagers/Monsters/Piglags` в NBT заменить одним булевым. **Сигнатура NBT
несовместима**, см. F.6.
☐ F.5 `util/MiscUtil.java:993-1014` — иконка и имя фракции.
☐ F.6 Миграция миров не нужна (решение владельца: мод для нового сервера). Старый тег
`faction` в `RTSPlayerSaveData` просто перестаёт читаться — важно, чтобы код не вызывал
`Faction.valueOf()` на старом NBT и не падал. Проверить, что `ResearchSaveData` и
`BuildingSaveData` тоже не содержат вызовов удаляемого перечисления.
☐ F.7 Локализация `hud.faction.reignofnether.*` — удалить из 22 файлов.

⚠ **Не делать** `FactionDefinition`. Это отдельная фича для нескольких играбельных рас, и
enum из шести значений ей не фундамент. Материалы уже есть в `HOWTO_FACTION.md` §2.

---

## 9. Этап G — камера

Требование владельца: менять высоту камеры и не рендерить уровни выше камеры.

Половина есть: `orthoviewPlayerBaseY = 100`, `orthoviewPlayerMaxY = 160`
(`orthoview/OrthoviewClientEvents.java:124-125`), пересчёт от рельефа (`:171-172`), зажим
телепорта по границе (`PlayerServerEvents.movePlayer`). Мёртвый геймрул `groundYLevel` влияет
только на плоскость тумана на клиенте — починить или удалить.

☐ G.1 Сделать высоту настраиваемой и проверяемой, а не вычисляемой от рельефа
(`.setBaseY()` из расчёта `:171-172`, `isValidCameraHeight()`).
☐ G.2 Отсечение секций выше камеры — миксин на `SectionRenderDispatcher.RenderSection` с
проверкой Y. Камера смотрит строго вниз, значит секции выше не видны. Один новый миксин, риск
низкий.
☐ G.3 Снять побочные эффекты входа: `PlayerServerEvents.enableOrthoview` вызывает
`removeAllEffects()` (стирает и ванильные зелья), а
`OrthoviewClientEvents.switchToEasyIfPeaceful()` молча повышает одиночный Peaceful до Easy.
**Обязательно**, иначе орторежим непригоден для обычной игры.
☐ G.4 `TopdownGuiClientEvents` форсит `guiScale = 3` и блокирует клавиши инвентаря и
достижений — снять или за флагом.
☐ G.5 После B.5.7 (возврат удушья) проверить, что камера на высоте не застревает в блоках.

**Проверка:** визуально — высота двигается, секции сверху не рисуются, эффекты игрока не
стираются при входе и выходе, Peaceful не меняется сам.

---

## 10. Этап H — РТС-режим для обычной игры

Задачи владельца, требующие своего кода. **Выполняются после того, как у каркаса появится
контент** — иначе проверять нечем.

☐ H.1 Вход в РТС-режим отдельной командой с проверкой прав. Сейчас вход возможен только
через стартовую позицию или `startRTS` с фракцией `NONE`, а режим `NONE` удаляется в C.
☐ H.2 Ограничение «назначенный игрок». Сейчас `rtsPlayers` может содержать кого угодно;
плюс `/rts-lock` (`player/PlayerServerEvents.java:1228-1256`).
☐ H.3 Юнит умирает за границей мира — `unit/interfaces/Unit.java:388-389`. На сервере с world
border армия будет вымирать.
☐ H.4 Спавн рядом с игроком в РТС самовольно отдаёт ему юнита —
`unit/UnitServerEvents.java:847-857`. Для ГМа это ловушка: поспавнил врага рядом с союзником —
он стал его.
☐ H.5 Хук стартовой армии: сколько юнитов какого типа давать на старте. Сейчас `startRTS`
создаёт только воркера и скаута; спавн пачки уже умеет `UnitServerEvents.spawnMobs`
(`unit/UnitServerEvents.java:891-927`).
☐ H.6 Класс генератора ресурсов. **Не реализуется в этом плане** — владелец сделает его при
написании своей фракции. Форма, которую надо заложить:
`getTickInterval()`, `getResourceAmount()`, `getResourceType()`, необязательный `getCapacity()`,
чтобы генератор не давал бесконечно за тик. Описание расширения — в `HOWTO_FACTION.md`.

---

## 11. Порядок и гейты

| Этап | Что | Зависит от | Гейт |
|---|---|---|---|
| **A** | Поломки | — | 1–4 + ручная проверка |
| **B** | Интрузивность | — | 1–4 + новый мир, `diff` папки |
| **C** | Режимы | B.4 | 1–4 + `runClient` |
| **D** | Удаление контента | C | 1–4; **в игре не проверяемо** |
| **E** | Дыры каркаса | D | 1–4 |
| **F** | `Faction` | D | 1–4 + решение по миграции миров |
| **G** | Камера | независим | визуальная проверка |
| **H** | РТС для обычной игры | контент владельца | ручная проверка |

Гейты на каждом этапе: `compileJava`, `validateMixins`, `runData`, `runServer`, `runClient`
до титольного экрана.

**Этапы D–F делать подряд, одним прицеванием.** Между ними мод нерабочий, и держать
промежуточные коммиты с «почти пустым» содержимым незачем.

## 12. Что потребует отдельного решения владельца

Закрыто на 2026-10-05:

* **Стена границы мира рисуется** (B.4.3) — миксин удаляется, остаётся ванильное поведение.
* **`doPlayerGriefing` и `doUnitGriefing` — как в ванилле** (B.5.4, B.5.6). Для
  `doPlayerGriefing` это дефолт `false`. У `doUnitGriefing` ванильного аналога нет, поэтому
  «как в ванилле» означает отказ от срезания урона взрывов по блокам; геймрул удаляется, а
  перехват ограничивается уроном по зданиям.
* **Миграция старых миров не нужна** (F.6) — старые теги игнорируются, вызовов
  `Faction.valueOf()` на старом NBT остаться не должно.
* **Чары и способности — в документацию и удаляются** (D.12, E.1), вместе с читателями в
  каркасе.
* **Туман войны удаляется целиком** (B.3), вместе с обёртками блоков и моделей.
* **Шаблон сохраняется:** `VillagerUnit`, `TownCentre`, `Barracks`, `VindicatorUnit` (решение 7).
* **Палитра спецблоков сохраняется** (D.20).
* **Инвентарь юнита сохраняется** (D.15) — миксин остаётся, NBT пишется только при
  непустом инвентаре (B.6.10).

Осталось открытым:

1. Побочные эффекты размещения здания (B.8) — какие из них остаются как механика здания, а
   какие удаляются. и требуют ли здания ровной земли и лесов.
2. `ResourceCost` — оставить плоскую структуру стоимости или убрать стоимости совсем (D.14, E.4).
3. Пять из 28 эффектов, использовавшихся только удаляемым контентом (E.3).
4. Шесть атрибутов без потребителей — реализовать вызов или удалить (E.2).
5. Вход и выход из РТС-режима (H.1, H.2) — командой ли, и что считается концом сессии.
6. Чинить ли скрытую связность `EntityRegistrar.getEntityType` (E.9).
