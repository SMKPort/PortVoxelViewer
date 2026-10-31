# CaldoControl 2.1 para Android

Aplicativo de acompanhamento do **tratamento de caldo para açúcar e etanol**, com calagem, pH do caldo misto e caleado/dosado (mesma etapa), decantadores e lodo, aquecedores, relatórios por turno, histórico e conexão **somente de leitura** com gateway WebSocket autorizado.

## APK

No GitHub, acesse **Actions > CaldoControl APK Android > execução mais recente > Artifacts > CaldoControl-2.1-debug-APK**. Baixe o ZIP e extraia o arquivo **app-debug.apk** para instalar no Android. Este é um APK debug para teste, não assinado para distribuição pela Play Store.

O workflow `.github/workflows/caldocontrol-apk.yml` compila o projeto com Java 17, Gradle 8.9 e Android SDK 35. Também pode ser executado manualmente via **Run workflow**.


## Novidades da versão 2.1

- Aba **Médias por turno**, com médias aritméticas separadas de pH caleado/dosado, pH clarificado, pH da torre de resfriamento e Brix caleado, clarificado e xarope. Cada resultado mostra `n`, o número de amostras utilizadas. É necessário indicar o turno ao registrar a análise; as médias são recalculadas quando novos registros são salvos.
- Campo de **caldo misto removido** da ficha. Dados legados continuam no histórico e backup, quando já existentes.
- Três campos de **°Brix separados**. Um Brix genérico de versões anteriores permanece no backup sem ser atribuído automaticamente a uma etapa.
- **Importação de fotos por OCR offline** no APK Android com Google ML Kit. Escolha a imagem, revise as sugestões e selecione quais números serão atribuídos a cada campo antes de aplicá-los. Nem todos os formatos ou letras manuscritas serão interpretados corretamente. No navegador HTML, a foto pode ser anexada, mas o OCR exige o APK Android.
- **Exportação** de registros em CSV, médias por turno em CSV e **backup JSON completo, incluindo miniaturas de fotos anexadas e texto reconhecido**. A importação/restauração JSON conserva o histórico de arquivos compatíveis, com prevenção de IDs duplicados.

A versão é de apoio ao acompanhamento e exige conferência humana. O OCR pode confundir leituras e **não deve gerar correções operacionais automáticas** no CLP.

## Organização

- `app/src/main/assets/index.html`: CaldoControl 2.1 completo, com suporte Android a CSV/JSON e impressão.
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
