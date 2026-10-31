# CaldoControl 2.0 para Android

Aplicativo de acompanhamento do **tratamento de caldo para açúcar e etanol**, com calagem, pH do caldo misto e caleado/dosado (mesma etapa), decantadores e lodo, aquecedores, relatórios por turno, histórico e conexão **somente de leitura** com gateway WebSocket autorizado.

## APK

No GitHub, acesse **Actions > CaldoControl APK Android > execução mais recente > Artifacts > CaldoControl-2.0-debug-APK**. Baixe o ZIP e extraia o arquivo **app-debug.apk** para instalar no Android. Este é um APK debug para teste, não assinado para distribuição pela Play Store.

O workflow `.github/workflows/caldocontrol-apk.yml` compila o projeto com Java 17, Gradle 8.9 e Android SDK 35. Também pode ser executado manualmente via **Run workflow**.

## Organização

- `app/src/main/assets/index.html`: CaldoControl 2.0 completo, com suporte Android a CSV/JSON e impressão.
- `app/src/main/java/br/com/caldocontrol/app/MainActivity.java`: WebView com origem local HTTPS de recursos, seleção de arquivos, exportação e impressão.
- `app/build.gradle`: configurações de compilação Android.
- `../.github/workflows/caldocontrol-apk.yml`: compilação automatizada e artefato APK.

## Dados e segurança

Os dados ficam no armazenamento privado do WebView do Android. **O APK não recebe automaticamente o histórico salvo no Chrome**: exporte Backup JSON na versão web e importe no APK em Ajustes. Faça backups periódicos, pois desinstalar o APK ou limpar seus dados pode apagar os registros.

A integração com CLPs permanece **somente leitura** e requer gateway autorizado em `wss://` (TLS/certificado confiável); não há acesso direto a Modbus/OPC UA nem envio de comandos. A origem do app é HTTPS local via AndroidX WebViewAssetLoader. Evite conectar o APK a redes industriais sem validação pela equipe de automação/cibersegurança.

Protótipo para apoio a registros. Não substitui SCADA/SDCD, proteções/intertravamentos ou procedimentos homologados da usina. Faixas operacionais exigem aprovação da engenharia.

## Compilação local

Com Android SDK 35, JDK 17 e Gradle 8.9:

```bash
cd CaldoControl
gradle :app:assembleDebug
```

O resultado fica em `app/build/outputs/apk/debug/app-debug.apk`.
