import java.io.*;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

class Param {
    Param(String name, String value) {
        this.name = name;
        this.value = value;
    }

    String name;
    String value;
}

public class Processor implements Runnable {
    Processor(Socket client) throws IOException {
        this.client = client;
        currFile = "";
        Thread t = new Thread(this);
        t.start();
    }

    private Socket client;
    private String currFile;

    @Override
    public void run() {
        int code;
        byte[] content;
        FileInputStream fileReader = null;

        try {
            readRequest();
            File file = new File(Constants.ROOT_PATH + currFile);
            if (file.exists()) {
                if (!currFile.equals("favicon.ico") && !file.isDirectory()) {
                    fileReader = new FileInputStream(Constants.ROOT_PATH + currFile);

                    content = new byte[fileReader.available()];
                    fileReader.read(content);

                    String contentType = getType(currFile);
                    code = 200;
                    writeResponse(content, code, contentType);
                }
                else if (file.isDirectory()) {
                    String filesList = "";
                    for (File files : file.listFiles()) {
                        filesList += "<li>" + files.getName() + "</li>";
                    }
                    filesList = Constants.DIR_SHOW.replace("%FILES%", filesList);
                    filesList = filesList.replace("%DIR%", file.getName());

                    content = filesList.getBytes();
                    code = 200;
                    writeResponse(content, code, Constants.HTML);
                }
            }
            else if (!file.equals("favicon.ico")){
                code = 404;
                content = Constants.NOT_FOUND_RESPONSE.getBytes();
                writeResponse(content, code, Constants.TEXT);
            }
        }
        catch (IOException e) {
            code = 500;
            writeResponse(new byte[0], code, Constants.TEXT);
        }
        finally {
            try {
                fileReader.close();
            }
            catch (Exception e) {}
        }
    }

    private String getType(String file) {
        String extension = file.split("\\.")[1];
        switch (extension) {
            case "html":
                return Constants.HTML;
            case "pdf":
                return Constants.PDF;
            case "jpg":
            case "jpeg":
                return Constants.JPG;
            case "png":
                return Constants.PNG;
            case "gif":
                return Constants.GIF;
            case "mp4":
                return Constants.MP;
            default:
                return Constants.TEXT;
        }
    }

    String getFile(String requestLine) {
        int start = requestLine.indexOf("/") + 1;
        String result = "";
        int i = start;

        while (i < requestLine.length() && requestLine.codePointAt(i) != ' ' && requestLine.codePointAt(i) != '?') {
            result += (char)requestLine.codePointAt(i);
            i++;
        }

        return result;
    }

    ArrayList<Param> getParams(String requestLine) {
        ArrayList<Param> paramsList = new ArrayList<>();
        int start = requestLine.indexOf("?") + 1;

        if (start == 0)
            return paramsList;

        String name = "";
        String value = "";
        boolean isValue = false;

        for (int i = start; i < requestLine.length(); i++) {
            if (requestLine.codePointAt(i) == '=') {
                isValue=true;
            }
            else if ((requestLine.codePointAt(i) == '&' || requestLine.codePointAt(i) == ' ') && !value.equals("")) {
                Param param = new Param(name, value);
                paramsList.add(param);
                isValue = false;
                name = "";
                value = "";
            }
            else if (!isValue) {
                name += (char)requestLine.codePointAt(i);
            }
            else {
                value += (char)requestLine.codePointAt(i);
            }
        }

        if (!name.equals("") && !value.equals("")) {
            Param param = new Param(name, value);
            paramsList.add(param);
        }

        return paramsList;
    }

    private void doGet(String request) {
        String requestLine = request.split("\n")[0];
        currFile = getFile(requestLine);

        if (!currFile.equals("") && !currFile.equals("favicon.ico"))
            System.out.println("Get file " + currFile + "\n");
    }

    private void doParams(String request) {
        String requestLine = request.split("\n")[0];
        ArrayList<Param> params = getParams(requestLine);

        if (params.size() != 0) {
            System.out.println("Request params:\n");
            for (Param param : params) {
                System.out.println("Name: " + param.name);
                System.out.println("Value: " + param.value + "\n");
            }
        }
        else {
            System.out.println("No request params\n");
        }
    }

    private String getDate() {
        Date date = new Date();
        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        return formatter.format(date);
    }

    private void writeResponse(byte[] content, int code, String type) {
        String state = "";
        if (code == 200) {
            state = "200 OK";
        }
        else if (code == 404) {
            state = "404 Not Found";
        }
        else {
            state = "500 Internal Server Error";
        }
        String response = "HTTP/1.1 " + state + "\r\n" +
                "Server: MyServer/" + getDate() + "\r\n" +
                "Contet-Type: " + type + "\r\n" +
                "Content-Length: " + content.length + "\r\n" +
                "Connection: close\r\n\r\n";

        try {
            System.out.print(response);
            DataOutputStream writer = new DataOutputStream(client.getOutputStream());
            writer.writeUTF(response);
            writer.write(content);
            writer.flush();
        }
        catch (IOException e) {
            code = 500;
        }
        finally {
            System.out.println("Result code: " + code + "\n");
            System.out.println("--------------------------------------------------------\n");
        }
    }

    private void readRequest() {
        try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(client.getInputStream()));
            String request = "";
            String line = "";

            do {
                if (line != "")
                    request += line + "\n";
                line = bufferedReader.readLine();
            } while(line != null && line.trim().length() != 0);

            System.out.println(request);
            doGet(request);
            doParams(request);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}
