const fs = require('fs')

let appReqIds = JSON.parse(fs.readFileSync('./apps/ManualEventSubmitter/AppReqIds.log'))

let results = JSON.parse(fs.readFileSync('one.json'));
let d = {}
results.forEach(a =>{
    d[a.appRequestId]=a;
})

results = JSON.parse(fs.readFileSync('two.json'));
results.forEach(a => {
    if (d[a.appRequestId]){
        let appreq =  d[a.appRequestId];
        let newEvts = appreq.events.concat(a.events)
       
        d[a.appRequestId].events=newEvts;// console.log(a.events)
        // d[a.appRequestId].events= oldEvents.concat(a.events)
    } else {
        d[a.appRequestId]=a;  
    }
})

console.log(d)


appReqIds.forEach(e=>{
    console.log(`For ${e.appreqid}  results are ${JSON.stringify(d[e.appreqid].txAttempts)}`)
})