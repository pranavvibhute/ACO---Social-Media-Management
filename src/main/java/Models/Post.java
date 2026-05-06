
package Models;
public class Post {
    private int id;
    private ContentType type; // Reel, Meme, Educational

    public Post(int id, ContentType type) {
        this.id = id;
        this.type = type;
    }

    public int getId() {
        return id;
    }

    public ContentType getType() {
        return type;
    }

    @Override
    public String toString() {
        return "Post " + id + " (" + type + ")";
    }
}