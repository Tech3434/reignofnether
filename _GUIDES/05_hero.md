# Герои

> Переписано под текущий каркас: юниты data-driven, интерфейс `HeroUnit` **удалён** — герой это
> `Unit` с ролью `hero` и с геройским состоянием на `UnitMobMixin`.

**Герой — это юнит (`role: "hero"`), который прокачивается: у него есть уровень.** Часть способностей
может требовать уровень героя и ману, а не только кулдаун (в дополнение к обычным требованиям `Ability`:
кулдаун/мана/исследование — см. `07_research.md`).

## Определение героя (JSON)

Тот же `UnitDefinition`, что и у обычного юнита, но `"role": "hero"`; при спавне
`UnitDefinitionRuntime` применяет характеристики уровня 1, далее `Unit.tick` вызывает `tickHero`
(мана/опыт/ранги).

```json
{ "base": "minecraft:vindicator", "role": "hero",
  "attributes": {
    "reignofnether:base_max_health": 100,
    "reignofnether:attack_damage": 8,
    "reignofnether:base_max_mana": 100,
    "reignofnether:mana_regen_per_second": 2,
    "reignofnether:max_health_bonus_per_level": 10,
    "reignofnether:attack_damage_bonus_per_level": 1,
    "reignofnether:max_mana_bonus_per_level": 5
  },
  "abilities": [ { "type": "myns:x", "cooldown": 100 } ] }
```

⚠ У героя `setStatsForLevel` **перезаписывает** `minecraft:generic.max_health`/`attack_damage` из
`reignofnether:base_max_health`/`attack_damage`, так что задавай именно модовые hero-атрибуты
(`base_max_health`, `attack_damage`, `base_max_mana`, `mana_regen_per_second`, `*_bonus_per_level`).

## Состояние героя (на `Unit` / `UnitMobMixin`)

* `Unit.isHero()` — роль `hero`.
* `Unit.MAX_LEVEL = 10`.
* Опыт/уровень: `getHeroLevel()`, `getExperience`/`addExperience`, `getExpOnCurrentLevel`/`getExpToNextlevel`.
* Мана: `getMana`/`setMana`/`getMaxMana`; реген раз в секунду в `tickHero`.
* Очки навыка/ранги: `getSkillPoints`, `getHeroAbilityRanks`, `isRankUpMenuOpen()`, меню прокачки.
* `setStatsForLevel(boolean)` пересчитывает HP/урон/макс. ману/реген под уровень.
* Синхронизация: `needsStatSync`.

Состояние хранится в `@Unique`-полях `UnitMobMixin` и переживает сейв (`addHeroUnitSaveData`/
`readHeroUnitSaveData`): опыт, очки навыка, мана, ранги, заряды.

## Способности героя

* `HeroAbility` — подвид `Ability`. Ранги: `getHeroAbilityRank`/`setHeroAbilityRank`
  (`getHeroAbilityRanks()`), усиливаются `updateStatsForRank(Unit)`.
* Очки навыка тратятся на ранги; ранг способности — это и есть требование по уровню героя.
* UI рангов — `isRankUpMenuOpen()` / `showRankUpMenu(...)`; полоса маны — `BarState.MANA`.

## Примечание

Готовых героев в каркасе нет — заводите своего юнита с `role: "hero"`. Требование способности по
уровню героя реализуется в вашем гейте применения (по аналогии с маной/кулдауном/исследованием).
