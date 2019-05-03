package com.bubbleftp;

import java.io.*;
import java.net.Socket;
import java.util.StringTokenizer;

public class ClientHandler implements Runnable {
    private static final int STATUS_OK = 220;
    private static final int STATUS_FINISH = 221;
    private static final int STATUS_NOT_IMPLEMENTED = 502;

    private static final String COMMAND_PARAM_DELIM = " ";
    private static final String COMMAND_QUIT = "QUIT";
    private static final String COMMAND_USER = "USER";
    private static final String COMMAND_PASS = "PASS";
    private static final String COMMAND_SYST = "SYST";
    private static final String COMMAND_PORT = "PORT";
    private static final String COMMAND_CWD = "CWD";
    private static final String COMMAND_PWD = "PWD";
    private static final String COMMAND_LIST = "LIST";
    private static final String COMMAND_TYPE = "TYPE";
    private static final String COMMAND_RETR = "RETR";
    
    private static final int STATUS_USER_OK = 331;
    private static final int STATUS_PASS_OK = 230;

    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;
    private boolean finished = false;

    private LoginHandler loginHandler;

    private File cwd;

    private Socket dataConnectionSocket;
    private boolean dataConnectionSet;
    private String dataConnIp;
    private int dataConnPort;

    public ClientHandler(Socket socket) throws IOException {
        clientSocket = socket;
        out = new PrintWriter(clientSocket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        cwd = new File(".");

        loginHandler = new LoginHandler();
    }

    public void sayHello() {
        write(STATUS_OK, "Bubble 0.1 ftp ready.");
    }

    private void write(int code, String output) {
        out.write(new StringBuffer(4 + output.length() + 1)
                .append(code)
                .append(" ")
                .append(output)
                .append("\n")
                .toString());
        out.flush();
    }

    private String[] readCommand() throws IOException {
        String command = in.readLine();
        System.out.println("DEBUG received " + command);

        if (command != null) {
            StringTokenizer tokenizer = new StringTokenizer(command, COMMAND_PARAM_DELIM);

            String[] commands = new String[tokenizer.countTokens()];
            for (int i = 0; i < commands.length; i++) {
                commands[i] = tokenizer.nextToken();
            }

            return commands;
        }

        return new String[]{""};
    }

    public void commandLoop() {
        while (!finished) {
            try {
                String[] command = readCommand();

                switch (command[0]) {
                    case "":
                    case COMMAND_QUIT:
                        write(STATUS_FINISH, "See ya!");
                        finished = true;
                        break;

                    case COMMAND_USER:
                        validateCommandWithParam(command);
                        handleUser(command[1]);
                        break;

                    case COMMAND_PASS:
                        validateCommandWithParam(command);
                        handleAuthentication(command[1]);
                        break;

                    case COMMAND_SYST:
                        write(215, "UNIX Type: L8");
                        break;


                    case COMMAND_CWD:
                        String newDirectory = command[1];
                        cwd = new File(newDirectory);
                        write(250, "OK");
                        break;

                    case COMMAND_PWD:
                        write(257, cwd.getCanonicalPath() + " is the current working directory.");
                        break;

                    case COMMAND_PORT:
                        configureDataConnection(command[1]);
                        break;

                    case COMMAND_LIST:
                        initiateDataConnection();
                        sendFileList();
                        break;
                    case COMMAND_TYPE:
                    case COMMAND_RETR:
                    default:
                        StringBuilder cmd = new StringBuilder();
                        for (String part : command) {
                            cmd.append(part).append(" ");
                        }
                        write(STATUS_NOT_IMPLEMENTED, "Unknown command: " + cmd.toString());
                        break;
                }

            } catch (IOException e) {
                write(STATUS_NOT_IMPLEMENTED, "Error processing command");

                e.printStackTrace();
                finished = true;
            }
        }
    }

    private void validateCommandWithParam(String[] command) {
        if (command == null || command.length < 2) {
            writeError("Command missing parameters");
        }
    }

    private void handleAuthentication(String pass) {
        if (loginHandler.authUser(pass)) {
            write(STATUS_PASS_OK, "Authentication successfull");
            return;
        }

        writeError("Cannot authenticate user " + loginHandler.getUser());
    }

    private void handleUser(String user) {
        if (user == null || user.isBlank()) {
            writeError("User cannot be empty");
        }
        loginHandler.setUser(user);

        write(STATUS_USER_OK, "Password please for " + user);

    }

    private void writeError(String errorMessage) {
        write(STATUS_NOT_IMPLEMENTED, errorMessage);
    }

    private void sendFileList() {
        if (dataConnectionSocket != null && dataConnectionSocket.isConnected()) {
            try {
                PrintWriter writer = new PrintWriter(this.dataConnectionSocket.getOutputStream());
                for (String currFile : cwd.list()) {
                    writer.write(currFile);
                    writer.write(0x0d);
                    writer.write(0x0a);
                }
                writer.flush();
                dataConnectionSocket.close();
                write(226, "Directory list has been submitted.");
            } catch (IOException e) {
                write(STATUS_NOT_IMPLEMENTED, "Error creating data connection with client DTP");
                e.printStackTrace();
            }

        } else {
            write(STATUS_NOT_IMPLEMENTED, "Data connection not opened");
        }
    }

    private void initiateDataConnection() {
        if (!dataConnectionSet) {
            write(STATUS_NOT_IMPLEMENTED, "Data connection params not set");
            return;
        }

        try {
            this.dataConnectionSocket = new Socket(dataConnIp.toString(), dataConnPort);
            write(150, "BINARY data connection established.");

        } catch (IOException e) {
            write(STATUS_NOT_IMPLEMENTED, "Error establishing data connection with client DTP");

            e.printStackTrace();
        }

    }

    private void configureDataConnection(String userDataPort) {
        StringTokenizer ipPortTokenizer = new StringTokenizer(userDataPort, ",");

        if (ipPortTokenizer.countTokens() != 6) {
            write(STATUS_NOT_IMPLEMENTED, "Uknown address format: " + userDataPort);
            return;
        }

        StringBuilder ip = new StringBuilder(15);
        for (int i = 0; i < 4; i++) {
            ip.append(ipPortTokenizer.nextToken());
            if (i < 3) {
                ip.append(".");
            }
        }

        int port = 0;
        port = Integer.parseInt(ipPortTokenizer.nextToken()) << 8;
        port |= Integer.parseInt(ipPortTokenizer.nextToken());

        this.dataConnectionSet = true;
        this.dataConnIp = ip.toString();
        this.dataConnPort = port;
        write(200, "PORT " + userDataPort);
    }

    @Override
    public void run() {
        sayHello();
        commandLoop();
    }
}