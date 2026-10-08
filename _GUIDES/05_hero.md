# Герои: уровни, мана, прокачка

> **Требует ревизии (2026-10-08).** Контент героев вырезан; интерфейс `HeroUnit` сохранён как
> контракт. Гайд описывает прежнюю систему (герои-юниты, опыт, воскрешение) и требует сверки с
> текущим `unit/interfaces/HeroUnit.java`.

## Что даёт `HeroUnit`

Интерфейс `unit/interfaces/HeroUnit.java`. Реализуется **вместе** с обычными интерфейсами
юнита:

```java
public class XHeroUnit extends Vindicator
        implements Unit, AttackerUnit, HeroUnit, KeyframeAnimated {
```

Ключевое:
* **уровни 1..10** (`HeroUnit.MAX_LEVEL = 10`), опыт по кривой: первый уровень — 320,
  далее +160 за уровень;
* **очки навыков** — +1 за уровень;
* **мана** — пул и реген, растёт с уровнем;
* **ранги способностей** — тратят очки навыков, требуют
  `heroLevel >= rank * 2 + 1` (то есть ранг 1 доступен с 3 уровня, ранг 2 — с 5);
* **воскрешение** — состояние сохраняется в `HeroUnitSave` при смерти.

## Обязательные элементы

```java
@Override protected void defineSynchedData() { ... }     // плюс свои поля

public static AttributeSupplier.Builder createAttributes() {
    return HeroUnit.createDefaultAttributes()             // добавляет ману и HP за уровень
            .add(Attributes.MAX_HEALTH, 125)
            .add(Attributes.ATTACK_DAMAGE, 6)
            .add(Attributes.MOVEMENT_SPEED, 0.28);
}

private final Object2ObjectArrayMap<HeroAbility, Integer> ranks = new Object2ObjectArrayMap<>();
@Override public Object2ObjectArrayMap<HeroAbility, Integer> getHeroAbilityRanks() { return ranks; }

@Override public void tick() {
    super.tick();
    Unit.tick(this);
    AttackerUnit.tick(this);
    HeroUnit.tick(this);          // реген маны, кулдауны способностей
}
```

Также нужно: `needsStatSync`/`setNeedsStatSync`, `getMana`/`setMana`,
`getMaxMana`/`setMaxMana`, `getSkillPoints`/`setSkillPoints`,
`isRankUpMenuOpen`/`showRankUpMenu`, `getExperience`/`setExperience`.

`HeroUnit.setStatsForLevel()` пишет бонусные атрибуты в **ванильные**
`MAX_HEALTH`/`ATTACK_DAMAGE`, поэтому переопределять это вручную не нужно.

## Геройские способности

`ability/HeroAbility.java` — наследник `Ability`. Отличия: `maxRank`, `manaCost`,
`rankUp(hero)`, `updateStatsForRank(hero)`.

```java
public class XHeroAbility extends HeroAbility {
    public static final int MAX_RANK = 3;

    public XHeroAbility() {
        super(UnitAction.CAST_X_HERO, UnitAction.RANK_X_HERO,
              30 * ResourceCost.TICKS_PER_SECOND, RANGE, RADIUS, true, true,
              manaCostFor(1), MAX_RANK);
    }
    @Override protected int manaCostFor(int rank) { return 50; }
    @Override public int getCooldownSeconds(int rank) { return 30; }
}
```

Константа `UnitAction` на ранг нужна для кнопки прокачки (`heroLevel >= rank * 2 + 1`).

## Производство героя

Герой получается через производство — `HeroProductionItem` плюс отдельный
`ReviveHeroProductionItem`. Оба требуют здания-производителя.

⚠ **После удаления контента (`*Prod` уходят в документацию) эта связь рвётся.**
`HeroUnit.getHeroesInTraining()` перечисляет `ProductionPlacement.productionQueue`,
а `ReviveHeroProductionItem.onComplete` вызывает `placement.produceUnit(...)`. Если владелец
захочет получать героев иначе (например, только орторежимом без зданий), выноси создание
в отдельный путь — так же, как это делается для обычных юнитов.

Лимит одновременных героев задаётся геймрулом `allowedHeroes` (по умолчанию 2).
Проверяется в `HeroProductionItem`.

## Анимации

`KeyframeAnimated` + класс анимаций в `unit/modelling/animations/` (паттерн —
`WildfireAnimations`). Регистрируется в `ClientModEvents.registerLayerDefinitions`.
Слои состояний задаются в рендерере.

## Ассеты

* Модель и текстуры — `unit/modelling/models/`, `textures/entities/`.
* Собственный рендерер в `unit/modelling/renderers/`, если ванильный не подходит.
* Частица подъёма уровня — `level_up` в `registrars/ParticleRegistrar`.
* Ключи: `entity.reignofnether.x_hero`, `unitstats.reignofnether.*`,
  `abilities.reignofnether.x_hero.*`, `hud.unitinfo.*`.

## Проверка в игре

* Опыт капает с убийств, уровень растёт, цифры на портрете обновляются.
* Мена регенерируется, способность не срабатывает без маны.
* Ранг 1 открывается на 3 уровне, попытка ранговать раньше не тратит очко.
* Смерть сохраняет опыт и ранги; воскрешение их восстанавливает.
