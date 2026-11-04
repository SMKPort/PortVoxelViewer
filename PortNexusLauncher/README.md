# Port Nexus Launcher 2.0

Launcher experimental e autoral para Android 8.0+, sem root, com UI própria em Kotlin + Jetpack Compose.

## Funcionalidades já implementadas

- Home Nexus com núcleo arcano animado em duas rotações, relógio, data, bateria e dock mutável.
- Sete modos de uso e temas: Arcano, Infernal, Angelical, Industrial, Música, Estudo e Performance.
- Gaveta com Grade, Arsenal (categorias automáticas) e Nexus Inteligente (ordenação local por apps abertos no Nexus + favoritos).
- Favoritos, apps ocultos com opção de reexibir, filtro de busca, tamanho de ícones editável.
- Painéis laterais: Command Deck com brilho local da janela e atalhos de configurações oficiais. Archive com notas locais e histórico de aplicativos abertos pelo Nexus.
- Hub de comandos locais: `abrir [app]`, `modo música`, `wifi`, `bluetooth`, `spotify`, `alarme`, `configurações`. Outros termos filtram a gaveta.
- Gestos horizontais na Home para abrir painéis, seleção do launcher como tela inicial via sistema Android.
- Persistência em SharedPreferences, sem serviço remoto nem rastreamento de uso de outros apps.

## Montagem APK

Requer Android SDK 35 e JDK 17. Abra a pasta `PortNexusLauncher` no Android Studio ou execute `gradle :app:assembleDebug` com Gradle 8.13.

No GitHub, o workflow `Port Nexus Launcher - APK` compila a partir da branch `port-nexus-launcher` e disponibiliza o arquivo instalável como artefato de execução.

## Limites e próximas integrações

Os atalhos de Wi-Fi, Bluetooth e outros controles protegidos abrem configurações nativas (o Android não permite alternar muitos desses ajustes diretamente por apps comuns). Brilho afeta apenas a janela Nexus, não o brilho global. O hub é local baseado em comandos, sem integração com IA online. A organização automática usa heurísticas, sem acesso a uso do sistema.

Recursos planejados, mas ainda não incluídos: arrastar ícones, pastas personalizadas, widgets de apps externos, player com controle direto por MediaSession, clima com fonte de dados, automações contextuais e IA conectada com consentimento.

Documentação técnica: `ARCHITECTURE_V2.md`.
