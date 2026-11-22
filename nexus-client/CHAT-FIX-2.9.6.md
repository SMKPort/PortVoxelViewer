# Nexus Client 2.9.6: correção do canal personalizado

## Causa corrigida

O painel do Nexus/Render aceitava comandos personalizados como
`/f chat`, `/somake:rp` e `/nexusconversa`, mas o JAR antigo usava
uma lista fixa de 11 comandos e rejeitava os demais antes de enviar
ao Minecraft. O erro era informado como `Canal de chat personalizado
não autorizado.`, embora o painel tivesse aceitado a configuração.

O cliente agora valida comandos da mesma maneira que a Render:
apenas prefixos fixos de chat com até dois subcomandos estáticos,
sem interpolação, seletores ou caracteres de shell, bloqueando os
comandos administrativos. O servidor Minecraft ainda precisa ter o
comando de chat instalado e autorizar o jogador a utilizá-lo.

## Cores e estilos

Agora o JAR também aplica `format` e `bodyFormat` separadamente.
Exemplo para canal `/f chat`:

```text
/f chat &6&l[Nexus Aterna] &r&fDesintegrando armadura.
```

Se o plugin do servidor mostrar `&6&l` literalmente ou recusar o
texto, desative as cores nos dois campos em Configurações.
A entrega do pacote não é prova de que o servidor publicou a
mensagem; confirme no chat do jogo.

## Como instalar

Baixe o artefato `Nexus-Aterna-Client-1.21.1-NeoForge` do GitHub
Actions da branch `nexus-aterna-rp`, substitua o JAR antigo e
reinicie o Minecraft. Ative `/nexusclient chat on` se necessário.
Na aba Configurações, confirme que o canal está em
**Personalizado** e que o comando contém somente o prefixo, por
exemplo `/f chat`, sem incluir a mensagem.

Este reparo é no **mod**. A versão antiga continuará rejeitando
comandos mesmo que o site seja atualizado. A API Render existente
já encaminha `channel`, `command`, `format` e `bodyFormat`.

Os testes isolados de Java verificam os comandos `/rp`,
`/f chat`, `/somake:rp`, estilos independentes e bloqueio
de comandos administrativos.
