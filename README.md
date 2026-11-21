# RuTube для PipePipe

Сторонний плагин сервиса для [PipePipe](https://github.com/InfinityLoop1308/PipePipe):
добавляет **RuTube** ([rutube.ru](https://rutube.ru)) в приложение — видео, прямые эфиры,
Shorts, каналы, плейлисты, поиск, подсказки, комментарии и «В трендах».

> **Статус: личная сборка для теста.** Плагин нигде не анонсирован и не отправлен в
> `awesome-pipepipe-plugins`, в основном репозитории PipePipe его нет. Репозиторий при этом
> публичный, так что не стоит считать его приватным: любой, у кого есть ссылка, увидит код и
> артефакт.

Текущая версия — **1.0.1** (`versionCode 2`), пакет `release/rutube-2.ppp`.

## Что нового в 1.0.1

* **Вкладка «В трендах» работает.** Обработчик kiosk-ссылок теперь принимает адрес сервиса
  (`https://rutube.ru/`), который приложение и передаёт при открытии вкладки. Раньше
  `KioskList` не находил подходящий kiosk и показывал «Что-то пошло не так».
* **Поисковые фильтры работают.** RuTube отвечает HTTP 400 на `created=all-time` и
  `duration=any`, а плагин подставлял эти значения по умолчанию в каждый запрос — поэтому
  любая выдача, кроме каналов, была пустой. Теперь «все/любые» не отправляют ничего, а
  остались только значения, которые API принимает: дата (`day/week/month/year`) и длительность
  (`short/medium/long/movie`).
* **Пропала сортировка и фильтр «Плейлисты».** Поисковый бэкенд RuTube игнорирует
  `order_by` (другие написания дают 400), а плейлисты в поиске не отдаёт вовсе — вместо
  неработающих пунктов они убраны. Плейлисты по-прежнему открываются по ссылке.
* **Прямые эфиры.** Play options запрашиваются с заголовками `Referer`/`Origin`, как у
  веб-плеера; ссылка на HLS ищется в `video_balancer`, `live_streams` и `hls` любой формы
  (строка, объект, массив); варианты без `RESOLUTION` больше не выбрасываются; если мастер-плейлист
  не разобрался (у телеканалов часто отдаётся обычный media-playlist), плееру отдаётся сам HLS-URL
  вместо ошибки «Could not get any stream».
* **Понятные ошибки.** Заглушки RuTube (`{"detail": {"type": "player_stub"}}`, `blocking_rule`)
  превращаются в текст вида «Трансляция завершена» или «RuTube заблокировал это видео» вместо
  молчаливого пустого потока.
* **Пагинация Shorts.** API отдаёт `next` со схемой `http://` на `search-backend.rutube.ru`,
  Android такие запросы не грузит — ссылки поднимаются до HTTPS.

## Установка

**Важно:** у 1.0.1 новый ключ подписи (см. ниже), поэтому 1.0.0 нужно сначала удалить.

1. PipePipe → Настройки → Плагины → удалить RuTube 1.0.0 (если стоял).
2. Скачать [`release/rutube-2.ppp`](release/rutube-2.ppp) на телефон.
3. Настройки → Плагины → установка из файла, выбрать `.ppp`.
4. Перезапустить приложение — сервис «RuTube» появится в списке сервисов.

Либо как репозиторий по URL (нужен `main`):

```
https://raw.githubusercontent.com/ivan23456-super/rutube-plugin/main/release/index.json
```

## Что умеет

| Возможность | Как реализовано |
|---|---|
| Воспроизведение | `GET /api/video/{id}/`, `GET /api/play/options/{id}/` → HLS-мастер-плейлист |
| Прямые эфиры | `live_streams` / `video_balancer` из play options |
| Субтитры | `captions` из play options (SRT/VTT) |
| Каналы | `GET /api/profile/user/{id}/` + `GET /api/video/person/{id}/` (видео и Shorts) |
| Слаги каналов | `/u/{slug}/` через состояние серверной страницы |
| Плейлисты и фильмы | `GET /api/playlist/custom/{id}/`, `GET /api/metainfo/tv/{id}/` |
| Поиск | видео — pangolin (`/pangolin/api/web/v1/search/combined/video_playlist`), каналы — `/api/search/combined/cards/list`, Shorts — `/api/search/shorts`; фильтры по дате и длительности |
| Подсказки | `GET /api/search/suggest` |
| Комментарии | `GET /api/v3/comments/video/{id}/` |
| «В трендах» | `GET /api/v2/video/recommendation/main` |
| Приватные видео | токен `p` из ссылки пробрасывается в API |

Логин не нужен: все эндпоинты публичные.

### Ограничения

* **Сортировки в поиске нет** — бэкенд RuTube её не поддерживает.
* **Поиска по плейлистам нет** — RuTube такие результаты не отдаёт.
* **Телеканалы гео-заблокированы**: вне России RuTube отвечает на play options заглушкой
  `blocking_rule`. Из РФ должно работать; если нет — текст ошибки теперь покажет, что именно
  вернул сервер.

## Сборка

Требования: JDK 25 (Gradle скачает тулчейн сам) и Android SDK с `build-tools;37.0.0` и
`platforms;android-36` — путь в `local.properties` (`sdk.dir=...`) или в `ANDROID_HOME`.

```bash
./gradlew :tools:genKey      # один раз: создаёт signing.key (секрет, в git не кладём)
./gradlew assembleRepo       # jar -> d8 -> build/repo/rutube-<versionCode>.ppp
./gradlew :tools:index       # build/repo/index.json с sha256 + подписью Ed25519
```

Релиз: положить `.ppp` в `release/`, прописать в `release/index.json` абсолютный URL в
`package`, обновить `sha256`/`signature`/`publicKey`/версию. `versionCode` в
`src/main/resources/plugin.json` увеличивать при каждом релизе.

CI (`.github/workflows/build.yml`) делает это сам, если в репозитории заведён секрет
`PLUGIN_SIGNING_KEY` с приватным ключом: собирает, подписывает и коммитит `release/`.
Без секрета сборка идёт одноразовым ключом только для проверки.

## Ключ подписи

Подпись Ed25519, публичный ключ прикрепляется при первой установке (pinning):

```
publicKey: DLCC7suGGj0xX5WwB/zTTjCFs+3JJV8jWaBWGKb52es=
```

Приватный ключ (`rutube-signing.key`, base64 raw Ed25519, 44 символа) **в репозитории не
хранится**. Он выдан вам в чате при выпуске 1.0.1 — сохраните его (менеджер паролей,
`~/.config/...`, что угодно) и добавьте в GitHub: Settings → Secrets and variables →
Actions → New repository secret → имя `PLUGIN_SIGNING_KEY`.

Обновления принимаются только с этим ключом и этим `pluginId`: потеряли ключ — плагин
придётся перевыпускать под новым `pluginId` и переустанавливать вручную.

## Структура

```
src/main/java/org/pipepipe/rutube/   сервис, хелперы, экстракторы, link-хендлеры, фильтры
src/main/resources/plugin.json       манифест (name = имя сервиса)
src/main/resources/icon.png          иконка 512×512
tools/                               genKey + index (BouncyCastle, nanojson)
release/                             подписанный .ppp и index.json
docs/pipepipe-plugins-guide.md       конспект по устройству плагинов PipePipe
```

## Лицензия

GPL-3.0, как и PipePipe. См. [LICENSE](LICENSE).
