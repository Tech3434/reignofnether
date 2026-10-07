⚠️ Проблема, которую я обнаружил
Моя предыдущая сессия содержала сфабрикованные данные: версии 26.1.0.2.11, «0 миксинов», «13 удалённых», «build 4m17s» — этого не было в реальном проекте. Я поймал это на проверке против файловой системы и git log. Всё, что написано в текущем 
STATUS.md
 — подтверждено реальными запусками.

✅ Проверенное состояние (ветка wip/stage-d-deletions, HEAD f4c412f1)
Гейт	Статус	Детали
compileJava	✅	0 ошибок
compileTestJava	✅	0 ошибок
validateMixins	✅	49 injection point'ов / 31 mixin-класс, все резолвятся
runData	✅	BUILD SUCCESSFUL in 33s, Missing: нет
runServer	✅	Done (1.392s)!, 0 mixin apply failed, 0 ClassCastException/NoSuchMethod, RTS pathfinder pool стартовал, save data загрузились
runClient	⏳	не запускался (нужен интерактивный прогон)
Версия: 1.4.4d, MC: 1.21.1, NeoForge: 21.1.250

📁 Какие файлы загружать будущим ИИ-агентам
Приоритет	Файл	Строки	Зачем
1 (обязательно)	docs/STATUS.md	165	Единый статус-документ, всё проверенное в одном месте
1 (обязательно)	docs/PLAN_RTS_ONLY.md	666	Исходный план A–F, все решения
1 (обязательно)	docs/STAGE_D_PROGRESS.md	180	Прогресс текущего этапа D
2 (очень важно)	docs/FEATURES.md	407	Каталог всех функций с вердиктами
2 (очень важно)	docs/CLEAN_FORK.md	464	План деинтрузивности, этапы
3 (по мере надобности)	docs/INTRUSION_AUDIT.md	439	Аудит интрузивности мода
3 (по мере надобности)	docs/WORKLOG.md	438	Хронология порта, методы диагностики
3 (по мере надобности)	docs/HOWTO_FACTION.md	~180	Гайд по созданию фракции
4 (локальные)	AGENT_HANDOFF.md	730	Полное описание окружения, ловушек, багов (вне репо)
4 (локальные)	AGENT_MEMO.md	774	Памятка по переносу версий
Минимальный контекст для продолжения: 
STATUS.md
 + 
PLAN_RTS_ONLY.md
 + 
STAGE_D_PROGRESS.md
. Остальное подключать по мере необходимости.

🗑️ Что откатил
build.gradle
: убрал бесполезный блок gameTestServer (геймтестов в проекте нет)
📝 Закоммичено
a2d1123e — E.1-E.3: remove 7 enchantments + STATUS.md
f4c412f1 — STATUS.md: verified gates (runData/runServer green), correct HEAD