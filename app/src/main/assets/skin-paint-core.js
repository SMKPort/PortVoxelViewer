/* Port Voxel Viewer - deterministic pixel editing operations (no network) */
(function(root){'use strict';
function region(rect,w,h){
 if(!Array.isArray(rect)||rect.length!==4)throw Error('Face UV inválida');
 const x=Math.max(0,Math.floor(Math.min(rect[0],rect[2]))),y=Math.max(0,Math.floor(Math.min(rect[1],rect[3])));
 const right=Math.min(w,Math.ceil(Math.max(rect[0],rect[2]))),bottom=Math.min(h,Math.ceil(Math.max(rect[1],rect[3])));
 if(right<=x||bottom<=y)throw Error('Face fora da textura');
 return {x,y,w:right-x,h:bottom-y};
}
function pixelFromUV(u,v,width,height){
 if(!Number.isFinite(u)||!Number.isFinite(v)||u<0||v<0||u>1||v>1)return null;
 return {x:Math.min(width-1,Math.max(0,Math.floor(u*width))),
         y:Math.min(height-1,Math.max(0,Math.floor((1-v)*height)))};
}
function equalPixel(bytes,index,color){return bytes[index]===color[0]&&bytes[index+1]===color[1]&&bytes[index+2]===color[2]&&bytes[index+3]===color[3];}
function putPixel(image,x,y,color,size){
 const w=image.width,h=image.height,arr=image.data;
 const rad=Math.max(0,Math.floor((Math.max(1,size)-1)/2)),far=Math.max(0,Math.ceil((Math.max(1,size)-1)/2));
 let count=0;
 for(let dy=-rad;dy<=far;dy++)for(let dx=-rad;dx<=far;dx++){
  const xx=x+dx,yy=y+dy;if(xx<0||yy<0||xx>=w||yy>=h)continue;
  const pos=(yy*w+xx)*4;if(equalPixel(arr,pos,color))continue;
  arr.set(color,pos);count++;
 }
 return count;
}
function drawLine(image,x0,y0,x1,y1,color,size){
 let count=0;let dx=Math.abs(x1-x0),dy=Math.abs(y1-y0),sx=x0<x1?1:-1,sy=y0<y1?1:-1,err=dx-dy;
 for(let step=0;step<Math.max(image.width,image.height)*4+4;step++){
  count+=putPixel(image,x0,y0,color,size);if(x0===x1&&y0===y1)break;
  const e2=2*err;if(e2>-dy){err-=dy;x0+=sx;}if(e2<dx){err+=dx;y0+=sy;}
 }return count;
}
function floodFill(image,x,y,color,limit=2000000,area=null){
 const w=image.width,h=image.height,p=image.data;if(x<0||y<0||x>=w||y>=h)return 0;
 const bounds=area?region([area.x,area.y,area.x+area.w,area.y+area.h],w,h):{x:0,y:0,w,h};
 if(x<bounds.x||y<bounds.y||x>=bounds.x+bounds.w||y>=bounds.y+bounds.h)return 0;
 const offset=(y*w+x)*4,old=p.slice(offset,offset+4);
 if(old.every((n,i)=>n===color[i]))return 0;
 const queue=new Int32Array(bounds.w*bounds.h),marked=new Uint8Array(bounds.w*bounds.h);
 const local=(xx,yy)=>(yy-bounds.y)*bounds.w+(xx-bounds.x);
 let read=0,write=1,count=0;queue[0]=y*w+x;marked[local(x,y)]=1;
 while(read<write&&count<limit){
  const xy=queue[read++],px=xy%w,py=(xy-px)/w;
  const pos=xy*4;p.set(color,pos);count++;
  const candidates=[[px-1,py],[px+1,py],[px,py-1],[px,py+1]];
  for(const [nx,ny] of candidates){
   if(nx<bounds.x||ny<bounds.y||nx>=bounds.x+bounds.w||ny>=bounds.y+bounds.h)continue;
   const li=local(nx,ny);if(marked[li])continue;marked[li]=1;
   if(!equalPixel(p,(ny*w+nx)*4,old))continue;queue[write++]=ny*w+nx;
  }
 }
 return count;
}
function flipFace(image,rect,direction){
 const r=region(rect,image.width,image.height),arr=image.data,buf=new Uint8ClampedArray(r.w*r.h*4);
 for(let y=0;y<r.h;y++)for(let x=0;x<r.w;x++){
  const src=((r.y+y)*image.width+r.x+x)*4;
  const xx=direction==='horizontal'||direction==='rotate180'?r.w-1-x:x;
  const yy=direction==='vertical'||direction==='rotate180'?r.h-1-y:y;
  buf.set(arr.subarray(src,src+4),(yy*r.w+xx)*4);
 }
 for(let y=0;y<r.h;y++)for(let x=0;x<r.w;x++){
  const dst=((r.y+y)*image.width+r.x+x)*4;
  arr.set(buf.subarray((y*r.w+x)*4,(y*r.w+x)*4+4),dst);
 }
 return r;
}
function copyFace(image,srcRect,dstRect,mirrorX=false){
 const a=region(srcRect,image.width,image.height),b=region(dstRect,image.width,image.height);
 if(a.w!==b.w||a.h!==b.h)throw Error('Faces têm tamanhos diferentes');
 const part=new Uint8ClampedArray(a.w*a.h*4),bytes=image.data;
 for(let y=0;y<a.h;y++)for(let x=0;x<a.w;x++){
  const src=((a.y+y)*image.width+a.x+x)*4;
  part.set(bytes.subarray(src,src+4),(y*a.w+x)*4);
 }
 for(let y=0;y<b.h;y++)for(let x=0;x<b.w;x++){
  const xx=mirrorX?a.w-1-x:x,pos=((b.y+y)*image.width+b.x+x)*4;
  bytes.set(part.subarray((y*a.w+xx)*4,(y*a.w+xx)*4+4),pos);
 }return b;
}
root.PortSkinPaintCore={region,pixelFromUV,putPixel,drawLine,floodFill,flipFace,copyFace};
})(typeof window!=='undefined'?window:globalThis);
