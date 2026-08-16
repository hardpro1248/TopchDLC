package cc.snais;


import lombok.Getter;

@Getter
public class RPCData {
    private final String name;
    private final String avatar;
    private final long id;

    public RPCData(String name, long id, String avatar) {
        this.name = name;
        this.avatar = avatar;
        this.id = id;
    }
}
