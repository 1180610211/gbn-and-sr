import java.io.FileInputStream;
import java.io.IOException;

public class GBNServer {
    public static void main(String[] args) throws IOException {
        String IP="127.0.0.1";
        int server_recv_port=12340;
        int server_send_port=12341;
        int client_recv_port=12342;
        int client_send_port=12343;
//        FileInputStream fis=new FileInputStream("./test1.txt");
//        GBNSender gbnSender=new GBNSender(server_send_port,IP,client_recv_port,fis.readAllBytes());
//        new Thread(gbnSender).start();

        GBNReceiver gbnReceiver=new GBNReceiver(server_recv_port,IP,client_send_port,"lec26_copy.pdf");
        new Thread(gbnReceiver).start();
    }
}
