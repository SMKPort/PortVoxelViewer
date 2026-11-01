# LumiPort v0.4.0

Aplicativo Android com cinco abas: Início, Dispositivos, Cores, Cenas e Ajustes. Mantém a interface preta, vermelha e dourada.

## Fita HappyLighting QHM-0CFE

Na captura do usuário a fita se anuncia como QHM-0CFE. O app agora inclui o nome QHM nos resultados Bluetooth e tenta os comandos QHM/Triones documentados no serviço FFD5 e característica FFD9: CC 23 33 (ligar), CC 24 33 (desligar), 56 RR GG BB 00 F0 AA (cor). Brilho é aproximado por escala RGB. Os dispositivos LEDBLE/LEDCAR FFE1 seguem suportados. O controle da lâmpada Smart Life continua via Home Assistant.

## Testar

Feche o HappyLighting, ligue o Bluetooth do celular, abra a aba Ajustes, toque em Procurar fita Bluetooth e selecione QHM-0CFE. Use Dispositivos para testar ligar e desligar e Cores para testar RGB. O nome QHM não garante que o protocolo seja igual, portanto é necessário testar a fita real. Se a conexão der erro, documente os serviços GATT encontrados antes de tentar outro comando.

## Fontes do protocolo

https://github.com/madhead/saberlight/blob/master/protocols/Triones/protocol.md

https://peaku.co/pt/questions/107100-tira-de-led-de-control-web-bluetooth
