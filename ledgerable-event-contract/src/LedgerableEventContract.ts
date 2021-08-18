/*
 * SPDX-License-Identifier: Apache-2.0
 */

import { Context, Contract, Info, Returns, Transaction } from 'fabric-contract-api';
import { LedgerableEvent } from './LedgerableEvent';
interface Exists {
    exists: boolean;
    data: Uint8Array;
}

const KEY_TYPE = 'EVENT';

@Info({ title: 'LedgerableEvent', description: 'Store ledgerable events' })
export class EventContract extends Contract {
    @Transaction(false)
    @Returns('boolean')
    public async exists(ctx: Context, eventId: string, subId: string): Promise<boolean> {
        const { exists } = await this._exists(ctx, eventId, subId);
        return exists;
    }

    private async _exists(ctx: Context, eventId: string, subId: string): Promise<Exists> {
        const key = ctx.stub.createCompositeKey(KEY_TYPE, [eventId, subId]);
        const data: Uint8Array = await ctx.stub.getState(key);
        const exists = !!data && data.length > 0;
        return { exists, data };
    }

    @Transaction()
    public async create(ctx: Context, event: LedgerableEvent): Promise<void> {
        console.log(`create::Ledgerable Event  ${JSON.stringify(event)}`);
        const { exists, data } = await this._exists(ctx, event.eventId, event.subId);

        const key = ctx.stub.createCompositeKey(KEY_TYPE, [event.eventId, event.subId]);
        let e: LedgerableEvent = event;
        if (exists) {
            const existingEvent: LedgerableEvent = JSON.parse(data.toString());
            existingEvent.logs.push(...event.logs);
            e = existingEvent;
        }

        const buffer: Buffer = Buffer.from(JSON.stringify(e));
        await ctx.stub.putState(key, buffer);
    }

    @Transaction(false)
    @Returns('LedgerableEvent[]')
    public async retrieve(ctx: Context, eventId: string): Promise<LedgerableEvent[]> {
        console.log(`retrieve:: event ${eventId}`);
        const events = [];

        const promiseOfIterator = ctx.stub.getStateByPartialCompositeKey(KEY_TYPE, [eventId]);
        for await (const res of promiseOfIterator) {
            const event: LedgerableEvent = JSON.parse(res.value.toString());
            events.push(event);
        }
        return events;
    }
}
