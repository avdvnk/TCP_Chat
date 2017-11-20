package server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class Server {
    private static ConcurrentHashMap<String, Socket> clients = new ConcurrentHashMap<>();
    private static Logger logger = Logger.getLogger(Server.class.getName());
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        logger.info("Port to listen: ");
        int port = Integer.parseInt(scanner.nextLine());
        logger.info("Server running." + " Waiting clients...");
        try(ServerSocket serverSocket = new ServerSocket(port)) {
            while(true) {
                Socket socket = serverSocket.accept();
                if(socket.isClosed()) {
                    break;
                }
                ClientThread clientThread = new ClientThread(socket);
                clientThread.start();
            }
        } catch (IOException e) {
            logger.info(e.getMessage());
        }
    }

    private static class ClientThread extends Thread {
        private String name;
        private Socket socket;
        private DataInputStream inputStream;
        private DataOutputStream outputStream;
        ClientThread(Socket socket) {
            this.socket = socket;
            logger.info("Connect user: " + socket.getInetAddress().getHostAddress());
            try {
                inputStream = new DataInputStream(socket.getInputStream());
                outputStream = new DataOutputStream(socket.getOutputStream());
            } catch (IOException e) {
                logger.info(e.getMessage());
            }
        }
        @Override
        public void run() {
            try {
                name = inputStream.readUTF();
                clients.put(name, socket);
                while(true) {
                    String message = inputStream.readUTF();
                    if(message.equals("@quit")) {
                        sendAll(name + " is quited");
                        clients.remove(name);
                        break;
                    }
                    else if(message.equals("@info")){
                        sendAll(usersInfo());
                    }
                    else if(message.contains("@sendUser")) {
                        String[] command = message.split(": ");
                        String userName = null;
                        String msg = command[1];
                        try (Scanner sc = new Scanner(command[0])){
                            while (sc.hasNext()){
                                if (sc.next().equals("@sendUser")){
                                    userName = sc.next();
                                }
                                else {
                                    sc.next();
                                }
                            }
                        }
                        sendPrivate(userName, msg);
                    }
                    else {
                        sendAll(name + ": " + message);
                        String info = name + message;
                        logger.info(info);
                    }
                }
            } catch (IOException e) {
                logger.info(e.getMessage());
            }
        }
        private void sendAll(String message) throws IOException{
            for(Map.Entry<String, Socket> entry : clients.entrySet()) {
                DataOutputStream out = new DataOutputStream(entry.getValue().getOutputStream());
                out.writeUTF(message);
                out.flush();
            }
        }
        private void sendPrivate(String userName, String message) {
            try {
                if (clients.containsKey(userName)) {
                    DataOutputStream out = new DataOutputStream(clients.get(userName).getOutputStream());
                    out.writeUTF(name + ": " + message);
                    out.flush();
                } else {
                    outputStream.writeUTF("There is no such user with that name: " + userName);
                }
            } catch (IOException e) {
                logger.info(e.getMessage());
            }
        }
        private String usersInfo (){
            StringBuilder sb = new StringBuilder();
            sb.append("Now online: ").append(clients.size()).append(" user(s)\n");
            for (Map.Entry<String, Socket> entry: clients.entrySet()) {
                sb.append("User: ");
                sb.append(entry.getKey()).append("\n");
            }
            return sb.toString();
        }
    }
}