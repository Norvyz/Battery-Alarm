<div align="center">

<img src="brand/logo.svg" width="112" height="112" alt="Logo de Battery Alarm">

# Battery Alarm

*Avisa cuando tu celular termina de cargar.*

`Gratis · Open Source · Sin publicidad · Sin Internet · Sin cuentas`

[![Licencia MIT](https://img.shields.io/badge/Licencia-MIT-0FAE68?style=flat-square)](LICENSE)
[![Android 7.0+](https://img.shields.io/badge/Android-7.0%2B-3DDC84?style=flat-square)](https://developer.android.com/studio)
[![Última release](https://img.shields.io/github/v/release/Norvyz/Battery-Alarm?style=flat-square&color=1CD27F)](https://github.com/Norvyz/Battery-Alarm/releases)
[![Build](https://img.shields.io/github/actions/workflow/status/Norvyz/Battery-Alarm/release.yml?style=flat-square&color=1CD27F)](https://github.com/Norvyz/Battery-Alarm/actions)

</div>

---

## Qué hace

Si tu cargador o tu celular no te avisan cuando la batería está llena, **Battery Alarm** lo hace por ti: abres la app, pulsas **Iniciar monitoreo**, conectas el cargador y te olvidas. Cuando la batería llega al 100 %, la app espera el tiempo que tú elijas y suena una alarma para que desconectes el cargador.

```
Iniciar monitoreo → detectar la carga → detectar 100% → esperar el tiempo configurado → alarma
```

## Cómo funciona

1. Abres la app y pulsas **Iniciar monitoreo**.
2. Aparece una notificación permanente con el nivel de batería y el estado de la carga, con un botón **Detener**.
3. La app vigila la batería **mientras está cargando** (y solo mientras está cargando).
4. Al llegar al **100 %**, espera el tiempo configurado. Por defecto son **2 minutos**, pero puedes elegir 1, 5, 10, 15 o un valor personalizado (1 a 120 minutos).
5. Suena la alarma con el sonido que elegiste: alarma del sistema, notificación del sistema o un **archivo de audio propio**.
6. Se muestra el aviso **Carga completa**.
7. El monitoreo termina solo: si desconectas el cargador antes del 100 %, o si lo detienes desde la notificación.

## Capturas

> Pendientes. Agrega aquí las capturas de pantalla de la app. Sugerencia de estructura:
>
> ```
> docs/
>   screenshots/
>     pantalla-inicio.png
>     monitoreo-activo.png
>     carga-completa.png
> ```

| Pantalla de inicio | Monitoreo activo | Carga completa |
| --- | --- | --- |
| `<img src="docs/screenshots/pantalla-inicio.png" width="220">` | `<img src="docs/screenshots/monitoreo-activo.png" width="220">` | `<img src="docs/screenshots/carga-completa.png" width="220">` |

## Características

- **Gratis** y **open source** (MIT)
- Sin publicidad, sin Internet y sin cuentas
- Sin monitoreo permanente: solo funciona mientras tú decides vigilar una carga
- Lo detienes cuando quieras, desde la app o desde la notificación
- Sonido configurable, incluido un archivo de audio tuyo
- Tiempo configurable después del 100 %

## Instalar

Descarga la última APK desde la pestaña [Releases](https://github.com/Norvyz/Battery-Alarm/releases) o compílala tú mismo:

```bash
./gradlew assembleRelease
```

La APK queda en `app/build/outputs/apk/release/app-release.apk`.

> Nota: la compilación release usa una firma de depuración para que la APK pueda instalarse sin configuración extra. Para publicar en una tienda, sustituye el `signingConfig` por el de tu propio keystore.

## Diseño e identidad

Toda la iconografía es **SVG** hecha a medida para el proyecto, sin depender de emojis del sistema.

**El logo** es una batería **llena al 100 %** cuyo terminal superior es una **campana de alarma**, con ondas de sonido hacia un costado: la carga completa "toca la campana".

Los iconos de la interfaz siguen un estilo consistente (trazo redondeado, mismas proporciones, iconos 24×24 dp) y se usan dentro de la app como *VectorDrawables*, la implementación de SVG en Android, por lo que se ven idénticos en cualquier dispositivo:

`ic_battery_charging` · `ic_bell` · `ic_bolt` · `ic_stop` · `ic_sound` · `ic_timer` · `ic_check`

### Estructura de la marca

| Archivo | Descripción |
| --- | --- |
| `brand/logo.svg` | Logo principal (fondo + símbolo), para README y web |
| `brand/icon-foreground.svg` | Símbolo con fondo transparente (foreground del icono adaptativo) |
| `brand/IconRenderer.java` | Genera los mipmaps PNG heredados a partir de la misma geometría |
| `res/drawable/ic_launcher_foreground.xml` | Icono adaptativo (Android 8+) |
| `res/drawable/logo.xml` | Emblema usado dentro de la app |

### Sistema de diseño

- **Paleta** — verde volt `#1CD27F`, menta `#B2FBD7`, ámbar `#FFB224`, grafito `#0D2419`
- **Iconos** — 24×24 dp, trazo de 1,8 px, extremos redondeados
- **Tipografía** — tipografía del sistema, pesos por jerarquía

Para añadir un icono nuevo, crea el SVG y conviértelo a VectorDrawable siguiendo las mismas proporciones.

## Desarrollar

```bash
./gradlew assembleDebug    # APK de depuración
./gradlew assembleRelease  # APK release (firmada con clave de depuración)
```

Al etiquetar una versión (`git tag vX.Y.Z && git push origin vX.Y.Z`), el workflow [release.yml](.github/workflows/release.yml) publica la APK automáticamente en **GitHub Releases**.

## Licencia

[MIT](LICENSE) © Battery Alarm contributors