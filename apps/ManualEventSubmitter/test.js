// Generate stream of JSON

const crypto = require('crypto')
const randomString = crypto.randomBytes(8).toString("hex");
const got = require('got');

const delay = ms => new Promise(resolve => setTimeout(resolve, ms))

const iterations = 6;
const main = async () => {

    // Submit a set of transactions, and record the request ids.
    let txids = [];
    subEventsCount = 6;//Math.floor(Math.random()*6)+1;
    for (let i = 0; i < subEventsCount; i++) {
        now = Date.now();
        subId = `subid:${i}`;
        let event = { "eventId": randomString, "subId": subId, "logs": [{ "timestamp": now, "type": "starting", "dataHash": "thing-to-happen-hash" }] };
        console.log(event)

        if (i>0){
            previousSubId =`subid:${i-1}`; 
            let event = { "eventId": randomString, "subId": previousSubId, "logs": [{ "timestamp":now, "type": "ended", "dataHash": "thing-has-happened-hash" }] };
            console.log(event)
        }

        // const { body } = await got.post("http://localhost:4444/event", { json: event })
       // got.post("http://localhost:4444/event", { json: event });
        //  txids.push(body);
    }


    await delay(iterations * 1000);
    // check the response for each request id
    txids.forEach(async element =>  {
        const {body} = await got.get(`http://localhost:4444/status/${element}`); 
        console.log(`[${element}] ${body}`)
    });

    

}

main().catch(e => { console.log(e) })
