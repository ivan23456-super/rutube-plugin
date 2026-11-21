# Плагины PipePipe: документация и как написать свой

Конспект по состоянию на 21.11.2026. Плагины появились в PipePipe **5.6.0** (beta с 27.10.2026):
сервисы (SoundCloud, PeerTube, media.ccc.de, Bandcamp) вынесены из приложения в отдельные
плагины, а сторонние авторы могут добавлять свои (Twitch — первый такой).

---

## 1. Где вообще документация

Отдельного сайта-мануала нет. Вся спецификация живёт в четырёх местах, и вместе они
покрывают практически всё:

| Что нужно | Где |
| --- | --- |
| **Справочник по `plugin.json`, сборка, подпись, публикация** — де-факто главный документ | [InfinityLoop1308/PipePipePluginTemplate](https://github.com/InfinityLoop1308/PipePipePluginTemplate) → README |
| **API-контракт** (javadoc прямо в коде) | `PipePipeExtractor/extractor/src/main/java/org/schabi/newpipe/extractor/plugin/` — `PipePipePlugin`, `PluginContext`, `PluginPreferences`, `PluginManifest`, `PluginLoader`, `PluginPackage`, `PluginLogin` |
| **Формат репозитория плагинов (`index.json`) и правила приёма** | [InfinityLoop1308/awesome-pipepipe-plugins](https://github.com/InfinityLoop1308/awesome-pipepipe-plugins) |
| **Живые примеры кода** | [PipePipe-builtin-plugins](https://github.com/InfinityLoop1308/PipePipe-builtin-plugins) (бывшие встроенные сервисы) и [TwitchPipePipePlugin](https://github.com/MohrJonas/TwitchPipePipePlugin) |

Плюс обсуждение #3080 и issue #3081 в репозитории PipePipe — там же задаются вопросы.

Ключевая мысль: **плагин — это не «модуль UI», а один класс-наследник `StreamingService`**
из NewPipeExtractor. Всё, что умеет NewPipe (поиск, канал, плейлист, потоки, комментарии,
kiosk, подсказки), расширяется через уже существующий интерфейс экстрактора. Новый API — это
только «как загрузить чужой код и отдать ему id сервиса».

---

## 2. Как это устроено внутри

```
.ppp (zip = одновременно jar + classes.dex + plugin.json в корне)
   │  sha256 + подпись Ed25519
   ▼
PluginPackage.verify()          ← проверка хэша и подписи, pinning ключа при 1-й установке
   ▼
PluginLoader.createJvmClassLoader(File)   /  DexClassLoader на Android
   ▼
Class.forName(manifest.entryClass).newInstance()   →  PipePipePlugin
   ▼
plugin.createService(serviceId, pluginContext)     →  StreamingService
   ▼
ServiceList.register(service)   →  сервис появляется в боковом меню выбора сервисов
```

Важные правила:

* **`serviceId` назначает хост** (id 1–4 зарезервированы за бывшими встроенными сервисами,
  новые плагины начинают с 1000). Он выводится из `pluginId`, поэтому одинаков на всех
  устройствах. Передавать его в конструктор `StreamingService` нужно **без изменений** —
  иначе `PluginLoader` бросит `PluginException`.
* **`name` в `plugin.json` должен совпадать** с `getServiceInfo().getName()`.
* **`apiVersion`** — `major.minor`. Хост грузит плагины с тем же major и minor не выше своего
  (extractor сейчас объявляет API 1.1). Плагин, собранный под 1.0, работает на 1.1.
* Класс-загрузчик плагина — дочерний к загрузчику экстрактора, поэтому классы хоста
  (extractor, jsoup, nanojson) видны, а свои — грузятся из пакета.

---

## 3. Что реально писать

Минимум — два класса (шаблон уже лежит в `~/pipepipe-plugin-template`):

```java
// 1. Точка входа, её имя указано в plugin.json → entryClass
public final class MyServicePlugin implements PipePipePlugin {
    @Override
    public StreamingService createService(int serviceId, PluginContext context) {
        return new MyService(serviceId, context.getPreferences());
    }
}
```

```java
// 2. Сам сервис
public class MyService extends StreamingService {
    public MyService(int id, PluginPreferences prefs) {
        super(id, "MyService", singletonList(VIDEO));  // имя = name в plugin.json
    }
    // ... фабрики и экстракторы
}
```

Дальше — по возможностям. Что не поддерживается, возвращайте `null` (или бросайте
`ExtractionException`), приложение просто спрячет соответствующие вкладки:

| Возможность / экран в приложении | Что реализовать |
| --- | --- |
| Разбор ссылки на видео | `getStreamLHFactory()` → `LinkHandlerFactory` |
| Экран видео, потоки, метаданные | `getStreamExtractor(LinkHandler)` → `StreamExtractor` |
| Поиск | `getSearchQHFactory()` + `getSearchExtractor(...)` → `SearchQueryHandlerFactory`, `SearchExtractor` |
| Канал + его вкладки | `getChannelLHFactory()`, `getChannelExtractor(...)`, `getChannelTabLHFactory()`, `getChannelTabExtractor(...)` |
| Плейлист | `getPlaylistLHFactory()`, `getPlaylistExtractor(...)` |
| Комментарии | `getCommentsLHFactory()`, `getCommentsExtractor(...)` |
| «Главная»/тренды (kiosk) | `getKioskList()` |
| Подсказки в поиске | `getSuggestionExtractor()` |
| Импорт подписок | `getSubscriptionExtractor()` |
| Срок жизни кэша | `getInfoCacheExpirationMillis()` (новое в API 1.1) |

Сами экстракторы пишутся как в NewPipe: наследуете `StreamExtractor` / `ChannelExtractor` /
`SearchExtractor`, реализуете `onFetchPage(Downloader)`, `getId()`, `getName()`, геттеры, а
списки собираете через `InfoItemsCollector` + свой `InfoItemExtractor`. Если вы уже писали
сервис для NewPipeExtractor — код переносится почти один-в-один, разница только в фабрике
и в манифесте.

### Сеть и библиотеки

* HTTP — **только** через `NewPipe.getDownloader()` (тот же загрузчик, что у хоста: прокси,
  cookies, настройки пользователя). Никаких okhttp/retrofit.
* Разрешено использовать классы, которые предоставляет хост: **extractor API, nanojson,
  jsoup, commons-lang3, spotbugs-annotations**. В сборке они `compileOnly`.
* Всё остальное нужно либо написать самому, либо бандлить с осторожностью — конфликт классов
  с хостом уронит приложение (все плагины живут в одном процессе).
* JSON — `nanojson`, HTML — `jsoup`. Этого хватает: Twitch-плагин, например, на этом и живёт.

---

## 4. `plugin.json`

```json
{
  "pluginId": "com.example.myservice",
  "name": "MyService",
  "versionCode": 1,
  "versionName": "1.0.0",
  "apiVersion": "1.0",
  "author": "Your Name",
  "entryClass": "com.example.myservice.MyServicePlugin",
  "issueUrl": "https://github.com/you/myservice/issues",
  "description": "Что даёт этот сервис",
  "icon": "icon.png",
  "settings": [
    { "key": "base_url", "type": "string", "title": "Base URL",
      "summary": "Переопределить сайт", "default": "https://example.com" }
  ]
}
```

Опциональные поля, которые часто нужны:

* `settings` — `string`/`boolean`; UI рисует приложение, значения читаются через
  `context.getPreferences().getString("base_url")`, `addChangeListener` сообщает об изменениях.
* `login` — cookie-логин: `{"url": "https://...", "cookie": "<имя cookie>", "key": "<ключ>"}`.
  Приложение показывает кнопки Login / Log out, открывает web view и складывает cookie-заголовок
  в настройки. Для сервисов вроде Twitch это и есть авторизация.
* `theme` — `{"light": "#F57C00", "dark": "#A35300"}`, применяется как overlay к темам приложения.
* `issueUrl` — при падении в коде плагина пользователя отправляют к вам, а не в трекер PipePipe.
* `pluginId` после публикации **не менять** (от него зависит `serviceId`), `versionCode`
  увеличивать при каждом релизе.

---

## 5. Сборка, подпись, публикация

Требования шаблона: JDK 25, Android SDK с `build-tools;37.0.0` и `platforms;android-36`.

```bash
./gradlew :tools:genKey     # один раз → signing.key (секрет, в git не кладём)
./gradlew assembleRepo      # jar → d8 → build/repo/<name>-<versionCode>.ppp
./gradlew :tools:index      # build/repo/index.json с sha256 + подписью Ed25519
```

Тонкости:

* `.ppp` — zip, который одновременно является jar (для JVM-хостов) и содержит `classes.dex`
  (для Android). DEX собирается `d8` с тем же desugaring, что у хоста (`desugar_jdk_libs`
  2.1.5, `pluginMinSdk=24` — ниже хостового нельзя, иначе краш).
* Подпись **Ed25519**, публичный ключ прикрепляется при первой установке (**pinning**):
  обновление, подписанное другим ключом, будет отвергнуто. Потеряли `signing.key` — плагин
  придётся перевыпускать под новым `pluginId`.
* Библиотека экстрактора подтягивается через JitPack (`extractorVersion=main-SNAPSHOT`).
  Хотите собирать против локального чекаута — `extractorDir=../PipePipeExtractor` в
  `local.properties`.
* CI в шаблоне подписывает всё одноразовым ключом — только для проверки сборки.

**Публикация.** Кладёте `.ppp` на стабильный HTTPS (проще всего GitHub Release asset),
берёте свою запись из `build/repo/index.json`, подставляете абсолютный URL в `package`,
и делаете PR в `awesome-pipepipe-plugins` (запись + строка в таблице + ссылка на исходники).
Пользователь добавляет URL `index.json` как репозиторий в Настройки → Плагины.

Свой репозиторий — это буквально один `index.json` на любом статичном хостинге:

```json
{
  "formatVersion": 1,
  "name": "My PipePipe Plugins",
  "plugins": [
    {
      "pluginId": "com.example.myservice",
      "name": "MyService",
      "versionCode": 1, "versionName": "1.0.0", "apiVersion": "1.0",
      "author": "You",
      "package": "https://example.com/my-1.ppp",
      "sha256": "…", "signature": "…", "publicKey": "…",
      "description": "…", "nsfw": false
    }
  ]
}
```

---

## 6. Про безопасность (важно понимать до того, как ставить чужое)

Плагин — это обычный Java-код в **том же процессе** с правами PipePipe: полный доступ к сети,
файлам, cookies от YouTube-аккаунта, истории. Никакой песочницы нет. Подпись Ed25519 защищает
только от подмены пакета при обновлении и от «злого зеркала» — она не означает, что код
проверен. Поэтому правило простое: исходники открыты и вы их прочитали, иначе не ставить.
Лицензия проекта — GPL-3.0, плагины наследуют это же требование.

---

## 7. Практический маршрут «сделать свой»

1. Скопировать шаблон (в воркспейсе уже лежит: `~/pipepipe-plugin-template`).
2. Переименовать `com.example.myservice`, классы и `rootProject.name` в `settings.gradle`.
3. Взять ближайший по типу пример из `PipePipe-builtin-plugins`:
   * SoundCloud — JSON API + токен, поиск/канал/плейлист;
   * Bandcamp / media.ccc.de — парсинг HTML через jsoup;
   * PeerTube — многоинстансовость и настройка базового URL из `settings`.
4. Для сервиса с авторизацией/токенами смотреть TwitchPipePipePlugin (`plugin.json` с `login`,
   отдельные authorized/unauthorized API-клиенты).
5. Собрать `.ppp`, поставить через Настройки → Плагины (локально можно просто добавить
   файл-репозиторий), перезапустить приложение — сервис появится в списке сервисов.
6. Отладить по логам: все ошибки загрузки оборачиваются в `PluginException` с указанием
   `pluginId`, а `issueUrl` подскажет конечному пользователю, куда писать.

### Чек-лист перед публикацией

- [ ] `pluginId` финальный, `name` = имя сервиса
- [ ] `entryClass` реализует `PipePipePlugin`, публичный no-arg конструктор
- [ ] `serviceId` из `createService` передан в `StreamingService` без изменений
- [ ] сеть только через `NewPipe.getDownloader()`, новых тяжёлых зависимостей нет
- [ ] `versionCode` увеличен, подпись тем же ключом
- [ ] указаны `author`, `issueUrl`, иконка 512 px (одноцветная, прозрачный фон)
- [ ] проверено на актуальном PipePipe 5.6.0+ (и на бете тоже)
- [ ] исходники открыты, GPL-3.0
