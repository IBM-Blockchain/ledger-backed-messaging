// SPDX-License-Identifier: Apache-2.0

package org.ledgerable.adts;

public class LedgerableEvent {
    public static class Log {
        public String timestamp;
        public String type;
        public String dataHash;
    }
    public String eventId;
    public String subId;
    public Log[] logs;
}
