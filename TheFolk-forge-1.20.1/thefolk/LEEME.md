# The Folk — mod de terror para Minecraft Java 1.20.1 (Forge)

## Qué hace
Entidad tipo dweller de 3 bloques de alto. Ciclo de 18 días de Minecraft (se repite):

| Fase | Días | Comportamiento |
|---|---|---|
| 1 | 5 | Aparece lejos, a la vista, y solo te mira. Desaparece si te acercas a menos de 12 bloques. |
| 2 | 8 | Aparece a la vista y camina lento hacia ti. Al llegar a ~5 bloques se queda quieto y desaparece. |
| 3 | 5 | Aparece cerca (14-24 bloques) y escondido, te persigue 1 minuto, más rápido que tú, rompe bloques y golpea (3 corazones). |

Es invulnerable (solo el modo creativo o /kill lo eliminan). Solo aparece en el Overworld, en dificultad
distinta de Pacífico, y no en jugadores en creativo/espectador. Fases 1 y 2 aparecen de noche o en lugares oscuros;
la fase 3 aparece a cualquier hora.

## Comandos de prueba (necesitan permisos de operador / trucos activados)
- `/thefolk spawn 1|2|3` — lo invoca en esa fase
- `/thefolk status` — muestra el día del mundo, día del ciclo y fase actual
- Para saltar de fase: `/time add 24000` suma un día

## Ajustes
Todo lo que puedes cambiar está al inicio de dos archivos, en el bloque CONFIGURACIÓN:
- `FolkDirector.java`: duración de las fases, cada cuánto aparece, distancias
- `TheFolkEntity.java`: velocidades, daño, duración de la persecución

## Compilar (necesitas Java 17)
1. Descarga el **MDK de Forge 1.20.1 (47.3.0)** desde files.minecraftforge.net y descomprímelo en cualquier carpeta.
2. Copia de ese MDK estas 3 cosas a esta carpeta: `gradlew`, `gradlew.bat` y la carpeta `gradle/`.
3. Abre una terminal en esta carpeta y ejecuta:
   - Windows: `gradlew.bat build`
   - Linux/Mac: `./gradlew build`
4. El mod queda en `build/libs/thefolk-forge-1.20.1-1.0.0.jar` (la primera vez tarda varios minutos descargando cosas).

## Instalar en TLauncher
1. En TLauncher elige la versión **1.20.1 con Forge** (aparece en la lista de versiones; si no, instala Forge 1.20.1 y selecciónalo).
2. Abre el juego una vez para que se cree la carpeta `mods`.
3. Pon el `.jar` en `.minecraft/mods` (en Windows: `%appdata%\.minecraft\mods`).
4. Abre el juego con el perfil de Forge.
