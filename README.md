# RuTube для PipePipe

Сторонний плагин сервиса для [PipePipe](https://github.com/InfinityLoop1308/PipePipe):
добавляет **RuTube** ([rutube.ru](https://rutube.ru)) в приложение — видео, прямые эфиры,
Shorts, каналы, плейлисты, поиск, подсказки, комментарии и «в трендах».

> **Статус: личная сборка для теста.** Плагин нигде не анонсирован и не отправлен в
> `awesome-pipepipe-plugins`, в основном репозитории PipePipe его нет. Репозиторий при этом
> публичный, так что не стоит считать его приватным: любой, кто знает ссылку, увидит код и
> артефакт.

## Что умеет

| Возможность | Как реализовано |
|---|---|
| Воспроизведение | `GET /api/video/{id}/` для метаданных, `GET /api/play/options/{id}/` для HLS-мастер-плейлиста, эфиров и субтитров |
| Прямые эфиры | `live_streams.hls` из play options |
| Субтитры | `captions` из play options (SRT/VTT) |
| Каналы | `GET /api/profile/user/{id}/` + `GET /api/video/person/{id}/` (вкладки «Видео» и «Shorts») |
| Слаги каналов | `/u/{slug}/` разбираются через состояние серверной страницы |
| Плейлисты и фильмы | `GET /api/playlist/custom/{id}/`, `GET /api/metainfo/tv/{id}/` |
| Поиск | видео/плейлисты через pangolin-бэкенд, каналы через combined cards, Shorts через shorts-эндпоинт; фильтры по дате, длительности и сортировке |
| Подсказки | `GET /api/search/suggest` |
| Комментарии | `GET /api/v3/comments/video/{id}/` с курсорной пагинацией |
| «В трендах» (kiosk) | `GET /api/v2/video/recommendation/main` |
| Приватные видео | токен `p` из ссылки пробрасывается в API |
| Старые embed-ссылки | числовые `/video/embed/{trackId}` резолвятся через `effective_video` |

Логин не нужен: все перечисленные эндпоинты публичные.

## Установка для личного теста

В репозитории лежит готовый подписанный пакет `release/rutube-1.ppp`
(pluginId `org.pipepipe.rutube`, версия 1.0.0, API 1.0, требуется PipePipe 5.6.0+).

**Вариант А — из файла (работает сразу, без мержа ветки).**

1. Скачайте [`release/rutube-1.ppp`](release/rutube-1.ppp) на телефон (страница файла →
   «Download raw file»), например в `Download`.
2. PipePipe → Настройки → Плагины → установка из файла (в разных сборках пункт может
   называться «Добавить репозиторий» с выбором локального файла). Выберите скачанный `.ppp`.
3. Перезапустите приложение — в списке сервисов появится «RuTube».

**Вариант Б — как репозиторий по URL** (нужен публичный HTTPS, то есть ветка должна быть
в `main`, иначе ссылка отдаст 404):

```
https://raw.githubusercontent.com/ivan23456-super/rutube-plugin/main/release/index.json
```

Добавьте этот адрес в Настройки → Плагины → Добавить репозиторий. Поле `package` в
`release/index.json` уже указывает на `release/rutube-1.ppp` в `main`.

## Сборка

Требования: JDK 25 (Gradle скачает тулчейн сам) и Android SDK с `build-tools;37.0.0` и
`platforms;android-36` — путь к SDK в `local.properties` (`sdk.dir=...`) или в `ANDROID_HOME`.

```bash
./gradlew :tools:genKey      # один раз: создаёт signing.key (секрет, в git не кладём)
./gradlew assembleRepo       # jar -> d8 -> build/repo/rutube-<versionCode>.ppp
./gradlew :tools:index       # build/repo/index.json с sha256 + подписью Ed25519
```

Для релиза: положите `.ppp` в `release/`, пропишите в `release/index.json` абсолютный URL в
`package`, обновите `sha256`/`signature`/`versionCode`/`versionName` и увеличьте `versionCode`
в `src/main/resources/plugin.json`.

## Ключ подписи

Подпись Ed25519, публичный ключ прикрепляется при первой установке (pinning):

```
publicKey: uhLHW2c0F7CKho5VNJwJFlLuN6cs1UKcPPN3SeaL738=
```

Приватный ключ (`rutube-signing.key`, base64 raw Ed25519) **в репозитории не хранится** —
он есть только локально. Обновления плагина должны быть подписаны этим же ключом, иначе
PipePipe отклонит пакет; при потере ключа плагин придётся перевыпускать под новым `pluginId`.
`pluginId` после первой установки тоже менять нельзя — от него зависит `serviceId`.

Если понадобится подписывать релизы в CI, положите ключ в GitHub Actions secret и передавайте
его задачам через `-PpluginSigningKey=...` (как это делает `.github/workflows/ci.yml`, только
с настоящим ключом вместо одноразового).

## Структура

```
src/main/java/org/pipepipe/rutube/   сервис, хелперы, экстракторы, link-хендлеры, фильтры
src/main/resources/plugin.json       манифест плагина (name = имя сервиса)
src/main/resources/icon.png          иконка 512×512
tools/                               genKey + index (BouncyCastle, nanojson)
release/                             готовый .ppp и подписанный index.json
docs/pipepipe-plugins-guide.md       конспект по устройству плагинов PipePipe
```

Текущий `release/rutube-1.ppp` содержит `classes.dex`, `plugin.json` и `icon.png` — этого
достаточно для Android-хоста. Пакет, собранный локально через `assembleRepo`, дополнительно
содержит `.class`-файлы для JVM-хостов.

## Лицензия

GPL-3.0, как и PipePipe. См. [LICENSE](LICENSE).
