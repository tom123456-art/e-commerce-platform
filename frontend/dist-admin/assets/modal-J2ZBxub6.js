function i(t){if(!t)return"";const e=document.createElement("div");return e.textContent=t,e.innerHTML}function p(){const t=document.createElement("div");t.style.cssText="position:fixed;top:0;left:0;width:100%;height:100%;background:rgba(0,0,0,0.45);z-index:9999;display:flex;align-items:center;justify-content:center;";const e=document.createElement("div");return e.style.cssText="background:#fff;border-radius:16px;padding:28px 32px;max-width:420px;width:90%;box-shadow:0 20px 60px rgba(0,0,0,0.2);font-family:system-ui,-apple-system,sans-serif;",t.appendChild(e),document.body.appendChild(t),{overlay:t,modal:e}}function u(t,e={}){return new Promise(a=>{const{overlay:n,modal:r}=p(),f=e.title||"操作确认";r.innerHTML=`
      <h3 style="margin:0 0 14px 0;font-size:18px;font-weight:700;color:#1e293b;">${i(f)}</h3>
      <p style="margin:0 0 22px 0;font-size:15px;color:#475569;line-height:1.6;">${i(t)}</p>
      <div style="display:flex;gap:12px;justify-content:flex-end;">
        <button class="modal-cancel-btn" style="padding:10px 22px;border-radius:10px;border:1px solid #cbd5e1;background:#fff;color:#475569;font-size:14px;font-weight:600;cursor:pointer;">${i(e.cancelText||"取消")}</button>
        <button class="modal-confirm-btn" style="padding:10px 22px;border-radius:10px;border:none;background:#4f7cff;color:#fff;font-size:14px;font-weight:600;cursor:pointer;">${i(e.confirmText||"确认")}</button>
      </div>
    `;const d=r.querySelector(".modal-confirm-btn"),s=r.querySelector(".modal-cancel-btn"),o=l=>{document.body.removeChild(n),a(l)};d.addEventListener("click",()=>o(!0)),s.addEventListener("click",()=>o(!1)),n.addEventListener("click",l=>{l.target===n&&o(!1)});const c=l=>{l.key==="Escape"&&(o(!1),document.removeEventListener("keydown",c))};document.addEventListener("keydown",c),d.focus()})}function x(t,e={}){return new Promise(a=>{const{overlay:n,modal:r}=p(),f=e.title||"提示";r.innerHTML=`
      <h3 style="margin:0 0 14px 0;font-size:18px;font-weight:700;color:#1e293b;">${i(f)}</h3>
      <p style="margin:0 0 22px 0;font-size:15px;color:#475569;line-height:1.6;">${i(t)}</p>
      <div style="display:flex;justify-content:flex-end;">
        <button class="modal-ok-btn" style="padding:10px 22px;border-radius:10px;border:none;background:#4f7cff;color:#fff;font-size:14px;font-weight:600;cursor:pointer;">${i(e.buttonText||"确定")}</button>
      </div>
    `;const d=r.querySelector(".modal-ok-btn"),s=()=>{document.body.removeChild(n),a()};d.addEventListener("click",s),n.addEventListener("click",c=>{c.target===n&&s()});const o=c=>{c.key==="Escape"&&(s(),document.removeEventListener("keydown",o))};document.addEventListener("keydown",o),d.focus()})}export{x as a,u as c};
