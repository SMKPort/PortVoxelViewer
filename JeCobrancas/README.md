# Jé Cobranças Android v1.0
Aplicativo privado da Adega da Jé para controle de clientes, compras fiadas, saldos e pagamentos.
## Recursos
- Cadastro de clientes (nome, telefone, observações).
- Lançamentos com total automático (quantidade × preço).
- Pagamentos parciais, vencimentos, extratos e painel financeiro.
- Cobrança manual por WhatsApp.
- Backup JSON, importação JSON e exportação CSV com seletor nativo Android.
- Todos os dados são mantidos no armazenamento local privado do aplicativo (WebView), sem servidor ou sincronização.
## Compilação
No GitHub, Actions → **Je Cobrancas APK Android** → execução verde → Artifacts → **Je-Cobrancas-Android-APK** → extrair `app-debug.apk`.
Também é possível executar `cd JeCobrancas/android && gradle :app:assembleDebug` com Android SDK 35 e JDK 17.
## Atenção
Faça backup regularmente, especialmente antes de desinstalar ou limpar os dados do app. O app não cobra nem processa pagamentos automaticamente.
