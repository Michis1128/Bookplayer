# Michis Player

Reproductor de audiolibros local para Android. Michis Player organiza y reproduce
archivos elegidos por el usuario sin cuentas, telemetría ni acceso a Internet.

> El proyecto está en desarrollo activo. Las fases 1 a 3 ya están implementadas;
> las funciones indicadas en la hoja de ruta todavía pueden cambiar.

## Funciones disponibles

- Selección de carpetas mediante Storage Access Framework (SAF), sin solicitar
  acceso general al almacenamiento.
- Escaneo incremental de MP3, AAC, M4A, M4B, FLAC y OGG.
- Biblioteca en cuadrícula o lista, con búsqueda, filtros y ordenamiento natural.
- Lectura de metadatos y carátulas incrustadas o ubicadas junto al audio.
- Reproducción con Media3 y un servicio en primer plano.
- Controles de reproducción, pausa, avance de 30 segundos y retroceso de 10.
- Mini reproductor y pantalla de reproducción completa.
- Restauración del libro, archivo y posición más recientes.
- Guardado periódico del progreso y guardado inmediato al pausar o buscar.
- Quince modos de color: Sistema y las catorce paletas de Michis Reader.

## Privacidad

Michis Player funciona completamente sin conexión. La aplicación no declara el
permiso `INTERNET`, no incluye analítica ni envía información fuera del dispositivo.
Los archivos se abren desde las ubicaciones que el usuario autoriza explícitamente
con SAF.

## Tecnologías

- Kotlin y Jetpack Compose con Material 3.
- Arquitectura modular con capas `domain`, `data`, `playback` y `feature`.
- Room para la biblioteca y el progreso de reproducción.
- DataStore para preferencias.
- Media3/ExoPlayer y MediaSession para reproducción en segundo plano.
- Hilt para inyección de dependencias.
- Coroutines y StateFlow para estado asíncrono.

La interfaz está escrita en Kotlin con Jetpack Compose; no utiliza layouts XML
tradicionales.

## Estructura del proyecto

```text
app/                    Navegación y punto de entrada de Android
core/common/            Utilidades independientes de la interfaz
core/ui/                Tema y componentes compartidos de Compose
domain/                 Modelos, contratos y casos de uso
data/                   Room, DataStore, metadatos y escaneo SAF
playback/               ExoPlayer, MediaSession y servicio de reproducción
feature/library/        Biblioteca, búsqueda, filtros y escaneo
feature/player/         Mini reproductor y reproductor completo
feature/bookdetails/    Detalle de cada audiolibro
feature/bookmarks/      Base para marcadores
feature/settings/       Preferencias de la aplicación
```

## Requisitos y compilación

- Android Studio compatible con Android Gradle Plugin 9.2.1.
- JDK 17.
- Android SDK 36.
- Dispositivo o emulador con Android 8.0 (API 26) o posterior.

En Windows:

```powershell
.\gradlew.bat :app:assembleDebug
```

En macOS o Linux:

```bash
./gradlew :app:assembleDebug
```

El APK de desarrollo se genera en `app/build/outputs/apk/debug/`.

Para ejecutar las comprobaciones principales:

```powershell
.\gradlew.bat :app:compileDebugKotlin testDebugUnitTest :domain:test :core:common:test :app:lintDebug
```

## Hoja de ruta

- [x] Fase 1: base del proyecto, arquitectura, base de datos y navegación.
- [x] Fase 2: selección de carpetas, escaneo y biblioteca local.
- [x] Fase 3: reproducción en segundo plano y persistencia del progreso.
- [ ] Fase 4: capítulos M4B/CUE, velocidad por libro, saltos configurables,
  auto-retroceso y marcadores.
- [ ] Fases posteriores: pulido de experiencia, accesibilidad y pruebas en más
  dispositivos.

## Estado de las pruebas

El proyecto cuenta con pruebas unitarias para modelos y ordenamiento natural, además
de las comprobaciones de compilación y lint. La reproducción en pantalla apagada,
bloqueo, auriculares y Bluetooth debe validarse en dispositivos físicos antes de
considerar una versión estable.

## Licencia

Todavía no se ha definido una licencia. Mientras no exista un archivo `LICENSE`,
se conservan todos los derechos sobre el código.
