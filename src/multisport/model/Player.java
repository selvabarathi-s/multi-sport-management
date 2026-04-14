package multisport.model;

public class Player {
    private int playerId;
    private String name;
    private int age;
    private Integer teamId;

    public Player() {}

    public Player(String name, int age, Integer teamId) {
        this.name = name;
        this.age = age;
        this.teamId = teamId;
    }

    public Player(int playerId, String name, int age, Integer teamId) {
        this.playerId = playerId;
        this.name = name;
        this.age = age;
        this.teamId = teamId;
    }

    public int getPlayerId() { return playerId; }
    public void setPlayerId(int playerId) { this.playerId = playerId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }

    public Integer getTeamId() { return teamId; }
    public void setTeamId(Integer teamId) { this.teamId = teamId; }

    @Override
    public String toString() {
        return "Player{id=" + playerId + ", name='" + name + "', age=" + age + ", teamId=" + teamId + "}";
    }
}
