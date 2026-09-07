<div align="center">

<img src="brand/logo.svg" width="120" height="120" alt="Battery Alarm logo">

# Battery Alarm

**Avisa cuando tu celular termina de cargar.**

Gratis · Open Source · Sin publicidad · Sin Internet · Sin cuentas

</div>

## ¿Qué hace?

Si tu cargador o tu celular no te avisan cuando la batería está llena, **Battery Alarm** lo hace por ti: abre la aplicación, pulsa **Iniciar monitoreo**, conecta el celular al cargador y olvídate. Cuando la batería llega al 100%, la app espera el tiempo que tú elijas y suena una alarma para que desconectes el cargador.

```
Iniciar monitoreo → detectar la carga → detectar 100% → esperar el tiempo configurado → 🔔 alarma
```

## Cómo funciona

1. Abres la app y pulsas **Iniciar monitoreo**.
2. Aparece una notificación permanente con el nivel de batería y el estado de la carga.
3. La app vigila la batería mientras está cargando (y solo mientras está cargando).
4. Al llegar al **100%**, espera el tiempo configurado (por defecto **2 minutos**, configurable: 1, 2, 5, 10, 15 o un valor personalizado).
5. Suena la alarma (sonido del sistema o un **archivo de audio propio**) y se muestra **¡Carga completa!**.
6. El monitoreo termina solo: si desconectas el cargador antes del 100%, o si lo detienes desde la notificación.

## Filosofía

- 🆓 **Gratis** y **open source**
- 🚫 **Sin publicidad**
- 🌐 **Sin Internet** (no usa red para nada)
- 👤 **Sin cuentas**
- 🔋 **Sin monitoreo permanente**: solo funciona mientras tú decides vigilar una carga
- 🛑 Lo detienes cuando quieras, desde la app o desde la notificación
- 🎵 **Sonido configurable** (incluido un archivo de audio tuyo)
- ⏱️ **Tiempo configurable** después del 100%

## Instalar

Descarga la última APK desde [Releases](https://github.com/USER/battery-alarm/releases). También puedes compilarla tú mismo:

1. Clona el repositorio.
2. Abre el proyecto en Android Studio (o ejecuta `./gradlew assembleRelease`).
3. La APK estará en `app/build/outputs/apk/release/`.

> Nota: la compilación release usa una firma de depuración para que la APK pueda instalarse sin configuración extra. Para publicar en una tienda, sustituye el `signingConfig` por el de tu propio keystore.

## Diseño e identidad

Toda la iconografía es **SVG** hecha a medida para el proyecto, sin emojis:

- 🎨 **Logo**: una batería **llena al 100%** cuyo terminal es una **campana de alarma**, con ondas de sonido hacia un costado. La carga completa "toca la campana".
- 📐 **Iconos de la interfaz** con un estilo consistente (trazo redondeado, misma proporción):
  `ic_battery_charging`, `ic_bell`, `ic_bolt`, `ic_stop`, `ic_sound`, `ic_timer`, `ic_settings`, `ic_check`.
- 🖥️ Los iconos se usan dentro de la app como *VectorDrawables* (la implementación de SVG en Android), por lo que se ven idénticos en cualquier dispositivo.

### Estructura de la marca

| Archivo | Descripción |
| --- | --- |
| `brand/logo.svg` | Logo principal (fondo + símbolo), para README y web |
| `brand/icon-foreground.svg` | Símbolo con fondo transparente (foreground del icono adaptativo) |
| `brand/IconRenderer.java` | Genera los mipmaps PNG heredados a partir de la misma geometría |
| `res/drawable/ic_launcher_foreground.xml` | Icono adaptativo (Android 8+) |
| `res/drawable/logo.xml` | Emblema usado dentro de la app |

### Sistema de diseño

- **Paleta** — verde volt (#1CD27F), menta (#B2FBD7), ámbar (#FFB224), grafito (fondo)
- **Iconos** — 24×24 dp, trazo de 1,8 px, extremos redondeados
- **Tipografía** — `FontFamily.Default` (la del sistema), pesos según jerarquía

Para añadir un icono nuevo, crea el SVG y conviértelo a VectorDrawable siguiendo las mismas proporciones.

## Desarrollar

- `./gradlew assembleDebug` — compila la APK de depuración
- `./gradlew assembleRelease` — compila la APK release (firmada con clave de depuración)
- Cada vez que se etiqueta un `vX.Y.Z`, el workflow [release.yml](.github/workflows/release.yml) publica la APK en **GitHub Releases**

## Licencia

[MIT](LICENSE) © Battery Alarm contributors