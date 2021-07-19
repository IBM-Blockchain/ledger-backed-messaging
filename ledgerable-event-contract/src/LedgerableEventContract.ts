/*
 * SPDX-License-Identifier: Apache-2.0
 */

import { Context, Contract, Info, Returns, Transaction } from 'fabric-contract-api';
import { LedgerableEvent } from './LedgerableEvent';

@Info({ title: 'MyAssetContract', description: 'My Smart Contract' })
export class EventContract extends Contract {
    @Transaction(false)
    @Returns('boolean')
    public async exists(ctx: Context, eventId: string, subId: string): Promise<boolean> {
        const key = ctx.stub.createCompositeKey('EVENT', [eventId, subId]);
        const data: Uint8Array = await ctx.stub.getState(key);
        return !!data && data.length > 0;
    }

    @Transaction()
    public async create(ctx: Context, event: LedgerableEvent): Promise<void> {
        const exists: boolean = await this.exists(ctx, event.eventId, event.subId);
        if (exists) {
            throw new Error(`${event.eventId}/${event.subId} already exists`);
        }
        const key = ctx.stub.createCompositeKey('EVENT', [event.eventId, event.subId]);
        const buffer: Buffer = Buffer.from(JSON.stringify(event));
        await ctx.stub.putState(key, buffer);
    }

    @Transaction(false)
    @Returns('LedgerableEvent[]')
    public async retrieve(ctx: Context, eventId: string): Promise<LedgerableEvent[]> {
        // const exists: boolean = await this.exists(ctx, eventId);
        // if (!exists) {
        //     throw new Error(`${eventId} does not exist`);
        // }

        const events = [];

        const promiseOfIterator = ctx.stub.getStateByPartialCompositeKey('EVENT', [eventId]);
        for await (const res of promiseOfIterator) {
            const event: LedgerableEvent = JSON.parse(res.value.toString());
            events.push(event);
        }
        return events;
    }
}
