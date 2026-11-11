# Port Studio Suite 🎬🖥️

Dois mods independentes para Minecraft Java **1.21.1 NeoForge (Java 21)**.

## Port Cinematic Studio
Diretório: `port-cinematic-studio/`. Câmera virtual client-side, keyframes, interpolação suave, timeline interativa, editor com WASD/Q/E e salvamento local em JSON. Abra com `/portcinema`. A câmera não altera a posição do jogador nem é enviada ao servidor.

## Port HUD Studio
Diretório: `port-hud-studio/`. Editor de widgets arrastáveis para vida, armadura, fome, coordenadas, FPS e ping. Perfis RPG, Minimal e Performance com tamanho, transparência e visibilidade configuráveis. Abra com `/porthud`. Configurações são armazenadas localmente.

## Integração opcional
Os dois JARs funcionam separadamente. Quando ambos estão instalados, o HUD Studio detecta por reflexão a reprodução do Cinematic Studio e pode ocultar automaticamente os widgets personalizados. A reprodução cinematográfica também oculta o HUD vanilla e restaura as preferências de exibição ao terminar.

## Download
No GitHub Actions, abra o workflow **Port Studio Suite - NeoForge 1.21.1** e baixe os dois artefatos:
- `Port-Cinematic-Studio-NeoForge-1.21.1` (JAR `portcinema-1.0.0.jar`)
- `Port-HUD-Studio-NeoForge-1.21.1` (JAR `porthud-1.0.0.jar`)

Workflow: https://github.com/SMKPort/PortVoxelViewer/actions/workflows/port-studio-suite-build.yml

Extraia os JARs e coloque na pasta `mods` do Minecraft 1.21.1 NeoForge.

## Build
```bash
gradle -p port-cinematic-studio build --no-daemon
gradle -p port-hud-studio build --no-daemon
```

Não foi alterado o Nexus Aterna nem o Port Performance Monitor. O código permanece na branch separada `port-studio-suite` do repositório existente.

**Limitações:** sem gravação/exportação de vídeo, replay de outros jogadores, edição do HUD vanilla ou integração com servidores. A câmera usa apenas chunks carregados no cliente. Em servidores multiplayer, respeite regras que possam restringir câmeras livres. Os dois mods precisam de teste prático com o modpack do usuário após a compilação no GitHub.
