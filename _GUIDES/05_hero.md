# Герои

> Переписано 2026-10-08 под текущий каркас.

**Герой — это юнит, который прокачивается: у него есть уровень.** Некоторые способности могут
требовать определённый уровень героя (и ману), а не только кулдаун. Это в дополнение к обычным
требованиям `Ability` (кулдаун, мана, исследование — см. `07_research.md`).

## Контракт — `unit/interfaces/HeroUnit`

Реализуется **вместе** с обычными интерфейсами юнита:

```java
public class XHeroUnit extends Vindicator implements Unit, AttackerUnit, HeroUnit, KeyframeAnimated {
```

## Уровни и опыт

* `HeroUnit.MAX_LEVEL = 10`; `MAX_NEUTRAL_EXP_LEVEL = 5` (с этого уровня нейтралы опыт не дают).
* Опыт хранится суммарным; уровень считается по кривой `HeroUnit.getHeroLevel(exp)`
  (первый порог `200 * 1.6`, далее `+100 * 1.6`).
* `addExperience(amount)` → **+1 очко навыка за уровень**, звук/частицы, `setStatsForLevel()`, хил.

## Характеристики и мана

* База и прирост за уровень — через атрибуты: `AttributeRegistrar.BASE_MAX_HEALTH`, `BASE_MAX_MANA`,
  `MANA_REGEN_PER_SECOND`, `MAX_HEALTH_BONUS_PER_LEVEL`, `MAX_MANA_BONUS_PER_LEVEL`,
  `ATTACK_DAMAGE_BONUS_PER_LEVEL`. Точка входа — `HeroUnit.createDefaultAttributes()`.
* `setStatsForLevel()` пересчитывает HP/урон/макс. ману под текущий уровень.
* Мана: `getMana`/`setMana`/`getMaxMana`; реген раз в секунду в `HeroUnit.tick`.

## Способности героя

* `HeroAbility` — подвид `Ability`. Ранги: `getHeroAbilityRank` / `setHeroAbilityRank`
  (хранятся в `getHeroAbilityRanks()`), усиливаются `updateStatsForRank(this)`.
* Очки навыка тратятся на ранги; **ранг способности — это и есть требование по уровню героя**
  (вместе с маной при применении).
* UI рангов — `isRankUpMenuOpen()` / `showRankUpMenu(...)`.

## Сохранение

`addHeroUnitSaveData(CompoundTag)` / `readHeroUnitSaveData(CompoundTag)`: опыт, очки навыка, мана,
ранги способностей, произвольные `charges`.

## Примечание

Готовых героев в каркасе нет — `HeroUnit` это контракт, под который вы делаете своих героев.
Требования способности по уровню героя реализуются в вашем гейте применения (по аналогии с
маной/кулдауном/исследованием).
