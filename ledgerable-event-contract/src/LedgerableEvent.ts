/*
 * SPDX-License-Identifier: Apache-2.0
 */

/*

{"eventId":"000001","subId":"aaaaaa","type":"arrived","dataHash":"0xCAFEBABE"}

*/
import { Object as DataType, Property } from 'fabric-contract-api';

@DataType()
export class LedgerableEventLogEntry {
    @Property()
    public timestamp: string;

    @Property()
    public type: string;

    @Property()
    public dataHash: string;
}

@DataType()
export class LedgerableEvent {
    @Property()
    public eventId: string;

    @Property()
    public subId: string;

    @Property('logs', 'LedgerableEventLogEntry[]')
    public logs: LedgerableEventLogEntry[];
}
