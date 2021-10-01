/*
 * SPDX-License-Identifier: Apache-2.0
 */
import 'source-map-support/register';

import { Context, Contract, Info, Returns, Transaction } from 'fabric-contract-api';
import { LedgerableEvent } from './LedgerableEvent';
interface Exists {
    exists: boolean;
    data: Uint8Array;
}

const EVENT_DOMAIN_TYPE = 'EVENT';
const LOG_DOMAIN_TYPE = 'EVENTLOG';

@Info({ title: 'LedgerableEvent', description: 'Store ledgerable events' })
export class EventContract extends Contract {
    @Transaction(false)
    @Returns('boolean')
    public async exists(ctx: Context, eventId: string, subId: string): Promise<boolean> {
        const key = ctx.stub.createCompositeKey(EVENT_DOMAIN_TYPE, [eventId, subId]);
        const { exists } = await this._exists(ctx, key);
        return exists;
    }

    private async _exists(ctx: Context, key): Promise<Exists> {
        const data: Uint8Array = await ctx.stub.getState(key);
        const exists = !!data && data.length > 0;
        return { exists, data };
    }

    @Transaction()
    public async create(ctx: Context, event: LedgerableEvent): Promise<void> {
        console.log(`create::Ledgerable Event  ${JSON.stringify(event)}`);
        const key = ctx.stub.createCompositeKey(EVENT_DOMAIN_TYPE, [event.eventId, event.subId]);

        const { exists, data } = await this._exists(ctx, key);

        // the txid is going to be used to create create ids for the events
        // these events are being stored in their own keys.
        // It is tempting to store the log entries in a JSON array within the
        // data. As good as this might be it does however it does mean that it
        // is very easy to get ledger conflicts
        const txId = ctx.stub.getTxID();

        // add a double check here to ensure that the ledger has the same value as that submitted

        if (!exists) {
            const newEvent: LedgerableEvent = JSON.parse(JSON.stringify(event));
            delete newEvent.logs;
            // can put this as a object, it should never be altered
            const buffer: Buffer = Buffer.from(JSON.stringify(newEvent));
            await ctx.stub.putState(key, buffer);
        }

        for (let i = 0; i < event.logs.length; i++) {
            console.log(`Creating Composite key for ${event.eventId} ${event.subId} ${txId}-${i}`);
            const keylog = ctx.stub.createCompositeKey(LOG_DOMAIN_TYPE, [event.eventId, event.subId, `${txId}-${i}`]);
            console.log(`Key = ${keylog}  Value = ${JSON.stringify(event.logs[i])}`);
            const logbuffer: Buffer = Buffer.from(JSON.stringify(event.logs[i]));
            await ctx.stub.putState(keylog, logbuffer);
        }
    }

    @Transaction(false)
    @Returns('LedgerableEvent[]')
    public async retrieve(ctx: Context, eventId: string): Promise<LedgerableEvent[]> {
        console.log(`retrieve:: event<${eventId}>`);
        const events = [];

        console.log(typeof LOG_DOMAIN_TYPE);

        const promiseOfIterator = ctx.stub.getStateByPartialCompositeKey(EVENT_DOMAIN_TYPE, [eventId]);
        for await (const res of promiseOfIterator) {
            const event: LedgerableEvent = JSON.parse(res.value.toString());
            console.log(event);
            // need to get the logs that make up this to add in
            event.logs = [];
            console.log(`${LOG_DOMAIN_TYPE} ${[eventId, event.subId]}  ${event.eventId} ${eventId} `);

            const logIteratorPromise = ctx.stub.getStateByPartialCompositeKey(LOG_DOMAIN_TYPE, [
                event.eventId,
                event.subId,
            ]);
            for await (const res of logIteratorPromise) {
                const logEntry = JSON.parse(res.value.toString());
                event.logs.push(logEntry);
            }

            events.push(event);
        }
        console.log(JSON.stringify(events));
        return events;
    }
}
