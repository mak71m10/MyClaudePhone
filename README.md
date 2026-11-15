# Редактор кабельных схем

Один файл `index.html` — без сборки и зависимостей. Откройте его в браузере или выложите на любой статический хостинг (GitHub Pages, Netlify, Cloudflare Pages и т. п.).

Исходная схема: AUSTAL H392 70M — ECDIS & Echo Sounder, Equipment Cable Layout.

Возможности: перетаскивание блоков, добавление/удаление блоков, порты и кабели, зоны, текст, штамп чертежа, undo/redo, автосохранение в браузере, экспорт JSON / SVG / PNG, ссылка со всей схемой внутри.

---

# Гусь-Игрок — интерактивный лист персонажа (папка `goose-gambler/`)

Мобильное приложение (PWA) для D&D 5e: раса Goose/Waterfowl (PharesBueller, World Anvil) + класс Gambler, подкласс Dice Roller (phbz.org), 11 уровень.
Открыть: `goose-gambler/index.html` через GitHub Pages → в браузере телефона «Добавить на главный экран». Работает офлайн, всё сохраняется на устройстве.

## APK для Android
Готовый файл: `goose-gambler/Gus-Igrok.apk`. Исходники обёртки: `android/` (WebView + мост для вибрации, сохранения копии в «Загрузки» и режима «не гасить экран»).
Сборка: `cd android && ANDROID_HOME=<sdk> gradle assembleRelease`. Веб-часть берётся из `goose-gambler/index.html` автоматически.
Ключ подписи `android/goose-release.jks` лежит в репозитории специально, чтобы новые версии ставились поверх старой без потери данных.
