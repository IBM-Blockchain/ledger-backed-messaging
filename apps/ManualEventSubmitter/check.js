
const MongoClient = require('mongodb').MongoClient;
const fs = require('fs');
var url = "mongodb://adminuser:password123@localhost:27017";

let appreqids = JSON.parse(fs.readFileSync("./AppReqIds.log"));

const client = new MongoClient(url);

const main = async () => {
    try {
        // Connect the client to the server
        await client.connect();
        // Establish and verify connection

        const database = client.db("lm");
        const requests = database.collection("apprequest");


        // {
        //     _id: new ObjectId("618cfd578f9733420ec32edf"),
        //     appRequestId: 'AppReqId::af8fa9ee-fb96-455a-9413-2214483a5c58',
        //     b96f086d95bc4e2994ebcac1164355c5f53c499676b31024eeb95cbbc6744c70: 'VALID',
        //     result: 'b96f086d95bc4e2994ebcac1164355c5f53c499676b31024eeb95cbbc6744c70',
        //     events: [
        //       {
        //         appReqId: 'AppReqId::af8fa9ee-fb96-455a-9413-2214483a5c58',
        //         eventId: 'c1d3dc556bd5ed83',
        //         eventName: null,
        //         fabricTxId: 'b96f086d95bc4e2994ebcac1164355c5f53c499676b31024eeb95cbbc6744c70'
        //       }
        //     ]
        //   }

        // query for movies that have a runtime less than 15 minutes
        const options = { };

        let eventsCorrect=0;
        let txCorrect=0;
        for (let index = 0; index < appreqids.length; index++) {
            const id = appreqids[index].appreqid;
            console.log(id);
            query = { "appRequestId": id }
            const cursor = requests.find(query, options);
            // print a message if no documents were found
            if ((await cursor.count()) === 0) {
                console.log("No documents found!");
            }
            // replace console.dir with your callback to access individual elements
            await cursor.forEach((x=>{
                if (x.result){
                    console.log(`Transaction result is ${x[x.result]}`);
                    txCorrect++;
                } else {
                    console.log("!! No result set")
                }

                if (x.events){
                    console.log(`Event size is ${x.events.length}`)
                    eventsCorrect++;
                } else {
                    console.log("!! No events")
                }
                console.log()
                // console.log(x)
            }));
        }

        //        
        console.log(`Found ${appreqids.length} submitted requests with ${eventsCorrect} correct events, ${txCorrect} transactions correct`)

    } finally {
        // Ensures that the client will close when you finish/error
        await client.close();
    }
}


main().catch(console.dir)

// MongoClient.connect(url, function(err, db) {
//     if (err) throw err;
//     var dbo = db.db("lm");


//     appreqids.forEach(a =>{
//         query = { "appRequestId" :a}

//         dbo.collection("apprequest").find(query).toArray((err, result) => {
//           if (err) throw err;
//           console.log(result);
//         //   db.close();
//         });

//     })

// });
