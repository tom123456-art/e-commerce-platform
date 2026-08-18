const i=[{id:1,label:"手机数码"},{id:2,label:"电脑办公"},{id:3,label:"智能家电"},{id:4,label:"居家生活"},{id:5,label:"运动户外"},{id:6,label:"影音娱乐"}];function t(e){const l=i.find(a=>a.id===Number(e));return l?l.label:"未分类"}function n(e){const l=(e==null?void 0:e.image)||"";return l&&!l.includes("placeholder")?l:`data:image/svg+xml;charset=UTF-8,${encodeURIComponent(`<svg width="336" height="256" xmlns="http://www.w3.org/2000/svg">
      <rect width="336" height="256" rx="28" fill="#E7F0FF"/>
      <text x="168" y="128" text-anchor="middle" fill="#4F7CFF" font-size="20">${(e==null?void 0:e.name)||"商品"}</text>
    </svg>`)}`}export{i as P,t as g,n as r};
