// weather-widget.js
(function () {
  // ----- 공통 유틸 -----
  const cities = [
    { name: '서울', fullName: '서울특별시', lat: 37.5665, lng: 126.9780 },
    { name: '부산', fullName: '부산광역시', lat: 35.1796, lng: 129.0756 },
    { name: '대구', fullName: '대구광역시', lat: 35.8714, lng: 128.6014 },
    { name: '인천', fullName: '인천광역시', lat: 37.4563, lng: 126.7052 },
    { name: '광주', fullName: '광주광역시', lat: 35.1595, lng: 126.8526 },
    { name: '대전', fullName: '대전광역시', lat: 36.3504, lng: 127.3845 },
    { name: '울산', fullName: '울산광역시', lat: 35.5384, lng: 129.3114 },
    { name: '세종', fullName: '세종특별자치시', lat: 36.4800, lng: 127.2890 },
    { name: '경기', fullName: '경기도', lat: 37.4138, lng: 127.5183 },
    { name: '강원', fullName: '강원도', lat: 37.8228, lng: 128.1555 },
    { name: '충북', fullName: '충청북도', lat: 36.8000, lng: 127.7000 },
    { name: '충남', fullName: '충청남도', lat: 36.5184, lng: 126.8000 },
    { name: '전북', fullName: '전라북도', lat: 35.7175, lng: 127.1530 },
    { name: '전남', fullName: '전라남도', lat: 34.8679, lng: 126.9910 },
    { name: '경북', fullName: '경상북도', lat: 36.4919, lng: 128.8889 },
    { name: '경남', fullName: '경상남도', lat: 35.4606, lng: 128.2132 },
    { name: '제주', fullName: '제주특별자치도', lat: 33.4996, lng: 126.5312 }
  ];

  const getIcon = (code) => ({
    '01':'☀️','02':'🌤️','03':'⛅','04':'☁️','09':'🌧️','10':'🌦️','11':'⛈️','13':'🌨️','50':'🌫️'
  }[code] || '🌤️');

  const clamp = (v, a, b) => Math.max(a, Math.min(b, v));

  // 간단 캠핑 지수
  function getCampingIndex(temp, humidity, wind, code){
    let s = 5;
    if (temp < 10 || temp > 30) s -= 2; else if (temp < 15 || temp > 25) s -= 1;
    if (humidity > 80 || humidity < 30) s -= 1;
    if (wind > 8) s -= 2; else if (wind > 5) s -= 1;
    if (/^(09|11)/.test(code)) s -= 2; else if (/^10/.test(code) || /^13/.test(code)) s -= 1;
    return clamp(s,1,5);
  }

  // 상세 지수
  function detailedScores(temp, humidity, wind, code){
    const scoreBlock = (score, max, details) => ({score, maxScore:max, details});
    const T = temp>=15&&temp<=25?scoreBlock(5,5,'캠핑 최적 온도'):
              ((temp>=10&&temp<15)||(temp>25&&temp<=30))?scoreBlock(3,5,'가능하나 준비 필요'):
              ((temp>=5&&temp<10)||(temp>30&&temp<=35))?scoreBlock(2,5,'다소 어려움'):
              scoreBlock(1,5,'매우 어려움');
    const H = (humidity>=40&&humidity<=70)?scoreBlock(5,5,'적절한 습도'):
              ((humidity>=30&&humidity<40)||(humidity>70&&humidity<=80))?scoreBlock(3,5,'다소 높거나 낮음'):
              scoreBlock(2,5,'습도 불리');
    const W = (wind<=5)?scoreBlock(5,5,'바람 적당'):
              (wind<=8)?scoreBlock(3,5,'다소 강함, 주의'):
              (wind<=12)?scoreBlock(2,5,'강풍, 재고 권장'):
              scoreBlock(1,5,'매우 강함, 피하세요');
    const WX = /^01/.test(code)?scoreBlock(5,5,'맑음 최적'):
               (/^(02|03)/.test(code))?scoreBlock(4,5,'구름 조금/많음 양호'):
               (/^04/.test(code))?scoreBlock(3,5,'흐림, 변화 주의'):
               (/^10/.test(code))?scoreBlock(2,5,'비, 재고'):
               (/^(09|11|13)/.test(code))?scoreBlock(1,5,'악천후, 피하세요'):scoreBlock(3,5,'보통');
    return {temperature:T, humidity:H, wind:W, weather:WX};
  }
  const overall = (ds) => {
    const sum = Object.values(ds).reduce((a,b)=>a+b.score,0);
    const max = Object.values(ds).reduce((a,b)=>a+b.maxScore,0);
    return Math.round((sum/max)*5);
  };
  const gradeInfo = (idx)=> idx>=4?{text:'매우 좋음',cls:'good'}
                      : idx>=3?{text:'좋음',cls:'ok'}
                      : idx>=2?{text:'보통',cls:'mid'}
                      : {text:'나쁨',cls:'bad'};

  // fetch with timeout
  async function fetchWeather(location){
    const controller = new AbortController();
    const to = setTimeout(()=>controller.abort(),15000);
    try{
      const res = await fetch(`/api/weather/current?location=${encodeURIComponent(location)}`, {signal:controller.signal});
      if(!res.ok){
        let msg=`날씨 API 호출 실패 (${res.status})`;
        try{
          const j=await res.json(); if(j?.error) msg=j.error;
        }catch{}
        throw new Error(msg);
      }
      return await res.json();
    } finally {
      clearTimeout(to);
    }
  }

  function selectNearestCityByLatLng(lat,lng){
    let best=cities[0], min=Infinity;
    const R=6371, toRad=(d)=>d*Math.PI/180;
    for(const c of cities){
      const dLat=toRad(c.lat-lat), dLng=toRad(c.lng-lng);
      const a=Math.sin(dLat/2)**2 + Math.cos(toRad(lat))*Math.cos(toRad(c.lat))*Math.sin(dLng/2)**2;
      const d=2*Math.atan2(Math.sqrt(a),Math.sqrt(1-a))*R;
      if(d<min){min=d;best=c;}
    }
    return best.name;
  }

  function renderSelect(current){
    const sel=document.createElement('select');
    sel.className='location-select';
    for(const c of cities){
      const opt=document.createElement('option');
      opt.value=c.name; opt.textContent=c.fullName;
      if(c.name===current) opt.selected=true;
      sel.appendChild(opt);
    }
    return sel;
  }

  function makeEl(tag, cls, text){
    const el=document.createElement(tag);
    if(cls) el.className=cls;
    if(text!=null) el.textContent=text;
    return el;
  }

  async function mountWeatherWidget(container, initial){
    let location=initial||'서울';
    const root=makeEl('div','weather-widget');
    const header=makeEl('div','weather-header');
    const h3=makeEl('h3',null,'🌤️ 실시간 날씨');
    const selWrap=makeEl('div','location-selector');
    const select=renderSelect(location);
    selWrap.appendChild(select);
    header.append(h3, selWrap);

    const current=makeEl('div','weather-current');
    const main=makeEl('div','weather-main');
    const icon=makeEl('div','weather-icon','');
    const info=makeEl('div','weather-info');
    const tempEl=makeEl('div','temperature','');
    const feels=makeEl('div','feels-like','');
    const desc=makeEl('div','description','');
    info.append(tempEl,feels,desc);
    main.append(icon,info);

    const idxWrap=makeEl('div','camping-index');
    const idxLabel=makeEl('div','index-label','캠핑 지수');
    const idxStars=makeEl('div','index-stars','');
    const idxText=makeEl('div','index-text','');
    idxWrap.append(idxLabel, idxStars, idxText);

    const details=makeEl('div','weather-details');
    const d1=makeEl('div','detail-item'); d1.append(makeEl('span','label','습도'), makeEl('span','value',''));
    const d2=makeEl('div','detail-item'); d2.append(makeEl('span','label','바람'), makeEl('span','value',''));
    details.append(d1,d2);
    current.append(details);

    const forecast=makeEl('div','weather-forecast');
    const fh4=makeEl('h4',null,'시간별 예보');
    const fItems=makeEl('div','forecast-items');
    forecast.append(fh4, fItems);

    const topRow = makeEl('div','weather-top-row');
    topRow.append(main, idxWrap, current);
    root.append(header, topRow, forecast);
    container.innerHTML='';
    container.appendChild(root);

    const loading = makeEl('div','weather-loading','날씨 정보를 불러오는 중…');
    container.appendChild(loading);

    async function load(){
      loading.style.display='block';
      try{
        const data = await fetchWeather(location);
        loading.style.display='none';
        // 채우기
        icon.textContent = getIcon(data.current.weather_code);
        tempEl.textContent = `${data.current.temp}°C`;
        feels.textContent  = `체감 ${data.current.feels_like}°C`;
        desc.textContent   = data.current.description || '';

        d1.querySelector('.value').textContent = `${data.current.humidity}%`;
        d2.querySelector('.value').textContent = `${data.current.wind_speed}m/s`;

        const ci = getCampingIndex(data.current.temp, data.current.humidity, data.current.wind_speed, data.current.weather_code);
        idxStars.textContent = '⭐'.repeat(ci);
        idxText.textContent  = ci>=4?'매우 좋음':ci>=3?'좋음':ci>=2?'보통':'나쁨';

        fItems.innerHTML='';
        (data.forecast||[]).forEach(item=>{
          const it=makeEl('div','forecast-item');
          it.append(
            makeEl('span','forecast-time',item.time),
            makeEl('span','forecast-icon', getIcon(item.weather_code)),
            makeEl('span','forecast-temp', `${item.temp}°C`)
          );
          fItems.appendChild(it);
        });
      } catch(e){
        loading.textContent = (e && e.message) ? e.message : '날씨 정보를 불러오지 못했습니다.';
      }
    }

    select.addEventListener('change', (e)=>{ location=e.target.value; load(); });
    await load();
    // 5분마다 갱신
    setInterval(load, 300000);
  }

  async function mountCampingIndexCalculator(container){
    // 간단 버전: 위젯과 같은 데이터 재활용 + 상세막대
    const wrap = makeEl('div','camping-index-calculator');
    const header = makeEl('div','calculator-header');
    header.append(makeEl('h3',null,'🏕️ 실시간 캠핑 지수'));
    const selWrap = makeEl('div','location-selector'), select=renderSelect('서울');
    selWrap.appendChild(select); header.appendChild(selWrap);
    const cur = makeEl('div','current-weather');
    const wMain=makeEl('div','weather-main'), wIcon=makeEl('div','weather-icon'), wInfo=makeEl('div','weather-info');
    const wTemp=makeEl('div','temperature'), wDesc=makeEl('div','description');
    wInfo.append(wTemp,wDesc); wMain.append(wIcon,wInfo); cur.appendChild(wMain);

    const idxBox = makeEl('div','camping-index-display');
    const idxHdr = makeEl('div','index-header');
    const idxScore = makeEl('div','index-score');
    const idxGrade = makeEl('div','index-grade');
    idxHdr.append(makeEl('h4',null,'캠핑 지수'), idxScore, idxGrade);

    const detailWrap = makeEl('div','detailed-scores');
    detailWrap.appendChild(makeEl('h5',null,'상세 분석'));

    const itemsWrap = makeEl('div','score-items');
    function mkBar(label){
      const item=makeEl('div','score-item');
      item.append(makeEl('div','score-label',label));
      const bar=makeEl('div','score-bar'); const fill=makeEl('div','score-fill'); bar.appendChild(fill);
      const hint=makeEl('div','score-details'); item.append(bar,hint);
      return {item,fill,hint};
    }
    const bT=mkBar('온도'), bH=mkBar('습도'), bW=mkBar('바람'), bWX=mkBar('날씨');
    itemsWrap.append(bT.item,bH.item,bW.item,bWX.item);
    detailWrap.appendChild(itemsWrap);

    const tips = makeEl('div','camping-tips'); tips.append(makeEl('h5',null,'캠핑 팁'), makeEl('div','tips-content'));

    idxBox.append(idxHdr, detailWrap, tips);
    wrap.append(header, cur, idxBox);
    container.innerHTML=''; container.appendChild(wrap);

    const loading = makeEl('div','loading','캠핑 지수 계산 중…'); container.appendChild(loading);

    async function load(loc){
      loading.style.display='block';
      try{
        const data = await fetchWeather(loc);
        loading.style.display='none';
        wIcon.textContent = getIcon(data.current.weather_code);
        wTemp.textContent = `${data.current.temp}°C`;
        wDesc.textContent = data.current.description || '';

        const ds = detailedScores(data.current.temp, data.current.humidity, data.current.wind_speed, data.current.weather_code);
        const ov = overall(ds);
        const g  = gradeInfo(ov);
        idxScore.textContent = '⭐'.repeat(ov);
        idxGrade.textContent = g.text;
        idxScore.className = `index-score ${g.cls}`; idxGrade.className = `index-grade ${g.cls}`;

        const setBar = (bar, s)=>{
          const pct = (s.score/s.maxScore)*100;
          bar.fill.style.width = `${pct}%`;
          bar.fill.style.backgroundColor = s.score>=4?'#4CAF50' : s.score>=2?'#FFC107' : '#F44336';
          bar.hint.textContent = s.details;
        };
        setBar(bT, ds.temperature); setBar(bH, ds.humidity); setBar(bW, ds.wind); setBar(bWX, ds.weather);

        tips.querySelector('.tips-content').textContent =
          ov>=4?'🎉 최적의 날씨! 즐캠!':
          ov>=3?'👍 가능, 준비물 점검!':
          ov>=2?'⚠️ 다소 어려움, 변화 주의':
                '❌ 오늘은 피하는 게 좋아요';
      } catch(e){
        loading.textContent = (e&&e.message)?e.message:'데이터를 불러오지 못했습니다.';
      }
    }

    select.addEventListener('change', e=>load(e.target.value));
    await load(select.value);
    setInterval(()=>load(select.value), 300000);
  }

  // 자동 마운트: detail.html에 있는 컨테이너를 찾아서 장착
  document.addEventListener('DOMContentLoaded', () => {
    const weatherBox = document.getElementById('weather-widget-container');
    if (weatherBox) {
      const initial = (window.campingLocation // detail.html에서 주입한 값
                      || (window.mapY && window.mapX
                          ? selectNearestCityByLatLng(parseFloat(window.mapY), parseFloat(window.mapX))
                          : '서울'));
      mountWeatherWidget(weatherBox, initial);
    }
    const idxBox = document.getElementById('camping-index-calculator-container');
    if (idxBox) mountCampingIndexCalculator(idxBox);
  });
})();