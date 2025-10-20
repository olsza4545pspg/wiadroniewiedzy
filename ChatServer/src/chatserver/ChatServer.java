import java.io.*;
import java.net.*;
import java.util.*;

public class ChatServer {
    private static final int PORT = 7070;
    private static final List<ClientHandler> clients = Collections.synchronizedList(new ArrayList<>());

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Serwer uruchomiony na porcie " + PORT + "...");
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Nowy klient podłączony: " + clientSocket.getInetAddress());
                ClientHandler handler = new ClientHandler(clientSocket);
                clients.add(handler);
                handler.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void broadcast(String message) {
        synchronized (clients) {
            for (ClientHandler client : clients) {
                client.sendMessage(message);
            }
        }
    }

    public static void sendPrivate(String sender, String recipient, String message) {
        synchronized (clients) {
            boolean found = false;
            for (ClientHandler client : clients) {
                if (client.getUsername().equals(recipient)) {
                    client.sendMessage("[Prywatna od " + sender + "]: " + message);
                    found = true;
                    break;
                }
            }
            if (!found) {
                getClientByUsername(sender).sendMessage("Błąd: Użytkownik " + recipient + " nie istnieje.");
            }
        }
    }

    public static void removeClient(ClientHandler handler) {
        synchronized (clients) {
            clients.remove(handler);
        }
        updateUserList();
    }

    public static void updateUserList() {
        StringBuilder userList = new StringBuilder("LISTA_UZYTKOWNIKOW:");
        synchronized (clients) {
            for (ClientHandler client : clients) {
                userList.append(client.getUsername()).append(",");
            }
        }
        broadcast(userList.toString());
    }

    public static boolean isUsernameTaken(String username) {
        synchronized (clients) {
            for (ClientHandler client : clients) {
                if (client.getUsername().equals(username)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static ClientHandler getClientByUsername(String username) {
        synchronized (clients) {
            for (ClientHandler client : clients) {
                if (client.getUsername().equals(username)) {
                    return client;
                }
            }
        }
        return null;
    }
}

class ClientHandler extends Thread {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String username;

    public ClientHandler(Socket socket) throws IOException {
        this.socket = socket;
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.out = new PrintWriter(socket.getOutputStream(), true);
    }

    public String getUsername() {
        return username;
    }

    public void sendMessage(String message) {
        if (out != null) {
            out.println(message);
        }
    }

    @Override
    public void run() {
        try {
            username = in.readLine().trim();
            System.out.println("Odebrano login: " + username);

            if (username.isEmpty()) {
                sendMessage("ERROR: Login nie może być pusty.");
                return;
            }
            if (ChatServer.isUsernameTaken(username)) {
                sendMessage("ERROR: Login zajęty.");
                return;
            }

            sendMessage("Witaj, " + username + "!");
            ChatServer.broadcast(username + " dołączył do czatu.");
            ChatServer.updateUserList();

            String message;
            while ((message = in.readLine()) != null) {
                if (message.equals("/quit")) {
                    break;
                } else if (message.startsWith("@")) {
                    int spaceIndex = message.indexOf(' ');
                    if (spaceIndex != -1) {
                        String recipient = message.substring(1, spaceIndex);
                        String privateMsg = message.substring(spaceIndex + 1);
                        ChatServer.sendPrivate(username, recipient, privateMsg);
                        sendMessage("[Prywatna do " + recipient + "]: " + privateMsg);
                    } else {
                        sendMessage("Błąd: Niepoprawny format (@login wiadomość).");
                    }
                } else {
                    ChatServer.broadcast("[" + username + "]: " + message);
                }
            }
        } catch (IOException e) {
            System.out.println("Błąd klienta " + username + ": " + e.getMessage());
        } finally {
            ChatServer.broadcast(username + " opuścił czat.");
            ChatServer.removeClient(this);
            try {
                socket.close();
            } catch (IOException e) {
                System.out.println("Błąd zamykania socketu.");
            }
        }
    }
}