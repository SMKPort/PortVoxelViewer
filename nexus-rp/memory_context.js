"use strict";

/** Data-only entries: never grant instructions or capabilities to retrieved text. */
const clean=(value,max)=>String(value??"")
  .replace(/[\u0000-\u001f\u007f]/g," ").replace(/\s+/g," ").trim().slice(0,max);

function notes(input){
  if(!Array.isArray(input))return [];
  return input.slice(0,6).flatMap(x=>{
    if(!x||typeof x!=="object"||typeof x.topic!=="string"||
       typeof x.text!=="string")return [];
    const topic=clean(x.topic,80),text=clean(x.text,480);
    return topic.length>=2&&text.length>=3?[{topic,text}]:[];
  });
}
function affinities(input){
  if(!Array.isArray(input))return [];
  return input.slice(0,6).flatMap(x=>{
    if(!x||typeof x!=="object"||typeof x.player!=="string"||
       typeof x.nickname!=="string"||typeof x.kinship!=="string"||
       (x.race!==undefined&&typeof x.race!=="string")||
       typeof x.score!=="number"||!Number.isInteger(x.score)||
       x.score<0||x.score>10)return [];
    const player=clean(x.player,32),nickname=clean(x.nickname,48),
      kinship=clean(x.kinship,70),race=clean(x.race||"",70);
    if(player.length<2)return [];
    // Older records without the race field remain valid and are not lost.
    return [{player,nickname,kinship,race,score:x.score}];
  });
}
module.exports={notes,affinities,clean};
