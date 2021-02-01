package org.acme;

public class Message {
    
    public String to;
    public String from;
    public String uuid;
    public String contents;

    public Message(){

    }

    public Message(String to, String from, String uuid, String contents){
        this.to=to;
        this.from=from;
        this.uuid=uuid;
        this.contents=contents;
    }
}
