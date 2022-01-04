// Generate stream of JSON

const crypto = require('crypto')
const util = require('util')
const got = require('got');
const { send } = require('process');
const fs = require('fs');

const delay = ms => new Promise(resolve => setTimeout(resolve, ms))

const iterations = 6;
let logStream;
let txIds= [];
const main = async () => {

    fs.writeFileSync('AppReqIds.log','');
    logStream = fs.createWriteStream('AppReqIds.log', {flags: 'a'});

    let x = 0;
     while (x<20000) {
         try {
            await sendEventStream();
            // const { body } = got.get("http://localhost/api/ledger/system/mvcc")
         } catch (e) {
             console.log(e.message);
         }
         x++;
        // await delay(500);
     }

     
        logStream.write(`${JSON.stringify(txIds)}\n`);
     
     logStream.end()
    // for (let index = 0; index < iterations; index++) {
    //     await sendEvent();


    // }
}

const sendEvent = async () => {
    let randomString = crypto.randomBytes(8).toString("hex");
    // Submit a set of transactions, and record the request ids.
    let txids = [];
    subEventsCount = 6;//Math.floor(Math.random()*6)+1;
    for (let i = 0; i < subEventsCount; i++) {
        now = Date.now();
        subId = `subid:${i}`;
        let event = { "eventId": randomString, "subId": subId, "logs": [{ "timestamp": now, "type": "starting", "dataHash": "thing-to-happen-hash" }] };
        console.log(event)

        if (i > 0) {
            previousSubId = `subid:${i - 1}`;
            let event = { "eventId": randomString, "subId": previousSubId, "logs": [{ "timestamp": now, "type": "ended", "dataHash": "thing-has-happened-hash" }] };
            console.log(event)
        }

        const { body } = await got.post("http://localhost/api/ledger/event", { json: event })
        console.log("Sent tx:: "+body);
        txids.push(body);
    }

    console.log("------------------------------------------------------------------------")
    await delay(iterations * 1000);
    // check the response for each request id
    txids.forEach(async element => {
        const { body } = await got.get(`http://localhost/status/${element}`);
        console.log(`[${element}] ${body}`)
    });



}


const sendEventStream = async () => {
    let randomString = crypto.randomBytes(8).toString("hex");
    // Submit a set of transactions, and record the request ids.
    subEventsCount = 4;// Math.floor(Math.random()*6)+1;
    for (let i = 0; i < subEventsCount; i++) {
        now = Date.now();
        subId = `subid:${i}`;
        let event = { "eventId": randomString, "subId": subId, "logs": [{ "timestamp": now, "type": "starting", "dataHash": "thing-to-happen-hash" }] };
        console.log(event)

        if (i > 0) {
            previousSubId = `subid:${i - 1}`;
            let event = { "eventId": randomString, "subId": previousSubId, "logs": [{ "timestamp": now, "type": "ended", "dataHash": "thing-has-happened-hash" }] };
        }

        const { body } = await got.post("http://localhost/api/ledger/event", { json: event })
        console.log("Sent "+ body)
        txIds.push({"appreqid":body,"eventId":randomString});

    }


}

main().catch(e => { console.log(e) })
