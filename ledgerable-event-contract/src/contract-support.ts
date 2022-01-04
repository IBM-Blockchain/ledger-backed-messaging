import { Context, Contract, Info, Transaction } from 'fabric-contract-api';

@Info({ title: 'ContractSupport', description: 'Utilties to support the contract' })
export class ContractSupport extends Contract {
    @Transaction(false)
    public async setLogLevel(ctx: Context, loglevel: string): Promise<void> {
        const logger = ctx.logging.setLevel(loglevel);
    }

    @Transaction(false)
    public async ping(ctx: Context, text: string): Promise<string> {
        return `ping[${text}]`;
    }

    @Transaction(true)
    public async set(ctx: Context, key: string, value: string): Promise<void> {
        const buffer: Buffer = Buffer.from(JSON.stringify(value));
        const r = await ctx.stub.getState(key);
        await ctx.stub.putState(key, buffer);
    }

    @Transaction(true)
    public async fail(ctx: Context, text: string): Promise<void> {
        throw new Error(text);
    }
}
