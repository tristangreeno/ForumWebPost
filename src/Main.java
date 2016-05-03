import spark.ModelAndView;
import spark.Session;
import spark.Spark;
import spark.template.mustache.MustacheTemplateEngine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.*;

/**
 * Created by johnjastrow on 4/15/16.
 */
public class Main {
    static HashMap<String, User> users = new HashMap<>();
    static ArrayList<Message> messages = new ArrayList<>();

    static Message findMessage(String keyword){
        Message message = null;
        Pattern p = Pattern.compile(keyword);

        for(Message m : messages){
            Matcher matcher = p.matcher(m.text);
            if(matcher.find()){
                message = m;
            }
        }

        return message;
    }

    public static void main(String[] args) {
        addTestUsers();
        addTestMessages();

        Spark.init();

        Spark.get(
                "/",
                (request, response) -> {
                    String replyId = request.queryParams("replyId");
                    int replyIdNum = -1;
                    if (replyId != null) {
                        replyIdNum = Integer.valueOf(replyId);
                    }

                    HashMap m = new HashMap();
                    ArrayList<Message> threads = new ArrayList<>();
                    for (Message message : messages) {
                        if (message.replyId == replyIdNum) {
                            threads.add(message);
                        }
                    }

                    Session session = request.session();
                    String userName = session.attribute("userName");
                    String searchResult = session.attribute("searchResult");

                    m.put("messages", threads);
                    m.put("userName", userName);
                    m.put("replyId", replyIdNum);
                    m.put("searchResult", searchResult);
                    return new ModelAndView(m, "home.html");
                },
                new MustacheTemplateEngine()
        );

        Spark.post(
                "/find-message",
                (request, response) -> {
                    String keyword = request.queryParams("keyword");

                    if(keyword == null){
                        throw new Exception("Keyword is null");
                    }

                    Message result = findMessage(keyword);
                    Session s = request.session();
                    s.attribute("searchResult", result.text);

                    response.redirect("/");
                    return "";
                }
        );

        Spark.post(
                "/create-message",
                (request, response) -> {
                    String text = request.queryParams("messageText");
                    Session s = request.session();
                    String username = s.attribute("userName");
                    Integer replyID = Integer.parseInt(request.queryParams("replyId"));
                    if(username == null){
                        throw new Exception("Username is null");
                    }

                    if (text == null){
                        throw new Exception("Text is null");
                    }

                    Message m = new Message(messages.size(), replyID, username, text);
                    messages.add(m);

                    // Go back to origin
                    response.redirect(request.headers("Referer"));
                    return "";
                }
        );

        Spark.post(
                "/login",
                (request, response) -> {
                    String userName = request.queryParams("loginName");
                    if (userName == null) {
                        throw new Exception("Login name not found.");
                    }

                    User user = users.get(userName);
                    if (user == null) {
                        user = new User(userName);
                        users.put(userName, user);
                    }

                    Session session = request.session();
                    session.attribute("userName", userName);

                    response.redirect("/");
                    return "";
                }
        );

        Spark.post(
                "/logout",
                (request, response) -> {
                    Session session = request.session();
                    session.invalidate();
                    response.redirect("/");
                    return "";
                }
        );
    }

    static void addTestUsers() {
        users.put("Alice", new User("Alice"));
        users.put("Bob", new User("Bob"));
        users.put("Charlie", new User("Charlie"));
    }

    static void addTestMessages() {
        messages.add(new Message(0, -1, "Alice", "Hello world!"));
        messages.add(new Message(1, -1, "Bob", "This is another thread!"));
        messages.add(new Message(2, 0, "Charlie", "Cool thread, Alice."));
        messages.add(new Message(3, 2, "Alice", "Thanks"));
    }
}
