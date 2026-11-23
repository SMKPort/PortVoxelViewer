# Nexus RP 4.0.6 · Consolidação da interface

Retiradas do **site**, não do Minecraft, as páginas de Armadura Arcana, Rituais, RP ao Vivo e Emergência narrativa. Os links correspondentes também foram excluídos da navegação e da busca rápida. Nenhum dado antigo foi apagado automaticamente do navegador. Eventos reais, Nexus Client, Voz, Comunicador, Alertas, Nexus Mind e Afinidades permanecem disponíveis.

## Canal único e alertas
- Configurações aceitam comando de chat personalizado com até cinco argumentos fixos e identificação permanente `[Nexus Aterna]`.
- Ao salvar, o site envia `/api/client/chat/settings` com canal, comando, estilos de prefixo e corpo e autorização de anúncios.
- O mod persiste o canal para falas automáticas; o servidor Minecraft continua controlando permissões.
- O formulário de alertas oferece **DESLIGADO**, **SOMENTE HUD**, **SOMENTE CHAT** e **HUD E CHAT**, por categoria.
- Voz do Nexus no navegador sempre envia o canal selecionado na requisição `/api/client/voice`.

## Correções
- Corrigido formulário de configuração que poderia voltar a modo RCON após salvar a preferência client-only.
- Substituídos os comandos inexistentes do menu pelos comandos reais do mod.
- Página de Eventos agora somente lê telemetria, sem reexecutar dano/cura ou alterar o antigo RP da armadura.
- Mensagens do comunicador são tratadas como solicitações de envio, não como confirmação do plugin de chat.
- O projeto mantém o pareamento HTTPS, sem acesso administrativo ao Somake.
- Render e Gemini exigem variáveis de ambiente reais e conectividade. Sem acesso ao ambiente da Render não é possível validar segredos, quota ou produção.
