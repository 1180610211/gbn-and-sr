import java.io.IOException;
import java.net.*;
import java.util.LinkedList;
import java.util.List;

public class GBNSender implements Runnable {
    private int send_port;
    private InetSocketAddress recvSocketAddress;
    private byte[] data;

    public GBNSender(int send_port, String recv_IP, int recv_port, byte[] data) {
        this.send_port = send_port;
        recvSocketAddress = new InetSocketAddress(recv_IP, recv_port);
        this.data = data;
    }

    public int getUnsignedByte(byte data) {      //将data字节型数据转换为0~255 (0xFF 即BYTE)。
        return data & 0x0FF; // 部分编译器会把最高位当做符号位，因此写成0x0FF.
    }

    @Override
    public void run() {
        int BUFFER_SIZE = 1025;
        int MTU = 1024;
        byte[] packetBuffer = new byte[BUFFER_SIZE];

        int SEQ_SIZE = 32;         //序列号的个数，从 0~19 共计 20 个
        int SEND_WIND_SIZE = 10;   //发送窗口大小为 10，GBN 中应满足 W + 1 <= N,（W 为发送窗口大小，N 为序列号个数）
        List<DatagramPacket> sendBuffer = new LinkedList<>();   //发送窗口缓存

        int sendBase = 0;
        int nextSeqNum = 0;
        int sendPacket = 0;  //已发送数据包数量
        int ackPacket = 0;   //已确认数据包数量
        int totalPacket = (int) Math.ceil(data.length / (double) MTU);

        try {
            DatagramSocket sendSocket = new DatagramSocket(send_port);
            DatagramPacket packet;
            System.out.println("[sender]:开始测试GBN协议！");
            System.out.println("[sender]:Begin a file transfer !");
            sendSocket.setSoTimeout(1000);

            while (true) {
                int step = nextSeqNum - sendBase;
                step = step >= 0 ? step : step + SEQ_SIZE;
                while (step < SEND_WIND_SIZE && sendPacket < totalPacket) {
                    packetBuffer[0] = (byte) (nextSeqNum);
                    int length = Math.min(data.length - sendPacket * MTU, MTU);
                    System.arraycopy(data, sendPacket * MTU, packetBuffer, 1, length);
                    packet = new DatagramPacket(packetBuffer, length + 1, recvSocketAddress);
                    sendSocket.send(packet);

                    byte[] newPacketBuffer = new byte[BUFFER_SIZE];
                    System.arraycopy(packetBuffer, 0, newPacketBuffer, 0, BUFFER_SIZE);
                    sendBuffer.add(new DatagramPacket(newPacketBuffer, packet.getLength(), packet.getSocketAddress()));
                    System.out.println("[sender]:发送了一个数据包，序号为" + nextSeqNum);
                    nextSeqNum++;
                    nextSeqNum %= SEQ_SIZE;
                    sendPacket++;
                    step = nextSeqNum - sendBase;
                    step = step >= 0 ? step : step + SEQ_SIZE;
                }

                try {
                    packet = new DatagramPacket(packetBuffer, BUFFER_SIZE);
                    sendSocket.receive(packet);
                    int index = getUnsignedByte(packetBuffer[0]);
                    System.out.println("[sender]:收到序号为" + index + "的ACK确认");
//                    if (sendBase <= index) {
//                        for (int i = sendBase; i <= index; i++) {
//                            sendBuffer.remove(0);
//                            ackPacket++;
//                        }
//                    } else if (sendBase + SEND_WIND_SIZE > SEQ_SIZE && index < (sendBase + SEND_WIND_SIZE) % SEQ_SIZE) {
//                        //ack 超过了最大值，回到了 curAck 的左边
//                        for (int i = sendBase; i < SEQ_SIZE; ++i) {
//                            sendBuffer.remove(0);
//                            ackPacket++;
//                        }
//                        for (int i = 0; i <= index; ++i) {
//                            sendBuffer.remove(0);
//                            ackPacket++;
//                        }
//                    }
                    int tmp = (index + 1) % SEQ_SIZE;
                    if (tmp != sendBase) {
                        int cnt = (tmp - sendBase) > 0 ? tmp - sendBase : tmp - sendBase + SEQ_SIZE;
                        ackPacket += cnt;
                        for (int i = 0; i < cnt; i++) {
                            sendBuffer.remove(0);
                        }
                        sendBase = tmp;
                    }

                } catch (SocketTimeoutException e) {
                    System.out.println("[sender]:Timeout error");
                    int index;
                    for (int i = 0; i < sendBuffer.size(); i++) {
                        index = (i + sendBase) % SEQ_SIZE;
                        System.out.println("[sender]:重传序号为" + index + "的数据包");
                        sendSocket.send(sendBuffer.get(i));
                    }
                }

                if (ackPacket == totalPacket) {
                    packetBuffer[0] = (byte) 255;
                    packetBuffer[1] = '\0';
                    packet = new DatagramPacket(packetBuffer, 2, recvSocketAddress);
                    sendSocket.send(packet);
                    System.out.println("[sender]:finish a file transfer");
                    break;
                }
            }

            sendSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
