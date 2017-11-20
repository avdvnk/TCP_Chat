package client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Scanner;
import java.util.logging.Logger;

public class Client {
    private static Logger logger = Logger.getLogger(Client.class.getName());
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        logger.info("Write port to connect:");
        int port = Integer.parseInt(scanner.nextLine());
        logger.info("Write your name:");
        String name = scanner.nextLine();
        logger.info("You connected to server! Now you can type and send message!\n" +
                    "@quit for close chat\n" + "@info for take list about online users\n" +
                    "@sendUser <name>: for send private message");
        try(Socket socket = new Socket(InetAddress.getByName("localhost"), port)) {
            DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
            DataInputStream inputStream = new DataInputStream(socket.getInputStream());
            outputStream.writeUTF(name);
            outputStream.flush();
            Thread listener = new Thread(new ServerListener(inputStream, socket));
            listener.start();
            while (true) {
                String message = scanner.nextLine();
                outputStream.writeUTF(message);
                outputStream.flush();
                if (message.equals("@quit")) {
                    socket.close();
                    break;
                }
            }
        } catch (IOException e) {
            logger.info(e.getMessage());
        }
    }
    private static class ServerListener implements Runnable {
        DataInputStream inputStream;
        Socket socket;
        ServerListener(DataInputStream inputStream, Socket socket) {
            this.inputStream = inputStream;
            this.socket = socket;
        }
        public void run() {
            try {
                while(!(socket.isClosed())) {
                    String message = inputStream.readUTF();
                    logger.info(message);
                }
            } catch (IOException e) {
                try {
                    socket.close();
                } catch (IOException e1) {
                    logger.info(e1.getMessage());
                }
            }
        }
    }
}
