# Port Nexus Launcher 3.2 • Sovereign

Integra as melhorias visuais e funcionais da avaliação Sovereign à branch atual do Launcher, preservando as correções de IA da versão 3.1, incluindo o provedor Gemini.

## Recursos

- Studio acessível pela tela inicial e pela aba Visual.
- Núcleos vetoriais Serafim (seis asas), Eclipse e Órbita, assinatura pessoal e tamanho compacto.
- Coleções de aplicativos com criação, busca, seleção, renomeação e exclusão confirmada.
- Missões com edição, conclusão, salvamento local e desfazer a última exclusão.
- Foco de 1 a 120 minutos, pausa e retomada. O prazo usa relógio monotônico e continua correto após sair e voltar durante o mesmo boot. Não bloqueia aplicativos nem emite alarme em segundo plano. Reiniciar o celular encerra a contagem ativa.
- Backup JSON validado integralmente antes de restaurar, com confirmação. Inclui preferências permitidas, coleções, missões, favoritos, ocultos e notas. Exclui tokens, endpoint da IA, conteúdo de notificações e foco ativo.
- Apps ocultos fora do dock e das coleções. O Arsenal ainda permite mostrar ocultos explicitamente.
- Voltar fecha primeiro o painel e depois retorna ao início. Respeita as áreas das barras do Android.
- Animação da tela inicial para com bateria em 15% ou menos. A bateria é atualizada enquanto a Activity está visível.

## Validação e compilação

JDK 17, Android SDK 35, Gradle 8.13:

```
gradle -p PortNexusLauncher :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
node --test PortNexusLauncher/nexus-ai-server/server.test.mjs
```

Os sete testes Android cobrem ida e volta de dados Unicode, estado das missões, exclusão de credenciais do backup, rejeição de campos injetados, IDs duplicados, tamanho inválido de ícone e contagem por prazo. Os seis testes do servidor preservam a validação dos planos e os diagnósticos de OpenAI/Gemini.

O workflow publica o APK somente após testes, lint e compilação. Commits apenas na pasta download não iniciam outra compilação.

## Instalação

O pacote da versão principal permanece `com.port.nexuslauncher`. Esta é uma compilação debug. O Android só permite atualizar a instalação existente se as assinaturas forem compatíveis. Não desinstale uma instalação com dados importantes para contornar uma divergência de assinatura. A avaliação anterior `com.port.nexuslauncher.sovereign` é um app separado e não é substituída pela versão principal. O backup do Studio permite transferir preferências entre as duas edições, sem tokens de IA.

A seleção como launcher padrão, permissões e aparência precisam ser verificadas no aparelho. Os testes automatizados não substituem a validação física no Poco M5.
