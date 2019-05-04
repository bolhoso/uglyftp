package com.bubbleftp;

import java.io.*;
import java.net.Socket;

import static com.bubbleftp.ConnectionManager.ResultCode.STATUS_NOT_IMPLEMENTED;

public class ConnectionManager {
    private Socket clientSocket;
    private PrintWriter controlWriter;
    private BufferedReader controlReader;

    private Socket dataConnectionSocket;
    private boolean dataConnectionSet;
    private DataOutputStream dataWriter;

    private String dataConnIp;
    private int dataConnPort;

    public ConnectionManager(Socket socket) throws IOException {
        clientSocket = socket;
        controlWriter = new PrintWriter(clientSocket.getOutputStream(), true);
        controlReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
    }

    public void setDataConnectionParams(String clientIp, int clientPort) {
        this.dataConnIp = clientIp;
        this.dataConnPort = clientPort;
        this.dataConnectionSet = true;
    }

    public void openDataConnection() throws IOException {
        if (!dataConnectionSet) {
            writeControl(STATUS_NOT_IMPLEMENTED, "Data connection params not set");
            return;
        }

        this.dataConnectionSocket = new Socket(dataConnIp.toString(), dataConnPort);
        if (dataConnectionSocket != null && dataConnectionSocket.isConnected()) {
            this.dataWriter = new DataOutputStream(dataConnectionSocket.getOutputStream());
            writeControl(ResultCode.STATUS_DATA_CONNECTION_OK, "BINARY data connection established.");
        } else {
            writeControl(STATUS_NOT_IMPLEMENTED, "Data connection not opened");
        }
    }

    public void closeDataConnection() throws IOException {
        dataWriter.flush();
        dataWriter.close();
        dataConnectionSocket.close();
    }

    public String readLine() throws IOException {
        return controlReader.readLine();
    }


    public void writeData(byte[] output) throws IOException {
        if (!dataConnectionSet) {
            throw new IllegalArgumentException("Data connection is not set");
        }

        dataWriter.write(output);
    }

    public void writeControl(ResultCode status, String message) {
        // TODO: validation

        controlWriter.write(new StringBuffer(status.toString().length() + 1 + message.length())
                .append(status.toString())
                .append(" ")
                .append(message)
                .append("\n")
                .toString());
        controlWriter.flush();
    }


    public enum ResultCode {
        STATUS_OK("220"),
        STATUS_FINISH("221"),
        STATUS_SYSTEM_TYPE("215"),
        STATUS_USER_OK("331"),
        STATUS_PASS_OK("230"),
        STATUS_PWD_OK("257"),
        STATUS_CWD_OK("250"),
        STATUS_LS_OK("226"),
        STATUS_PORT_OK("200"),
        STATUS_DATA_CONNECTION_OK("150"),
        STATUS_NOT_IMPLEMENTED("502");

        private String code;

        ResultCode(String code) {
            this.code = code;
        }

        public String toString() {
            return code;
        }
    }

}
