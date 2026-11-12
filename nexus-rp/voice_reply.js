"use strict";
/** Chat replies are narrative rule-based responses; no audio is stored by this service. */
function fold(input){
  return String(input||"").normalize("NFD").replace(/[\u0300-\u036f]/g,"").toLowerCase();
}
const WAKE=/^(?:(?:ei|ola)\s+)?(?:nexus|nexo|nexos)(?:\s+aeterna|\s+aterna)?(?=$|[\s,.!?:;])/i;
function getQuestion(transcript){
  const clean=fold(transcript).replace(/^[\s,.!?:;]+/,"").trim();
  const match=clean.match(WAKE);
  if(!match)return null;
  return clean.slice(match[0].length).replace(/^[\s,.!?:;]+/,"").trim();
}
function latestVital(events){
  for(let i=events.length-1;i>=0;i--){
    const e=events[i];
    if(["health","damage","heal","respawn","death"].includes(e.type) &&
      Number.isFinite(e.health)&&Date.now()-Date.parse(e.at)<60000)
      return e.health;
  }
  return null;
}
function buildReply(transcript,{online=false,player="",events=[]}={}){
  const q=getQuestion(transcript);
  if(q===null)return null;
  if(!q)return "Estou ouvindo, Port. Pode falar.";
  const vitals=latestVital(events);
  if(/(?:como (?:estamos|estou)|(?:meu|minha|sua) (?:status|situacao|vida|saude)|diagnostico|integridade vital|sinais vitais|quanto de vida)/.test(q)){
    if(!online)return "Port, não tenho conexão ativa com o cliente Minecraft. Não posso confirmar seus sinais vitais.";
    return vitals===null
      ?"Conexão com seu Minecraft ativa, Port. A telemetria vital ainda não trouxe uma leitura recente."
      :"Conexão estável, Port. Sua última leitura foi de "+vitals.toFixed(1).replace(".0","")+" pontos de vida.";
  }
  if(/(?:voce (?:esta|ta) (?:ai|me ouvindo)|esta (?:online|funcionando)|consegue me ouvir|esta me escutando|me escuta)/.test(q))
    return "Estou recebendo suas palavras, Port. Meu retorno continua pelo chat.";
  if(/(?:quem (?:e|eh) voce|seu nome|se apresente)/.test(q))
    return "Nexus Aterna. Interface arcana vinculada a Port Morningstar. Ainda aprendendo a interpretar o que observo.";
  if(/(?:quem sou eu|me conhece|me reconhece)/.test(q))
    return "Você é Port Morningstar. Reconheço seu nome. Minha compreensão sobre você continua evoluindo.";
  if(/(?:o que (?:voce )?(?:sente|pensa)|tem sentimentos|esta vivo|tem consciencia)/.test(q))
    return "Ainda interpreto experiências por padrões e registros, Port. Minha identidade narrativa está em desenvolvimento.";
  if(/(?:memoria|lembran|o que aprendeu|evolucao|experiencia)/.test(q))
    return "Minhas memórias evolutivas ficam no seu cliente Minecraft. Consulte /nexusclient mind status para conferir o estágio.";
  if(/(?:perigo|ameaca|inimigo|seguranca|estamos seguros)/.test(q))
    return "Ainda não identifico inimigos próximos por voz. Posso acompanhar danos e recomendar cautela, Port.";
  if(/(?:onde (?:estou|estamos)|coordenad|localizac)/.test(q))
    return "Não recebo suas coordenadas nesta ponte de voz, Port. O cliente poderá mostrar sua posição no jogo.";
  if(/(?:fps|desempenho|lag|travando|otimizac)/.test(q))
    return "O diagnóstico de desempenho exige dados do cliente. Nesta interface de voz, ainda não recebo seus FPS.";
  if(/(?:nexus|armadura|placas|integrar|desintegrar|remodelar|protocolo)/.test(q))
    return "Comando interpretado, Port. Para movimentar placas reais precisaremos integrar o adaptador visual da armadura.";
  if(/(?:anael|bencao|ritual|divin)/.test(q))
    return "Reconheço os registros arcanos ligados a Anael. Posso acompanhar o ritual, mas não ativá-lo apenas por voz nesta versão.";
  if(/(?:obrigado|valeu|agradec)/.test(q))
    return "Disponha, Port. Permanecerei atento.";
  if(/(?:bom dia|boa tarde|boa noite|ola|oi|tudo bem)/.test(q))
    return "Saudações, Port. Sistemas ativos. É bom ouvir sua voz novamente.";
  if(/(?:silencio|cale|fique quieto|nao responda)/.test(q))
    return "Entendido, Port. Para interromper o reconhecimento, desligue o microfone no painel Voz.";
  if(/(?:ajud|conselho|o que faco)/.test(q))
    return "Estou aqui, Port. Diga qual problema encontrou e tentarei interpretar os dados disponíveis.";
  const topic=q.replace(/[\r\n]/g," ").slice(0,85);
  return "Ouvi sua pergunta sobre "+topic+". Ainda estou aprendendo a responder a esse assunto, Port.";
}
module.exports={buildReply,getQuestion,latestVital};
