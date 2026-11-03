# Jé Cobranças Android v1.2
Aplicativo da Adega da Jé para controle de clientes, compras fiadas, saldos e pagamentos.
## Correção 1.2\n- Corrige a exibição do HTML antigo pelo cache do Service Worker dentro do Android.\n- Desativa o Service Worker no APK, limpa apenas caches e preserva o armazenamento local.\n- O título exibe v1.2 para identificar a interface instalada.\n- Para reinstalação por troca de assinatura de desenvolvimento, exporte e depois restaure o backup JSON.\n\n## Integração sem servidor\n- Aba Cardápio: 33 itens reais e imagens ilustrativas, seleção de quantidades e lançamento de vários produtos na conta de um cliente em uma única confirmação.\n- Cinco produtos com preço pendente aparecem bloqueados para evitar cobrança incorreta.\n- Página de cardápio separada da base financeira, sem sincronização online, pedidos automáticos nem acesso público aos dados dos clientes.\n- O app preserva o armazenamento local `je_cobrancas_v1` e não altera registros existentes.\n\n## Recursos
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
