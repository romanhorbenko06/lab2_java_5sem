# Лабораторна робота №2 - Gradle та Java Collections Framework

## Про що проєкт

Аналізатор логів веб-сервера, зібраний Gradle з каталогом версій.

Клас `LogAnalyzer` приймає список рядків у форматі Common Log Format і вміє
дві речі:

- `getUniqueIps(List<String> logLines)` - повертає `Set` з усіма унікальними
  IP-адресами;
- `getTopIps(List<String> logLines, int topN)` - повертає `List` з `topN`
  найчастіших адрес за спаданням частоти.

Рядок, який не відповідає формату, просто пропускається і на результат не
впливає. Некоректні аргументи одразу дають `IllegalArgumentException`.

З колекцій використано лише стандартні `java.util.*`, сторонніх бібліотек
немає. Усе лежить у пакеті `ua.kpi.comsys.collections`.

## Розрахунок варіанта

Залікової книжки немає, тому беремо порядковий номер у списку групи: **N = 6**.

```
V = N mod 3 = 6 mod 3 = 0
```

**Варіант 0 - аналізатор логів серверів.**

## Формат рядка

Рядок Common Log Format складається із семи частин, розділених пробілом:

```
127.0.0.1 ident alice [01/May/2025:07:20:10 +0000] "GET /index.html HTTP/1.1" 200 9481
```

| Частина                        | Що перевіряється                              |
|--------------------------------|-----------------------------------------------|
| `127.0.0.1`                    | IPv4-адреса, кожен октет від 0 до 255         |
| `ident`                        | будь-який токен без пробілів або `-`          |
| `alice`                        | будь-який токен без пробілів або `-`          |
| `[01/May/2025:07:20:10 +0000]` | дата `dd/MMM/yyyy:HH:mm:ss` і зсув часу       |
| `"GET /index.html HTTP/1.1"`   | запит у подвійних лапках                      |
| `200`                          | тризначний код відповіді                      |
| `9481`                         | розмір відповіді: число або `-`               |

Перевірка робиться одним регулярним виразом на весь рядок. Пробіли на початку
і в кінці рядка не заважають. Рядок ігнорується, якщо:

- він порожній або це довільний текст;
- бракує якоїсь частини або в кінці є зайва;
- дата не в квадратних дужках чи в іншому форматі;
- запит не взято в лапки;
- код відповіді не число;
- на початку стоїть не IPv4-адреса (`localhost`, `256.0.0.1`, `127.0.1`);
- у списку замість рядка лежить `null`.

## Поведінка методів

### getUniqueIps

- `logLines == null` -> `IllegalArgumentException("logLines cannot be null")`;
- порожній список або жодного коректного рядка -> порожня множина;
- інакше - множина всіх адрес без повторів.

Адреси складаються в `TreeSet`, тому результат ще й відсортований. Це не
вимога завдання, просто так зручніше читати вивід.

### getTopIps

- `logLines == null` -> `IllegalArgumentException("logLines cannot be null")`;
- `topN <= 0` -> `IllegalArgumentException("topN must be greater than 0")`;
- список перевіряється першим, тому для `(null, 0)` буде повідомлення про
  `logLines`;
- адреси сортуються за спаданням частоти;
- за однакової частоти - за алфавітом (лексикографічно за зростанням), тобто
  `10.0.0.10` стоїть раніше за `10.0.0.9`, бо порівнюються рядки, а не числа;
- якщо унікальних адрес менше за `topN`, повертаються всі наявні в тому ж
  порядку.

Частоти рахуються в `HashMap` через `merge`, далі записи сортуються
`Comparator`-ом (спочатку значення у зворотному порядку, потім ключ)
і обрізаються до `topN`. Вхідний список при цьому не змінюється.

## Приклад роботи

Вхідні дані в `Main`:

```
127.0.0.1 ident alice [01/May/2025:07:20:10 +0000] "GET /index.html HTTP/1.1" 200 9481
192.168.0.15 - - [01/May/2025:07:20:11 +0000] "GET /about.html HTTP/1.1" 200 2310
10.0.0.7 - bob [01/May/2025:07:20:12 +0000] "POST /login HTTP/1.1" 302 -
192.168.0.15 - - [01/May/2025:07:20:13 +0000] "GET /style.css HTTP/1.1" 304 0
this line is not a log entry
127.0.0.1 ident alice [01/May/2025:07:20:14 +0000] "GET /logo.png HTTP/1.1" 200 5120
192.168.0.15 - - [01/May/2025:07:20:15 +0000] "GET /missing HTTP/1.1" 404 153
```

Вивід:

```
Unique IPs: [10.0.0.7, 127.0.0.1, 192.168.0.15]
Top 2 IPs: [192.168.0.15, 127.0.0.1]
```

`192.168.0.15` трапляється тричі, `127.0.0.1` двічі, `10.0.0.7` один раз,
а п'ятий рядок пропущено як некоректний.

## Gradle та Version Catalog

Проєкт збирається через Gradle Wrapper 9.3.1, скрипт збірки написаний на
Kotlin DSL (`build.gradle.kts`).

Версії та залежності винесені в `gradle/libs.versions.toml`:

```toml
[versions]
java = "23"
junit = "5.11.4"

[libraries]
junit-bom = { module = "org.junit:junit-bom", version.ref = "junit" }
junit-jupiter = { module = "org.junit.jupiter:junit-jupiter" }
junit-platform-launcher = { module = "org.junit.platform:junit-platform-launcher" }
```

У `build.gradle.kts` на них посилаються через `libs`:

```kotlin
dependencies {
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}
```

Версія JUnit задана один раз у BOM, решта модулів бере її звідти. Версія Java
для toolchain теж читається з каталогу (`libs.versions.java`). Плагін
`application` потрібен лише для того, щоб запускати `Main` командою
`./gradlew run`.

## Структура

```
build.gradle.kts              збірка: Java 23, JUnit 5, application
settings.gradle.kts           назва проєкту
gradle/libs.versions.toml     каталог версій (Java, JUnit)
gradle/wrapper/               Gradle Wrapper 9.3.1
gradlew, gradlew.bat          скрипти запуску wrapper
src/main/java/ua/kpi/comsys/collections/
    LogAnalyzer.java      getUniqueIps + getTopIps
    Main.java             демонстраційний запуск
src/test/java/ua/kpi/comsys/collections/
    LogAnalyzerTest.java  обидва методи, винятки, некоректні рядки
```

## Як запустити

Потрібен JDK 23. Gradle ставити не треба, його завантажить wrapper.

Збірка разом із тестами:

```bash
./gradlew build
```

Тести:

```bash
./gradlew test
```

Звіт із результатами тестів після цього лежить у
`build/reports/tests/test/index.html`.

Демонстрація в консолі:

```bash
./gradlew run
```

На Windows замість `./gradlew` пишемо `gradlew.bat`.

З IntelliJ IDEA: проєкт відкривається як Gradle-проєкт (File -> Open ->
`build.gradle.kts` -> Open as Project), далі правий клік на теці
`src/test/java` -> Run All Tests.

## Тести

35 тестів на JUnit 5 в одному класі `LogAnalyzerTest`, поділені на дві вкладені
групи.

`GetUniqueIps` (22 тести):

- `null`-список і точний текст винятку;
- порожній список;
- приклад рядка зі специфікації;
- повтори адрес згортаються;
- розмір `-`, екрановані лапки в запиті, пробіли по краях рядка;
- 15 варіантів некоректного рядка, кожен окремим параметризованим тестом;
- `null` усередині списку;
- список лише з некоректних рядків.

`GetTopIps` (13 тестів):

- `null`-список і `topN` зі значеннями `0`, `-1`, `Integer.MIN_VALUE`
  з точними текстами винятків;
- порядок перевірки аргументів;
- порожній список;
- сортування за спаданням частоти;
- обрізання до `topN`;
- tie-breaking за алфавітом, у тому числі на межі `topN`;
- унікальних адрес менше за `topN`;
- некоректні рядки не впливають на підрахунок;
- вхідний список не змінюється.
