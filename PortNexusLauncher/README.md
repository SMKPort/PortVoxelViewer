# Port Nexus Launcher (v0.1)

Launcher Android em Kotlin e Jetpack Compose, com identidade Nexus nas cores preto, vermelho e dourado. Projeto inicial funcional para Android 8.0+.

## Funcionalidades implementadas

- Atividade Android HOME registrável como tela inicial padrão.
- Lista de aplicativos instalados com atividades de lançamento (sem `QUERY_ALL_PACKAGES`).
- Gaveta em grade, pesquisa local, abertura de apps pelo componente correto.
- Favoritos persistentes e dock com atalhos.
- Relógio e data, leitura de bateria e núcleo animado.
- Central Nexus com atalhos para configurações Wi-Fi, Bluetooth e Android, relógio e Spotify (se instalado).
- Temas Nexus Prime, Infernal Core, Celestial Crown e Industrial, com seleção salva.
- Acesso à tela de seleção do launcher padrão.
- GitHub Actions para gerar APK debug como artefato.

## Abrir no Android Studio

1. Extraia o ZIP e abra a pasta `PortNexusLauncher` no Android Studio.
2. Use JDK 17. O projeto utiliza Android Gradle Plugin 8.13.2, Kotlin 2.2.20 e Gradle 8.13.
3. Como o ZIP não inclui o binário Gradle Wrapper, configure Gradle 8.13 localmente (ou execute `gradle wrapper --gradle-version 8.13` com Gradle já instalado). No GitHub Actions o Gradle é instalado automaticamente.
4. Aguarde a sincronização do Gradle e execute `app` em um celular Android.
5. No app, abra `Visual` > `Definir como tela inicial`. O Android pedirá a escolha do launcher.
6. Para voltar ao launcher anterior: Configurações do Android > Apps > Apps padrão > App de início.

### Gerar APK pelo GitHub

Crie um repositório e envie o conteúdo desta pasta para a raiz. Na aba **Actions**, execute **Gerar APK Android**. Quando o fluxo terminar com sucesso, abra a execução e baixe o artefato `Port-Nexus-Launcher-v0.1-debug`. O APK dentro do ZIP do artefato é `app-debug.apk`.

Obs.: as ações são preparadas e não foram executadas automaticamente nesta entrega. O APK ainda não foi compilado aqui.

## Limitações intencionais

A interface não altera a central nativa de notificações, o painel de configurações rápidas nem a tela de apps recentes. Os atalhos de Wi-Fi e Bluetooth abrem páginas autorizadas do Android e não alteram o rádio de forma silenciosa. O atalho Spotify apenas abre o app. A versão 0.1 ainda não oferece mídia integrada, widgets de terceiros, gestos personalizados ou automações externas. Não requer root, acesso a notificações, permissão de sobreposição nem acesso à internet.

## Segurança

A escolha do launcher padrão é feita pelo usuário no diálogo do Android. Evitamos permissões perigosas e não usamos serviços em segundo plano. O APK debug é para teste. Para uma distribuição pública, é necessário gerar e assinar uma versão release.
