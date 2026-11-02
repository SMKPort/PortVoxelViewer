# ChuvaPort Android

Aplicativo Android nativo leve, usando WebViewAssetLoader para carregar a interface local em HTTPS interno. A previsao e consultada na Open-Meteo via HTTPS pelo JavaScript da pagina.

## Compilar

O GitHub Actions executa `gradle :app:assembleDebug` dentro desta pasta e publica o artefato **ChuvaPort-Android-APK**.

Requisitos para compilacao local: JDK 17, Gradle 8.9 e Android SDK 35.

Os arquivos web sao copiados automaticamente de `ChuvaPort/index.html`, `ChuvaPort/assets`, `ChuvaPort/manifest.webmanifest` e `ChuvaPort/sw.js` para os assets do APK.

O app pede localizacao aproximada/precisa apenas quando o usuario toca em **GPS**. Pesquisa de cidade funciona sem permissao de GPS. Requer internet para atualizar previsoes.

O APK do workflow e de depuracao (debug), assinado automaticamente para testes. Nao e uma versao de publicacao na Play Store.
