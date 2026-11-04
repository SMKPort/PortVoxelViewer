# Port Nexus Launcher 2.0

A branch **port-nexus-launcher** desenvolve um launcher Android experimental e separado do projeto principal PortVoxelViewer. A branch **main** não foi alterada.

## Fontes
- `PortNexusLauncher/`: código-fonte Kotlin/Jetpack Compose, projeto Gradle, README, arquitetura v2
- `apps/PortNexusLauncher-source.tar.xz`: arquivo espelho usado pelo CI para restaurar o código
- `.github/workflows/nexus-launcher.yml`: compilação automática do APK

## Baixar APK de teste
[Abrir Actions do Nexus](https://github.com/SMKPort/PortVoxelViewer/actions/workflows/nexus-launcher.yml) e baixar o artefato **PortNexusLauncher-v2.0-debug** da execução concluída. Debug APK não é release assinada para distribuição pública.

## Recursos v2 implementados
Núcleo animado, sete modos e temas, dock mutável, gaveta Grade/Arsenal/Nexus Inteligente, favoritos, ocultação, busca, Archive com notas e histórico local, Command Deck e atalhos oficiais do Android, console local de comandos, personalização de ícones/efeitos.

## Limites atuais
Ainda não oferece drag-and-drop na Home, folders customizadas, hospedagem de widgets externos, IA remota, integração direta de smart home, notificações globais ou controle de mídia por sessão. Esses itens requerem módulos e permissões adicionais. Confira `PortNexusLauncher/ARCHITECTURE_V2.md`.
