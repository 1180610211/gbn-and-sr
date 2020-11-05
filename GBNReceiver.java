import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.Random;

public class GBNReceiver implements Runnable {
    private int recv_port;
    private InetSocketAddress sendSocketAddress;
    private String fileName;

    public GBNReceiver(int recv_port, String send_IP, int send_port, String fileName) {
        this.recv_port = recv_port;
        sendSocketAddress = new InetSocketAddress(send_IP, send_port);
        this.fileName = fileName;
    }

    public static int getUnsignedByte(byte data) {      //将data字节型数据转换为0~255 (0xFF 即BYTE)。
        return data & 0x0FF; // 部分编译器会把最高位当做符号位，因此写成0x0FF.
    }

    boolean lossInLossRatio(double lossRatio) {
        Random random = new Random();
        int lossBound = (int) (lossRatio * 100);
        int r = (random.nextInt(100) + 1) % 101;
        if (r <= lossBound) {
            return true;
        }
        return false;
    }

    @Override
    public void run() {
        int BUFFER_SIZE = 1025;
        int MTU = 1024;
        byte[] packetBuffer = new byte[BUFFER_SIZE];

        double packetLossRatio = 0.1;
        double ackLossRatio = 0.1;

        int SEQ_SIZE = 32;         //序列号的个数，从 0~19 共计 20 个
        int seq;     //接受到的包的序列号
        int expectedSeq = 0; //期望接受到的包的序列号
        int recvSeq = -1; //接收窗口大小为 1，已确认的序列号

        try {
            DatagramSocket clientSocket = new DatagramSocket(recv_port);
            DatagramPacket packet;
            System.out.println("[receiver]:开始测试GBN协议！");
            System.out.println("[receiver]:Ready for file transmission");
            FileOutputStream fos = new FileOutputStream("./" + fileName);

            while (true) {
                packet = new DatagramPacket(packetBuffer, BUFFER_SIZE);
                clientSocket.receive(packet);

                if (getUnsignedByte(packetBuffer[0]) == 255) {
                    System.out.println("[receiver]:finish a file transfer");
                    fos.flush();
                    fos.close();
                    break;
                }

                seq = getUnsignedByte(packetBuffer[0]);
                if (lossInLossRatio(packetLossRatio)) {
                    System.out.println("[receiver]:序号为" + seq + "的数据包丢失");
                    continue;
                }
                System.out.println("[receiver]:收到序号为" + seq + "的数据包");

                if (seq == expectedSeq) {
                    fos.write(packetBuffer, 1, packet.getLength() - 1);
                    expectedSeq++;
                    expectedSeq %= SEQ_SIZE;
                    recvSeq = seq;
                } else {
                    if (recvSeq == -1) continue;
                }
                packetBuffer[0] = (byte) recvSeq;
                packetBuffer[1] = '\0';
                if (lossInLossRatio(ackLossRatio)) {
                    System.out.println("[receiver]:序号为" + recvSeq + "的ACK确认丢失");
                    continue;
                }
                packet = new DatagramPacket(packetBuffer, 2, packet.getSocketAddress());
                clientSocket.send(packet);
                System.out.println("[receiver]:发送了一个ACK确认，序号为" + recvSeq);
            }

            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
