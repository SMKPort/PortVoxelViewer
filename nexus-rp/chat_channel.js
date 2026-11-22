"use strict";

/**
 * A site-configured Minecraft chat channel is a fixed command prefix followed
 * by a message. Never take command text from Gemini or append unchecked syntax.
 * Minecraft still enforces all server permissions.
 */
const BLOCKED_ROOTS=new Set([
  "op","deop","ban","ban-ip","banlist","pardon","pardon-ip","kick",
  "stop","restart","reload","save-all","save-off","save-on","whitelist",
  "give","clear","effect","attribute","gamemode","defaultgamemode",
  "difficulty","gamerule","setblock","fill","clone","summon","kill",
  "tp","teleport","spreadplayers","execute","function","data","item",
  "loot","place","setworldspawn","spawnpoint","worldborder","weather",
  "time","experience","xp","advancement","bossbar","schedule","debug",
  "jfr","forceload","locate","seed","team","scoreboard","tag",
  "tellraw","title","playsound","stopsound","particle","say",
  "msg","tell","w","r","reply","minecraft:me","help","list",
  "permission","permissions","lp","luckperms","pex","promote","demote",
  "sudo","runas","console","rcon","pex","plugin","plugins",
  "bukkit:plugins","version","bukkit:version"
]);
const CHANNELS=new Set(["local","global","custom"]);
const FORMAT=/^(?:&[0-9a-fk-or]){0,8}$/;
const PREFIX=/^[a-z0-9_.:-]+(?: [a-z0-9_.:-]+){0,2}$/;

function validateCustomCommand(value){
  if(typeof value!=="string")throw Error("Informe o comando personalizado.");
  // One optional leading slash; no placeholders, quotes, selectors, pipes or
  // separators. Extra fixed subcommands are allowed for server custom routes.
  let candidate=value.trim().toLowerCase();
  if(candidate.startsWith("/"))candidate=candidate.slice(1);
  if(!candidate||candidate.length>64||!PREFIX.test(candidate))
    throw Error("Comando personalizado inválido. Ex.: /rp, /f chat ou /somake:rp. Não coloque a mensagem no campo.");
  const root=candidate.split(" ")[0];
  const bare=root.split(":").pop();
  if(BLOCKED_ROOTS.has(root)||BLOCKED_ROOTS.has(bare))
    throw Error("Esse comando não pode ser usado como canal de chat.");
  return candidate;
}
function route(data={}){
  const channel=data.channel===undefined?"local":data.channel;
  if(!CHANNELS.has(channel))throw Error("Canal de chat inválido.");
  const format=channel==="local"?
    (data.format===undefined?"&6&l":String(data.format||"").trim().toLowerCase()):"";
  if(!FORMAT.test(format))throw Error("Código de formatação inválido. Exemplo: &6&l.");
  const command=channel==="local"?"l":
    channel==="custom"?validateCustomCommand(data.command):"";
  return {channel,command,format};
}
module.exports={route,validateCustomCommand};
