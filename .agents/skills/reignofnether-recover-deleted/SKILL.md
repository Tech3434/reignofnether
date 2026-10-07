---
name: reignofnether-recover-deleted
description: Как найти и восстановить код/ассеты, удалённые при вырезании фракций в Reign of Nether. Использовать, когда нужно вернуть удалённый класс, юнит, здание или ресурс как основу для нового контента.
---

# Восстановление удалённого контента

На этапе D (ветка `wip/stage-d-deletions`) из мода вырезаны все фракции: ~385 файлов. Полное
рабочее дерево **до** удаления — коммит **`5079004e`**. Он же источник инвентаря в
`docs/reference/`.

## Найти

```bash
cd ___temp
# что было в пакете
git ls-tree -r --name-only 5079004e -- src/main/java/com/solegendary/reignofnether/unit/units
# поиск по имени по всему старому дереву
git ls-tree -r --name-only 5079004e | grep -i <подстрока>
```

## Восстановить один файл

```bash
git show 5079004e:src/main/java/com/solegendary/reignofnether/<путь> > <новый_путь>
```
Под PowerShell 5.1 перенаправление пишет UTF-16LE → используй `cmd /c "git show ... > file"`.

## Что где искать

| Нужно | Путь в `5079004e` | Инвентарь |
|---|---|---|
| юнит | `unit/units/<Name>Unit.java` + `<Name>Prod.java` | `docs/reference/units.md` |
| здание | `building/buildings/**` + `placements/**` | `docs/reference/buildings.md` |
| способность | `ability/abilities/**`, `ability/heroAbilities/**` | `docs/reference/abilities.md` |
| исследование | `research/researchItems/**` | `docs/reference/research.md` |
| экономика | `resources/**` | `docs/reference/economy.md` |
| волны | `survival/**` | `docs/reference/waves.md` |
| предметы | `items/**` | `docs/reference/items.md` |
| ассеты | `src/main/resources/assets/reignofnether/**` | `docs/reference/assets.md` |

## Важно

* Восстановленный класс почти наверняка не скомпилируется сам: интерфейсы (`Unit`, `AttackerUnit`)
  и цели (`unit/goals/**`) сильно изменились после этапа D. Переноси смысл, а не текст.
* **Не** восстанавливай `Faction` как `FactionDefinition` — это заведомо отвергнутое решение
  (см. `docs/reference/faction.md`).
* Прежде чем удалять что-то из каркаса, проверь, нет ли на это ссылки в удалённом контенте —
  иначе потеряешь рабочий пример.
