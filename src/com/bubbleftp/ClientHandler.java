package com.bubbleftp;

import com.bubbleftp.exception.CommandMissingParamsException;

import java.io.*;
import java.net.Socket;
import java.nio.file.NoSuchFileException;
import java.util.StringTokenizer;

import static com.bubbleftp.ConnectionManager.ResultCode.*;

import com.bubbleftp.ConnectionManager.ResultCode;

public class ClientHandler implements Runnable {


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

    private ConnectionManager connMannager;
    private boolean finished = false;

    private LoginHandler loginHandler;
    private FileManager fileManager;

    public ClientHandler(Socket socket) throws IOException {
        connMannager = new ConnectionManager(socket);
        loginHandler = new LoginHandler();
        fileManager = new FileManager();
    }

    private void sayHello() {
        write(STATUS_OK, "Bubble FTP 0.1 ready.");
    }

    private void write(ResultCode code, String output) {
        connMannager.writeControl(code, output);
    }

    private String[] readCommand() throws IOException {
        String command = connMannager.readLine();
        System.out.println("DEBUG received " + command); // TODO use log4j

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

    private void commandLoop() {
        sayHello();

        while (!finished) {
            try {
                String[] command = readCommand();

                switch (command[0]) {
                    case "":
                    case COMMAND_QUIT:
                        write(ConnectionManager.ResultCode.STATUS_FINISH, "See ya!");
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
                        write(ResultCode.STATUS_SYSTEM_TYPE, "UNIX Type: L8");
                        break;


                    case COMMAND_CWD:
                        validateCommandWithParam(command);
                        handleCwd(command[1]);
                        break;

                    case COMMAND_PWD:
                        handlePwd();
                        break;

                    case COMMAND_PORT:
                        configureDataConnection(command[1]);
                        break;

                    case COMMAND_LIST:
                        handlListFiles();
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

            } catch (CommandMissingParamsException e) {
                write(STATUS_NOT_IMPLEMENTED, "Command missing parameters");
            } catch (IOException e) {
                write(STATUS_NOT_IMPLEMENTED, "Fatal error, aborting connection");

                e.printStackTrace();
                finished = true;
            }
        }

        cleanupResources();
    }

    private void cleanupResources() {
        // tODO: stub
    }

    private void handlePwd() {
        try {
            write(STATUS_PWD_OK, fileManager.getPwd() + " is the current working directory.");
        } catch (IOException e) {
            writeError("Error retrieving current diretory");
            e.printStackTrace();
        }
    }

    private void handleCwd(String newDirectory) {
        try {
            String directory = fileManager.switchDirectory(newDirectory);
            write(STATUS_CWD_OK, "OK. New directory is " + directory);
        } catch (NoSuchFileException e) {
            writeError("Unknown directory " + e.getFile());
        } catch (IOException e) {
            writeError("Error retrieving current diretory");
            e.printStackTrace();
        }
    }

    private void validateCommandWithParam(String[] commands) throws CommandMissingParamsException {
        if (commands == null || commands.length < 2) {
            throw new CommandMissingParamsException();
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

    // TODO: Where's the right place for this?
    private void handlListFiles() {
        try {
            connMannager.openDataConnection();

            for (String currFile : fileManager.listFiles()) {
                connMannager.writeData(currFile.getBytes());
                connMannager.writeData(new byte[] {0x0D, 0x0A});
            }

            connMannager.closeDataConnection();
            connMannager.writeControl(STATUS_LS_OK, "Directory list has been submitted.");

        } catch (IOException e) {
            write(STATUS_NOT_IMPLEMENTED, "Error creating data connection with client DTP");
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

        this.connMannager.setDataConnectionParams(ip.toString(), port);
        write(STATUS_PORT_OK, "PORT " + userDataPort);
    }

    @Override
    public void run() {
        commandLoop();
    }
}