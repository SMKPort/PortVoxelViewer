# Port Nexus Launcher (Android)

Projeto Kotlin/Jetpack Compose para Android 8.0+ com interface preta, vermelha e dourada.

Esta branch `port-nexus-launcher` mantém o launcher separado do Port Voxel Viewer. A branch principal `main` permanece intacta.

O código-fonte está no diretório `PortNexusLauncher/` (extraído automaticamente pela ação) e também no arquivo `apps/PortNexusLauncher-source.tar.xz`.

## Gerar APK

Abra a aba **Actions** deste repositório, selecione **Port Nexus Launcher - APK** e consulte a última execução para baixar o artefato `PortNexusLauncher-v0.1-debug` com o APK de teste. Se não houver execução, o workflow pode ser acionado manualmente somente após habilitar esta branch como workflow elegível na interface do GitHub, pois o botão workflow_dispatch costuma depender da branch padrão.

## Funcionalidades v0.1

Launcher HOME, gaveta com busca, favoritos persistentes, dock, relógio, bateria, temas Nexus e painel de atalhos seguros. Sem root. Não substitui o painel de notificações nem a tela de recentes do Android.

O APK debug serve para teste. Instale apenas se confiar no código e nas fontes.
