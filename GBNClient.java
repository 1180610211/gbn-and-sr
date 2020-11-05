import java.io.FileInputStream;
import java.io.IOException;

public class GBNClient {
    public static void main(String[] args) throws IOException {
        String IP="127.0.0.1";
        int server_recv_port=12340;
        int server_send_port=12341;
        int client_recv_port=12342;
        int client_send_port=12343;
//        GBNReceiver gbnReceiver=new GBNReceiver(client_recv_port,IP,server_send_port,"test1_copy.txt");
//        new Thread(gbnReceiver).start();

        FileInputStream fis=new FileInputStream("./lec26.pdf");
        GBNSender gbnSender=new GBNSender(client_send_port,IP,server_recv_port,fis.readAllBytes());
        new Thread(gbnSender).start();
    }
}
