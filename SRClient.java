import java.io.FileInputStream;
import java.io.IOException;

public class SRClient {
    public static void main(String[] args) throws IOException {
        String IP = "127.0.0.1";
        int server_recv_port = 12340;
        int server_send_port = 12341;
        int client_recv_port = 12342;
        int client_send_port = 12343;

        SRReceiver srReceiver = new SRReceiver(client_recv_port, IP, server_send_port, "bigdata_copy.pdf");
        new Thread(srReceiver).start();

        FileInputStream fis = new FileInputStream("./lec26.pdf");
        SRSender srSender = new SRSender(client_send_port, IP, server_recv_port, fis.readAllBytes());
        new Thread(srSender).start();
    }
}
